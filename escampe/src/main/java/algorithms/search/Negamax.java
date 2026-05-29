package algorithms.search;

import algorithms.GameAlgorithm;
import interfaces.IHeuristic;
import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;
import java.util.logging.Logger;
import algorithms.TimeManager;

/**
 * Negamax : max(a, b) == -min(-a, -b)
 * optimized version of Alpha-Beta, use only maxValue
 */
public class Negamax<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> implements GameAlgorithm<M,R,B> {

	private static final Logger LOGGER = Logger.getLogger(Negamax.class.getName());

	// Constants
	/** Default value for depth limit */
	private static final int DEPTH_MAX_DEFAULT = 4;

	// Attributes
	/** Role of the max player */
	private final R playerMaxRole;

	/** Role of the min player */
	private final R playerMinRole;

	/** Algorithm max depth */
	private int depthMax = DEPTH_MAX_DEFAULT;

	/** Heuristic used by the max player */
	private IHeuristic<B, R> h;

	// Statistics
	/** number of internal visited (developed) nodes (for stats) */
	private int nbNodes;

	/** number of leaves nodes (for stats) */
	private int nbLeaves;

	/** number of pruned nodes (for stats) */
	private int nbPruned;

	// --------- Constructors ---------
	public Negamax(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h) {
		this.playerMaxRole = playerMaxRole;
		this.playerMinRole = playerMinRole;
		this.h = h;
	}

	public Negamax(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h, int depthMax) {
		this(playerMaxRole, playerMinRole, h);
		this.depthMax = depthMax;
	}

	/*
	 * IAlgo METHODS =============
	 */

	@Override
	public M bestMove(B board, R playerRole, long remainingTimeMs) {
		LOGGER.info("[Negamax]");
		return negamax(board, playerRole, remainingTimeMs);
	}

	// /**
	//  * Evaluate the board state from the perspective of playerMaxRole using Negamax search.
	//  */
	// public int evaluate(B board, int depth) {
	// 	return maxValue(board, playerMaxRole, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
	// }

	/*
	 * PUBLIC METHODS ==============
	 */

	@Override
	public String toString() {
		return "Negamax(ProfMax=" + depthMax + ")";
	}

	public int getNbNodes() {
		return nbNodes;
	}

	public int getNbLeaves() {
		return nbLeaves;
	}

	public int getNbPruned() {
		return nbPruned;
	}

	/**
	 * Reset statistics counters for a new game
	 */
	public void resetStatistics() {
		nbNodes = 0;
		nbLeaves = 0;
		nbPruned = 0;
	}

	/*
	 * PRIVATE METHODS ===============
	 */

	/**
	 * Root level search - tries all possible moves and returns the best one
	 */
	private M negamax(B board, R playerRole, long remainingTimeMs) {
		M bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;

		bestMove = board.possibleMoves(playerRole).get(0); // Default to first move if no better move is found
		TimeManager timeManager = new TimeManager(remainingTimeMs, board.possibleMoves(playerRole).size());

        R opponentRole = (playerRole.equals(playerMaxRole)) ? playerMinRole : playerMaxRole;
		for (M move : board.possibleMoves(playerRole)) {
			if (timeManager.shouldStopSoft()) {
				// LOGGER.info("Soft time limit reached, returning best move found so far.");
				break;
			}
			B nextBoard = board.copy();
			nextBoard.play(move, playerRole);
			int value = -maxValue(nextBoard, opponentRole, depthMax - 1, -beta, -alpha, timeManager);
			if (value > bestValue || bestMove == null) {
				bestValue = value;
				bestMove = move;
			}
			alpha = Math.max(alpha, bestValue);
		}
		return bestMove;
	}

	/**
	 * Maximizing player node
	 * Returns the maximum value among all child nodes
	 * Prunes branches when alpha >= beta
	 */
	private int maxValue(B board, R playerRole, int depth, int alpha, int beta, TimeManager timeManager) {

		if (timeManager.shouldStopHard()) {
			// LOGGER.info("Hard time limit reached, returning heuristic evaluation.");
			return h.eval(board, playerRole);
		}
		// Base case: game over or depth limit reached
		if (board.isGameOver() || depth == 0) {
			nbLeaves++;
			return h.eval(board, playerRole);
		}

		int value = Integer.MIN_VALUE;
        R opponentRole = (playerRole.equals(playerMaxRole)) ? playerMinRole : playerMaxRole;
		for (M move : board.possibleMoves(playerRole)) {
			B nextBoard = board.copy();
			nextBoard.play(move, playerRole);
			value = Math.max(value, -maxValue(nextBoard, opponentRole, depth - 1, -beta, -alpha, timeManager));
			alpha = Math.max(alpha, value);

			// Alpha-Beta Pruning: if alpha >= beta, we can prune remaining branches
			if (alpha >= beta) {
				nbPruned++;
				break;
			}
		}
		nbNodes++;
		return value;
	}
}
