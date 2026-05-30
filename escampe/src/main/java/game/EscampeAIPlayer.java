package game;

import interfaces.IJoueur;
import java.util.concurrent.atomic.AtomicReference;
import algorithms.evaluation.HeuristicConfig;
import algorithms.evaluation.Heuristic;
import algorithms.search.NegamaxKH;


public class EscampeAIPlayer implements IJoueur {
  private static final String TEAM_NAME = "Mat&Raph AIPlayer";

	private PlayerColor role;
  private EscampeBoard board;
	private algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> ai;

  // Helpful variables
  private PlayerColor opponentRole;

  private long remainingTimeMs = 590_000; // 9:50 minutes
  // private long remainingTimeMs = 160; // for tests

  // Constructors
  public EscampeAIPlayer() {
    // Default constructor for reflection
  }

  public EscampeAIPlayer(algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> alg) {
    this.ai = alg;
  }

  // Pondering support: compute a candidate move during opponent's turn
  private final AtomicReference<EscampeMove> ponderedMove = new AtomicReference<>(null);
  private Thread ponderThread = null;
  private static final long PONDER_TIME_MS = 2000; // max time to spend pondering

  // Interface Methods
  /**
   * @param mycolour La couleur dans laquelle vous allez jouer (-1=BLANC, 1=NOIR)
  */
  public void initJoueur(int mycolour) {
    this.remainingTimeMs = 590_000; // reset time at the start of the game
    this.role = (mycolour == -1) ? PlayerColor.WHITE : PlayerColor.BLACK;
    this.opponentRole = (this.role == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    
    this.board = new EscampeBoard();
    this.board.initializeBoard();

    if (this.ai == null) {
      HeuristicConfig bestConfig = HeuristicConfig.createAblated("bandcoverage");
      interfaces.IHeuristic<EscampeBoard, PlayerColor> heuristic = new Heuristic(bestConfig);
      this.ai = new NegamaxKH(this.role, this.opponentRole, heuristic, 6, true);
    }
  }

  // Doit retourner l'argument passé par la fonction ci-dessus (constantes BLANC ou NOIR)
  public int getNumJoueur() {
    return (this.role == PlayerColor.WHITE) ? -1 : 1;
  }

  public String choixMouvement() {
    if (board.isGameOver()) {
      return "xxxxx";
    }
    // TODO Choose an opening here or in board (at random maybe)
    // E is the pass move, present in board.possibleMoves() when no placements/moves are available
    // If we have a pondered move ready from the opponent's turn, use it.
    EscampeMove move = ponderedMove.getAndSet(null);
    if (move == null) {
      long startTime = System.nanoTime();
      move = ai.bestMove(board, this.role, remainingTimeMs);
      long endTime = System.nanoTime();
      remainingTimeMs -= (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
    } else {
      // we used a precomputed move; cancel any active ponder thread
      if (ponderThread != null && ponderThread.isAlive()) {
        try {
          ponderThread.interrupt();
        } catch (Exception ignored) {}
      }
      ponderThread = null;
    }
    System.out.println("Time left: " + remainingTimeMs + " ms");
    // if (remainingTimeMs <= 0) {
    //   // lose
    //   System.out.println("Time's up! I lose...");
    //   return "xxxxx";
    // }
    if (remainingTimeMs < 0) {
      remainingTimeMs = 0;
      System.out.println("Time's up!");
      // and now we just pick the first move every time pretty much
    }
    if (move == null) {
      return "xxxxx"; // Fallback to indicate end of game
    }
    
    board.play(move, this.role);
    return move.toString();
  }


  /**
   * @param coup
   * 			une chaine décrivant le mouvement:  par exemple: "A1-B2"
   */
  public void mouvementEnnemi(String coup) {
    EscampeMove move = new EscampeMove(coup);
    board.play(move, this.opponentRole);
    // Start background pondering for our next move (if not already pondering)
    // only start pondering after initial positions are set and if the game is not already over and opponent didn't skip last turn
    if (this.ai != null && !board.isGameOver() && board.getLastMoveRow() != -1 && board.getLastMoveCol() != -1) {
      if (ponderThread == null || !ponderThread.isAlive()) {
        ponderedMove.set(null);
        ponderThread = new Thread(() -> {
          try {
            System.out.println("Starting to ponder opponent's move...");
            EscampeBoard copy = board.copy();
            EscampeMove pm = ai.bestMove(copy, this.role, PONDER_TIME_MS);
            if (pm != null) ponderedMove.set(pm);
            System.out.println("Pondering complete. Move found: " + pm);
          } catch (Exception e) {
            // swallow exceptions from background pondering
          }
        }, "ponder-thread");
        ponderThread.setDaemon(true);
        ponderThread.start();
      }
    }
  }


  public void declareLeVainqueur(int colour) {
    if (colour == this.getNumJoueur()) {
      System.out.println("I win!");
    } else {
    System.out.println("I lose...");
    }
  }

  public String binoName() {
    return TEAM_NAME;
  }
}
