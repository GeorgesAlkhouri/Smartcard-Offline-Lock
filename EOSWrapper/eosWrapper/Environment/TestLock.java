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
	
	/**
	 * Environment tries to create smart card object.
	 */
	public TestLock() {
		try {
			SmartCard.start();
			CardRequest request = new CardRequest(CardRequest.ANYCARD,null,GeneralWrapper.class);
			request.setTimeout(10);
			this.smartCard = SmartCard.waitForCard(request);
		} catch (OpenCardPropertyLoadingException e) {
			e.printStackTrace();
		} catch (CardServiceException e) {
			e.printStackTrace();
		} catch (CardTerminalException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
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
