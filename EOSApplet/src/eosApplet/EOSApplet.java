package eosApplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class EOSApplet extends Applet {
	
	// CLA byte in the command APDU header
	final static byte CORE_CLA = (byte)0xC0;
	
	// INS bytes in the command APDU header
	final static byte SHOULD_OPEN_INS = (byte)0x10;
	final static byte CREATE_ACCESS_ITEM_INS = (byte)0x20;
	final static byte UPDATE_ACCESS_ITEM_INS = (byte)0x30;
	final static byte REMOVE_ACCESS_ITEM_INS = (byte)0x40;
	
	final static byte TRUE_BYTE = (byte)0x01;
	final static byte FALSE_BYTE = (byte)0x00;
	
	private EOSApplet() {
		
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
		
		// verify that the applet can accept this
		// APDU message
		if (buffer[ISO7816.OFFSET_CLA] != CORE_CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		switch (buffer[ISO7816.OFFSET_INS]) {
		case SHOULD_OPEN_INS: shouldOpen(apdu, buffer); return;
		case CREATE_ACCESS_ITEM_INS: createAccessItem(apdu, buffer); return;
		case UPDATE_ACCESS_ITEM_INS: updateAccessItem(apdu, buffer); return;
		case REMOVE_ACCESS_ITEM_INS: removeAccessItem(apdu, buffer); return;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	// APDU: C0 10 00 00 20 <32 bytes sha-256 hashed identity token> 01
	// Response: <1 byte> (01 ... true, 00 ... false)
	// Example: C0100000204cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe7701
	private void shouldOpen(APDU apdu, byte[] buffer) {
		// inform system that the applet has finished processing
		// the command and the system should now prepare to
		// construct a response APDU which contains data field
		apdu.setOutgoing();
		
		// indicate the number of bytes in the data field
		apdu.setOutgoingLength((byte)1);
		
		// move the data into the APDU buffer starting at offet 0
		// TODO allow access based on identity
		buffer[0] = TRUE_BYTE;
		
		// send 1 byte of data at offset 0 in the APDU buffer
		apdu.sendBytes((short)0, (short)1);
	}
	
	// APDU: C0 20 00 00 21 <32 bytes sha-256 hashed identity token> <1 byte weekdays bitmask>
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Weekday Bitmask Example: 11111000(2) -> f8(16)
	// Response: none
	// Example: C0200000214cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe77f8
	private void createAccessItem(APDU apdu, byte[] buffer) {
		
		// TODO
		// error if identity already exists?
	}

	// APDU: C0 30 00 00 21 <32 bytes sha-256 hashed identity token> <1 byte weekdays bitmask>
	// Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
	// Response: none
	// Example: C0300000214cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe7706
	private void updateAccessItem(APDU apdu, byte[] buffer) {
		
		// TODO
		// error if identity does not exist?
	}
	
	// APDU: C0 40 00 00 20 <32 bytes sha-256 hashed identity token>
	// Response: none
	// Example: C0400000204cede21074b1a48da2b0a19a0a3752f8bae500d348e4effe6932e2e26666fe77
	private void removeAccessItem(APDU apdu, byte[] buffer) {
		
		// TODO
		// error if identity does not exist?
	}
	
}
