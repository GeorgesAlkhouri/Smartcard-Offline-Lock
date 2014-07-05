package eosWrapper;

import java.util.List;

import opencard.core.service.CardService;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public abstract class Wrapper extends CardService {	
	public abstract boolean shouldOpen(IAccessItem item) throws Exception;
	public abstract boolean shouldOpen(IIdentity id, WeekDay day) throws Exception ;
	public abstract boolean shouldOpen(WeekDay day) throws Exception;
	
	public abstract void createAccessItem(IAccessItem item) throws Exception;
	public abstract void createAccessItems(List<IAccessItem> items) throws Exception;
	
	public abstract List<IIdentity> getAllIdentities() throws Exception;
	public abstract List<IAccessItem> getAllAccessItems() throws Exception;

	public abstract void updateAccessItem(IAccessItem item) throws Exception;
	
	public abstract void removeAccessItem(IAccessItem item) throws Exception;
	public abstract void removeAccessItems(List<IAccessItem> items) throws Exception;
}