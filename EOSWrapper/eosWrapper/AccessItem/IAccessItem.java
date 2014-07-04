package eosWrapper.AccessItem;

import java.util.List;

import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public interface IAccessItem {
	IIdentity getIdentity();
	List<WeekDay> getWeekDays();
}
