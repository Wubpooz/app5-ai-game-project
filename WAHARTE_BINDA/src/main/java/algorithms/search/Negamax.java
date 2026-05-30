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

	/**
	 * Safe infinity for Negamax alpha-beta bounds.
	 * Must be larger than any heuristic value (WIN_SCORE = 100_000)
	 * AND satisfy: -NEGAMAX_INF > Integer.MIN_VALUE (no overflow when negated).
	 * 1_000_000_000 negated = -1_000_000_000, well within int range.
	 */
	private static final int NEGAMAX_INF = 1_000_000_000;

	// Attributes
	/** Role of the max player */
	private final R playerMaxRole;

	/** Role of the min player */
	private final R playerMinRole;

	/** Algorithm max depth */
	private int depthMax = DEPTH_MAX_DEFAULT;

	/**
	 * When true (default), uses iterative deepening from depth 1..depthMax,
	 * returning the best move found within the time budget.
	 * When false, goes straight to depthMax (useful for fair tournament comparisons
	 * against fixed-depth AlphaBeta).
	 */
	private boolean iterativeDeepening = true;

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

	/**
	 * Full constructor with iterative deepening control.
	 * @param iterativeDeepening false to disable ID (for fair fixed-depth tournament comparisons)
	 */
	public Negamax(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h, int depthMax, boolean iterativeDeepening) {
		this(playerMaxRole, playerMinRole, h, depthMax);
		this.iterativeDeepening = iterativeDeepening;
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
	 * Root level search. When iterativeDeepening is true (default), searches from
	 * depth 1..depthMax and returns the best move found within the time budget.
	 * When false, goes directly to depthMax (for fair tournament comparisons).
	 */
	private M negamax(B board, R playerRole, long remainingTimeMs) {
		java.util.List<M> moves = board.possibleMoves(playerRole);
		if (moves == null || moves.isEmpty()) {
			return null;
		}

		M bestMove = moves.get(0);
		TimeManager timeManager = new TimeManager(remainingTimeMs, moves.size());
		int sign = playerRole.equals(playerMaxRole) ? 1 : -1;
		R opponentRole = playerRole.equals(playerMaxRole) ? playerMinRole : playerMaxRole;

		if (iterativeDeepening) {
			// --- Iterative deepening mode (default, used in real games) ---
			M lastCompletedBest = bestMove;
			for (int currentDepth = 1; currentDepth <= depthMax; currentDepth++) {
				if (timeManager.shouldStopSoft()) break;

				int alpha = -NEGAMAX_INF;
				int beta = NEGAMAX_INF;
				int bestValue = -NEGAMAX_INF;
				M iterBest = bestMove;

				for (M move : moves) {
					if (timeManager.shouldStopSoft()) break;
					board.play(move, playerRole);
					int value = -maxValue(board, opponentRole, -sign, currentDepth - 1, -beta, -alpha, timeManager);
					board.undo(move, playerRole);
					if (value > bestValue) {
						bestValue = value;
						iterBest = move;
					}
					alpha = Math.max(alpha, bestValue);
				}
				lastCompletedBest = iterBest;
				bestMove = iterBest;
				if (bestValue == Heuristic.WIN_SCORE) break;
			}
			return lastCompletedBest;

		} else {
			// --- Fixed-depth mode (for fair tournament comparisons) ---
			int alpha = -NEGAMAX_INF;
			int beta = NEGAMAX_INF;
			int bestValue = -NEGAMAX_INF;

			for (M move : moves) {
				if (timeManager.shouldStopSoft()) break;
				board.play(move, playerRole);
				int value = -maxValue(board, opponentRole, -sign, depthMax - 1, -beta, -alpha, timeManager);
				board.undo(move, playerRole);
				if (value > bestValue) {
					bestValue = value;
					bestMove = move;
				}
				alpha = Math.max(alpha, bestValue);
			}
			return bestMove;
		}
	}

	/**
	 * Negamax recursive node.
	 * @param sign +1 if it's playerMaxRole's turn, -1 if it's playerMinRole's turn.
	 *             This lets us evaluate always from playerMaxRole's perspective and negate correctly.
	 */
	private int maxValue(B board, R playerRole, int sign, int depth, int alpha, int beta, TimeManager timeManager) {

		if (timeManager.shouldStopHard()) {
			return sign * h.eval(board, playerMaxRole);
		}
		// Base case: game over or depth limit reached
		if (board.isGameOver() || depth == 0) {
			nbLeaves++;
			return sign * h.eval(board, playerMaxRole);
		}

		int value = -NEGAMAX_INF;
		R opponentRole = playerRole.equals(playerMaxRole) ? playerMinRole : playerMaxRole;
		for (M move : board.possibleMoves(playerRole)) {
			board.play(move, playerRole);
			value = Math.max(value, -maxValue(board, opponentRole, -sign, depth - 1, -beta, -alpha, timeManager));
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
