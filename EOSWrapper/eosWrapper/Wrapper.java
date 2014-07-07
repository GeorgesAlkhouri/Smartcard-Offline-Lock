package eosWrapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import opencard.core.service.CardService;

import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public abstract class Wrapper extends CardService {	

	protected final static byte[] INS_SHOULD_OPEN = {(byte)0xC0,(byte)0x10,(byte)0x00,(byte)0x00,(byte)0x20};
	protected final static byte[] INS_PUT_ACCESS_ITEM = {(byte)0xC0,(byte)0x30,(byte)0x00,(byte)0x00,(byte)0x41};
	protected final static byte[] INS_REMOVE_ACCESS_ITEM = {(byte)0xC0,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0x40};
	protected final static byte[] INS_GET_WEEKDAYS = {(byte)0xC0,(byte)0x50,(byte)0x00,(byte)0x00,(byte)0x20};
	
	public abstract boolean shouldOpen(IAccessItem item, WeekDay day) throws Exception;
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