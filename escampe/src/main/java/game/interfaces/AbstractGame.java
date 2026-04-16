package iialib.games.algs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import iialib.games.model.IBoard;
import iialib.games.model.IMove;
import iialib.games.model.IRole;
import iialib.games.model.Score;

public abstract class AbstractGame<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> {

    private static final Logger LOGGER = Logger.getLogger(AbstractGame.class.getName());

    // Attributes
    protected B currentBoard;
    protected List<AIPlayer<M,R,B>> players;

    // Constructor
    protected AbstractGame(List<AIPlayer<M,R,B>> players, B initialBoard) {
        this.currentBoard = initialBoard;
        this.players = players;
    }

    protected B getCurrentBoard() {
        return currentBoard;
    }

    protected List<AIPlayer<M,R,B>> getPlayers() {
        return players;
    }

    // Methods
    public void runGame() {
        int index = 0;
        AIPlayer<M,R,B> currentPlayer = players.get(index);
        LOGGER.log(Level.INFO, "Game beginning - First player is : {0}", currentPlayer);
        LOGGER.info("The board is :");
        LOGGER.info(() -> currentBoard.toString());

        while (!currentBoard.isGameOver()) {
            LOGGER.log(Level.INFO, "Next player is : {0}", currentPlayer);
            M nextMove = currentPlayer.bestMove(currentBoard);
            LOGGER.log(Level.INFO, "Best Move is : {0}", nextMove);
            currentBoard = currentPlayer.playMove(currentBoard, nextMove);
            LOGGER.info("The board is :");
            LOGGER.info(() -> currentBoard.toString());
            index = 1 - index;
            currentPlayer = players.get(index);
        }

        LOGGER.info("Game over !");
        ArrayList<Score<R>> scores = currentBoard.getScores();
        for (AIPlayer<M,R,B> p : players) {
            for (Score<R> s : scores) {
                if (p.getRole() == s.getRole()) {
                    LOGGER.info(() -> p + " score is : " + s.getStatus() + " " + s.getScore());
                }
            }
        }
    }
}
