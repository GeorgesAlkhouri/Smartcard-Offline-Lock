package eosWrapper.Identity;

public class AdminIdentity implements IIdentity {

	private final String token;
	
	public AdminIdentity(String token) {
		this.token = token;
	}
	
	public String getToken() {
		return this.token;
	}
}
