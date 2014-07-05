package eosWrapper.Environment;

import opencard.core.service.SmartCard;
import eosWrapper.Util.WeekDay;

public interface IEnvironment {
	WeekDay getWeekDay();
	SmartCard getSmartCard();
}
