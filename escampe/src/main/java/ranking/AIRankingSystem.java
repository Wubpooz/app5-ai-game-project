package ranking;

import algorithms.GameAlgorithm;
import algorithms.Opening;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AIRankingSystem {

  private static final Logger LOGGER = Logger.getLogger(AIRankingSystem.class.getName());
  private final int matchesPerPairing;
  private final TournamentConfig config;
  private final List<AlgorithmConfig> algorithms;
  private final Map<String, MatchResult> results;
  private final Map<String, Double> eloRatings;

  private static long lastStartTimeMs = 0;
  private static long lastUpdateMs = 0;
  private static int lastUpdateCurrent = 0;
  private static double currentSpeed = -1.0;

  /**
   * Creates an AIRankingSystem with the specified tournament configuration.
   *
   * @param config TournamentConfig containing matches per pairing and random seeds
   */
  public AIRankingSystem(TournamentConfig config) {
    this.config = config;
    this.matchesPerPairing = config.getMatchesPerPairing();
    this.algorithms = new ArrayList<>();
    this.results = new HashMap<>();
    this.eloRatings = new HashMap<>();
  }

  /**
   * Creates an AIRankingSystem with a simple integer (legacy constructor).
   * Uses default TournamentConfig with default seeds.
   *
   * @param matchesPerPairing number of matches per pairing
   * @deprecated Use AIRankingSystem(TournamentConfig) instead
   */
  @Deprecated
  public AIRankingSystem(int matchesPerPairing) {
    this(new TournamentConfig(matchesPerPairing, 2026L, 2027L));
  }

  public void addAlgorithm(AlgorithmConfig config) {
    if (!algorithms.contains(config)) {
      algorithms.add(config);
      eloRatings.put(config.toString(), 1500.0); // start at standard 1500 Elo
    }
  }

  public void executeFullTournament() {
    LOGGER.info(() -> "\n" + "=".repeat(60));
    LOGGER.info("ESCAMPE AI TOURNAMENT RANKING SYSTEM");
    LOGGER.info(() -> "Matches per pairing (pairs of White/Black): " + matchesPerPairing + " (Total "
        + (matchesPerPairing * 2) + " games per matchup)");
    LOGGER.info(() -> "Total algorithms: " + algorithms.size());
    LOGGER.info(() -> "=".repeat(60) + "\n");

    int totalMatchups = algorithms.size() * (algorithms.size() - 1) / 2;
    int currentMatchup = 0;

    // We use a fixed thread pool to run games concurrently
    int threads = Runtime.getRuntime().availableProcessors();
    try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
      LOGGER.info(() -> "Running games in parallel using " + threads + " threads...");

      // List to hold all game records for sequential Elo training later
      List<GameRecord> allGames = new ArrayList<>();

      for (int i = 0; i < algorithms.size(); i++) {
        for (int j = i + 1; j < algorithms.size(); j++) {
          currentMatchup++;
          AlgorithmConfig algo1 = algorithms.get(i);
          AlgorithmConfig algo2 = algorithms.get(j);

          LOGGER.log(Level.INFO, "[Matchup {0}/{1}] {2} vs {3}",
              new Object[] { currentMatchup, totalMatchups, algo1, algo2 });

          MatchResult result = new MatchResult(algo1, algo2);
          executeMatchSeriesParallel(executor, algo1, algo2, result, allGames);

          String key = algo1 + " vs " + algo2;
          results.put(key, result);
        }
      }

      executor.shutdown();
      try {
        if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
          LOGGER.warning("Tournament executor did not terminate in time!");
        }
      } catch (InterruptedException e) {
        LOGGER.log(Level.SEVERE, "Tournament execution interrupted", e);
        Thread.currentThread().interrupt();
      }

      // Perform Elo convergence updates
      computeConvergedElos(allGames);
    }
  }

  private void executeMatchSeriesParallel(ExecutorService executor, AlgorithmConfig algo1, AlgorithmConfig algo2,
      MatchResult result, List<GameRecord> allGames) {
    int numOpenings = Opening.WHITE_OPENINGS.length;
    List<Callable<Void>> tasks = new ArrayList<>();

    // To collect game records in a thread-safe manner
    List<GameRecord> localGames = Collections.synchronizedList(new ArrayList<>());

    for (int pair = 0; pair < matchesPerPairing; pair++) {
      final int pairIndex = pair;
      tasks.add(() -> {
        int opIndex = pairIndex % numOpenings;
        String whiteOpen = Opening.WHITE_OPENINGS[opIndex];
        String blackOpen = Opening.BLACK_OPENINGS[opIndex];

        // Game 1: Bot 1 is White, Bot 2 is Black
        double score1 = simulateGame(algo1, algo2, whiteOpen, blackOpen);

        // Game 2: Bot 2 is White, Bot 1 is Black
        double score2_white = simulateGame(algo2, algo1, whiteOpen, blackOpen);
        double score2 = 1.0 - score2_white; // Score from Bot 1's perspective

        result.recordPairResult(opIndex, score1, score2);

        // Record individual game details
        localGames.add(new GameRecord(algo1, algo2, score1));
        localGames.add(new GameRecord(algo2, algo1, score2_white));
        return null;
      });
    }

    List<Future<Void>> futures = new ArrayList<>();
    for (Callable<Void> task : tasks) {
      futures.add(executor.submit(task));
    }

    int completed = 0;
    int totalTasks = tasks.size();
    long startTimeMs = System.currentTimeMillis();
    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (ExecutionException | InterruptedException e) {
        LOGGER.log(Level.SEVERE, "Match series execution interrupted", e);
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      }
      completed++;
      printProgress(completed, totalTasks, startTimeMs, "matches");
    }
    System.out.println();

    allGames.addAll(localGames);

    LOGGER.info(() -> "  Result: " + algo1 + " " +
        String.format("%.1f%%", result.getWinRate1() * 100) + " (Elo diff: " +
        String.format("%.1f", result.getEloDifference()) + " ± " +
        String.format("%.1f", result.getEloErrorMargin()) + ") vs " + algo2);
  }

  private double simulateGame(AlgorithmConfig whiteConfig, AlgorithmConfig blackConfig, String whiteOpen,
      String blackOpen) {
    try {
      EscampeBoard board = new EscampeBoard();
      board.initializeBoard();

      // Set initial placements
      board.play(new EscampeMove(blackOpen), PlayerColor.BLACK);
      board.play(new EscampeMove(whiteOpen), PlayerColor.WHITE);

      PlayerColor turn = PlayerColor.WHITE;
      int moveCount = 2;
      int maxMoves = 200; // avoid infinite loops

      GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> algWhite = whiteConfig.createInstance(PlayerColor.WHITE,
          PlayerColor.BLACK);
      GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> algBlack = blackConfig.createInstance(PlayerColor.BLACK,
          PlayerColor.WHITE);

      while (!board.isGameOver() && moveCount < maxMoves) {
        GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> currentAlgo = (turn == PlayerColor.WHITE) ? algWhite : algBlack;

        EscampeMove move = currentAlgo.bestMove(board, turn);
        if (move == null || move.toString().equals("xxxxx")) {
          break;
        }

        board.play(move, turn);
        moveCount++;
        turn = turn.getOpponent();
      }

      if (board.isGameOver()) {
        boolean whiteUnicorn = false;
        boolean blackUnicorn = false;
        for (int r = 0; r < 6; r++) {
          for (int c = 0; c < 6; c++) {
            char p = board.getPieceAt(r, c);
            if (p == 'B')
              whiteUnicorn = true;
            if (p == 'N')
              blackUnicorn = true;
          }
        }
        if (!whiteUnicorn && blackUnicorn)
          return 0.0; // Black wins
        if (whiteUnicorn && !blackUnicorn)
          return 1.0; // White wins
      }
      return 0.5; // Draw

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in game simulation", e);
      return 0.5;
    }
  }

  private void computeConvergedElos(List<GameRecord> games) {
    // Run multiple updates over shuffled games to stabilize ratings
    int iterations = 20;
    double kFactor = 32.0;

    for (AlgorithmConfig algo : algorithms) {
      eloRatings.put(algo.toString(), 1500.0);
    }

    List<GameRecord> updateList = new ArrayList<>(games);
    Random eloRandom = new Random(config.getEloSeed());
    for (int iter = 0; iter < iterations; iter++) {
      Collections.shuffle(updateList, eloRandom);
      for (GameRecord game : updateList) {
        String pA = game.playerA.toString();
        String pB = game.playerB.toString();

        double rA = eloRatings.get(pA);
        double rB = eloRatings.get(pB);

        double eA = 1.0 / (1.0 + Math.pow(10.0, (rB - rA) / 400.0));
        double eB = 1.0 / (1.0 + Math.pow(10.0, (rA - rB) / 400.0));

        double sA = game.scoreA;
        double sB = 1.0 - game.scoreA;

        eloRatings.put(pA, rA + kFactor * (sA - eA));
        eloRatings.put(pB, rB + kFactor * (sB - eB));
      }
      // Slowly decay K-factor for convergence
      kFactor *= 0.9;
    }

    // Normalize Elo ratings relative to the average rating (which stays 1500)
    double sum = 0.0;
    for (double val : eloRatings.values()) {
      sum += val;
    }
    double avg = sum / eloRatings.size();
    double offset = 1500.0 - avg;
    for (Map.Entry<String, Double> entry : eloRatings.entrySet()) {
      eloRatings.put(entry.getKey(), entry.getValue() + offset);
    }
  }

  private static void printProgress(int current, int total, long startTimeMs, String unitName) {
    if (startTimeMs != lastStartTimeMs) {
      lastStartTimeMs = startTimeMs;
      lastUpdateMs = startTimeMs;
      lastUpdateCurrent = 0;
      currentSpeed = -1.0;
    }

    int width = 50;
    int progress = (int) ((double) current / total * width);
    long now = System.currentTimeMillis();
    long elapsedMs = now - startTimeMs;
    
    // Update speed estimate at most once every 1000ms, or on first/last updates
    long timeDiff = now - lastUpdateMs;
    if (timeDiff >= 1000 || current == total || lastUpdateCurrent == 0) {
      int workDiff = current - lastUpdateCurrent;
      if (workDiff > 0 && timeDiff > 0) {
        double instantSpeed = (double) workDiff / (timeDiff / 1000.0);
        if (currentSpeed < 0) {
          currentSpeed = instantSpeed;
        } else {
          currentSpeed = 0.8 * currentSpeed + 0.2 * instantSpeed;
        }
      }
      lastUpdateMs = now;
      lastUpdateCurrent = current;
    }
    
    double speed = currentSpeed > 0 ? currentSpeed : (elapsedMs > 0 ? (double) current / (elapsedMs / 1000.0) : 0);
    long remainingMs = speed > 0 ? (long) ((total - current) / speed * 1000) : 0;
    
    long h = remainingMs / 3600000;
    long m = (remainingMs % 3600000) / 60000;
    long s = (remainingMs % 60000) / 1000;
    String eta = String.format("%02d:%02d:%02d", h, m, s);
    
    java.time.LocalTime endTime = java.time.LocalTime.now().plus(java.time.Duration.ofMillis(remainingMs));
    String endClockStr = endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    
    StringBuilder sb = new StringBuilder("\r[");
    for (int i = 0; i < width; i++) {
      if (i < progress) sb.append("=");
      else if (i == progress) sb.append(">");
      else sb.append(" ");
    }
    sb.append(String.format("] %d%% (%d/%d) - %.1f %s/s - ETA: %s (End: %s)          ", 
        (int) ((double) current / total * 100), current, total, speed, unitName, eta, endClockStr));
    System.out.print(sb.toString());
  }

  public double getEloRating(String algorithmName) {
    return eloRatings.getOrDefault(algorithmName, 1500.0);
  }

  public int getMatchesPerPairing() {
    return matchesPerPairing;
  }

  public Collection<MatchResult> getResults() {
    return results.values();
  }

  public List<AlgorithmConfig> getAlgorithms() {
    return algorithms;
  }

  // Helper class to record game outcomes
  private static class GameRecord {
    final AlgorithmConfig playerA;
    final AlgorithmConfig playerB;
    final double scoreA;

    GameRecord(AlgorithmConfig playerA, AlgorithmConfig playerB, double scoreA) {
      this.playerA = playerA;
      this.playerB = playerB;
      this.scoreA = scoreA;
    }
  }
}
