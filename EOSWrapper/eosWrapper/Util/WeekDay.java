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
	
	public static byte convertWeekDay(WeekDay day) {																						
		if (day == SUNDAY)
			return (byte)Math.pow(2,1);
		else
			return (byte)Math.pow(2,values().length - day.ordinal() + 1);
	}
}
