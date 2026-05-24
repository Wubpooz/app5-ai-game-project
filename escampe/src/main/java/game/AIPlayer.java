package game;

import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;


public class AIPlayer<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> {
	public final R role;
	private algorithms.GameAlgorithm<M,R,B> ai;

  // Constructors
	public AIPlayer(R role) {
		this.role = role;
	}

	public AIPlayer(R role, algorithms.GameAlgorithm<M,R,B> alg) {
    this(role);
		this.ai = alg;
	}

  // Methods
	public M bestMove(B board) {
		return ai.bestMove(board,this.getRole());
	}

	public B playMove(B board, M move) {
		return board.play(move, this.getRole());
	}


  // Setters and getters
  public void setAi(algorithms.GameAlgorithm<M, R, B> ai) {
		this.ai = ai;
	}

	public R getRole() {
		return role;
	}

  @Override
	public String toString() {
		return "AIPlayer{" +
        "role=" + role +
        '}';
	}
}
