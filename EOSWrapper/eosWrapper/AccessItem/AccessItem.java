package eosWrapper.AccessItem;

import java.util.List;

import eosWrapper.WeekDay;
import eosWrapper.Identity.IIdentity;

public class AccessItem implements IAccessItem {

	private final IIdentity id;
	private final List<WeekDay> days;
	
	public AccessItem(IIdentity id, List<WeekDay> days) {
		this.id = id;
		this.days = days;
	}
	
	public IIdentity getIdentity() {
		return this.id;
	}

	public List<WeekDay> getWeekDays() {
		return this.days;
	}

}
