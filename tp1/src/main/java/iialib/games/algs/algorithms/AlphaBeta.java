package iialib.games.algs.algorithms;

import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.IHeuristic;
import iialib.games.model.IBoard;
import iialib.games.model.IMove;
import iialib.games.model.IRole;
import java.util.logging.Logger;

/**
 * Alpha-Beta Pruning Algorithm - optimized version of MiniMax
 * Eliminates branches that cannot affect the final decision
 */
public class AlphaBeta<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> 
    implements GameAlgorithm<M,R,B> {

	private static final Logger LOGGER = Logger.getLogger(AlphaBeta.class.getName());

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
	public AlphaBeta(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h) {
		this.playerMaxRole = playerMaxRole;
		this.playerMinRole = playerMinRole;
		this.h = h;
	}

	public AlphaBeta(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h, int depthMax) {
		this(playerMaxRole, playerMinRole, h);
		this.depthMax = depthMax;
	}

	/*
	 * IAlgo METHODS =============
	 */

	@Override
	public M bestMove(B board, R playerRole) {
		LOGGER.info("[AlphaBeta]");
		return alphaBeta(board, playerRole);
	}

	/*
	 * PUBLIC METHODS ==============
	 */

	@Override
	public String toString() {
		return "AlphaBeta(ProfMax=" + depthMax + ")";
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
	private M alphaBeta(B board, R playerRole) {
		M bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		
		for (M move : board.possibleMoves(playerRole)) {
			B newBoard = board.play(move, playerRole);
			int value = minValue(newBoard, playerMinRole, depthMax - 1, alpha, beta);
			if (value > bestValue) {
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
	private int maxValue(B board, R playerRole, int depth, int alpha, int beta) {
		// Base case: game over or depth limit reached
		if (board.isGameOver() || depth == 0) {
			nbLeaves++;
			return h.eval(board, playerMaxRole);
		}
		
		int value = Integer.MIN_VALUE;
		for (M move : board.possibleMoves(playerRole)) {
			B newBoard = board.play(move, playerRole);
			value = Math.max(value, minValue(newBoard, playerMinRole, depth - 1, alpha, beta));
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

	/**
	 * Minimizing player node
	 * Returns the minimum value among all child nodes
	 * Prunes branches when alpha >= beta
	 */
	private int minValue(B board, R playerRole, int depth, int alpha, int beta) {
		// Base case: game over or depth limit reached
		if (board.isGameOver() || depth == 0) {
			nbLeaves++;
			return h.eval(board, playerMaxRole);
		}
		
		int value = Integer.MAX_VALUE;
		for (M move : board.possibleMoves(playerRole)) {
			B newBoard = board.play(move, playerRole);
			value = Math.min(value, maxValue(newBoard, playerMaxRole, depth - 1, alpha, beta));
			beta = Math.min(beta, value);
			
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
