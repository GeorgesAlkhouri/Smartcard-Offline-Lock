package eosWrapper.AccessItem;

import java.util.List;

import eosWrapper.WeekDay;
import eosWrapper.Identity.IIdentity;

public interface IAccessItem {
	IIdentity getIdentity();
	List<WeekDay> getWeekDays();
}
