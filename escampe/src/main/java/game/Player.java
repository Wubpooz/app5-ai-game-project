package game;

import interfaces.IRole;

/**
 * used to associate an real player identifier to a role
 *
 * @param <Role>
 */


public class Player<R extends IRole> {
	
	// ----------- Attributes  ------------
	
	/**
	 * the role of the player in the game
	 */
	R role;
	/**
	 * An (optional) identifier characterizing the player having that role in the game
	 * This is useful for instance in a tournament, for keeping tra
	 */
	String id;

	// ----------- Getters / Setters  ------------
	
	public String getId() {
		return id;
	}

	public R getRole() {
		return role;
	}

	public void setRole(R role) {
		this.role = role;
			}
	// ----------- Constructors ------------

	public Player(R role) {
			this.role = role;
			this.id = "";
}
			
	public Player(R role, String id) {
		this(role);
		this.id = id;
	}
	
	// ----------- Other Methods ------------
	public String toString() {
		return "" + role + ( id.isEmpty() ? "" : " (" + id + ")");
	}
	

}
