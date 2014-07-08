package eosWrapper.Environment;

import java.util.Date;

import opencard.core.service.CardRequest;
import opencard.core.service.CardServiceException;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CardTerminalException;
import opencard.core.util.OpenCardPropertyLoadingException;
import eosWrapper.GeneralWrapper;
import eosWrapper.Util.WeekDay;

public class TestLock implements IEnvironment {

	//Need state here, because SmartCard.waitForCard could return null
	private SmartCard smartCard;
	
	public TestLock() {
		try {
			SmartCard.start();
			CardRequest request = new CardRequest(CardRequest.ANYCARD,null,GeneralWrapper.class);
			this.smartCard = SmartCard.waitForCard(request);
		} catch (OpenCardPropertyLoadingException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (CardServiceException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (CardTerminalException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		}
		
	}
	
	public WeekDay getWeekDay() {
		return WeekDay.getWeekDay(new Date());
	}

	public SmartCard getSmartCard() {
		return this.smartCard;
	}

}
