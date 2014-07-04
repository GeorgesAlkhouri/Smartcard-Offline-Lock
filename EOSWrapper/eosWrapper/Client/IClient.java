package eosWrapper.Client;

import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.IIdentity;

public interface IClient {
	void open(IEnvironment env);
	
	IIdentity getIdentity();
}
