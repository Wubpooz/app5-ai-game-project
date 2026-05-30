package algorithms.search;

import algorithms.GameAlgorithm;
import algorithms.TimeManager;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;
import interfaces.IHeuristic;

import java.util.List;
import java.util.logging.Logger;

/**
 * Alpha-Beta with Killer Moves + History Heuristic move ordering.
 *
 * <p>Identical logic to {@link AlphaBeta}, but before iterating children at each
 * internal node the move list is sorted using {@link MoveOrderer}:
 * <ul>
 *   <li><b>Killer moves</b>: up to 2 moves per depth that previously caused a
 *       beta cutoff are tried first (score = KILLER_SCORE).
 *   <li><b>History heuristic</b>: moves that historically cause cutoffs accumulate
 *       a depth²-weighted score and are sorted accordingly.
 * </ul>
 *
 * <p>Zero extra allocation: {@code possibleMoves()} already returns a fresh
 * {@code ArrayList} on every call, so we sort it in-place without copying.
 */
public class AlphaBetaKH implements GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> {

    private static final Logger LOGGER = Logger.getLogger(AlphaBetaKH.class.getName());
    private static final int DEPTH_MAX_DEFAULT = 4;

    private final PlayerColor playerMaxRole;
    private final PlayerColor playerMinRole;
    private int depthMax = DEPTH_MAX_DEFAULT;
    private final IHeuristic<EscampeBoard, PlayerColor> h;

    /** Per-search move ordering tables (reset each bestMove() call). */
    private final MoveOrderer orderer = new MoveOrderer();

    // --- Statistics ---
    private int nbNodes;
    private int nbLeaves;
    private int nbPruned;

    // --------- Constructors ---------
    public AlphaBetaKH(PlayerColor playerMaxRole, PlayerColor playerMinRole,
                       IHeuristic<EscampeBoard, PlayerColor> h) {
        this.playerMaxRole = playerMaxRole;
        this.playerMinRole = playerMinRole;
        this.h = h;
    }

    public AlphaBetaKH(PlayerColor playerMaxRole, PlayerColor playerMinRole,
                       IHeuristic<EscampeBoard, PlayerColor> h, int depthMax) {
        this(playerMaxRole, playerMinRole, h);
        this.depthMax = depthMax;
    }

    // --------- GameAlgorithm ---------

    @Override
    public EscampeMove bestMove(EscampeBoard board, PlayerColor playerRole, long remainingTimeMs) {
        orderer.reset();
        return alphaBeta(board, playerRole, remainingTimeMs);
    }

    @Override
    public String toString() {
        return "AlphaBeta+KH(ProfMax=" + depthMax + ")";
    }

    public int getNbNodes()  { return nbNodes; }
    public int getNbLeaves() { return nbLeaves; }
    public int getNbPruned() { return nbPruned; }

    public void resetStatistics() { nbNodes = 0; nbLeaves = 0; nbPruned = 0; }

    // --------- Private search ---------

    private EscampeMove alphaBeta(EscampeBoard board, PlayerColor playerRole, long remainingTimeMs) {
        List<EscampeMove> moves = board.possibleMoves(playerRole);
        if (moves == null || moves.isEmpty()) return null;

        EscampeMove bestMove = moves.get(0);
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta  = Integer.MAX_VALUE;

        TimeManager timeManager = new TimeManager(remainingTimeMs, moves.size());

        // possibleMoves() returns a fresh ArrayList — sort in-place, no copy needed.
        orderer.sort(moves, depthMax);

        for (EscampeMove move : moves) {
            if (timeManager.shouldStopSoft()) break;
            board.play(move, playerRole);
            int value = minValue(board, playerMinRole, depthMax - 1, alpha, beta, timeManager);
            board.undo(move, playerRole);
            if (value > bestValue) { bestValue = value; bestMove = move; }
            alpha = Math.max(alpha, bestValue);
        }
        return bestMove;
    }

    private int maxValue(EscampeBoard board, PlayerColor playerRole, int depth,
                         int alpha, int beta, TimeManager timeManager) {
        if (timeManager.shouldStopHard()) return h.eval(board, playerMaxRole);
        if (board.isGameOver() || depth == 0) { nbLeaves++; return h.eval(board, playerMaxRole); }

        // possibleMoves() returns a fresh ArrayList — sort in-place.
        List<EscampeMove> moves = board.possibleMoves(playerRole);
        orderer.sort(moves, depth);

        int value = Integer.MIN_VALUE;
        for (EscampeMove move : moves) {
            board.play(move, playerRole);
            value = Math.max(value, minValue(board, playerMinRole, depth - 1, alpha, beta, timeManager));
            board.undo(move, playerRole);
            alpha = Math.max(alpha, value);
            if (alpha >= beta) {
                nbPruned++;
                orderer.recordKiller(move, depth);
                orderer.recordCutoff(move, depth);
                break;
            }
        }
        nbNodes++;
        return value;
    }

    private int minValue(EscampeBoard board, PlayerColor playerRole, int depth,
                         int alpha, int beta, TimeManager timeManager) {
        if (timeManager.shouldStopHard()) return h.eval(board, playerMaxRole);
        if (board.isGameOver() || depth == 0) { nbLeaves++; return h.eval(board, playerMaxRole); }

        // possibleMoves() returns a fresh ArrayList — sort in-place.
        List<EscampeMove> moves = board.possibleMoves(playerRole);
        orderer.sort(moves, depth);

        int value = Integer.MAX_VALUE;
        for (EscampeMove move : moves) {
            board.play(move, playerRole);
            value = Math.min(value, maxValue(board, playerMaxRole, depth - 1, alpha, beta, timeManager));
            board.undo(move, playerRole);
            beta = Math.min(beta, value);
            if (alpha >= beta) {
                nbPruned++;
                orderer.recordKiller(move, depth);
                orderer.recordCutoff(move, depth);
                break;
            }
        }
        nbNodes++;
        return value;
    }
}
