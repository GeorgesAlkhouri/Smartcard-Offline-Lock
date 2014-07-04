package eosWrapper.Identity;

public class GuestIdentity implements IIdentity {

	private final String token;
	
	public GuestIdentity(String token) {
		this.token = token;
	}
	
	public String getToken() {
		return this.token;
	}
}
