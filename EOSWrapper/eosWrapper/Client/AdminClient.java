package eosWrapper.Client;

import opencard.core.service.CardServiceException;
import eosWrapper.GeneralWrapper;
import eosWrapper.Environment.IEnvironment;
import eosWrapper.Identity.IIdentity;

public class AdminClient implements IClient {

	private final IIdentity id;
	public AdminClient(IIdentity id) {
		this.id = id;
	}
	
	public void open(IEnvironment env) {
		try {
			GeneralWrapper service = (GeneralWrapper)env.getSmartCard().getCardService(GeneralWrapper.class,true);
			service.shouldOpen(this.id,env.getWeekDay());
		} catch (CardServiceException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Automatisch erstellter Catch-Block
			e.printStackTrace();
		}
	}

	public IIdentity getIdentity() {
		return this.id;
	}

}
