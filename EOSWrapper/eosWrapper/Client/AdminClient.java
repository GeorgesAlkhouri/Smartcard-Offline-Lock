package eosWrapper.Client;

import eosWrapper.GeneralWrapper;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.IIdentity;

public class AdminClient implements IClient {

	private final IIdentity id;
	public AdminClient(IIdentity id) {
		this.id = id;
	}
	
	public void open(IEnvironment env) {
		GeneralWrapper wrapper = new GeneralWrapper();
		try {
			wrapper.shouldOpen(env.getWeekDay());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public IIdentity getIdentity() {
		return this.id;
	}

}
