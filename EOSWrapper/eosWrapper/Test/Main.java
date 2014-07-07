package eosWrapper.Test;

import eosWrapper.Client.AdminClient;
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
		
		AdminClient client = new AdminClient(new AdminIdentity("super fucking secret"));
		client.open(testLock);
		
		client = new AdminClient(new GuestIdentity("again secret"));
		client.open(testLock);
		
//		try {
//			MessageDigest md = MessageDigest.getInstance("SHA-256");
//			String text = "This is some text";
//
//			md.update(text.getBytes("UTF-8")); // Change this to "UTF-16" if needed
//			byte[] digest = md.digest();
//			String s = Hex.toString(digest);
//			System.out.println(s);
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		}		
		
		//Prevents bug - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6476706
		System.exit(0);
	}
}
