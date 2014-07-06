package eosWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import opencard.core.service.CardServiceFactory;
import opencard.core.service.CardServiceScheduler;
import opencard.core.service.CardType;
import opencard.core.terminal.CardID;
import opencard.core.terminal.CardTerminalException;

public class WrapperServiceFactory extends CardServiceFactory {
		
	@Override
	protected CardType getCardType(CardID arg0, CardServiceScheduler arg1) throws CardTerminalException {
		//TODO: Should check on CardService if used card is known
		if (true)
			return new CardType(WRAPPER_CARDTYPE);
		else 
			return CardType.UNSUPPORTED;
	}

	@Override
	protected Enumeration getClasses(CardType arg0) {
		if (arg0.getType() == WRAPPER_CARDTYPE)
			return Collections.enumeration(getServiceClasses());
		else 
			//TODO: Throw exception
			return null; //
	}
	
	private final static int WRAPPER_CARDTYPE = 0;
	
	private static List<Class> getServiceClasses() {
		Class[] helper = {GeneralWrapper.class};
		return Arrays.asList(helper);
	}
}
