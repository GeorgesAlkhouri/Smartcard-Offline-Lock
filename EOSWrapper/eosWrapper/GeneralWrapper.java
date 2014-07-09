package eosWrapper;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

import eosWrapper.AccessItem.AccessItem;
import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.GuestIdentity;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class GeneralWrapper extends CardService {

	protected final static byte[] SELECT = {
		(byte)0x00,(byte)0xA4,(byte)0x04,(byte)0x00,(byte)0x09,(byte)0x45,(byte)0x4F,(byte)0x53,(byte)0x41,(byte)0x70,(byte)0x70,(byte)0x6C,(byte)0x65,(byte)0x74,(byte)0x00
	};
	protected final static byte[] INIT_NONCE = {(byte)0x00, (byte)0x00};
	
	protected final static byte[] CLA_BYTE = {(byte)0xC0};
	
	protected final static byte[] INS_GET_COMMAND_NONCE = {(byte)0x01};
	
	protected final static byte[] INS_SHOULD_OPEN = {(byte)0x10};
	protected final static byte[] INS_SHOULD_OPEN_GLOBAL = {(byte)0x20};
	protected final static byte[] INS_GET_GLOBAL_ACCESS = {(byte)0x22};
	protected final static byte[] INS_SET_GLOBAL_ACCESS = {(byte)0x21};
	
	protected final static byte[] INS_PUT_ACCESS_ITEM = {(byte)0x30};
	protected final static byte[] INS_REMOVE_ACCESS_ITEM = {(byte)0x40};
	protected final static byte[] INS_REMOVE_ALL_ACCESS_ITEMS = {(byte)0x41};
	
	protected final static byte[] INS_GET_WEEKDAYS = {(byte)0x50};
	protected final static byte[] INS_GET_ACCESS_ITEM_AT_POS = {(byte)0x51};
	protected final static byte[] INS_GET_MAX_SIZE = {(byte)0x60};
	
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
		processResponseAPDU(response);
	}
	
	public boolean shouldOpen(IAccessItem item, WeekDay day) throws Exception {
		return shouldOpen(item.getIdentity(), day);
	}

	public boolean shouldOpen(IIdentity id, WeekDay day) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_SHOULD_OPEN,
				new byte[]{(byte)day.convertWeekDay()},
				new byte[]{0x00,0x08},
				id.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[]{0x01});
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] result = processResponseAPDU(response);
		if (result[0] == 1)
			return true;
		else
			return false;
	}
	
	public boolean shouldOpen(WeekDay day) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_SHOULD_OPEN_GLOBAL,
				new byte[] {(byte)day.convertWeekDay()},
				new byte[] {(byte)0x00}
				);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] result = processResponseAPDU(response);
		if (result[0] == 1)
			return true;
		else
			return false;
	}
	
	public void setGlobalAcsess(IAccessItem admin, List<WeekDay> days) throws Exception {
		setGlobalAcsess(admin.getIdentity(),days);
	}
	
	public void setGlobalAcsess(IIdentity admin, List<WeekDay> days) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_SET_GLOBAL_ACCESS,
				new byte[]{(byte)WeekDay.convertWeekDays(days)},
				new byte[]{(byte)0x00,(byte)0x08},
				admin.getToken().getBytes(Charset.forName("US-ASCII"))
				);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		processResponseAPDU(response);	
	}
	
	public List<WeekDay> getGlobalAccess(IAccessItem admin) throws Exception{
		return getGlobalAccess(admin.getIdentity());
	}
	
	public List<WeekDay> getGlobalAccess(IIdentity admin) throws Exception{
		byte[] apdu = concat(
				CLA_BYTE,
				INS_GET_GLOBAL_ACCESS,
				new byte[] {(byte)0x00},
				new byte[]{(byte)0x00,(byte)0x08},
				admin.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[] {(byte)0x01}
				);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] result = processResponseAPDU(response);
		
		return WeekDay.convertWeekDays(result[0]& 0xff);
	}
	
	public void createAccessItem(IIdentity admin, IAccessItem item) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_PUT_ACCESS_ITEM,
				new byte[] {(byte)0x00},
				new byte[]{(byte)0x00,(byte)0x11},
				admin.getToken().getBytes(Charset.forName("US-ASCII")),
				item.getIdentity().getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[]{(byte)WeekDay.convertWeekDays(item.getWeekDays())}
				);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		processResponseAPDU(response);
	}
	
	public void createAccessItems(IIdentity admin, List<IAccessItem> items) throws Exception {
		for (IAccessItem item : items) {
			createAccessItem(admin, item);
		}
	}
	
	public List<WeekDay> getWeekdays(IIdentity admin, IIdentity id) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_GET_WEEKDAYS,
				new byte[] {(byte)0x00},
				new byte[]{(byte)0x00,(byte)0x10},
				id.getToken().getBytes(Charset.forName("US-ASCII")),
				id.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[] {(byte)0x00});
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] result = processResponseAPDU(response);
		return WeekDay.convertWeekDays(result[0]& 0xff);
	}
	
	public List<IIdentity> getAllIdentities(IIdentity admin) throws Exception {
		List<IAccessItem> items = getAllAccessItems(admin);
		List<IIdentity> identities = new ArrayList<IIdentity>();
		for (IAccessItem item : items) {
			identities.add(item.getIdentity());
		}
		return identities;
	}
	
	public List<IAccessItem> getAllAccessItems(IIdentity admin) throws Exception {
		byte[] result = getMaxSize(admin);
		int maxSize = result[0] + result[1];
		ArrayList<IAccessItem> items = new ArrayList<IAccessItem>();
		for (int i = 0; i < maxSize; i++) {
			byte[] temp = ByteBuffer.allocate(4).putInt(i).array(); 
			IAccessItem item = getAccessItemAtPosition(admin,Arrays.copyOfRange(temp,2,4));
			if (item != null)
				items.add(item);
		}
		return items;
	}
	
	private IAccessItem getAccessItemAtPosition(IIdentity admin, byte[] position) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_GET_ACCESS_ITEM_AT_POS,
				position,
				new byte[]{(byte)0x08},
				admin.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[] {(byte)0x09}
				);
//		String d = HexString.hexify(apdu);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] result = processResponseAPDU(response);
		if (result == null)
			return null;
		String token = convertHexToASCII(Arrays.copyOfRange(result,0,7));
		List<WeekDay> days = WeekDay.convertWeekDays(result[8]& 0xff);
		
		IAccessItem item = new AccessItem(new GuestIdentity(token),days);
		return item;
	}
	
	private byte[] getMaxSize(IIdentity admin) throws Exception{
		byte[] apdu = concat(
				CLA_BYTE,
				INS_GET_MAX_SIZE,
				new byte[] {(byte)0x00},
				new byte[]{(byte)0x00,(byte)0x08},
				admin.getToken().getBytes(Charset.forName("US-ASCII")),
				new byte[] {(byte)0x02}
				);
		String d = HexString.hexify(apdu);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		return processResponseAPDU(response);
	}
		
	public void updateAccessItem(IIdentity admin, IAccessItem item) throws Exception {
		createAccessItem(admin,item);
	}
	
	public void removeAccessItem(IIdentity admin, IAccessItem item) throws Exception {
		byte[] apdu = concat(
				CLA_BYTE,
				INS_REMOVE_ACCESS_ITEM,
				new byte[] {(byte)0x00},
				new byte[]{(byte)0x00,(byte)0x10},
				admin.getToken().getBytes(Charset.forName("US-ASCII")),
				item.getIdentity().getToken().getBytes(Charset.forName("US-ASCII"))
				);
		byte[] commandNonce = requestNonce();
		CommandAPDU command = createCommandAPDU(commandNonce,createNonce(2),apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		processResponseAPDU(response);
	}
	
	public void removeAccessItems(IIdentity admin, List<IAccessItem> items) throws Exception {
		for (IAccessItem item : items) {
			removeAccessItem(admin,item);
		}
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
				CLA_BYTE,
				INS_GET_COMMAND_NONCE,
				INIT_NONCE,
				new byte[]{0x02});
		CommandAPDU command = createCommandAPDU(INIT_NONCE, createNonce(2), apdu);
		ResponseAPDU response = sendCommandAPDU(command);
		byte[] nonce = processResponseAPDU(response);
		assert !(nonce.length == 2);
		
		return nonce;
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
	 * and valides the contained responce nonce. If the APDU contains no data (by selecting applet)
	 * but is a successfully return emtpy array. If validated it tuncates the response nonce.
	 * 
	 * @param response The received APDU response.
	 * @return Returns the truncated response APDU.
	 * @throws Exception 
	 */
	private byte[] processResponseAPDU(ResponseAPDU response) throws Exception {
		String code = HexString.hexifyShort(response.sw1(),response.sw2());
		if (!code.equalsIgnoreCase("9000"))
			if (code.equalsIgnoreCase("6a83"))
				return null;
			else
				throw processErrorCode(code);
		else {
			if (response.data() == null && this.validateNonce != null)
				throw new Exception("No response nonce.");
			if (response.data() == null)
				return new byte[0];
			
			byte[] decrypted = decrypt(response.data());
			String as = HexString.hexify(decrypted);
			byte[] responceNonce = Arrays.copyOfRange(decrypted,0,2);
			if (validateNonce(responceNonce)) {
				byte[] data = Arrays.copyOfRange(decrypted,2,decrypted.length);
				return rightTrim(data);
			} else
				throw new Exception("Wrong response nonce.");
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
	 * Creates an exception for an error code.
	 * @param error
	 * @return
	 */
	private Exception processErrorCode(String error) {
		if (error.equalsIgnoreCase("6700"))
			return new Exception("Wrong data length.");
		else if (error.equalsIgnoreCase("6a80"))
			return new Exception("No admin.");
		else if (error.equalsIgnoreCase("6f00"))
			return new Exception("Datastructur is full.");
		else
			return new Exception("Unknown error: " + error);
	}
	
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
	
	/**
	 * Removes the filled zeros from the rigth.
	 * @param a
	 * @return
	 */
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
	
	private String convertHexToASCII(byte[] hex) {
		String hexString = HexString.hexify(hex).replace(" ","");
		   StringBuilder output = new StringBuilder();
		    for (int i = 0; i < hexString.length(); i+=2) {
		        String str = hexString.substring(i, i+2);
		        output.append((char)Integer.parseInt(str, 16));
		    }
		    return output.toString();
	}
}
