package model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.charset.StandardCharsets;

import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;
import algorithms.search.AlphaBeta;
import algorithms.evaluation.Heuristic;
import algorithms.evaluation.HeuristicConfig;
import algorithms.Opening;

public class DataExport {
  static final int DEFAULT_LABEL_DEPTH = 5;
  static final int DEFAULT_NUM_POSITIONS = 50_000;
  static final String DEFAULT_OUTPUT_FILE = "training_data.json";
  static final float MATE_SCORE = 100000f;

  private static long lastStartTimeMs = 0;
  private static long lastUpdateMs = 0;
  private static int lastUpdateCurrent = 0;
  private static double currentSpeed = -1.0;

  public static void main(String[] args) throws Exception {
    int numPositions = DEFAULT_NUM_POSITIONS;
    String outputFile = DEFAULT_OUTPUT_FILE;
    int labelDepth = DEFAULT_LABEL_DEPTH;
    long seed = 42;
    String heuristicName = "default";
    int numThreads = Runtime.getRuntime().availableProcessors();

    // Parse command line arguments using an iterator to avoid modifying the loop counter
    List<String> argList = Arrays.asList(args);
    ListIterator<String> it = argList.listIterator();
    while (it.hasNext()) {
      String a = it.next();
      if ("--size".equals(a) && it.hasNext()) {
        numPositions = Integer.parseInt(it.next());
      } else if ("--output".equals(a) && it.hasNext()) {
        outputFile = it.next();
      } else if ("--depth".equals(a) && it.hasNext()) {
        labelDepth = Integer.parseInt(it.next());
      } else if ("--seed".equals(a) && it.hasNext()) {
        seed = Long.parseLong(it.next());
      } else if ("--heuristic".equals(a) && it.hasNext()) {
        heuristicName = it.next();
      } else if ("--threads".equals(a) && it.hasNext()) {
        numThreads = Integer.parseInt(it.next());
      }
    }

    System.out.println("Dataset Exporter Configuration:");
    System.out.println("  Positions to generate: " + numPositions);
    System.out.println("  Output file:          " + outputFile);
    System.out.println("  Label search depth:   " + labelDepth);
    System.out.println("  Random seed:          " + seed);
    System.out.println("  Heuristic config:     " + heuristicName);
    System.out.println("  Threads:              " + numThreads);

    AtomicInteger generated = new AtomicInteger(0);
    ReentrantLock ioLock = new ReentrantLock();
    ObjectMapper mapper = new ObjectMapper();
    long startTimeMs = System.currentTimeMillis();

    try(BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
      bw.write("[\n");

      try(ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
        for(int t = 0; t < numThreads; t++) {
          final int threadId = t;
          final int targetPositions = numPositions;
          final long finalSeed = seed;
          final String finalHeuristicName = heuristicName;
          final int finalLabelDepth = labelDepth;

          executor.submit(() -> {
            Random rng = new Random(finalSeed + threadId);
            EscampeBoard board = new EscampeBoard();
            HeuristicConfig config = createHeuristicConfig(finalHeuristicName);
            Heuristic heuristic = new Heuristic(config);
            
            AlphaBeta<EscampeMove, PlayerColor, EscampeBoard> engineWhite = new AlphaBeta<>(PlayerColor.WHITE, PlayerColor.BLACK, heuristic);
            AlphaBeta<EscampeMove, PlayerColor, EscampeBoard> engineBlack = new AlphaBeta<>(PlayerColor.BLACK, PlayerColor.WHITE, heuristic);

            while(generated.get() < targetPositions) {
              // Reset the board and play a random opening placement for both players
              board.initializeBoard();
              String whiteOpening = Opening.WHITE_OPENINGS[rng.nextInt(Opening.WHITE_OPENINGS.length)];
              board.play(new EscampeMove(whiteOpening), PlayerColor.WHITE);
              String blackOpening = Opening.BLACK_OPENINGS[rng.nextInt(Opening.BLACK_OPENINGS.length)];
              board.play(new EscampeMove(blackOpening), PlayerColor.BLACK);

              boolean whiteToMove = true; // White moves first after placement phase

              // Play a random-ish game, collect positions along the way
              int maxPly = 60 + rng.nextInt(40);
              for(int ply = 0; ply < maxPly && !board.isGameOver(); ply++) {
                if(generated.get() >= targetPositions) break;

                // Every few plies, label current position
                if(ply > 4 && rng.nextFloat() < 0.3f) {
                  AlphaBeta<EscampeMove, PlayerColor, EscampeBoard> engine = whiteToMove ? engineWhite : engineBlack;
                  float score = engine.evaluate(board, finalLabelDepth);
                  // Normalize to [-1, 1]
                  float normalized = Math.max(-1f, Math.min(1f, score / MATE_SCORE));

                  // Extract bitboards and unicorn indices
                  long whitePaladins = 0L;
                  long blackPaladins = 0L;
                  int whiteUnicorn = -1;
                  int blackUnicorn = -1;

                  for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < 6; c++) {
                      char piece = board.getPieceAt(r, c);
                      int sq = r * 6 + c;
                      if (piece == 'b') {
                        whitePaladins |= (1L << sq);
                      } else if (piece == 'n') {
                        blackPaladins |= (1L << sq);
                      } else if (piece == 'B') {
                        whiteUnicorn = sq;
                      } else if (piece == 'N') {
                        blackUnicorn = sq;
                      }
                    }
                  }

                  int requiredBand = 0;
                  if (board.getLastMoveRow() != -1) {
                    requiredBand = board.getLiseretAt(board.getLastMoveRow(), board.getLastMoveCol());
                  }

                  Map<String, Object> entry = new HashMap<>();
                  entry.put("white_paladins", whitePaladins);
                  entry.put("white_unicorn", whiteUnicorn);
                  entry.put("black_paladins", blackPaladins);
                  entry.put("black_unicorn", blackUnicorn);
                  entry.put("required_band", requiredBand);
                  entry.put("white_to_move", whiteToMove);
                  entry.put("score", normalized); // from current player's perspective

                  String json = null;
                  try {
                    json = mapper.writeValueAsString(entry);
                  } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    e.printStackTrace();
                  }

                  if (json != null) {
                    ioLock.lock();
                    try {
                      int currentCount = generated.get();
                      if (currentCount >= targetPositions) {
                        break;
                      }

                      if (currentCount > 0) {
                        bw.write(",\n");
                      }
                      bw.write(json);

                      int updatedCount = generated.incrementAndGet();
                      printProgress(updatedCount, targetPositions, startTimeMs, "pos");
                      if (updatedCount >= targetPositions) {
                        System.out.println();
                        break;
                      }
                    } catch (IOException e) {
                      e.printStackTrace();
                    } finally {
                      ioLock.unlock();
                    }
                  }
                }

                if (generated.get() >= targetPositions) break;

                // Get legal moves
                List<EscampeMove> moves = board.possibleMoves(whiteToMove ? PlayerColor.WHITE : PlayerColor.BLACK);
                if (moves.isEmpty()) {
                  board.play(new EscampeMove("E"), whiteToMove ? PlayerColor.WHITE : PlayerColor.BLACK);
                  whiteToMove = !whiteToMove;
                  continue;
                }

                // Play a semi-random move (mix of best and random for diversity)
                EscampeMove move;
                if (rng.nextFloat() < 0.7f) {
                  move = moves.get(rng.nextInt(moves.size())); // random
                } else {
                  AlphaBeta<EscampeMove, PlayerColor, EscampeBoard> engine = 
                      whiteToMove ? engineWhite : engineBlack;
                  move = engine.bestMove(board, whiteToMove ? PlayerColor.WHITE : PlayerColor.BLACK, 1000);
                }

                board.play(move, whiteToMove ? PlayerColor.WHITE : PlayerColor.BLACK);
                whiteToMove = !whiteToMove;
              }
            }
          });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      }

      bw.write("\n]\n");
      System.out.println("Generated " + generated.get() + " positions → " + outputFile);

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
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

  private static HeuristicConfig createHeuristicConfig(String name) {
    return switch (name.toLowerCase()) {
      case "default" -> HeuristicConfig.createDefault();
      case "spsa-full" -> HeuristicConfig.createSpsaFull();
      case "spsa-nobland" -> HeuristicConfig.createSpsaNoBand();
      case "bayes-full" -> HeuristicConfig.createBayesFull();
      case "bayes-nobland" -> HeuristicConfig.createBayesNoBand();
      case "simplex-full" -> HeuristicConfig.createSimplexFull();
      case "simplex-nobland" -> HeuristicConfig.createSimplexNoBand();
      default -> {
        System.err.println("Unknown heuristic: " + name + ". Using default.");
        yield HeuristicConfig.createDefault();
      }
    };
  }
}
