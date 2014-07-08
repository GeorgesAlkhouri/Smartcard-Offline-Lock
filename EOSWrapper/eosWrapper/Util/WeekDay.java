package eosWrapper.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
	
	public int convertWeekDay() {
		return convertWeekDay(this);
	}
	
	public static int convertWeekDay(WeekDay day) {																						
		if (day == SUNDAY)
			return (int)Math.pow(2,1);
		else
			return (int)Math.pow(2,values().length - day.ordinal() + 1);
	}
	
	public static int convertWeekDays(List<WeekDay> days) {
		int bitMask = 0;
		for (WeekDay day : days) {
			bitMask += convertWeekDay(day);
		}
		return bitMask;
	}
}
