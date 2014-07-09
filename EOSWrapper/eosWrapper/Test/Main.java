package eosWrapper.Test;

import eosWrapper.Client.AdminClient;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Environment.TestLock;
import eosWrapper.Identity.AdminIdentity;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IEnvironment testLock = new TestLock();
		
		AdminClient client = new AdminClient(new AdminIdentity("meister1"));
		client.open(testLock);
		
		//Prevents bug - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6476706
		System.exit(0);
	}
}
