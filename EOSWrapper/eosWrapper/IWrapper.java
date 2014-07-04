package eosWrapper;

import java.util.List;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;

public interface IWrapper {
	boolean shouldOpen(IAccessItem item);
	boolean shouldOpen(IIdentity id, WeekDay day);
	boolean shouldOpen(WeekDay day);
	
	void createAccessItem(IAccessItem item);
	void createAccessItems(List<IAccessItem> items);
	
	List<IIdentity> getAllIdentities();
	List<IAccessItem> getAllAccessItems();

	void updateAccessItem(IAccessItem item);
	
	void removeAccessItem(IAccessItem item);
	void removeAccessItems(List<IAccessItem> items);
}