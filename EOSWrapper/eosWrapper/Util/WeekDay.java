package eosWrapper.Util;

import java.util.Date;
import java.util.Calendar;

public enum WeekDay {
	SUNDAY,
	MONDAY, 
	TUESDAY, 
	WEDNESDAY,
    THURSDAY, 
    FRIDAY, 
    SATURDAY;

	public static WeekDay getWeekDay(Date date) {
		 Calendar calendar = Calendar.getInstance();
		 calendar.setTime(date);
		 return WeekDay.values()[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	 }
}
