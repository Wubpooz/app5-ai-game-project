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

public class ParameterTuner {
    private static final Logger LOGGER = Logger.getLogger(ParameterTuner.class.getName());
    
    private static long lastStartTimeMs = 0;
    private static long lastUpdateMs = 0;
    private static int lastUpdateCurrent = 0;
    private static double currentSpeed = -1.0;

    private static final String[] PARAM_NAMES = {
        "weightMinDist",
        "weightAvgDist",
        "weightUnicornDangerMinDist",
        "weightUnicornDangerAvgDist",
        "weightEscapability",
        "weightTrappedUnicorn",
        "weightLegalMoves",
        "weightBandControl",
        "weightOppBandControl"
    };

    // Default weights (starting point)
    private static final double[] DEFAULT_THETA = {10.0, 2.0, 5.0, 5.0, 50.0, 30.0, 10.0, 0.0, 0.0};
    
    // Scale vectors to adjust search step sizes to the range of each parameter
    private static final double[] SCALE = {10.0, 5.0, 5.0, 5.0, 30.0, 20.0, 10.0, 0.0, 0.0};
    
    // Parameter constraints
    private static final double[] MIN_VAL = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] MAX_VAL = {100.0, 50.0, 100.0, 50.0, 300.0, 200.0, 100.0, 0.0, 0.0};

    // SPSA hyperparameters
    private static final double SPSA_A = 15.0;       // Step size parameter a
    private static final double SPSA_C = 3.0;        // Perturbation step size parameter c
    private static final double SPSA_A_OFFSET = 5.0; // Stability offset A
    private static final double SPSA_ALPHA = 0.602;  // Learning rate decay exponent
    private static final double SPSA_GAMMA = 0.101;  // Perturbation size decay exponent

    public static void main(String[] args) {
        LOGGER.info("\n╔════════════════════════════════════════════════════════╗");
        LOGGER.info("║         ESCAMPE SPSA PARAMETER OPTIMIZATION            ║");
        LOGGER.info("╚════════════════════════════════════════════════════════╝\n");

        int iterations = 50;
        int numPairs = 16; // 32 games per iteration
        
        double[] theta = Arrays.copyOf(DEFAULT_THETA, DEFAULT_THETA.length);
        
        int threads = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            LOGGER.info(() -> "Running tuning with " + threads + " threads across " + iterations + " iterations...");
            
            LOGGER.info("\n>>> RUN 1: Tuning All 9 Heuristic Parameters...");
            double[] tuned9 = runSpsa(9, DEFAULT_THETA, SCALE, MIN_VAL, MAX_VAL, false, iterations, numPairs, threads, executor);
            
            LOGGER.info("\n>>> RUN 2: Tuning 7 Heuristic Parameters (Forcing Band Control to 0)...");
            double[] start7 = {DEFAULT_THETA[0], DEFAULT_THETA[1], DEFAULT_THETA[2], DEFAULT_THETA[3], DEFAULT_THETA[4], DEFAULT_THETA[5], DEFAULT_THETA[6]};
            double[] scale7 = {SCALE[0], SCALE[1], SCALE[2], SCALE[3], SCALE[4], SCALE[5], SCALE[6]};
            double[] min7 = {MIN_VAL[0], MIN_VAL[1], MIN_VAL[2], MIN_VAL[3], MIN_VAL[4], MIN_VAL[5], MIN_VAL[6]};
            double[] max7 = {MAX_VAL[0], MAX_VAL[1], MAX_VAL[2], MAX_VAL[3], MAX_VAL[4], MAX_VAL[5], MAX_VAL[6]};
            double[] tuned7 = runSpsa(7, start7, scale7, min7, max7, true, iterations, numPairs, threads, executor);
            
            System.out.println("\nCopy-paste Java code for HeuristicConfig.java (SPSA-Tuned-Full):");
            System.out.format(Locale.US, "return new HeuristicConfig(\"SPSA-Tuned-Full\", %d, %d, %d, %d, %d, %d, %d, %d, %d);\n\n",
                (int) Math.round(tuned9[0]), (int) Math.round(tuned9[1]), (int) Math.round(tuned9[2]), 
                (int) Math.round(tuned9[3]), (int) Math.round(tuned9[4]), (int) Math.round(tuned9[5]), 
                (int) Math.round(tuned9[6]), (int) Math.round(tuned9[7]), (int) Math.round(tuned9[8]));
                
            System.out.println("Copy-paste Java code for HeuristicConfig.java (SPSA-Tuned-NoBand):");
            System.out.format(Locale.US, "return new HeuristicConfig(\"SPSA-Tuned-NoBand\", %d, %d, %d, %d, %d, %d, %d, 0, 0);\n\n",
                (int) Math.round(tuned7[0]), (int) Math.round(tuned7[1]), (int) Math.round(tuned7[2]), 
                (int) Math.round(tuned7[3]), (int) Math.round(tuned7[4]), (int) Math.round(tuned7[5]), 
                (int) Math.round(tuned7[6]));
        }
    }
    
    private static double[] runSpsa(int dim, double[] start, double[] scale, double[] minVal, double[] maxVal, 
                                   boolean forceZeroBand, int iterations, int numPairs, int threads, ExecutorService executor) {
        double[] theta = Arrays.copyOf(start, dim);
        for (int k = 0; k < iterations; k++) {
            final int iter = k;
                
                // 1. Calculate step sizes for this iteration
                double ak = SPSA_A / Math.pow(iter + 1 + SPSA_A_OFFSET, SPSA_ALPHA);
                double ck = SPSA_C / Math.pow((double) iter + 1, SPSA_GAMMA);
                
                // 2. Generate Rademacher perturbation vector delta (elements are either +1 or -1)
                double[] delta = new double[theta.length];
                Random rand = new Random();
                for (int i = 0; i < delta.length; i++) {
                    delta[i] = rand.nextBoolean() ? 1.0 : -1.0;
                }
                
                // 3. Perturb parameters in both directions
                double[] thetaPlus = new double[theta.length];
                double[] thetaMinus = new double[theta.length];
            for (int i = 0; i < dim; i++) {
                thetaPlus[i] = clamp(theta[i] + ck * scale[i] * delta[i], minVal[i], maxVal[i]);
                thetaMinus[i] = clamp(theta[i] - ck * scale[i] * delta[i], minVal[i], maxVal[i]);
            }
            
            // Convert to heuristic configs
            HeuristicConfig hPlus;
            HeuristicConfig hMinus;
            if (forceZeroBand) {
                hPlus = makeConfig("SPSA_Plus", thetaPlus[0], thetaPlus[1], thetaPlus[2], thetaPlus[3], thetaPlus[4], thetaPlus[5], thetaPlus[6], 0, 0);
                hMinus = makeConfig("SPSA_Minus", thetaMinus[0], thetaMinus[1], thetaMinus[2], thetaMinus[3], thetaMinus[4], thetaMinus[5], thetaMinus[6], 0, 0);
            } else {
                hPlus = makeConfig("SPSA_Plus", thetaPlus[0], thetaPlus[1], thetaPlus[2], thetaPlus[3], thetaPlus[4], thetaPlus[5], thetaPlus[6], thetaPlus[7], thetaPlus[8]);
                hMinus = makeConfig("SPSA_Minus", thetaMinus[0], thetaMinus[1], thetaMinus[2], thetaMinus[3], thetaMinus[4], thetaMinus[5], thetaMinus[6], thetaMinus[7], thetaMinus[8]);
            }
                
            AlgorithmConfig configPlus = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, hPlus);
            AlgorithmConfig configMinus = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, hMinus);
                
                // 4. Run matches between configPlus and configMinus in parallel
                List<Callable<Double>> tasks = new ArrayList<>();
                int numOpenings = Opening.WHITE_OPENINGS.length;
                
                for (int pair = 0; pair < numPairs; pair++) {
                    final int pairIdx = pair;
                    tasks.add(() -> {
                        int opIndex = pairIdx % numOpenings;
                        String whiteOpen = Opening.WHITE_OPENINGS[opIndex];
                        String blackOpen = Opening.BLACK_OPENINGS[opIndex];
                        
                        // Game 1: plus is White, minus is Black
                        double score1 = simulateGame(configPlus, configMinus, whiteOpen, blackOpen);
                        
                        // Game 2: minus is White, plus is Black
                        double score2_white = simulateGame(configMinus, configPlus, whiteOpen, blackOpen);
                        double score2 = 1.0 - score2_white; // Score from plus's perspective
                        
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
                    LOGGER.log(Level.SEVERE, "Error during games execution", e);
                    Thread.currentThread().interrupt();
                    return theta;
                }
                
                // 5. Calculate gradient and update parameters
                double scorePlus = totalPoints / (2.0 * numPairs);
                double diff = 2.0 * scorePlus - 1.0; // Delivers value in range [-1.0, 1.0]
                
                double[] nextTheta = new double[theta.length];
            for (int i = 0; i < dim; i++) {
                // Update: theta = theta + ak * scale * (diff / (2 * ck * delta))
                double gHat = diff / (2.0 * ck * delta[i]);
                nextTheta[i] = clamp(theta[i] + ak * scale[i] * gHat, minVal[i], maxVal[i]);
            }
                
                // Display iteration info
                LOGGER.info(() -> String.format(Locale.US, "[Iteration %02d/%02d] ck=%.3f, ak=%.3f | Score(Plus)=%.3f (Diff=%.3f)", 
                    iter + 1, iterations, ck, ak, scorePlus, diff));
                
                LOGGER.info("  Weights:");
            for (int i = 0; i < dim; i++) {
                String msg = String.format(Locale.US, "    %-26s : %5.1f -> %5.1f (perturb: %5.1f, delta: %2.0f)", 
                    PARAM_NAMES[i], theta[i], nextTheta[i], ck * scale[i] * delta[i], delta[i]);
                LOGGER.info(msg);
            }
                
                theta = nextTheta;
            }
            
        LOGGER.info("\n========================================================");
        LOGGER.info("OPTIMIZATION RUN COMPLETE");
        LOGGER.info("========================================================\n");
        return theta;
    }
    
    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
    
    private static HeuristicConfig makeConfig(String name, double p0, double p1, double p2, double p3, double p4, double p5, double p6, double p7, double p8) {
        return new HeuristicConfig(
            name,
            (int) Math.round(p0),
            (int) Math.round(p1),
            (int) Math.round(p2),
            (int) Math.round(p3),
            (int) Math.round(p4),
            (int) Math.round(p5),
            (int) Math.round(p6),
            (int) Math.round(p7),
            (int) Math.round(p8)
        );
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
}
