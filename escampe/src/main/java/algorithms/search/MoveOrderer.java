package algorithms.search;

import game.EscampeMove;
import interfaces.IMove;

/**
 * Move ordering tables for killer moves and history heuristic.
 *
 * <p><b>Killer moves</b>: at each depth level, we store up to 2 moves that recently
 * caused a beta cutoff (even in sibling nodes). Trying them first can produce early
 * cutoffs in the current node too.
 *
 * <p><b>History heuristic</b>: a global table keyed by move (from×to square pair)
 * that accumulates scores whenever a move causes a beta cutoff, weighted by 2^depth.
 * Over many nodes, good moves bubble to the top of the ordering.
 *
 * <p>Both tables are <em>per-search</em> (reset at the start of each {@code bestMove()}
 * call) and completely allocation-free at runtime (no HashMap, no boxing).
 *
 * <p><b>Move encoding</b>: for a 6×6 board, each square is 0..35 (row*6+col).
 * A move is encoded as {@code from*36 + to}, giving 0..1295. This fits in a fixed
 * {@code int[1296]} array with O(1) lookup and update.
 */
public final class MoveOrderer {

    /** Maximum search depth supported for killer table */
    private static final int MAX_DEPTH = 32;

    /** Number of killer slots per depth */
    private static final int KILLER_SLOTS = 2;

    /**
     * Bonus score given to killer moves during ordering.
     * Must be less than the history values for the best non-killer moves,
     * but higher than the baseline so killers are tried before unscored moves.
     */
    public static final int KILLER_SCORE = 900_000;

    /** History table indexed by move encoding (from*36 + to). */
    private final int[] historyTable = new int[36 * 36];

    /** Killer moves: killers[depth][slot] = move encoding, or -1 if empty. */
    private final int[][] killers = new int[MAX_DEPTH][KILLER_SLOTS];

    public MoveOrderer() {
        reset();
    }

    /** Reset all tables for a fresh search. */
    public void reset() {
        java.util.Arrays.fill(historyTable, 0);
        for (int[] row : killers) java.util.Arrays.fill(row, -1);
    }

    // ------------------------------------------------------------------ //
    //  Encoding                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Encode a move as a single int for table lookup.
     * Returns -1 for non-normal moves (pass / placement).
     */
    public static int encode(EscampeMove move) {
        int fr = move.getFromRow(), fc = move.getFromCol();
        int tr = move.getToRow(), tc = move.getToCol();
        if (fr < 0 || tr < 0) return -1; // pass or placement
        return (fr * 6 + fc) * 36 + (tr * 6 + tc);
    }

    // ------------------------------------------------------------------ //
    //  History heuristic                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Record a beta-cutoff for {@code move} at the given depth.
     * Score grows with depth so deeper cutoffs are weighted more.
     */
    public void recordCutoff(EscampeMove move, int depth) {
        int code = encode(move);
        if (code < 0) return;
        historyTable[code] += depth * depth; // depth² weight
    }

    /** Return the history score for a move (0 if never caused a cutoff). */
    public int historyScore(EscampeMove move) {
        int code = encode(move);
        return (code < 0) ? 0 : historyTable[code];
    }

    // ------------------------------------------------------------------ //
    //  Killer moves                                                        //
    // ------------------------------------------------------------------ //

    /**
     * Store a new killer move at the given depth.
     * The second slot gets the old first slot; the first slot gets the new move.
     */
    public void recordKiller(EscampeMove move, int depth) {
        if (depth < 0 || depth >= MAX_DEPTH) return;
        int code = encode(move);
        if (code < 0) return;
        if (killers[depth][0] == code) return; // already stored
        killers[depth][1] = killers[depth][0];
        killers[depth][0] = code;
    }

    /** Return true if {@code move} is a killer at the given depth. */
    public boolean isKiller(EscampeMove move, int depth) {
        if (depth < 0 || depth >= MAX_DEPTH) return false;
        int code = encode(move);
        if (code < 0) return false;
        return killers[depth][0] == code || killers[depth][1] == code;
    }

    // ------------------------------------------------------------------ //
    //  Move ordering                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Compute a priority score for ordering. Higher = tried first.
     * <pre>
     *   Killer at this depth  →  KILLER_SCORE
     *   Otherwise             →  history score (0..many)
     * </pre>
     */
    public int score(EscampeMove move, int depth) {
        if (isKiller(move, depth)) return KILLER_SCORE;
        return historyScore(move);
    }

    /**
     * Sort a list of moves in-place, highest score first.
     * Uses insertion sort — efficient for the small move counts typical in Escampe (≤30).
     */
    public void sort(java.util.List<EscampeMove> moves, int depth) {
        int n = moves.size();
        for (int i = 1; i < n; i++) {
            EscampeMove key = moves.get(i);
            int keyScore = score(key, depth);
            int j = i - 1;
            while (j >= 0 && score(moves.get(j), depth) < keyScore) {
                moves.set(j + 1, moves.get(j));
                j--;
            }
            moves.set(j + 1, key);
        }
    }
}
