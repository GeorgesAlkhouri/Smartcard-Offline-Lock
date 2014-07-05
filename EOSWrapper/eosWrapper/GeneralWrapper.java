package eosWrapper;

import java.util.List;

import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class GeneralWrapper extends Wrapper {

//	private final static byte[] APPLET_AID = {'T','E','S','T'};
//	private final static byte[] SELECT_APDU = {0x00,0x01};
	private final static byte[] SHOULD_OPEN_APDU = {0x00,0x02};
		
	@Override
	public boolean shouldOpen(IAccessItem item) throws Exception {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}
	@Override
	public boolean shouldOpen(IIdentity id, WeekDay day) throws Exception {
		CommandAPDU command = new CommandAPDU(SHOULD_OPEN_APDU);
		
		allocateCardChannel();
		ResponseAPDU response = getCardChannel().sendCommandAPDU(command); 
		String result = HexString.hexifyShort(response.sw1(),response.sw2());
		if (result.equals(""))
			return true;
		else
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
	
}
