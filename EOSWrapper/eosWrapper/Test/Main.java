package eosWrapper.Test;

import opencard.core.service.CardRequest;
import opencard.core.service.CardServiceException;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CardTerminalException;
import opencard.core.util.OpenCardPropertyLoadingException;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Environment.TestLock;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IEnvironment testLock = new TestLock();
		System.out.println(testLock.getWeekDay());
		
//		try {
//			SmartCard.start();
//			CardRequest cr = new CardRequest(CardRequest.ANYCARD,null,null);
//			
//			SmartCard sm = SmartCard.waitForCard(cr);
//			if (sm != null)
//				System.out.println("YES");
//			else
//				System.out.println("NO");
//			
//			SmartCard.shutdown();
//			
//		} catch (OpenCardPropertyLoadingException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		} catch (CardServiceException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		} catch (CardTerminalException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Automatisch erstellter Catch-Block
//			e.printStackTrace();
//		}
	}

}
