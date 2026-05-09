package game;

import interfaces.IRole;

public class Player<R extends IRole> {
	R role;
	String id; // (optional)

	public Player(R role) {
    this.role = role;
    this.id = "";
  }
		
	public Player(R role, String id) {
		this(role);
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public R getRole() {
		return role;
	}

	public void setRole(R role) {
		this.role = role;
	}

	public String toString() {
		return "" + role + ( id.isEmpty() ? "" : " (" + id + ")");
	}
}
