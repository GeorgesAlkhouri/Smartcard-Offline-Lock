package eosApplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class EOSApplet extends Applet {
	
	// CLA byte in the command APDU header
	private final static byte CORE_CLA = (byte) 0xC0;
	
	// INS bytes in the command APDU header
	private final static byte INS_SHOULD_OPEN = (byte)0x10;
	//final static byte INS_CREATE_ACCESS_ITEM = (byte)0x20;
	//final static byte INS_UPDATE_ACCESS_ITEM = (byte)0x30;
	private final static byte INS_PUT_ACCESS_ITEM = (byte)0x30;
	private final static byte INS_REMOVE_ACCESS_ITEM = (byte)0x40;
	private final static byte INS_GET_WEEKDAYS = (byte)0x50;
	
	private final static byte TRUE_BYTE = (byte)0x01;
	private final static byte FALSE_BYTE = (byte)0x00;
	
	//final static byte IDENTITY_BSIZE = 32;
	//final static byte WEEKDAY_BITMASK_BSIZE = 1;
	//final static byte ENTRY_COUNT = 100;
	
	private final static byte MAX_SIZE = 4; // entries possible
	private final static byte KEY_BYTE_SIZE = 32; // hash
	private final static byte VALUE_BYTE_SIZE = 1; // weekday bitmask
	private final static byte ENTRY_BYTE_SIZE = (byte) (KEY_BYTE_SIZE + VALUE_BYTE_SIZE);
	private final static short MAX_BYTE_SIZE = (short) (ENTRY_BYTE_SIZE * MAX_SIZE);

    private byte[] entries; // array in EEPROM
    
    private final static byte[] EMPTY_ENTRY = new byte[] {
    	0,0,0,0,0,0,0,0,
    	0,0,0,0,0,0,0,0,
    	0,0,0,0,0,0,0,0,
    	0,0,0,0,0,0,0,0,
    	0};
    
    // c6621e596b69db6ae3d1703c26a528cc0afd7c358b0892a81b8e38df6e6cb3d7
    private final static byte[] ADMIN_IDENTITY = new byte[] {
    	(byte)0xc6, (byte)0x62, (byte)0x1e, (byte)0x59, (byte)0x6b, (byte)0x69, (byte)0xdb, (byte)0x6a, 
    	(byte)0xe3, (byte)0xd1, (byte)0x70, (byte)0x3c, (byte)0x26, (byte)0xa5, (byte)0x28, (byte)0xcc, 
    	(byte)0x0a, (byte)0xfd, (byte)0x7c, (byte)0x35, (byte)0x8b, (byte)0x08, (byte)0x92, (byte)0xa8, 
    	(byte)0x1b, (byte)0x8e, (byte)0x38, (byte)0xdf, (byte)0x6e, (byte)0x6c, (byte)0xb3, (byte)0xd7
    };
	
	private EOSApplet() {
		this.entries = new byte[MAX_BYTE_SIZE];
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
		return true;
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buffer = apdu.getBuffer();
		
		// verify that the applet can accept this APDU message
		if (buffer[ISO7816.OFFSET_CLA] != CORE_CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		switch (buffer[ISO7816.OFFSET_INS]) {
		case INS_SHOULD_OPEN: shouldOpen(apdu); return;
		//case INS_CREATE_ACCESS_ITEM: createAccessItem(apdu); return;
		//case INS_UPDATE_ACCESS_ITEM: updateAccessItem(apdu); return;
		case INS_PUT_ACCESS_ITEM: putAccessItem(apdu); return;
		case INS_REMOVE_ACCESS_ITEM: removeAccessItem(apdu); return;
		case INS_GET_WEEKDAYS: getWeekdays(apdu); return;
		
		case (byte)0x90: debugGetAll(apdu); return;
		
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	
	private void debugGetAll(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		short le = (short)(buffer[ISO7816.OFFSET_LC] & (short) 0x00FF);
		if (le != MAX_BYTE_SIZE) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		apdu.setOutgoing();                           // set transmission to outgoing data
		apdu.setOutgoingLength((short) le);            // set the number of bytes to send to the IFD
		apdu.sendBytesLong(entries, (short) 0, (short) le);
	}

	
	// ask if identity is granted access on this specific weekday
	// no admin identity required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	// Current Weekday: number of weekday between 01 (Monday) .. 07 (Sunday)
	
	// Command-APDU: C0 10 <1 byte current weekday> 00 20 <32 bytes sha-256 hashed identity token> 01
	// Example: C0100600204cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe7701
	
	// Response-APDUs: 
	// No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void shouldOpen(APDU apdu) {
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
    	apdu.setOutgoingAndSend((short) 0, (short) 1);
	}

	
	// create or update (put) access item
	// admin identitiy required

	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)
	
	// Command-APDU: C0 30 00 00 41 <32 bytes sha-256 hashed admin identity token> <32 bytes sha-256 hashed identity token> <1 byte weekday bitmask>
	// Example: C030000041c6621e596b69db6ae3d1703c26a528cc0afd7c358b0892a81b8e38df6e6cb3d74cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe7706
	
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
	
	// Command-APDU: C0 40 00 00 40 <32 bytes sha-256 hashed admin identity token> <32 bytes sha-256 hashed identity token>
	// Example: C040000040c6621e596b69db6ae3d1703c26a528cc0afd7c358b0892a81b8e38df6e6cb3d74cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe77
	
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

	
	// get weekday bitmask for specific identity
	// admin identitiy required
	
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Example: 11111000(2) -> f8(16)

	// Command-APDU: C0 50 00 00 40 <32 bytes sha-256 hashed admin identity token> <32 bytes sha-256 hashed identity token> 01
	// Example: C050000040c6621e596b69db6ae3d1703c26a528cc0afd7c358b0892a81b8e38df6e6cb3d74cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe7701
	
	// Response-APDUs:
	// No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: 069000
	// Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
	// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
	// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80
	
	private void getWeekdays(APDU apdu) {
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
        	// valid position, so return weekday bitmask
    		buffer[0] = entries[(pos + KEY_BYTE_SIZE)];
        	apdu.setOutgoingAndSend((short) 0, VALUE_BYTE_SIZE);
    	}
	}
	
	
	
	
	/*
    private boolean containsKey(byte[] keyArr, short keyOff) {
    	short pos = findPosition(
    			keyArr, keyOff, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// the key does not exist
    		return false;
    	} else {
    		return true;
    	}
    }

    // containsKey should be used before to test existence of key
    // if not found the return value is undefined
    public byte get(byte[] keyArr, short keyOff) {
    	
    	short pos = findPosition(
    			keyArr, keyOff, KEY_BYTE_SIZE, 
    			ENTRY_BYTE_SIZE, 
    			entries, (short) 0, MAX_BYTE_SIZE);
    	if (pos == entries.length) {
    		// the key does not exist
    		// the return value is undefined (containsKey should be used before)
    		// returning first entry
    		pos = 0;
    	}
    	// setting position to value of entry
    	pos = (short) (pos + KEY_BYTE_SIZE);
    	
        return entries[pos];
    }
    */
    
    
    
    
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