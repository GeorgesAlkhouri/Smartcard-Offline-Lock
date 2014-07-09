package eosWrapper.Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eosWrapper.GeneralWrapper;
import eosWrapper.AccessItem.AccessItem;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.GuestIdentity;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class AdminClient implements IClient {

	private final IIdentity id;
	public AdminClient(IIdentity id) {
		this.id = id;
	}
	
	public void open(IEnvironment env) {
		try {
			GeneralWrapper service = (GeneralWrapper)env.getSmartCard().getCardService(GeneralWrapper.class,true);
			service.selectApplet();
			service.createAccessItem(
					this.id, new AccessItem(
							new GuestIdentity("12345678"), 
							Arrays.asList(
									new WeekDay[]{WeekDay.FRIDAY,WeekDay.WEDNESDAY}
					)));
			service.getAllAccessItems(this.id);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public IIdentity getIdentity() {
		return this.id;
	}

}
