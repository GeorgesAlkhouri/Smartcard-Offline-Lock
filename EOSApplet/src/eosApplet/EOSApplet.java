package eosApplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.RandomData;

public class EOSApplet extends Applet {
	
	// CLA byte in the command APDU header
	private final static byte CORE_CLA = (byte) 0xC0;
	
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
    private static final byte NONCE_LENGTH = (byte)0x02;
    private boolean commandNonceIsValid = false;
    private short commandNonce;
    private RandomData randomData;
    
    private byte[] originalAPDU;
	

	// applet constructor
	private EOSApplet() {
		entries = new byte[MAX_BYTE_SIZE];
		globalAccessItem = (byte)0x00;
		randomData = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		originalAPDU = JCSystem.makeTransientByteArray((short) 200, JCSystem.CLEAR_ON_DESELECT);

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

		byte[] buffer = originalAPDU;
		
		// verify that the applet can accept this APDU message
		if (buffer[ISO7816.OFFSET_CLA] != CORE_CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		switch (buffer[ISO7816.OFFSET_INS]) {
		case INS_GET_COMMAND_NONCE: getCommandNonce(apdu); return;
		case INS_SHOULD_OPEN: shouldOpen(apdu); return;
		case INS_PUT_ACCESS_ITEM: putAccessItem(apdu); return;
		case INS_REMOVE_ACCESS_ITEM: removeAccessItem(apdu); return;
		case INS_GET_WEEKDAYS: getWeekdays(apdu); return;
		case INS_SHOULD_OPEN_GLOBAL: shouldOpenGlobal(apdu); return;
		case INS_GET_GLOBAL_ACCESS: getGlobalAccessItem(apdu); return;
		case INS_SET_GLOBAL_ACCESS: setGlobalAccessItem(apdu); return;
		case INS_REMOVE_ALL_ACCESS_ITEMS: removeAllAccessItems(apdu); return;
		case INS_GET_ACCESS_ITEM_AT_POS: getAccessItemAtPos(apdu); return;
		case INS_GET_MAX_SIZE: getMaxSize(apdu); return;
		
		case (byte)0x90: debugGetAll(apdu); return;
		
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	
	// TODO only for debugging C090000084
	private void debugGetAll(APDU apdu) {
		byte[] buffer = originalAPDU;
		short le = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (le != MAX_BYTE_SIZE) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		apdu.setOutgoing();                           // set transmission to outgoing data
		apdu.setOutgoingLength((short) le);            // set the number of bytes to send to the IFD
		apdu.sendBytesLong(entries, (short) 0, (short) le);
	}


	// get command nonce for sending with next apdu
	
	// Command-APDU: C0 01 00 00 01
	// Example: C001000001

	// Response-APDUs: 
	// No error: <2 bytes command nonce> <2 bytes SW_NO_ERROR>   Example: 58A09000

	private void getCommandNonce(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		randomData.generateData(buffer, (short) 0, NONCE_LENGTH);
		commandNonce = Util.makeShort(buffer[0], buffer[1]);
		commandNonceIsValid = true;
		apdu.setOutgoingAndSend((short) 0, (short) NONCE_LENGTH);
	}

	
	// ask if identity is granted access on this specific weekday
	// no admin identity required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	// Current Weekday: number of weekday between 01 (Monday) .. 07 (Sunday)
	
	// Command-APDU: C0 10 <1 byte current weekday> 00 08 <8 bytes identity token> 01
	// Example: C010060008abcdefgh01
	
	// Response-APDUs: 
	// No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void shouldOpen(APDU apdu) {
		byte[] buffer = originalAPDU;
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
    	apdu.setOutgoingAndSend((short) 0, (short) 1);
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

	private void shouldOpenGlobal(APDU apdu) {
		byte[] buffer = originalAPDU;
		
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
    	apdu.setOutgoingAndSend((short) 0, (short) 1);
	}
	
	
	// get global access item
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 22 00 00 08 <8 bytes admin token> 01
	// Example: C0220000086d6569737465723101
	
	// Response-APDUs:
	// No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void getGlobalAccessItem(APDU apdu) {
		byte[] buffer = originalAPDU;
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);

		buffer[0] = globalAccessItem;
		apdu.setOutgoingAndSend((short) 0, VALUE_BYTE_SIZE);
	}
	

	// set global access item
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 21 <1 byte weekday bitmask> 00 08 <8 bytes admin token>
	// Example: C021f800086d65697374657231
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void setGlobalAccessItem(APDU apdu) {
		byte[] buffer = originalAPDU;
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
	// Example: C0300000116d65697374657231abcdefgh06
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// If full: <2 bytes SW_UNKNOWN>   Bytes: 6f00
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void putAccessItem(APDU apdu) {
		byte[] buffer = originalAPDU;
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
	// Example: C0400000106d65697374657231abcdefgh
	
	// Response-APDUs:
	// No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
	// Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void removeAccessItem(APDU apdu) {
		byte[] buffer = originalAPDU;
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
		byte[] buffer = originalAPDU;
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
	// Example: C0500000106d65697374657231abcdefgh01
	
	// Response-APDUs:
	// No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: 069000
	// Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void getWeekdays(APDU apdu) {
		byte[] buffer = originalAPDU;
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
        	// valid position, so return weekday bitmask
    		buffer[0] = entries[(pos + KEY_BYTE_SIZE)];
        	apdu.setOutgoingAndSend((short) 0, VALUE_BYTE_SIZE);
    	}
	}
	
	
	// get access item at pos in entries buffer
	// admin identitiy required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)

	// Command-APDU: C0 51 <2 bytes position (start 0)> 08 <8 bytes admin token> 09
	// Example: C0510003086d6569737465723109
	
	// Response-APDUs:
	// No error: <8 bytes identity token> <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: abcdefgh049000
	// Not found or empty: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void getAccessItemAtPos(APDU apdu) {
		byte[] buffer = originalAPDU;
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		short pos = (short) (Util.makeShort(buffer[ISO7816.OFFSET_P1], buffer[ISO7816.OFFSET_P2]) * ENTRY_BYTE_SIZE);
		if (pos + ENTRY_BYTE_SIZE <= entries.length) {
			
			short lessEqualGreater = Util.arrayCompare(EMPTY_ENTRY, (short) 0, entries, pos, ENTRY_BYTE_SIZE);
    		if (lessEqualGreater == 0) {
    			// entry at this pos is empty
    			ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
    		}
    		// copy non atomic, because we are reading from entries in this case
    		Util.arrayCopyNonAtomic(entries, pos, buffer, (short) 0, ENTRY_BYTE_SIZE);
        	apdu.setOutgoingAndSend((short) 0, ENTRY_BYTE_SIZE);
		} else {
			ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
		}
	}
	
	
	// get max size of entries buffer
	// admin identitiy required
	
	// Command-APDU: C0 60 00 00 08 <8 bytes admin token> 02
	// Example: C0600000086d6569737465723102
	
	// Response-APDUs:
	// No error: <2 bytes max size count> <2 bytes SW_NO_ERROR>   Example: 00049000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

	private void getMaxSize(APDU apdu) {
		byte[] buffer = originalAPDU;
		short lc = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (lc != ADMIN_IDENTITY.length) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		if (!isAdminIdentity(buffer, ISO7816.OFFSET_CDATA)) ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		
		Util.setShort(buffer, (short) 0, (short) MAX_SIZE);
		// sending short value: 2 bytes
		apdu.setOutgoingAndSend((short) 0, (short) 2);
	}
	

	
	
	private void decryptCommandAPDU(APDU apdu) {
		byte[] buffer = apdu.getBuffer();

		// command nonce
		short receivedCommandNonce = Util.makeShort(buffer[0], buffer[1]);
		// response nonce
		short receivedResponseNonce = Util.makeShort(buffer[2], buffer[3]);
		short length = (short) buffer.length;

		// copy original APDU into RAM
		Util.arrayCopyNonAtomic(buffer, (short) 4, originalAPDU, (short) 0, (short) 200);
//		Util.arrayCopyNonAtomic(originalAPDU, (short) 0, buffer, (short) 0, (short) (length - 4));

		
		byte insByte = buffer[5];
		// no valid nonce is required when getting one
		if ((insByte != INS_GET_COMMAND_NONCE) && 
			(!commandNonceIsValid || (receivedCommandNonce != commandNonce))) {
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}
		
		// 2. Byte-Array entschlüsseln -> <Command-Nonce><Response-Nonce><Command-APDU>
		// 3. Command-Nonce mit gespeichertem Command-Nonce vergleichen
		// 4. Gespeichertes Command-Nonce zurücksetzen
		// 4a. Wenn gleich, dann Command-APDU auswerten
		// 4b. Sonst Fehler
	}
	
	private void encryptResponseAPDU(APDU apdu) {
		
		// 5. Wenn ausgewertet, dann <Response-Nonce><Response-APDU> verschlüsseln
		// 6. Senden von verschlüsseltem Byte Array
	}
    
	
	// check sent command nonce
	
	private void checkCommandNonce(APDU apdu) {
		byte[] buffer = originalAPDU;
		
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
