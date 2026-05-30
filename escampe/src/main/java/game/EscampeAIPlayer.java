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
    
    long startTime = System.nanoTime();
    EscampeMove move = ai.bestMove(board, this.role, remainingTimeMs);
    long endTime = System.nanoTime();
    remainingTimeMs -= (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds

    System.out.println("Time left: " + remainingTimeMs + " ms");
    if (remainingTimeMs < 0) {
      remainingTimeMs = 0;
      System.out.println("Time's up!");
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
