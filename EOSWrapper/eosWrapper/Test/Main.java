package eosWrapper.Test;

import eosWrapper.Client.TestClient;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Environment.TestLock;
import eosWrapper.Identity.AdminIdentity;
import eosWrapper.Identity.GuestIdentity;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IEnvironment testLock = new TestLock();
		
		TestClient client = new TestClient(new AdminIdentity("super fucking secret"));
		client.open(testLock);
		
		client = new TestClient(new GuestIdentity("again secret"));
		client.open(testLock);
	}
}
