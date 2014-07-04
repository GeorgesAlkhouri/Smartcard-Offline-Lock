package eosWrapper.Client;

import eosWrapper.GeneralWrapper;
import eosWrapper.IWrapper;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.IIdentity;

public class TestClient implements IClient {

	private final IIdentity id;
	public TestClient(IIdentity id) {
		this.id = id;
	}
	
	public void open(IEnvironment env) {
		IWrapper wrapper = new GeneralWrapper();
		wrapper.shouldOpen(env.getWeekDay());
	}

	public IIdentity getIdentity() {
		return this.id;
	}

}
