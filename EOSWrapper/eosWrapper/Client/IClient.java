package eosWrapper.Client;

import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.IIdentity;

public interface IClient {
	void run(IEnvironment env);
	
	IIdentity getIdentity();
}
