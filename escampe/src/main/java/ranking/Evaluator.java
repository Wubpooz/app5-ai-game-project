package ranking;

import algorithms.GameAlgorithm;
import algorithms.Opening;
import algorithms.evaluation.HeuristicConfig;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;

import java.util.*;
import java.util.concurrent.*;

/**
 * Command-line evaluator for heuristic weight tuning.
 * Usage: java ranking.Evaluator <w1> <w2> <w3> <w4> <w5> <w6> <w7> <w8> <w9>
 * Runs 16 games (8 openings, swapping colors) against the No-bandcontrol baseline.
 * Outputs: SCORE: <win_rate>
 */
public class Evaluator {

    public static void main(String[] args) {
        if (args.length < 9) {
            System.err.println("Usage: java ranking.Evaluator <minDist> <avgDist> <dangerMin> <dangerAvg> <escapability> <trapped> <legalMoves> <bandControl> <oppBandControl>");
            System.exit(1);
        }

        int weightMinDist = Integer.parseInt(args[0]);
        int weightAvgDist = Integer.parseInt(args[1]);
        int weightUnicornDangerMinDist = Integer.parseInt(args[2]);
        int weightUnicornDangerAvgDist = Integer.parseInt(args[3]);
        int weightEscapability = Integer.parseInt(args[4]);
        int weightTrappedUnicorn = Integer.parseInt(args[5]);
        int weightLegalMoves = Integer.parseInt(args[6]);
        int weightBandControl = Integer.parseInt(args[7]);
        int weightOppBandControl = Integer.parseInt(args[8]);

        HeuristicConfig candidate = new HeuristicConfig(
            "Candidate",
            weightMinDist, weightAvgDist, weightUnicornDangerMinDist, weightUnicornDangerAvgDist,
            weightEscapability, weightTrappedUnicorn, weightLegalMoves,
            weightBandControl, weightOppBandControl
        );
        AlgorithmConfig candidateConfig = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, candidate);

        // Baseline: Default base (No-bandcontrol)
        HeuristicConfig baseline = HeuristicConfig.createDefault();
        AlgorithmConfig baselineConfig = new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, baseline);

        int numOpenings = Opening.WHITE_OPENINGS.length;
        int numPairs = 16; // 32 games for Bayes opt evaluator
        int threads = Runtime.getRuntime().availableProcessors();

        double totalPoints = 0.0;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Callable<Double>> tasks = new ArrayList<>();
            for (int pair = 0; pair < numPairs; pair++) {
                final int pairIdx = pair;
                tasks.add(() -> {
                    int opIndex = pairIdx % numOpenings;
                    String whiteOpen = Opening.WHITE_OPENINGS[opIndex];
                    String blackOpen = Opening.BLACK_OPENINGS[opIndex];

                    double score1 = simulateGame(candidateConfig, baselineConfig, whiteOpen, blackOpen);
                    double score2_white = simulateGame(baselineConfig, candidateConfig, whiteOpen, blackOpen);
                    double score2 = 1.0 - score2_white;

                    return score1 + score2;
                });
            }

            List<Future<Double>> results = executor.invokeAll(tasks);
            for (Future<Double> res : results) {
                totalPoints += res.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error during evaluation: " + e.getMessage());
            System.exit(2);
        }

        double winRate = totalPoints / (2.0 * numPairs);
        System.out.printf(Locale.US, "SCORE: %.6f%n", winRate);
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
                for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < 6; c++) {
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
            return 0.5;
        }
    }
}
