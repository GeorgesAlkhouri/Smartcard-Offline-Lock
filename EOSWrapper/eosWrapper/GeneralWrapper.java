package eosWrapper;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import opencard.core.service.CardService;
import opencard.core.service.InvalidCardChannelException;
import opencard.core.terminal.CardTerminalException;
import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class GeneralWrapper extends CardService {

	protected final static byte[] SELECT = {
		(byte)0x00,(byte)0xA4,(byte)0x04,(byte)0x00,(byte)0x09,(byte)0x45,(byte)0x4F,(byte)0x53,(byte)0x41,(byte)0x70,(byte)0x70,(byte)0x6C,(byte)0x65,(byte)0x74,(byte)0x00
	};
	protected final static byte[] CORE_CLA = {(byte)0xC0};
	
	protected final static byte[] INS_GET_COMMAND_NONCE = {(byte)0x01};
	protected final static byte[] INS_SHOULD_OPEN = {(byte)0x10};
	protected final static byte[] INS_SHOULD_OPEN_GLOBAL = {(byte)0x20};

	protected final static byte[] INIT_NONCE = {(byte)0x00, (byte)0x00};
	
	protected final static byte[] PHRASE = new byte[] { 
    	(byte)0x73, (byte)0x6f, (byte)0x73, (byte)0x65, (byte)0x63, (byte)0x75, (byte)0x72, (byte)0x65
    };
	protected final static byte[] IV = new byte[] {
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
	};
	
	private byte[] validateNonce;

	public void selectApplet() throws Exception {
		CommandAPDU select = new CommandAPDU(SELECT);
		ResponseAPDU response = sendCommandAPDU(select);
		System.out.println("");
	}
	
	public boolean shouldOpen(IAccessItem item, WeekDay day) throws Exception {
		return shouldOpen(item.getIdentity(), day);
	}

	public boolean shouldOpen(IIdentity id, WeekDay day) throws Exception {
		byte[] apdu = concat(
				CORE_CLA,
				INS_SHOULD_OPEN,
				new byte[]{(byte)day.convertWeekDay()},
				new byte[]{0x00,0x08},
				id.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[]{0x01});
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		if (response == null)
			return false;
		if (Arrays.equals(response.data(),new byte[]{0x00,0x01}))
			return true;
		else
			return false;
	}

	
	public boolean shouldOpen(WeekDay day) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}
	
	public void createAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public void createAccessItems(List<IAccessItem> items) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public List<IIdentity> getAllIdentities() throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}
	
	public List<IAccessItem> getAllAccessItems() throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}
	
	public void updateAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public void removeAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public void removeAccessItems(List<IAccessItem> items) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	// Privates
		
	/**
	 * Request a nonce from the smart card exclude a replay attack.
	 * This is the entry point for nonce communicating traffic. 
	 * 
	 * @return Return a command nonce that the smart card will accept.
	 * @throws Exception when if an error occurred.
	 */
	private byte[] requestNonce() throws Exception {
		byte[] apdu = concat(
				CORE_CLA,
				INS_GET_COMMAND_NONCE,
				INIT_NONCE,
				new byte[]{0x02});
		CommandAPDU command = createCommandAPDU(INIT_NONCE, createNonce(2), apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] decrypted = processResponseAPDU(response);
		return decrypted;
	}
	
	/**
	 * Creates a CommandAPDU and encrypts it.
	 * 
	 * @sideEffect Sets the validateNonce to verify the next incomming response.
	 * @param commandNonce The nonce for the smart card to verify.
	 * @param responseNonce The nonce for the offcard to verify.
	 * @param commandAPDU 
	 * @return Returns a ready to submit CommandAPDU.
	 * @throws Exception 
	 */
	private CommandAPDU createCommandAPDU(byte[] commandNonce, byte[] responseNonce, byte[] commandAPDU) throws Exception {
		this.validateNonce = responseNonce;
			
		int toFill = 64 - (commandNonce.length + responseNonce.length + commandAPDU.length);
		byte[] debug = concat(commandNonce,responseNonce,commandAPDU,new byte[toFill]);
		String d = HexString.hexify(debug);
		
		byte[] encrypted;
		if (toFill > 0)
			encrypted = encrypt(concat(commandNonce,responseNonce,commandAPDU,new byte[toFill]));
		else
			encrypted = encrypt(concat(commandNonce,responseNonce,commandAPDU));
		
		byte[] finale = concat(new byte[]{(byte)(encrypted.length * 2)},encrypted);
		
		String dd = HexString.hexify(finale);
		return new CommandAPDU(finale);
	}
	
	/**
	 * Sends a given command APDU to the smart card.
	 * 
	 * @sideEffect Creates necessary services to communicate with 
	 * smart card.
	 * @param command The CommandAPDU encaplsulades the pure APDU.
	 * @return Returns an unprocessed an properly encrypted APDU or
	 * null if an error occures.
	 * @throws CardTerminalException 
	 * @throws InvalidCardChannelException 
	 */
	private ResponseAPDU sendCommandAPDU(CommandAPDU command) throws InvalidCardChannelException, CardTerminalException {
		allocateCardChannel();
		ResponseAPDU response = getCardChannel().sendCommandAPDU(command);
		releaseCardChannel();
		return response;
	}
	
	/**
	 * Checks the response APDU for general status error, decrypts it 
	 * and valides the contained responce nonce. If validated
	 * it tuncates the response nonce.
	 * 
	 * @param response The received APDU response.
	 * @return Returns the truncated response APDU.
	 * @throws Exception 
	 */
	private byte[] processResponseAPDU(ResponseAPDU response) throws Exception {
		String result = HexString.hexifyShort(response.sw1(),response.sw2());
		if (!result.equals("9000")) {
			System.out.println("Response status wrong: " + result);
			return null;
		}
		else {
			byte[] decrypted = decrypt(response.data());
			byte[] responceNonce = Arrays.copyOfRange(decrypted,0,2);
			if (validateNonce(responceNonce)) {
				byte[] data = Arrays.copyOfRange(decrypted,2,decrypted.length);
				return rightTrim(data);
			} else {
				System.out.println("Wrong response nonce.");
				return null;
			}
		}
	}
	
	/**
	 * Creates a random nonce in the given length.
	 * 
	 * @param nonceLength The length for the created byte array nonce.
	 * @return Return the random nonce in a byte array.
	 */
	private byte[] createNonce(int nonceLength) {
		byte[] nonce = new byte[nonceLength];
		new Random().nextBytes(nonce);
		return nonce;
	}
	
	/**
	 * Checks the response nonce for validity.
	 * 
	 * @sideEffect Sets the saved validateNonce property back to null
	 * @param responseNonce
	 * @return Returns true if the responseNonce is valid else false.
	 */
	private boolean validateNonce(byte[] responseNonce) {
		if (this.validateNonce == null)
			return false;
		
		boolean result = Arrays.equals(responseNonce,this.validateNonce);
		this.validateNonce = null;
		return result;
	}
	
	/**
	 * Template methode for decryption.
	 * 
	 * @param data
	 * @return
	 * @throws Exception 
	 */
	private byte[] decrypt(byte[] data) throws Exception {
		return cryptography(Cipher.DECRYPT_MODE, data);
	}
	
	/**
	 * Template methode for encryption.
	 * 
	 * @param data
	 * @return
	 * @throws Exception 
	 */
	private byte[] encrypt(byte[] data) throws Exception {
		return cryptography(Cipher.ENCRYPT_MODE, data);
	}
	
	/**
	 * @param opmode Operation mode, determines whether
	 * to encrypt or dycrypt
	 * @param data
	 * @return Returns processed data or empty byte array
	 * if an error occured.
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	private byte[] cryptography(int opmode, byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
		SecretKeySpec key = new SecretKeySpec(PHRASE, "DES");
		IvParameterSpec iv = new IvParameterSpec(IV);
		cipher.init(opmode, key, iv);
		byte[] crypted = cipher.doFinal(data);
		return crypted;
	}
	
	//Util
	/**
	 * Concatenates the given byte array in the given order. 
	 * 
	 * @param args Parameter list of byte arrays.
	 * @return Concatenated byte array.
	 */
	private byte[] concat(byte[]... args) {
        int fulllength = 0;
        for (byte[] arrItem : args) {
            fulllength += arrItem.length;
        }

	    byte[] retArray = new byte[fulllength];
	    int start = 0;
	    for (byte[] arrItem : args) {
	        System.arraycopy(arrItem, 0, retArray, start, arrItem.length);
	        start += arrItem.length;
	    }
	    return retArray;
	}
	
	private byte[] rightTrim(byte[] a) {
		int count = a.length - 1;
		while (count > 0) {
			if (a[count] != 0)
				break;
			else
				count--;
		}
		if (count == a.length - 1)
			return a;
		else
			return Arrays.copyOfRange(a,0,count + 1);
	}
}
