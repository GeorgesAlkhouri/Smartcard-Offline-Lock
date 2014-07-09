package eosApplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

public class EOSApplet extends Applet {
	
	// Wrapper APDU
	private final static byte OFFSET_ENC_COM_DATA = 1;
	private final static byte OFFSET_COM_COMMAND_NONCE = 0;
	private final static byte OFFSET_COM_RESPONSE_NONCE = 2;
	private final static byte OFFSET_COM_APDU_DATA = 4;
	private final static byte OFFSET_RES_RESPONSE_NONCE = 0;
	private final static byte OFFSET_RES_APDU_DATA = 2;
	
	// CLA byte in the command APDU header
	private final static byte CLA_BYTE = (byte) 0xC0;
	
	// INS bytes in the command APDU header
	private final static byte INS_GET_COMMAND_NONCE = (byte)0x01;

	private final static byte INS_SHOULD_OPEN = (byte)0x10;
	private final static byte INS_SHOULD_OPEN_GLOBAL = (byte)0x20;
	private final static byte INS_GET_GLOBAL_ACCESS = (byte)0x22;
	private final static byte INS_SET_GLOBAL_ACCESS = (byte)0x21;
	
	private final static byte INS_PUT_ACCESS_ITEM = (byte)0x30;
	private final static byte INS_REMOVE_ACCESS_ITEM = (byte)0x40;
	private final static byte INS_REMOVE_ALL_ACCESS_ITEMS = (byte)0x41;
	
	private final static byte INS_GET_WEEKDAYS = (byte)0x50;
	private final static byte INS_GET_ACCESS_ITEM_AT_POS = (byte)0x51;
	private final static byte INS_GET_MAX_SIZE = (byte)0x60;
	
	// boolean bytes
	private final static byte TRUE_BYTE = (byte)0x01;
	private final static byte FALSE_BYTE = (byte)0x00;
	
	// entries constants
	private final static byte MAX_SIZE = 10; // entries possible
	private final static byte KEY_BYTE_SIZE = 8; // identity
	private final static byte VALUE_BYTE_SIZE = 1; // weekday bitmask
	private final static byte ENTRY_BYTE_SIZE = (byte) (KEY_BYTE_SIZE + VALUE_BYTE_SIZE);
	private final static short MAX_BYTE_SIZE = (short) (ENTRY_BYTE_SIZE * MAX_SIZE);

    private byte[] entries; // array in EEPROM
    private byte globalAccessItem; // byte in EEPROM
    
    private final static byte[] EMPTY_ENTRY = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    
    // admin identity token: meister1 in hex: 6d 65 69 73 74 65 72 31
    private final static byte[] ADMIN_IDENTITY = new byte[] {
    	(byte)0x6d, (byte)0x65, (byte)0x69, (byte)0x73, (byte)0x74, (byte)0x65, (byte)0x72, (byte)0x31
    };

    // for nonce handling
    private static final byte NONCE_BYTE_SIZE = (byte)0x02;
    private boolean commandNonceIsValid = false;
    private short commandNonce;
    private RandomData randomData;
    private short responseNonce = 0x0000;
    
    // for enryption and decryption handling
    private byte[] cipherTemp;
    private static final short BLOCK_SIZE = 0x0040;
	
    // phrase: sosecure
    private byte[] PHRASE = new byte[] { 
    	(byte)0x73, (byte)0x6f, (byte)0x73, (byte)0x65, (byte)0x63, (byte)0x75, (byte)0x72, (byte)0x65
    };
    private DESKey key;
    private Cipher cipher;
    

	// applet constructor
	private EOSApplet() {
		entries = new byte[MAX_BYTE_SIZE];
		globalAccessItem = (byte)0x00;
		randomData = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		
		// generating DES Key and cipher
		key = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);
		key.setKey(PHRASE, (short) 0);
		cipher = Cipher.getInstance(Cipher.ALG_DES_CBC_NOPAD, false);
		cipherTemp = JCSystem.makeTransientByteArray(BLOCK_SIZE, JCSystem.CLEAR_ON_DESELECT);
		
		register();
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		// new EOSApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		
		new EOSApplet();
	}
	
	public boolean select() {
		
		// returns true to JCRE to indicate that the applet
		// is ready to accept incoming APDUs

		// set nonce to invalid state
		commandNonceIsValid = false;

		return true;
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}
		
		decryptCommandAPDU(apdu);
		
		byte[] buffer = apdu.getBuffer();
		// verify that the applet can accept this APDU message
		if (buffer[ISO7816.OFFSET_CLA] != CLA_BYTE) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		short outgoingLength = 0;
		switch (buffer[ISO7816.OFFSET_INS]) {
		case INS_GET_COMMAND_NONCE: outgoingLength = getCommandNonce(apdu); break;
		case INS_SHOULD_OPEN: outgoingLength = shouldOpen(apdu); break;
		case INS_PUT_ACCESS_ITEM: putAccessItem(apdu); break;
		case INS_REMOVE_ACCESS_ITEM: removeAccessItem(apdu); break;
		case INS_GET_WEEKDAYS: outgoingLength = getWeekdays(apdu); break;
		case INS_SHOULD_OPEN_GLOBAL: outgoingLength = shouldOpenGlobal(apdu); break;
		case INS_GET_GLOBAL_ACCESS: outgoingLength = getGlobalAccess(apdu); break;
		case INS_SET_GLOBAL_ACCESS: setGlobalAccess(apdu); break;
		case INS_REMOVE_ALL_ACCESS_ITEMS: removeAllAccessItems(apdu); break;
		case INS_GET_ACCESS_ITEM_AT_POS: outgoingLength = getAccessItemAtPos(apdu); break;
		case INS_GET_MAX_SIZE: outgoingLength = getMaxSize(apdu); break;		
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		encryptResponseAPDUAndSend(apdu, outgoingLength);
	}


	// get command nonce for sending with next apdu
	
	// Command-APDU: C0 01 00 00 02
	// Example: C001000002

	// Response-APDUs: 
	// No error: <2 bytes command nonce> <2 bytes SW_NO_ERROR>   Example: 58A09000

	private short getCommandNonce(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		randomData.generateData(buffer, (short) 0, NONCE_BYTE_SIZE);
		commandNonce = Util.makeShort(buffer[0], buffer[1]);
		commandNonceIsValid = true;
		return (short) NONCE_BYTE_SIZE;
		//apdu.setOutgoingAndSend((short) 0, (short) NONCE_BYTE_SIZE);
	}

	
	// ask if identity is granted access on this specific weekday
	// no admin identity required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	// Current Weekday: number of weekday between 01 (Monday) .. 07 (Sunday)
	
	// Command-APDU: C0 10 <1 byte current weekday> 00 08 <8 bytes identity token> 01
	// Example: C010060008abcd1234abcd567801
	
	// Response-APDUs: 
	// No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private short shouldOpen(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != KEY_BYTE_SIZE) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
    	short pos = findPosition(
    			buffer, ISO7816.OFFSET_CDATA, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// entry does not exist
    		buffer[0] = FALSE_BYTE;
    	} else {
        	// valid position, so compare weekday bitmask with current weekday
    		byte currentWeekday = buffer[ISO7816.OFFSET_P1];
    		// test for valid current weekday
    		if (currentWeekday < 1 || currentWeekday > 7) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
    		// create a current weekday mask by shifting msb
    		byte currentWeekdayMask = (byte) ((short)0x80 >>> (currentWeekday - 1));
    		byte weekdayBitmask = entries[(pos + KEY_BYTE_SIZE)];
    		// Example:
    		// currentWeekdayMask: 0001000
    		// weekdayBitmask: 1101001
    		// currentWeekday & weekdayBitmask: 0001000 != 0
    		buffer[0] = ((currentWeekdayMask & weekdayBitmask) != 0) ? TRUE_BYTE : FALSE_BYTE;
    	}
    	return (short) 1;
    	//apdu.setOutgoingAndSend((short) 0, (short) 1);
	}

	
	// ask if every identity is granted access on this specific weekday
	// no admin identity required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	// Current Weekday: number of weekday between 01 (Monday) .. 07 (Sunday)
	
	// Command-APDU: C0 20 <1 byte current weekday> 00
	// Example: C0200600
	
	// Response-APDUs: 
	// No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
	// Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private short shouldOpenGlobal(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
		byte currentWeekday = buffer[ISO7816.OFFSET_P1];
		// test for valid current weekday
		if (currentWeekday < 1 || currentWeekday > 7) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		// create a current weekday mask by shifting msb
		byte currentWeekdayMask = (byte) ((short)0x80 >>> (currentWeekday - 1));
		byte weekdayBitmask = globalAccessItem;
		// Example:
		// currentWeekdayMask: 0001000
		// weekdayBitmask: 1101001
		// currentWeekday & weekdayBitmask: 0001000 != 0
		buffer[0] = ((currentWeekdayMask & weekdayBitmask) != 0) ? TRUE_BYTE : FALSE_BYTE;
		return (short) 1;
    	// apdu.setOutgoingAndSend((short) 0, (short) 1);
	}
	
	
	// get global access
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 22 00 00 08 <8 bytes admin token> 01
	// Example: C0220000086d6569737465723101
	
	// Response-APDUs:
	// No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private short getGlobalAccess(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);

		buffer[0] = globalAccessItem;
		return VALUE_BYTE_SIZE;
		// apdu.setOutgoingAndSend((short) 0, VALUE_BYTE_SIZE);
	}
	

	// set global access
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 21 <1 byte weekday bitmask> 00 08 <8 bytes admin token>
	// Example: C021f800086d65697374657231
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void setGlobalAccess(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);

		globalAccessItem = buffer[ISO7816.OFFSET_P1];
	}

	
	// create or update (put) access item
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 30 00 00 11 <8 bytes admin token> <8 bytes identity token> <1 byte weekday bitmask>
	// Example: C0300000116d65697374657231abcd1234abcd567806
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// If full: <2 bytes SW_UNKNOWN>   Bytes: 6f00
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void putAccessItem(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != (ADMIN_IDENTITY.length + ENTRY_BYTE_SIZE)) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		short entryOff = (short) (ISO7816.OFFSET_CDATA + ADMIN_IDENTITY.length);
    	short pos = findPosition(
    			buffer, entryOff, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// entry does not exist
    		// find free position
    		pos = findPosition(
    				EMPTY_ENTRY, (short) 0, KEY_BYTE_SIZE,
    				ENTRY_BYTE_SIZE,
    				entries, (short) 0, MAX_BYTE_SIZE);
    		if (pos == entries.length) {
    			// no free entry found
    			ISOException.throwIt(ISO7816.SW_UNKNOWN);
    		}
    	}
    	// valid position, so copy entry from buffer into entries
    	Util.arrayCopy(buffer, entryOff, entries, pos, ENTRY_BYTE_SIZE);
	}
	

	// remove access item for specific identity
	// admin identitiy required
	
	// Command-APDU: C0 40 00 00 10 <8 bytes admin token> <8 bytes identity token>
	// Example: C0400000106d65697374657231abcd1234abcd5678
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void removeAccessItem(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != (ADMIN_IDENTITY.length + KEY_BYTE_SIZE)) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		short keyOff = (short) (ISO7816.OFFSET_CDATA + ADMIN_IDENTITY.length);
    	short pos = findPosition(
    			buffer, keyOff, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// entry does not exist
    		ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
    	} else {
        	// valid position, so remove entry by overriding with empty entry
        	Util.arrayCopy(EMPTY_ENTRY, (short) 0, entries, pos, ENTRY_BYTE_SIZE);
    	}
	}
	

	// remove all access items by overriding with 0
	// admin identitiy required

	// Command-APDU: C0 41 00 00 08 <8 bytes admin token>
	// Example: C0410000086d65697374657231

	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void removeAllAccessItems(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		// overriding with 0s by using arrayCopy
		for (short pos = 0; pos < entries.length; pos = (short) (pos + ENTRY_BYTE_SIZE)) {
			Util.arrayCopy(EMPTY_ENTRY, (short) 0, entries, pos, ENTRY_BYTE_SIZE);
		}
	}
	

	// get weekday bitmask for specific identity
	// admin identitiy required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)

	// Command-APDU: C0 50 00 00 10 <8 bytes admin token> <8 bytes identity token> 01
	// Example: C0500000106d65697374657231abcd1234abcd567801
	
	// Response-APDUs:
	// No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: 069000
	// Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private short getWeekdays(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != (ADMIN_IDENTITY.length + KEY_BYTE_SIZE)) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		short keyOff = (short) (ISO7816.OFFSET_CDATA + ADMIN_IDENTITY.length);
    	short pos = findPosition(
    			buffer, keyOff, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// entry does not exist
    		ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
    	}
    	// valid position, so return weekday bitmask
		buffer[0] = entries[(pos + KEY_BYTE_SIZE)];
		return VALUE_BYTE_SIZE;
    	// apdu.setOutgoingAndSend((short) 0, VALUE_BYTE_SIZE);
	}
	
	
	// get access item at pos in entries buffer
	// admin identitiy required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)

	// Command-APDU: C0 51 <2 bytes position (start 0)> 08 <8 bytes admin token> 09
	// Example: C0510003086d6569737465723109
	
	// Response-APDUs:
	// No error: <8 bytes identity token> <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: abcd1234abcd5678049000
	// Not found or empty: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private short getAccessItemAtPos(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		short pos = (short) (Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]) * ENTRY_BYTE_SIZE);
		if (pos + ENTRY_BYTE_SIZE > entries.length) {
			ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
		}
		short lessEqualGreater = Util.arrayCompare(EMPTY_ENTRY, (short) 0, entries, pos, ENTRY_BYTE_SIZE);
		if (lessEqualGreater == 0) {
			// entry at this pos is empty
			ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
		}
		// copy non atomic, because we are reading from entries in this case
		Util.arrayCopyNonAtomic(entries, pos, buffer, (short) 0, ENTRY_BYTE_SIZE);
		return ENTRY_BYTE_SIZE;
    	// apdu.setOutgoingAndSend((short) 0, ENTRY_BYTE_SIZE);
	}
	
	
	// get max size of entries buffer
	// admin identitiy required
	
	// Command-APDU: C0 60 00 00 08 <8 bytes admin token> 02
	// Example: C0600000086d6569737465723102
	
	// Response-APDUs:
	// No error: <2 bytes max size count> <2 bytes SW_NO_ERROR>   Example: 00049000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private short getMaxSize(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		Util.setShort(buffer, (short) 0, (short) MAX_SIZE);
		// sending short value: 2 bytes
		return (short) 2;
		// apdu.setOutgoingAndSend((short) 0, (short) 2);
	}
	

	// decrypt and validate byte array
	
	private void decryptCommandAPDU(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
		// buffer: <wrapper cla byte><enrypt(<Command-Nonce><Response-Nonce><Command-APDU>)>

		// omitting wrapper cla byte while copying to cipherTemp
		Util.arrayCopyNonAtomic(buffer, OFFSET_ENC_COM_DATA, cipherTemp, (short) 0, BLOCK_SIZE);
		
		// decrypt
		cipher.init(key, Cipher.MODE_DECRYPT);
		cipher.doFinal(cipherTemp, (short) 0, BLOCK_SIZE, buffer, (short) 0);
		// clear cipherTemp
		Util.arrayFillNonAtomic(cipherTemp, (short) 0, BLOCK_SIZE, (byte) 0);

		// buffer: <Command-Nonce><Response-Nonce><Command-APDU>		
		
		// command nonce
		short receivedCommandNonce = Util.getShort(buffer, OFFSET_COM_COMMAND_NONCE);
		// response nonce
		responseNonce = Util.getShort(buffer, OFFSET_COM_RESPONSE_NONCE);
		
		// tuncate nonces
		Util.arrayCopyNonAtomic(buffer, OFFSET_COM_APDU_DATA, buffer, (short) 0, (short) (BLOCK_SIZE - OFFSET_COM_APDU_DATA));

		byte insByte = buffer[ISO7816.OFFSET_INS];
		// no valid nonce is required when getting one
		if ((insByte != INS_GET_COMMAND_NONCE) && 
			(!commandNonceIsValid || (receivedCommandNonce != commandNonce))) {
			commandNonceIsValid = false;
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}
		commandNonceIsValid = false;
	}
	
	
	// encrypt to byte array and send
	
	private void encryptResponseAPDUAndSend(APDU apdu, short outgoingLength) {
		byte[] buffer = apdu.getBuffer();
		
		// buffer: <Response-APDU>
		
		// adding response nonce and length at the top
		Util.arrayCopyNonAtomic(buffer, (short) 0, buffer, OFFSET_RES_APDU_DATA, (short) outgoingLength);
		Util.setShort(buffer, (short) OFFSET_RES_RESPONSE_NONCE, (short) responseNonce);
		
		// buffer: <Response-Nonce><Response-APDU>

		// encrypt
		short bufferLength = (short) (OFFSET_RES_APDU_DATA + outgoingLength);
		Util.arrayCopyNonAtomic(buffer, (short) 0, cipherTemp, (short) 0, bufferLength);
		cipher.init(key, Cipher.MODE_ENCRYPT);
		short cipOutLength = cipher.doFinal(cipherTemp, (short) 0, BLOCK_SIZE, buffer, (short) 0);
		// clear cipherTemp
		Util.arrayFillNonAtomic(cipherTemp, (short) 0, BLOCK_SIZE, (byte) 0);

		// buffer: <enrypt(<Response-Nonce><Response-APDU>)>
		
		apdu.setOutgoingAndSend((short) 0, (short) cipOutLength);
	}
    

    
    // check if given identity is admin identity using arrayCompare
    private boolean isAdminIdentity(byte[] idArr, short idOff) {
    	return (Util.arrayCompare(
    		idArr, idOff, 
    		ADMIN_IDENTITY, (short) 0, 
    		(short) ADMIN_IDENTITY.length) == 0) ? true : false;
    }
    
    //  if not found then position == (srcOff + srcLen)
    // patArr ... the array holding the pattern to search for
    // patOff ... the offset the pattern starts from
    // patLen ... the length of the pattern
    // entryLen ... the length of an entry in source array
    // srcArr ... the array to search in
    // srcOff ... the offset at which to start searching
    // srcLen ... the length until search should take place
    public short findPosition(
    		byte[] patArr, short patOff, short patLen, 
    		short entryLen, 
    		byte[] srcArr, short srcOff, short srcLen) {
    	
    	// until now the entry was not found so pos should be a non valid position in srcArr
    	short pos = (short) (srcOff + srcLen);
    	// tempOff is the moving temporary offset for comparison
    	short tempOff = srcOff;
    	// init with -2 to mark this variable as not set with valid value (valid values are -1, 0, 1)
    	byte lessEqualGreater = -2;
    	// repeat if not found and still entries to look at
    	while ((lessEqualGreater != 0) && (tempOff + patLen < srcOff + srcLen )) {
    		lessEqualGreater = Util.arrayCompare(patArr, patOff, srcArr, tempOff, patLen);
    		if (lessEqualGreater != 0) {
    			// not found, increase temporary offset to next entry
    			tempOff = (short) (tempOff + entryLen);
    		} else {
    			// found, left most position is temporary offset
    			pos = tempOff;
    		}
    	}
    	return pos;
	}
}
