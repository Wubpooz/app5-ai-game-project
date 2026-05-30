package game;

import interfaces.IJoueur;
import java.util.concurrent.atomic.AtomicReference;
import algorithms.evaluation.HeuristicConfig;
import algorithms.evaluation.Heuristic;
import algorithms.search.NegamaxKH;

public class EscampeAIPlayer implements IJoueur {
  private static final String TEAM_NAME = "Mat&Raph AIPlayer";

  private PlayerColor role;
  private PlayerColor opponentRole;
  private EscampeBoard board;
  private algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> ai;

  private long remainingTimeMs = 590_000; // 9:50 minutes

  // Pondering Toggle (configurable via System property -Descampe.ponder=true/false, default is true)
  private boolean usePonder = Boolean.parseBoolean(System.getProperty("escampe.ponder", "true"));

  // Pondering State Variables
  private EscampeMove predictedOpponentMove = null;
  private Thread ponderThread = null;
  private final AtomicReference<EscampeMove> ponderedResponse = new AtomicReference<>(null);
  private boolean isPonderHit = false;

  public EscampeAIPlayer() {
    // Default constructor for reflection
  }

  public EscampeAIPlayer(algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> alg) {
    this.ai = alg;
  }

  public boolean isUsePonder() {
    return usePonder;
  }

  public void setUsePonder(boolean usePonder) {
    this.usePonder = usePonder;
    if (!usePonder) {
      stopPonder();
    }
  }

  @Override
  public void initJoueur(int mycolour) {
    stopPonder();
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

  @Override
  public int getNumJoueur() {
    return (this.role == PlayerColor.WHITE) ? -1 : 1;
  }

  @Override
  public String choixMouvement() {
    if (board.isGameOver()) {
      return "xxxxx";
    }

    EscampeMove chosenMove = null;

    // Check if we have a precomputed move from a successful ponder hit
    if (isPonderHit) {
      chosenMove = ponderedResponse.get();
      if (chosenMove != null) {
        System.out.println("Ponder HIT! Playing precomputed move: " + chosenMove);
      }
      stopPonder(); // Clean up state for the next turn
    } else {
      // Ponder miss or no pondering active -> abort background thread
      stopPonder();
    }

    // Fallback if ponder hit didn't finish or it was a ponder miss
    if (chosenMove == null) {
      long startTime = System.nanoTime();
      chosenMove = ai.bestMove(board, this.role, remainingTimeMs);
      long endTime = System.nanoTime();
      remainingTimeMs -= (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
    }

    System.out.println("Time left: " + remainingTimeMs + " ms");
    if (remainingTimeMs < 0) {
      remainingTimeMs = 0;
      System.out.println("Time's up!");
    }
    
    if (chosenMove == null) {
      return "xxxxx"; // Fallback to indicate end of game
    }

    board.play(chosenMove, this.role);

    // Start background pondering for the next turn
    if (usePonder) {
      startPonder(chosenMove);
    }

    return chosenMove.toString();
  }

  @Override
  public void mouvementEnnemi(String coup) {
    EscampeMove move = new EscampeMove(coup);
    board.play(move, this.opponentRole);

    // Determine if opponent played our predicted move (Ponder Hit or Ponder Miss)
    if (predictedOpponentMove != null && predictedOpponentMove.toString().equals(coup)) {
      System.out.println("Ponder HIT detected! Opponent played predicted move: " + coup);
      isPonderHit = true;
      // Do NOT call stopPonder() here; let the background thread keep searching on our time!
    } else {
      if (predictedOpponentMove != null) {
        System.out.println("Ponder MISS! Opponent played: " + coup + " (predicted: " + predictedOpponentMove + ")");
      }
      stopPonder(); // Immediately abort the incorrect search thread
    }
  }

  private void startPonder(EscampeMove ourMove) {
    if (this.ai == null || board.isGameOver()) return;

    // Pondering is only relevant in normal play phase (not placement)
    if (board.getLastMoveRow() == -1 || board.getLastMoveCol() == -1) return;

    try {
      // 1. Predict opponent's best response to ourMove
      EscampeBoard predictionBoard = board.copy();
      // Give it 100ms for a quick search to predict
      EscampeMove predicted = ai.bestMove(predictionBoard, this.opponentRole, 100);
      if (predicted == null) return;

      this.predictedOpponentMove = predicted;
      this.isPonderHit = false;

      // 2. Play predicted move on our copied board
      predictionBoard.play(predicted, this.opponentRole);

      // 3. Start thread searching for our best response to the predicted state
      ponderThread = new Thread(() -> {
        try {
          System.out.println("Pondering background search started for response to predicted opponent move: " + predicted);
          // Run full search with a generous time bound
          EscampeMove response = ai.bestMove(predictionBoard, this.role, 4000);
          if (!Thread.currentThread().isInterrupted()) {
            ponderedResponse.set(response);
            System.out.println("Pondering completed. Move precomputed: " + response);
          }
        } catch (Exception ignored) {}
      }, "ponder-thread");
      ponderThread.setDaemon(true);
      ponderThread.start();

    } catch (Exception e) {
      System.err.println("Failed to start ponder thread: " + e.getMessage());
    }
  }

  private void stopPonder() {
    if (ponderThread != null) {
      ponderThread.interrupt();
      try {
        ponderThread.join(50); // wait briefly for termination
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      ponderThread = null;
    }
    ponderedResponse.set(null);
    predictedOpponentMove = null;
    isPonderHit = false;
  }

  @Override
  public void declareLeVainqueur(int colour) {
    stopPonder();
    if (colour == this.getNumJoueur()) {
      System.out.println("I win!");
    } else {
      System.out.println("I lose...");
    }
  }

  @Override
  public String binoName() {
    return TEAM_NAME;
  }
}
