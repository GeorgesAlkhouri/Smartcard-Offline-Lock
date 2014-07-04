package eosWrapper;

import java.util.List;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class GeneralWrapper implements IWrapper {

	public boolean shouldOpen(IAccessItem item) {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}

	public boolean shouldOpen(IIdentity id, WeekDay day) {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}

	public boolean shouldOpen(WeekDay day) {
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}

	public void createAccessItem(IAccessItem item) {
		// TODO Automatisch erstellter Methoden-Stub

	}

	public void createAccessItems(List<IAccessItem> items) {
		// TODO Automatisch erstellter Methoden-Stub

	}

	public List<IIdentity> getAllIdentities() {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}

	public List<IAccessItem> getAllAccessItems() {
		// TODO Automatisch erstellter Methoden-Stub
		return null;
	}

	public void updateAccessItem(IAccessItem item) {
		// TODO Automatisch erstellter Methoden-Stub

	}

	public void removeAccessItem(IAccessItem item) {
		// TODO Automatisch erstellter Methoden-Stub

	}

	public void removeAccessItems(List<IAccessItem> items) {
		// TODO Automatisch erstellter Methoden-Stub

	}

}
