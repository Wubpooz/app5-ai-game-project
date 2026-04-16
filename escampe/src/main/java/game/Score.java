package game;

import interfaces.IRole;

/**
 * class used to describe the score corresponding to each player role when the game is over
 */
public class Score<R extends IRole> {
	/**
	 * 
	 */
  public enum Status {WIN,LOOSE,TIE}

  /**
   * 
   */
  private R role;

	/**
	 * 
	 */
	private Status status;

	/**
	 * score can be just 1/0 or a real score depending on the game
	 */
	private int score;

	// ----------- Constructors ------------
	public Score(R role, Status status, int score) {
		super();
		this.role = role;
		this.status = status;
		this.score = score;
	}

	// ----------- Getter / Setters  ------------
	public R getRole() {
		return role;
	}

	public Status getStatus() {
		return status;
	}

	public int getScore() {
		return score;
	}
	
	// ----------- Other public methods  ------------
	public String toString() {
		return "Score <" + role + "," + status + "," + score + ">";
	}
	
}