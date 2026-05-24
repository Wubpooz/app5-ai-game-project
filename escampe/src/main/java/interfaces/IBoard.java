package interfaces;

import java.util.List;

import game.EscampeMove;
import game.PlayerColor;

/**
 * Used to chararacterize the boards.he 
 * It has to be impemented by some class in a real game.
 * 
 * @param <Move> Class implementing the moves for the game
 * @param <Role> Class implementing the roles for the game
 * 
 * 
 */
public interface IBoard<M extends IMove, R extends IRole, B extends IBoard<M, R, B>> {

	/**
	 * returns the possible moves a player having the playerRole	
	 * @param playerRole
	 * @return a list of all possible moves for the player having the playerRole
	 */
	List<M> possibleMoves(R playerRole);

	/** play move on the board, played by a player having the playerRole  
	 * 
	 * @param move
	 * @param playerRole
	 * @return the successor board
	 */
	void play(M move, R playerRole);

	/**
	 * checks that the board corresponds to an end of game
	 * @return yes if the game completed
	 */
	boolean isGameOver();

	/**
	 * Returns a deep copy of this board.
	 * @return a deep copy of the board
	 */
	B copy();
}
