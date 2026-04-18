package game;

import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;

public class AIPlayer<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> extends Player<R> {

	private algorithms.GameAlgorithm<M,R,B> ai;

	public void setAi(algorithms.GameAlgorithm<M, R, B> ai) {
		this.ai = ai;
	}

	public AIPlayer(R role) {
		super(role);
	}

	public AIPlayer(R role, algorithms.GameAlgorithm<M,R,B> alg) {
		super(role);
		this.ai = alg;
	}

	public M bestMove(B board) {
		return ai.bestMove(board,this.getRole());
	}

	public B playMove(B board, M move) {
		return board.play(move, this.getRole());
	}
}
