package algorithms.search;

import algorithms.GameAlgorithm;
import interfaces.IHeuristic;
import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;
import java.util.logging.Logger;
import algorithms.TimeManager;
import algorithms.evaluation.Heuristic;

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
		java.util.List<M> moves = board.possibleMoves(playerRole);
		if (moves == null || moves.isEmpty()) {
			return null;
		}

		// Default to first move
		M bestMove = moves.get(0);
		M lastCompletedBest = bestMove;

		TimeManager timeManager = new TimeManager(remainingTimeMs, moves.size());

		R opponentRole = (playerRole.equals(playerMaxRole)) ? playerMinRole : playerMaxRole;

		// Iterative deepening: increase search depth from 1..depthMax
		for (int currentDepth = 1; currentDepth <= depthMax; currentDepth++) {
			if (timeManager.shouldStopSoft()) {
				break; // return last completed iteration's best move
			}

			int alpha = Integer.MIN_VALUE;
			int beta = Integer.MAX_VALUE;
			int bestValue = Integer.MIN_VALUE;
			M iterBest = bestMove;

			for (M move : moves) {
				if (timeManager.shouldStopSoft()) {
					break; // stop this iteration early
				}
				board.play(move, playerRole);
				int value = -maxValue(board, opponentRole, currentDepth - 1, -beta, -alpha, timeManager);
				board.undo(move, playerRole);
				if (value > bestValue || iterBest == null) {
					bestValue = value;
					iterBest = move;
				}
				alpha = Math.max(alpha, bestValue);
			}

			// If we completed the iteration without hitting soft stop, update lastCompletedBest
			if (iterBest != null) {
				lastCompletedBest = iterBest;
				bestMove = iterBest;
				System.out.println("Depth=" + currentDepth + ", bestMove=" + bestMove + ", lastCompletedBest=" + lastCompletedBest + ", bestValue=" + bestValue);
			}
			if (bestValue == Heuristic.WIN_SCORE) {
				// Found a winning move, no need to search deeper
				break;
			}
		}

		return lastCompletedBest;
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
			board.play(move, playerRole);
			value = Math.max(value, -maxValue(board, opponentRole, depth - 1, -beta, -alpha, timeManager));
			board.undo(move, playerRole);
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
