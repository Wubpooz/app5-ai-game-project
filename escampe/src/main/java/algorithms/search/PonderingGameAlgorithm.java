package algorithms.search;

import algorithms.GameAlgorithm;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PonderingGameAlgorithm implements GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> {
    private final GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> base;
    private final PlayerColor role;
    private final PlayerColor opponentRole;

    private EscampeBoard lastBoardAtEndOfMyTurn = null;
    private EscampeMove predictedOpponentMove = null;
    private Thread ponderThread = null;
    private final AtomicReference<EscampeMove> ponderedResponse = new AtomicReference<>(null);
    private boolean isPonderHit = false;

    public PonderingGameAlgorithm(GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> base,
                                  PlayerColor role, PlayerColor opponentRole) {
        this.base = base;
        this.role = role;
        this.opponentRole = opponentRole;
    }

    @Override
    public EscampeMove bestMove(EscampeBoard board, PlayerColor playerRole, long remainingTimeMs) {
        // Detect opponent's actual move by comparing current board with the state at the end of our last turn
        if (lastBoardAtEndOfMyTurn != null) {
            EscampeMove actualMove = detectOpponentMove(lastBoardAtEndOfMyTurn, board);
            if (actualMove != null && predictedOpponentMove != null && actualMove.toString().equals(predictedOpponentMove.toString())) {
                isPonderHit = true;
            } else {
                stopPonder();
            }
        } else {
            stopPonder();
        }

        EscampeMove chosenMove = null;
        if (isPonderHit) {
            // Wait briefly for the ponder thread to finish if it hasn't already
            if (ponderThread != null && ponderThread.isAlive()) {
                try {
                    ponderThread.join(100); // Wait up to 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            chosenMove = ponderedResponse.get();
            stopPonder();
        } else {
            stopPonder();
        }

        if (chosenMove == null) {
            chosenMove = base.bestMove(board, playerRole, remainingTimeMs);
        }

        // Setup pondering for the next turn
        startPonder(board, chosenMove);

        return chosenMove;
    }

    private EscampeMove detectOpponentMove(EscampeBoard before, EscampeBoard after) {
        List<EscampeMove> possible = before.possibleMoves(opponentRole);
        for (EscampeMove m : possible) {
            EscampeBoard test = before.copy();
            test.play(m, opponentRole);
            if (test.toString().equals(after.toString())) {
                return m;
            }
        }
        return null;
    }

    private void startPonder(EscampeBoard board, EscampeMove ourMove) {
        if (board.isGameOver()) return;
        if (board.getLastMoveRow() == -1 || board.getLastMoveCol() == -1) return;

        try {
            EscampeBoard predictionBoard = board.copy();
            predictionBoard.play(ourMove, role);

            // Predict opponent's best response (using a quick 50ms search)
            EscampeMove predicted = base.bestMove(predictionBoard, opponentRole, 50);
            if (predicted == null) return;

            this.predictedOpponentMove = predicted;
            this.isPonderHit = false;

            predictionBoard.play(predicted, opponentRole);
            
            this.lastBoardAtEndOfMyTurn = predictionBoard.copy();

            ponderThread = new Thread(() -> {
                try {
                    // Search for our best response to the predicted move
                    EscampeMove response = base.bestMove(predictionBoard, role, 4000);
                    if (!Thread.currentThread().isInterrupted()) {
                        ponderedResponse.set(response);
                    }
                } catch (Exception ignored) {}
            }, "tournament-ponder-thread");
            ponderThread.setDaemon(true);
            ponderThread.start();

        } catch (Exception e) {
            // ignore
        }
    }

    private void stopPonder() {
        if (ponderThread != null) {
            ponderThread.interrupt();
            try {
                ponderThread.join(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ponderThread = null;
        }
        ponderedResponse.set(null);
        predictedOpponentMove = null;
        lastBoardAtEndOfMyTurn = null;
        isPonderHit = false;
    }

    @Override
    public String toString() {
        return base.toString() + "+Ponder";
    }
}
