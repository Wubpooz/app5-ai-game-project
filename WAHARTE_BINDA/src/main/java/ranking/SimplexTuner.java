package ranking;

import algorithms.GameAlgorithm;
import algorithms.Opening;
import algorithms.evaluation.HeuristicConfig;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimplexTuner {
    private static final Logger LOGGER = Logger.getLogger(SimplexTuner.class.getName());
    
    private static long lastStartTimeMs = 0;
    private static long lastUpdateMs = 0;
    private static int lastUpdateCurrent = 0;
    private static double currentSpeed = -1.0;

    private static final String[] PARAM_NAMES_9 = {
        "weightMinDist", "weightAvgDist", "weightUnicornDangerMinDist", "weightUnicornDangerAvgDist",
        "weightEscapability", "weightTrappedUnicorn", "weightLegalMoves", "weightBandControl", "weightOppBandControl"
    };

    private static final double[] MIN_VAL_9 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -150.0, -150.0};
    private static final double[] MAX_VAL_9 = {100.0, 50.0, 100.0, 50.0, 300.0, 200.0, 100.0, 150.0, 150.0};
    
    // Starting point (SPSA tuned weights)
    private static final double[] START_9 = {35.0, 0.0, 4.0, 8.0, 95.0, 68.0, 12.0, 0.0, 0.0};
    private static final double[] STEP_9 = {10.0, 5.0, 5.0, 5.0, 20.0, 15.0, 10.0, 15.0, 15.0};

    // Nelder-Mead hyperparameters
    private static final double ALPHA = 1.0;  // Reflection
    private static final double GAMMA = 2.0;  // Expansion
    private static final double RHO = 0.5;    // Contraction
    private static final double SIGMA = 0.5;  // Shrink

    public static void main(String[] args) {
        LOGGER.info("\n╔════════════════════════════════════════════════════════╗");
        LOGGER.info("║         ESCAMPE NELDER-MEAD SIMPLEX TUNING             ║");
        LOGGER.info("╚════════════════════════════════════════════════════════╝\n");

        int threads = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            
            // RUN 1: 9-Parameter Optimization (Commented out to save time)
            System.out.println(">>> RUN 1: Tuning All 9 Heuristic Parameters...");
            double[] tuned9 = runSimplex(9, START_9, STEP_9, MIN_VAL_9, MAX_VAL_9, false, executor);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("RUN 1 COMPLETE - 9-PARAMETER RESULTS");
            System.out.println("=".repeat(60));
            for (int i = 0; i < 9; i++) {
                System.out.format(Locale.US, "%-26s : %d\n", PARAM_NAMES_9[i], (int) Math.round(tuned9[i]));
            }
            
            System.out.println("\nCopy-paste Java code for HeuristicConfig.java (Simplex-Tuned-Full):");
            System.out.format(Locale.US, "return new HeuristicConfig(\"Simplex-Tuned-Full\", %d, %d, %d, %d, %d, %d, %d, %d, %d);\n\n",
                (int) Math.round(tuned9[0]), (int) Math.round(tuned9[1]), (int) Math.round(tuned9[2]), 
                (int) Math.round(tuned9[3]), (int) Math.round(tuned9[4]), (int) Math.round(tuned9[5]), 
                (int) Math.round(tuned9[6]), (int) Math.round(tuned9[7]), (int) Math.round(tuned9[8]));
            
            // RUN 2: 7-Parameter Optimization (Force Band Control to 0)
            System.out.println("\n>>> RUN 2: Tuning 7 Heuristic Parameters (Forcing Band Control to 0)...");
            double[] start7 = {START_9[0], START_9[1], START_9[2], START_9[3], START_9[4], START_9[5], START_9[6]};
            double[] step7 = {STEP_9[0], STEP_9[1], STEP_9[2], STEP_9[3], STEP_9[4], STEP_9[5], STEP_9[6]};
            double[] min7 = {MIN_VAL_9[0], MIN_VAL_9[1], MIN_VAL_9[2], MIN_VAL_9[3], MIN_VAL_9[4], MIN_VAL_9[5], MIN_VAL_9[6]};
            double[] max7 = {MAX_VAL_9[0], MAX_VAL_9[1], MAX_VAL_9[2], MAX_VAL_9[3], MAX_VAL_9[4], MAX_VAL_9[5], MAX_VAL_9[6]};
            
            double[] tuned7 = runSimplex(7, start7, step7, min7, max7, true, executor);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("RUN 2 COMPLETE - 7-PARAMETER RESULTS (Forced Band Control = 0)");
            System.out.println("=".repeat(60));
            for (int i = 0; i < 7; i++) {
                System.out.format(Locale.US, "%-26s : %d\n", PARAM_NAMES_9[i], (int) Math.round(tuned7[i]));
            }
            System.out.println("weightBandControl          : 0 (FORCED)");
            System.out.println("weightOppBandControl       : 0 (FORCED)");
            
            System.out.println("\nCopy-paste Java code for HeuristicConfig.java (Simplex-Tuned-NoBand):");
            System.out.format(Locale.US, "return new HeuristicConfig(\"Simplex-Tuned-NoBand\", %d, %d, %d, %d, %d, %d, %d, 0, 0);\n\n",
                (int) Math.round(tuned7[0]), (int) Math.round(tuned7[1]), (int) Math.round(tuned7[2]), 
                (int) Math.round(tuned7[3]), (int) Math.round(tuned7[4]), (int) Math.round(tuned7[5]), 
                (int) Math.round(tuned7[6]));
        }
    }

    private static double[] runSimplex(int dim, double[] start, double[] step, double[] minVal, double[] maxVal, 
                                      boolean forceZeroBand, ExecutorService executor) {
        int maxIterations = 10;
        int n = dim;
        List<Vertex> simplex = new ArrayList<>();

        // Initialize vertices
        // Vertex 0 is starting point
        double[] p0 = Arrays.copyOf(start, n);
        simplex.add(new Vertex(p0, evaluate(p0, forceZeroBand, executor)));

        // Generate N other vertices by perturbing each coordinate
        for (int i = 0; i < n; i++) {
            double[] pi = Arrays.copyOf(start, n);
            pi[i] = clamp(pi[i] + step[i], minVal[i], maxVal[i]);
            simplex.add(new Vertex(pi, evaluate(pi, forceZeroBand, executor)));
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            Collections.sort(simplex);
            
            double bestScore = -simplex.get(0).value;
            double worstScore = -simplex.get(n).value;
            
            System.out.format(Locale.US, "[Iteration %02d/%02d] Best Score: %.3f | Worst Score: %.3f\n", 
                iter + 1, maxIterations, bestScore, worstScore);

            // Compute centroid of all vertices except the worst (simplex.get(n))
            double[] centroid = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    centroid[i] += simplex.get(j).point[i];
                }
                centroid[i] /= n;
            }

            // Reflection
            double[] reflected = new double[n];
            for (int i = 0; i < n; i++) {
                reflected[i] = clamp(centroid[i] + ALPHA * (centroid[i] - simplex.get(n).point[i]), minVal[i], maxVal[i]);
            }
            double reflectedVal = evaluate(reflected, forceZeroBand, executor);

            if (reflectedVal < simplex.get(0).value) {
                // Try Expansion
                double[] expanded = new double[n];
                for (int i = 0; i < n; i++) {
                    expanded[i] = clamp(centroid[i] + GAMMA * (reflected[i] - centroid[i]), minVal[i], maxVal[i]);
                }
                double expandedVal = evaluate(expanded, forceZeroBand, executor);
                if (expandedVal < reflectedVal) {
                    simplex.set(n, new Vertex(expanded, expandedVal));
                } else {
                    simplex.set(n, new Vertex(reflected, reflectedVal));
                }
                continue;
            }

            if (reflectedVal < simplex.get(n - 1).value) {
                simplex.set(n, new Vertex(reflected, reflectedVal));
                continue;
            }

            // Contraction
            if (reflectedVal < simplex.get(n).value) {
                // Outside contraction
                double[] contracted = new double[n];
                for (int i = 0; i < n; i++) {
                    contracted[i] = clamp(centroid[i] + RHO * (reflected[i] - centroid[i]), minVal[i], maxVal[i]);
                }
                double contractedVal = evaluate(contracted, forceZeroBand, executor);
                if (contractedVal < reflectedVal) {
                    simplex.set(n, new Vertex(contracted, contractedVal));
                    continue;
                }
            } else {
                // Inside contraction
                double[] contracted = new double[n];
                for (int i = 0; i < n; i++) {
                    contracted[i] = clamp(centroid[i] - RHO * (centroid[i] - simplex.get(n).point[i]), minVal[i], maxVal[i]);
                }
                double contractedVal = evaluate(contracted, forceZeroBand, executor);
                if (contractedVal < simplex.get(n).value) {
                    simplex.set(n, new Vertex(contracted, contractedVal));
                    continue;
                }
            }

            // Shrink
            double[] bestPoint = simplex.get(0).point;
            double bestValue = simplex.get(0).value;
            for (int j = 1; j <= n; j++) {
                double[] pj = simplex.get(j).point;
                for (int i = 0; i < n; i++) {
                    pj[i] = clamp(bestPoint[i] + SIGMA * (pj[i] - bestPoint[i]), minVal[i], maxVal[i]);
                }
                simplex.set(j, new Vertex(pj, evaluate(pj, forceZeroBand, executor)));
            }
        }

        Collections.sort(simplex);
        return simplex.get(0).point;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static double evaluate(double[] params, boolean forceZeroBand, ExecutorService executor) {
        // Construct HeuristicConfig from parameters
        HeuristicConfig candidate;
        if (forceZeroBand) {
            candidate = new HeuristicConfig(
                "Candidate",
                (int) Math.round(params[0]),
                (int) Math.round(params[1]),
                (int) Math.round(params[2]),
                (int) Math.round(params[3]),
                (int) Math.round(params[4]),
                (int) Math.round(params[5]),
                (int) Math.round(params[6]),
                0,
                0
            );
        } else {
            candidate = new HeuristicConfig(
                "Candidate",
                (int) Math.round(params[0]),
                (int) Math.round(params[1]),
                (int) Math.round(params[2]),
                (int) Math.round(params[3]),
                (int) Math.round(params[4]),
                (int) Math.round(params[5]),
                (int) Math.round(params[6]),
                (int) Math.round(params[7]),
                (int) Math.round(params[8])
            );
        }

        AlgorithmConfig candidateConfig = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, candidate);
        HeuristicConfig baselineHeuristic = HeuristicConfig.createDefault();
        AlgorithmConfig baselineConfig = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, baselineHeuristic);

        List<Callable<Double>> tasks = new ArrayList<>();
        int numOpenings = Opening.WHITE_OPENINGS.length;
        int numPairs = 16; // 32 games total

        for (int pair = 0; pair < numPairs; pair++) {
            final int pairIdx = pair;
            tasks.add(() -> {
                int opIndex = pairIdx % numOpenings;
                String whiteOpen = Opening.WHITE_OPENINGS[opIndex];
                String blackOpen = Opening.BLACK_OPENINGS[opIndex];
                
                // Game 1: candidate is White, baseline is Black
                double score1 = simulateGame(candidateConfig, baselineConfig, whiteOpen, blackOpen);
                
                // Game 2: baseline is White, candidate is Black
                double score2_white = simulateGame(baselineConfig, candidateConfig, whiteOpen, blackOpen);
                double score2 = 1.0 - score2_white; // Score from candidate perspective
                
                return score1 + score2;
            });
        }

        double totalPoints = 0.0;
        try {
            List<Future<Double>> futures = new ArrayList<>();
            for (Callable<Double> task : tasks) {
                futures.add(executor.submit(task));
            }
            int completed = 0;
            int totalTasks = tasks.size();
            long startTimeMs = System.currentTimeMillis();
            for (Future<Double> future : futures) {
                totalPoints += future.get();
                completed++;
                printProgress(completed, totalTasks, startTimeMs, "pairs");
            }
            System.out.println();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error in parallel evaluation", e);
            Thread.currentThread().interrupt();
        }

        double winRate = totalPoints / (2.0 * numPairs);
        
        // Simplex minimizes, so return negative win rate (or 1.0 - winRate)
        return -winRate;
    }

    private static double simulateGame(AlgorithmConfig whiteConfig, AlgorithmConfig blackConfig, String whiteOpen, String blackOpen) {
        try {
            EscampeBoard board = new EscampeBoard();
            board.initializeBoard();
            
            board.play(new EscampeMove(blackOpen), PlayerColor.BLACK);
            board.play(new EscampeMove(whiteOpen), PlayerColor.WHITE);
            
            PlayerColor turn = PlayerColor.WHITE;
            int moveCount = 2;
            int maxMoves = 200;
            
            GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> algWhite = 
                    whiteConfig.createInstance(PlayerColor.WHITE, PlayerColor.BLACK);
            GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> algBlack = 
                    blackConfig.createInstance(PlayerColor.BLACK, PlayerColor.WHITE);
            
            while (!board.isGameOver() && moveCount < maxMoves) {
                GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> currentAlgo = (turn == PlayerColor.WHITE) ? algWhite : algBlack;
                
                EscampeMove move = currentAlgo.bestMove(board, turn, 10000); // 10 seconds per move max
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
                for (int r = 0; r < SIZE(); r++) {
                    for (int c = 0; c < SIZE(); c++) {
                        char p = board.getPieceAt(r, c);
                        if (p == 'B') whiteUnicorn = true;
                        if (p == 'N') blackUnicorn = true;
                    }
                }
                if (!whiteUnicorn && blackUnicorn) return 0.0;
                if (whiteUnicorn && !blackUnicorn) return 1.0;
            }
            return 0.5;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in game simulation", e);
            return 0.5;
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

    private static int SIZE() {
        return 6;
    }

    private static class Vertex implements Comparable<Vertex> {
        double[] point;
        double value;

        Vertex(double[] point, double value) {
            this.point = point;
            this.value = value;
        }

        @Override
        public int compareTo(Vertex other) {
            return Double.compare(this.value, other.value);
        }
    }
}
