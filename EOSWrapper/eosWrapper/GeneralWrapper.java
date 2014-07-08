package eosWrapper;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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

	protected final static byte[] CORE_CLA = {(byte)0xC0};
	
	protected final static byte[] INS_GET_COMMAND_NONCE = {(byte)0x01};
	
	protected final static byte[] INS_SHOULD_OPEN = {(byte)0x10};
	protected final static byte[] INS_SHOULD_OPEN_GLOBAL = {(byte)0x20};

	protected final static byte[] INIT_NONCE = {(byte)0x00, (byte)0x00};
	
	private byte[] validateNonce;
	
	public boolean shouldOpen(IAccessItem item, WeekDay day) throws Exception {
		return shouldOpen(item.getIdentity(), day);
	}

	public boolean shouldOpen(IIdentity id, WeekDay day) throws Exception {
		byte[] apdu = concat(
				CORE_CLA,
				INS_SHOULD_OPEN,
				new byte[]{(byte)day.convertWeekDay()},
				new byte[]{0x00,0x08},
				id.getToken().getBytes(Charset.forName("US-ASCII")));
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
				new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00},
				CORE_CLA,
				INS_GET_COMMAND_NONCE,
				INIT_NONCE,
				new byte[]{0x01});
		CommandAPDU command = createCommandAPDU(INIT_NONCE, createNonce(2), apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		//TODO: implement processResponseAPDU
		byte[] decrypted = decrypt(response.data());
		byte[] responceNonce = Arrays.copyOfRange(decrypted,0,2);
		if (validateNonce(responceNonce)) {
			return Arrays.copyOfRange(decrypted,2,4);
		} else {
			throw new Exception("Response nonce wrong.");
		}
	}
	
	/**
	 * Creates a CommandAPDU and encrypts it.
	 * 
	 * @sideEffect Sets the validateNonce to verify the next incomming response.
	 * @param commandNonce The nonce for the smart card to verify.
	 * @param responseNonce The nonce for the offcard to verify.
	 * @param commandAPDU 
	 * @return Returns a ready to submit CommandAPDU.
	 */
	private CommandAPDU createCommandAPDU(byte[] commandNonce, byte[] responseNonce, byte[] commandAPDU) {
		this.validateNonce = responseNonce;
		byte[] encrypted = encrypt(concat(commandNonce,responseNonce,commandAPDU));
		byte[] finale = concat(new byte[]{(byte)encrypted.length},encrypted);
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
	 */
	private ResponseAPDU sendCommandAPDU(CommandAPDU command) {
		allocateCardChannel();
		ResponseAPDU response = null;
		try {
			response = getCardChannel().sendCommandAPDU(command);
		} catch (InvalidCardChannelException e) {
			e.printStackTrace();
			return null;
		} catch (CardTerminalException e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}
	
	/**
	 * Checks the response APDU for general status error, decrypts it 
	 * and valides the contained responce nonce. If validated
	 * it tuncates the response nonce.
	 * 
	 * @param response The received APDU response.
	 * @return Returns the truncated response APDU.
	 */
	private byte[] processResponseAPDU(ResponseAPDU response) {
		String result = HexString.hexifyShort(response.sw1(),response.sw2());
		if (!result.equals("9000")) {
			System.out.println("Response status wrong: " + result);
			return null;
		}
		else {
			byte[] decrypted = decrypt(response.data());
			byte[] responceNonce = Arrays.copyOfRange(decrypted,0,2);
			if (validateNonce(responceNonce)) {
				return Arrays.copyOfRange(decrypted,3,decrypted.length);
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
	 */
	private byte[] decrypt(byte[] data) {
		return cryptography(Cipher.DECRYPT_MODE, data);
	}
	
	/**
	 * Template methode for encryption.
	 * 
	 * @param data
	 * @return
	 */
	private byte[] encrypt(byte[] data) {
		return cryptography(Cipher.ENCRYPT_MODE, data);
	}
	
	/**
	 * @param opmode Operation mode, determines whether
	 * to encrypt or dycrypt
	 * @param data
	 * @return Returns processed data or empty byte array
	 * if an error occured.
	 */
	private byte[] cryptography(int opmode, byte[] data) {
		byte[] crypted = new byte[0];
		try {
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			SecretKeySpec key = new SecretKeySpec(null, "AES");
			cipher.init(opmode, key);
			crypted = cipher.doFinal(data);
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		}
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
}
