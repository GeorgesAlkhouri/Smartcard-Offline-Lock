package eosWrapper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class GeneralWrapper extends Wrapper {

	@Override
	public boolean shouldOpen(IAccessItem item, WeekDay day) throws Exception {
		return shouldOpen(item.getIdentity(), day);
	}
	@Override
	public boolean shouldOpen(IIdentity id, WeekDay day) throws Exception {
//		CommandAPDU command = new CommandAPDU(SHOULD_OPEN_APDU);
//		
//		allocateCardChannel();
//		ResponseAPDU response = getCardChannel().sendCommandAPDU(command); 
//		String result = HexString.hexifyShort(response.sw1(),response.sw2());
//		if (result.equals(""))
//			return true;
//		else
			return false;
	}
	@Override
	public boolean shouldOpen(WeekDay day) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}
	@Override
	public void createAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	@Override
	public void createAccessItems(List<IAccessItem> items) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	@Override
	public List<IIdentity> getAllIdentities() throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}
	@Override
	public List<IAccessItem> getAllAccessItems() throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}
	@Override
	public void updateAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	@Override
	public void removeAccessItem(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	@Override
	public void removeAccessItems(List<IAccessItem> items) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	// Privates
	
//	private ArrayList hashIdentity(IIdentity id) {
//		try {
//			MessageDigest md = MessageDigest.getInstance("SHA-256");
//			md.update(id.getToken().getBytes("UTF-8"));
//			byte[] digest = md.digest();
//			return new ArrayList(Arrays.asList(digest));
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		}
//	}
	
}
