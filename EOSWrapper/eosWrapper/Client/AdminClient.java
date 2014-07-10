package eosWrapper.Client;

import java.util.Arrays;
import java.util.List;

import eosWrapper.GeneralWrapper;
import eosWrapper.AccessItem.AccessItem;
import eosWrapper.AccessItem.IAccessItem;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.GuestIdentity;
import eosWrapper.Identity.IIdentity;
import eosWrapper.Util.WeekDay;

public class AdminClient implements IClient {

	private final IIdentity id;
	public AdminClient(IIdentity id) {
		this.id = id;
	}
	
	public void run(IEnvironment env) {
		GeneralWrapper service;
		try {
			service = (GeneralWrapper)env.getSmartCard().getCardService(GeneralWrapper.class,true);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		
		IIdentity guest = new GuestIdentity("AAAAAAAA");
		IIdentity anotherGuest = new GuestIdentity("BBBBBBBB");
		
		service.selectApplet();
		
		System.out.println("Create access items");
		
		service.createAccessItem(
				this.id, new AccessItem(guest,
						Arrays.asList(
						new WeekDay[]{WeekDay.FRIDAY,WeekDay.WEDNESDAY,WeekDay.THURSDAY}
				)));
		service.createAccessItem(this.id, new AccessItem(anotherGuest,
						Arrays.asList(
						new WeekDay[]{WeekDay.FRIDAY,WeekDay.WEDNESDAY,WeekDay.THURSDAY}
				)));
		
		
		List<IAccessItem> items = service.getAllAccessItems(this.id);
		for (IAccessItem item : items) {
			System.out.println("ID token: " + item.getIdentity().getToken());
			for (WeekDay day : item.getWeekDays()) {
				System.out.println("Day " + day.toString());
			}
		}
		
		System.out.println("Try to open lock");
		
		boolean result = service.shouldOpen(guest,env.getWeekDay());
		System.out.println(result);
		
		System.out.println("Set global access");
		
		service.setGlobalAcsess(this.id, Arrays.asList(
						new WeekDay[]{WeekDay.FRIDAY,WeekDay.WEDNESDAY,WeekDay.THURSDAY}
				));
		
		System.out.println("Get global access");
		
		List<WeekDay> days = service.getGlobalAccess(this.id);
		if (days != null)
			for (WeekDay day : days)
				System.out.println("Day " + day.toString());
		
		System.out.println("Try to open lock");
		
		result = service.shouldOpen(env.getWeekDay());
		System.out.println(result);
		
		System.out.println("Get week days for identity: " + guest.getToken());
		
		days = service.getWeekdays(this.id,guest);
		if (days != null)
			for (WeekDay day : days) {
				System.out.println(day.toString());
		}
		
		System.out.println("Remove identity " + anotherGuest.getToken());
		
		service.removeAccessItem(this.id,new AccessItem(anotherGuest,
						Arrays.asList(
						new WeekDay[]{WeekDay.FRIDAY,WeekDay.WEDNESDAY,WeekDay.THURSDAY}
				)));
		
		items = service.getAllAccessItems(this.id);
		if (items != null)
			for (IAccessItem item : items) {
				System.out.println("ID token: " + item.getIdentity().getToken());
				for (WeekDay day : item.getWeekDays()) {
					System.out.println("Day " + day.toString());
				}
			}
	}

	public IIdentity getIdentity() {
		return this.id;
	}

}
