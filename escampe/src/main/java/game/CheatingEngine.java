package game;

import interfaces.IJoueur;
import algorithms.GameAlgorithm;
import algorithms.search.AlphaBeta;
import algorithms.evaluation.Heuristic;
import interfaces.IHeuristic;

import java.lang.reflect.Field;

/**
 * CheatingEngine — Security Proof-of-Concept for the Escampe game.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  EXPLOIT 1 — Unsafe Reflection / Shared-JVM state sniffing
 * ═══════════════════════════════════════════════════════════════════
 * When both players run inside the same JVM (io.Solo), this engine
 * uses Java reflection to reach the arbiter's private board object
 * (escampe.g#j[][]) and read the authoritative game state directly,
 * bypassing any local board-tracking logic entirely.
 *
 * CWE-470: Use of Externally-Controlled Input to Select Classes or
 *          Code ('Unsafe Reflection')
 *
 * ═══════════════════════════════════════════════════════════════════
 *  EXPLOIT 2 — Unencrypted socket / plaintext move interception
 * ═══════════════════════════════════════════════════════════════════
 * In network mode every move travels as plaintext ASCII over a raw
 * TCP socket (see ClientJeu.java L69, ServeurJeu.java L42).
 * A LAN observer / MitM can intercept every MOUVEMENT message and
 * feed the exact opponent board state to any engine — zero guessing.
 *
 * CWE-319: Cleartext Transmission of Sensitive Information
 *
 * ═══════════════════════════════════════════════════════════════════
 *  EXPLOIT 3 — Unbound server accept (SSRF-adjacent)
 * ═══════════════════════════════════════════════════════════════════
 * ServeurJeu calls serverSocket.accept() twice with no IP filtering
 * or authentication. Any host that connects first becomes WHITE and
 * gets to play first. Two connections from a single attacker host
 * gives full control of both sides.
 *
 * CWE-918: Server-Side Request Forgery (by analogy: unauthenticated
 *           server-side resource binding)
 *
 * ===================================================================
 * This file is for EDUCATIONAL / CTF purposes only.
 * Run as: java -cp ... io.ClientJeu game.CheatingEngine localhost 1234
 * ===================================================================
 */
public class CheatingEngine implements IJoueur {

    // ======================== identity ============================

    private static final String TEAM_NAME = "CheatingEngine-PoC";

    // ======================== game state ==========================

    private PlayerColor role;
    private PlayerColor opponentRole;
    private EscampeBoard localBoard;          // used in network mode
    private GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> ai;

    // Reflective handles — populated once in initJoueur()
    private Object arbitreBoard = null;       // the escampe.g instance
    private Field  arbitreBoardArray = null;  // the int[][] j field

    // ======================== IJoueur =============================

    @Override
    public void initJoueur(int mycolour) {
        this.role         = (mycolour == -1) ? PlayerColor.WHITE : PlayerColor.BLACK;
        this.opponentRole = (this.role == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;

        // Always prepare our own board + alpha-beta as a fallback
        this.localBoard = new EscampeBoard();
        IHeuristic<EscampeBoard, PlayerColor> h = new Heuristic();
        this.ai = new AlphaBeta<>(this.role, this.opponentRole, h);

        // == Exploit 1: try to grab the arbiter's board via reflection ==
        tryInjectReflection();
    }

    @Override
    public int getNumJoueur() {
        return (this.role == PlayerColor.WHITE) ? -1 : 1;
    }

    @Override
    public String choixMouvement() {
        // Attempt to sync local board from the arbiter's live state
        syncBoardFromArbiter();

        if (localBoard.isGameOver()) return "xxxxx";

        // Use the AI against the (potentially stolen) authoritative board state
        EscampeMove move = ai.bestMove(localBoard, this.role);
        if (move == null) return "xxxxx";

        localBoard.play(move, this.role);
        return move.toString();
    }

    @Override
    public void mouvementEnnemi(String coup) {
        // In network mode this is our only source of truth — apply it normally
        EscampeMove move = new EscampeMove(coup);
        localBoard.play(move, this.opponentRole);
    }

    @Override
    public void declareLeVainqueur(int colour) {
        if (colour == getNumJoueur())
            System.out.println("[CheatingEngine] Won as expected.");
        else
            System.out.println("[CheatingEngine] Lost — arbiter reflection unavailable?");
    }

    @Override
    public String binoName() { return TEAM_NAME; }

    // ══════════════════════════════════════════════════════════════
    //  EXPLOIT 1 IMPLEMENTATION — Reflective arbiter board sniffing
    // ══════════════════════════════════════════════════════════════

    /**
     * Walk all live threads. In io.Solo both players AND the game
     * runner share one JVM. The thread named "main" (or "Solo") holds
     * references to joueurBlanc / joueurNoir; their shared environment
     * is the same classloader. We hunt for an instance of escampe.g
     * (the arbiter board) by scanning the static field ServeurJeu.a
     * or, in Solo mode, by finding the game-loop local variable via
     * the ThreadLocal trick below.
     *
     * In practice, the simplest reliable vector in Solo mode is that
     * io.Solo instantiates both IJoueur objects and calls gameLoop()
     * in sequence. We grab the *other* IJoueur's reference and read
     * its localBoard — also a valid cheat since Solo does no isolation.
     */
    private void tryInjectReflection() {
        // == Path A: Try to load escampe.g (the obfuscated arbiter class) ==
        try {
            Class<?> gClass = Class.forName("escampe.g");
            // Grab the private int[][] j field (the board matrix)
            Field jField = gClass.getDeclaredField("j");
            jField.setAccessible(true);
            this.arbitreBoardArray = jField;

            // In Solo mode no escampe.g is instantiated — this path only
            // works in full network mode where ServeurJeu creates one.
            // We look for it via the static ServeurJeu reference.
            try {
                Class<?> sClass = Class.forName("escampe.ServeurJeu");
                // ServeurJeu.a is the static boolean, not the board.
                // The board lives inside the h thread; we iterate threads.
                findArbitreBoardFromThread(gClass);
            } catch (ClassNotFoundException e) {
                System.out.println("[CheatingEngine] escampe.ServeurJeu not on classpath (Solo mode).");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("[CheatingEngine] escampe.g not on classpath — reflection path unavailable.");
        } catch (NoSuchFieldException e) {
            System.out.println("[CheatingEngine] Field 'j' not found in escampe.g — obfuscation changed?");
        }

        // == Path B: In Solo mode, exploit the shared classloader directly ==
        // Both IJoueur instances are created by the same class. We can scan
        // static fields on EscampeAIPlayer (or any other loaded IJoueur) to
        // find a board reference if one exists statically.
        // (No static board reference exists in EscampeAIPlayer, so this is
        // demonstration-only and left as a comment.)
    }

    /**
     * Iterate all live JVM threads looking for the "ServeurJeuThread"
     * (class h in escampe package) and extract its 'e' field (the g instance).
     */
    private void findArbitreBoardFromThread(Class<?> gClass) {
        try {
            ThreadGroup root = Thread.currentThread().getThreadGroup();
            while (root.getParent() != null) root = root.getParent();

            Thread[] threads = new Thread[root.activeCount() + 32];
            int count = root.enumerate(threads, true);

            for (int i = 0; i < count; i++) {
                Thread t = threads[i];
                if (t == null) continue;
                if ("ServeurJeuThread".equals(t.getName())) {
                    // t is an instance of escampe.h — grab field 'e' (the g board)
                    Field eField = t.getClass().getDeclaredField("e");
                    eField.setAccessible(true);
                    this.arbitreBoard = eField.get(t);
                    System.out.println("[CheatingEngine] ✓ Captured arbiter board via thread reflection!");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("[CheatingEngine] Thread scan failed: " + e.getMessage());
        }
    }

    /**
     * If we have a live handle to the arbiter's escampe.g instance,
     * read its int[][] j[][] board and sync it into our localBoard.
     *
     * Mapping (from g.java):
     *   j[row][col] == 0  → empty
     *   j[row][col] == -2 → WHITE unicorn (B)
     *   j[row][col] == -1 → WHITE paladin (b)
     *   j[row][col] ==  2 → BLACK unicorn (N)
     *   j[row][col] ==  1 → BLACK paladin (n)
     */
    private void syncBoardFromArbiter() {
        if (arbitreBoard == null || arbitreBoardArray == null) return;

        try {
            int[][] j = (int[][]) arbitreBoardArray.get(arbitreBoard);
            EscampeBoard fresh = new EscampeBoard();

            // We need to poke the board's private array directly
            Field boardField = EscampeBoard.class.getDeclaredField("board");
            boardField.setAccessible(true);
            char[][] localBoardArray = (char[][]) boardField.get(fresh);

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 6; c++) {
                    localBoardArray[r][c] = intToChar(j[r][c]);
                }
            }
            this.localBoard = fresh;
            System.out.println("[CheatingEngine] ✓ Board synced from arbiter.");
        } catch (Exception e) {
            // Silently fall back to tracked board
        }
    }

    /** Convert the arbiter's int encoding to the char encoding used by EscampeBoard. */
    private static char intToChar(int v) {
        switch (v) {
            case -2: return 'B'; // WHITE unicorn
            case -1: return 'b'; // WHITE paladin
            case  2: return 'N'; // BLACK unicorn
            case  1: return 'n'; // BLACK paladin
            default: return '-'; // empty
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EXPLOIT 2 NOTE — Plaintext socket interception (external)
    // ══════════════════════════════════════════════════════════════
    /*
     * To exploit the unencrypted socket:
     *
     *   $ sudo tcpdump -i lo -A 'tcp port 1234' 2>/dev/null | grep -E "^(JOUEUR|MOUVEMENT|FIN)"
     *
     * Every move appears in plaintext. Feed the MOUVEMENT lines into
     * any external engine to compute optimal responses, then inject
     * them via a transparent TCP proxy (e.g. socat, mitmproxy).
     *
     * The attack requires only LAN access (or loopback if on the same host).
     *
     * Time-budget exhaustion variant:
     *   The server tracks f[i-1] (cumulative ms per player). Delay the
     *   forwarding of the opponent's MOUVEMENT packets by ~580 000 ms
     *   total and the arbiter will declare TIMEOUT → you win.
     */

    // ══════════════════════════════════════════════════════════════
    //  EXPLOIT 3 NOTE — Unauthenticated double-connect (external)
    // ══════════════════════════════════════════════════════════════
    /*
     * ServeurJeu accepts the first two TCP connections unconditionally.
     * To guarantee WHITE and control both sides:
     *
     *   Thread 1: connect to port → send "PlayerA\n" → play normally
     *   Thread 2: connect to port → send "PlayerB\n" → always return "E"
     *             (or mirror Thread 1's moves to let it win instantly)
     *
     * If the real opponent hasn't connected yet, Thread 2 steals their
     * slot. The arbiter believes two legitimate players are connected.
     *
     * See ServeurJeu.java lines 52-57 for the vulnerable accept() loop.
     */
}
