package eosWrapper.Environment;

import java.util.Date;

import eosWrapper.Util.WeekDay;

public class TestLock implements IEnvironment {

	public WeekDay getWeekDay() {
		return WeekDay.getWeekDay(new Date());
	}

}
