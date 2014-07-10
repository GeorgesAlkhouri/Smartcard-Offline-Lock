package eosWrapper.Util;

import java.util.ArrayList;
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

	/**
	 * Converts a give date to an enum representation
	 * of WeekDay.
	 * 
	 * @param date
	 * @return
	 */
	public static WeekDay getWeekDay(Date date) {
		 Calendar calendar = Calendar.getInstance();
		 calendar.setTime(date);
		 return WeekDay.values()[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	 }
	
	/**
	 * See
	 * 
	 * public static int shiftWeekDay(WeekDay day)
	 * 
	 * @return
	 */
	public int shiftWeekDay() {
		return  shiftWeekDay(this);
	}
	
	/**
	 * See
	 * 
	 * public static int convertWeekDay(WeekDay day)
	 * 
	 * @return
	 */
	public int convertWeekDay() {
		return convertWeekDay(this);
	}
	
	/**
	 * Maps the the order (01) Sunday - (07) Saturday to
	 * the order (01) Monday - (07) Sunday.
	 * 
	 * @param day
	 * @return
	 */
	public static int shiftWeekDay(WeekDay day) {
		if (day == SUNDAY)
			return 7;
		else
			return day.ordinal();
	}
	

	/**
	 * Converts a week day into his byte representation.
	 * 
	 * Example:
	 * 00000010
	 * 
	 * Represents the week day monday.
	 * 
	 * @param day
	 * @return
	 */
	public static int convertWeekDay(WeekDay day) {																						
		if (day == SUNDAY)
			return (int)Math.pow(2,1);
		else
			return (int)Math.pow(2,values().length - day.ordinal() + 1);
	}
	

	/**
	 * Converts a list of week days to a bitmask representation in
	 * the order Sunday - Monday.
	 * 
	 * Example:
	 * 10100000
	 * 
	 * Represents a list of week days containing Sunday and
	 * Friday.
	 * 
	 * @param days
	 * @return
	 */
	public static int convertWeekDays(List<WeekDay> days) {
		int bitMask = 0;
		for (WeekDay day : days) {
			bitMask += convertWeekDay(day);
		}
		return bitMask;
	}
	
	
	/**
	 * Looks every bit position of the given byte and 
	 * translats it to a week day. (Ignoring the first byte,
	 * because it is not needed)
	 * 
	 * @param bitMask
	 * @return
	 */
	public static List<WeekDay> convertWeekDays(int bitMask) {
		ArrayList<WeekDay> days = new ArrayList<WeekDay>();
	
		// value = ((byte)bitMask) & (0x01 << position);
		if ((((byte)bitMask) & (0x01 << 7)) > 0)
			days.add(WeekDay.MONDAY);
		if ((((byte)bitMask) & (0x01 << 6)) > 0)
			days.add(WeekDay.TUESDAY);
		if ((((byte)bitMask) & (0x01 << 5)) > 0)
			days.add(WeekDay.WEDNESDAY);
		if ((((byte)bitMask) & (0x01 << 4)) > 0)
			days.add(WeekDay.THURSDAY);
		if ((((byte)bitMask) & (0x01 << 3)) > 0)
			days.add(WeekDay.FRIDAY);
		if ((((byte)bitMask) & (0x01 << 2)) > 0)
			days.add(WeekDay.SATURDAY);
		if ((((byte)bitMask) & (0x01 << 1)) > 0)
			days.add(WeekDay.SUNDAY);
		
		return days;
	}
}
