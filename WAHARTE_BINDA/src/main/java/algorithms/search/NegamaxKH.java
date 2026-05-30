package algorithms.search;

import algorithms.GameAlgorithm;
import algorithms.TimeManager;
import algorithms.evaluation.Heuristic;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;
import interfaces.IHeuristic;

import java.util.List;
import java.util.logging.Logger;

/**
 * Negamax with Killer Moves + History Heuristic move ordering.
 *
 * <p>Combines the fixed-overflow Negamax (NEGAMAX_INF bounds) with the same
 * {@link MoveOrderer} used by {@link AlphaBetaKH}.
 *
 * <p>Zero extra allocation: {@code possibleMoves()} already returns a fresh
 * {@code ArrayList} on every call, so we sort it in-place without copying.
 *
 * <p>The {@code iterativeDeepening} flag: set to {@code false} in the tournament
 * for a fair comparison against fixed-depth AlphaBeta.
 */
public class NegamaxKH implements GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> {

    private static final Logger LOGGER = Logger.getLogger(NegamaxKH.class.getName());
    private static final int DEPTH_MAX_DEFAULT = 4;

    /** Safe infinity — negation never overflows an int. */
    private static final int NEGAMAX_INF = 1_000_000_000;

    private final PlayerColor playerMaxRole;
    private final PlayerColor playerMinRole;
    private int depthMax = DEPTH_MAX_DEFAULT;
    private boolean iterativeDeepening = true;
    private final IHeuristic<EscampeBoard, PlayerColor> h;

    /** Per-search move ordering tables (reset each bestMove() call). */
    private final MoveOrderer orderer = new MoveOrderer();

    // --- Statistics ---
    private int nbNodes;
    private int nbLeaves;
    private int nbPruned;

    // --------- Constructors ---------
    public NegamaxKH(PlayerColor playerMaxRole, PlayerColor playerMinRole,
                     IHeuristic<EscampeBoard, PlayerColor> h) {
        this.playerMaxRole = playerMaxRole;
        this.playerMinRole = playerMinRole;
        this.h = h;
    }

    public NegamaxKH(PlayerColor playerMaxRole, PlayerColor playerMinRole,
                     IHeuristic<EscampeBoard, PlayerColor> h, int depthMax) {
        this(playerMaxRole, playerMinRole, h);
        this.depthMax = depthMax;
    }

    public NegamaxKH(PlayerColor playerMaxRole, PlayerColor playerMinRole,
                     IHeuristic<EscampeBoard, PlayerColor> h, int depthMax, boolean iterativeDeepening) {
        this(playerMaxRole, playerMinRole, h, depthMax);
        this.iterativeDeepening = iterativeDeepening;
    }

    // --------- GameAlgorithm ---------

    @Override
    public EscampeMove bestMove(EscampeBoard board, PlayerColor playerRole, long remainingTimeMs) {
        orderer.reset();
        return negamax(board, playerRole, remainingTimeMs);
    }

    @Override
    public String toString() { return "Negamax+KH(ProfMax=" + depthMax + ")"; }

    public int getNbNodes()  { return nbNodes; }
    public int getNbLeaves() { return nbLeaves; }
    public int getNbPruned() { return nbPruned; }

    public void resetStatistics() { nbNodes = 0; nbLeaves = 0; nbPruned = 0; }

    // --------- Private search ---------

    private EscampeMove negamax(EscampeBoard board, PlayerColor playerRole, long remainingTimeMs) {
        List<EscampeMove> moves = board.possibleMoves(playerRole);
        if (moves == null || moves.isEmpty()) return null;

        EscampeMove bestMove = moves.get(0);
        TimeManager timeManager = new TimeManager(remainingTimeMs, moves.size());
        int sign = playerRole.equals(playerMaxRole) ? 1 : -1;
        PlayerColor opponentRole = playerRole.equals(playerMaxRole) ? playerMinRole : playerMaxRole;

        if (iterativeDeepening) {
            EscampeMove lastCompletedBest = bestMove;
            for (int currentDepth = 1; currentDepth <= depthMax; currentDepth++) {
                if (timeManager.shouldStopSoft()) break;

                int alpha = -NEGAMAX_INF, beta = NEGAMAX_INF, bestValue = -NEGAMAX_INF;
                EscampeMove iterBest = bestMove;

                // possibleMoves() returns a fresh ArrayList — sort in-place.
                List<EscampeMove> ordered = board.possibleMoves(playerRole);
                orderer.sort(ordered, currentDepth);

                for (EscampeMove move : ordered) {
                    if (timeManager.shouldStopSoft()) break;
                    board.play(move, playerRole);
                    int value = -maxValue(board, opponentRole, -sign, currentDepth - 1, -beta, -alpha, timeManager);
                    board.undo(move, playerRole);
                    if (value > bestValue) { bestValue = value; iterBest = move; }
                    alpha = Math.max(alpha, bestValue);
                }
                lastCompletedBest = iterBest;
                bestMove = iterBest;
                if (bestValue >= Heuristic.WIN_SCORE) break;
            }
            return lastCompletedBest;

        } else {
            // Fixed-depth (tournament mode) — sort root moves in-place.
            orderer.sort(moves, depthMax);
            int alpha = -NEGAMAX_INF, beta = NEGAMAX_INF, bestValue = -NEGAMAX_INF;

            for (EscampeMove move : moves) {
                if (timeManager.shouldStopSoft()) break;
                board.play(move, playerRole);
                int value = -maxValue(board, opponentRole, -sign, depthMax - 1, -beta, -alpha, timeManager);
                board.undo(move, playerRole);
                if (value > bestValue) { bestValue = value; bestMove = move; }
                alpha = Math.max(alpha, bestValue);
            }
            return bestMove;
        }
    }

    private int maxValue(EscampeBoard board, PlayerColor playerRole, int sign, int depth,
                         int alpha, int beta, TimeManager timeManager) {
        if (timeManager.shouldStopHard()) return sign * h.eval(board, playerMaxRole);
        if (board.isGameOver() || depth == 0) {
            nbLeaves++;
            return sign * h.eval(board, playerMaxRole);
        }

        // possibleMoves() returns a fresh ArrayList — sort in-place.
        List<EscampeMove> moves = board.possibleMoves(playerRole);
        orderer.sort(moves, depth);

        PlayerColor opponentRole = playerRole.equals(playerMaxRole) ? playerMinRole : playerMaxRole;
        int value = -NEGAMAX_INF;
        for (EscampeMove move : moves) {
            board.play(move, playerRole);
            value = Math.max(value, -maxValue(board, opponentRole, -sign, depth - 1, -beta, -alpha, timeManager));
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
}
