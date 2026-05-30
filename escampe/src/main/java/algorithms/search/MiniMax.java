package algorithms.search;

import algorithms.GameAlgorithm;
import interfaces.IHeuristic;
import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;
import java.util.logging.Logger;
import algorithms.TimeManager;

public class MiniMax<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> implements GameAlgorithm<M,R,B> {

  private static final Logger LOGGER = Logger.getLogger(MiniMax.class.getName());

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

  /** number of internal visited (developed) nodes (for stats) */
  private int nbNodes;

  /** number of leaves nodes (for stats) */
  private int nbLeaves;

  // --------- Constructors ---------
  public MiniMax(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h) {
    this.playerMaxRole = playerMaxRole;
    this.playerMinRole = playerMinRole;
    this.h = h;
  }

  public MiniMax(R playerMaxRole, R playerMinRole, IHeuristic<B, R> h, int depthMax) {
    this(playerMaxRole, playerMinRole, h);
    this.depthMax = depthMax;
  }

  /*
    * IAlgo METHODS =============
    */

  @Override
  public M bestMove(B board, R playerRole, long remainingTimeMs) {
    LOGGER.info("[MiniMax]");
    return minimax(board, playerRole, remainingTimeMs);
  }

  /*
    * PUBLIC METHODS ==============
    */

  @Override
  public String toString() {
    return "MiniMax(ProfMax=" + depthMax + ")";
  }

  public int getNbNodes() {
    return nbNodes;
  }

  public int getNbLeaves() {
    return nbLeaves;
  }

  /**
   * Reset statistics counters for a new game
   */
  public void resetStatistics() {
    nbNodes = 0;
    nbLeaves = 0;
  }

  /*
    * PRIVATE METHODS ===============
    */
  private M minimax(B board, R playerRole, long remainingTimeMs) {
    M bestMove = null;
    int bestValue = Integer.MIN_VALUE;

    bestMove = board.possibleMoves(playerRole).get(0);
    TimeManager timeManager = new TimeManager(remainingTimeMs, board.possibleMoves(playerRole).size());

    for (M move : board.possibleMoves(playerRole)) {
      if(timeManager.shouldStopSoft()) {
        break;
      }
      board.play(move, playerRole);
      int value = minValue(board, playerMinRole, depthMax - 1, timeManager);
      board.undo(move, playerRole);
      if (value > bestValue || bestMove == null) {
        bestValue = value;
        bestMove = move;
      }
    }
    return bestMove;
  }

  private int maxValue(B board, R playerRole, int depth, TimeManager timeManager) {
    if (timeManager.shouldStopHard()) {
      return h.eval(board, playerMaxRole);
    }
    if (board.isGameOver() || depth == 0) {
      nbLeaves++;
      return h.eval(board, playerMaxRole);
    }

    int value = Integer.MIN_VALUE;
    for (M move : board.possibleMoves(playerRole)) {
      board.play(move, playerRole);
      value = Math.max(value, minValue(board, playerMinRole, depth - 1, timeManager));
      board.undo(move, playerRole);
    }
    nbNodes++;
    return value;
  }

  private int minValue(B board, R playerRole, int depth, TimeManager timeManager) {
    if (timeManager.shouldStopHard()) {
      return h.eval(board, playerMaxRole);
    }
    if (board.isGameOver() || depth == 0) {
      nbLeaves++;
      return h.eval(board, playerMaxRole);
    }

    int value = Integer.MAX_VALUE;
    for (M move : board.possibleMoves(playerRole)) {
      board.play(move, playerRole);
      value = Math.min(value, maxValue(board, playerMaxRole, depth - 1, timeManager));
      board.undo(move, playerRole);
    }
    nbNodes++;
    return value;
  }
}
