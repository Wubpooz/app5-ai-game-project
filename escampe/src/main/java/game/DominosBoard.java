package game;

import interfaces.IBoard;
import game.Score;

import java.util.ArrayList;

public class DominosBoard implements IBoard<DominosMove, DominosRole, DominosBoard> {

	private static int defaultGridSize = 7;

	// --------- Class Attribute ---------

	private static int gridSize = defaultGridSize;

	private enum SQUARE {
		EMPTY, VERTICAL, HORIZONTAL
	}

	// ---------------------- Attributes ---------------------

	private final SQUARE[][] boardGrid;

	// ---------------------- Constructors ---------------------

	public DominosBoard() {
		boardGrid = new SQUARE[gridSize][gridSize];
		for (int i = 0; i < gridSize; i++)
			for (int j = 0; j < gridSize; j++)
				boardGrid[i][j] = SQUARE.EMPTY;
	}

	// Constructors

	public DominosBoard(DominosBoard other) {
		boardGrid = other.copyGrid();
	}

	
	private DominosBoard(SQUARE[][] other) {
		boardGrid = new SQUARE[gridSize][gridSize];
		for (int i = 0; i < gridSize; i++)
			System.arraycopy(other[i], 0, boardGrid[i], 0, gridSize);
	}

	// ------------------- Getters / Setters -------------------

	protected int retGridSize() {
		return gridSize;
	}

	protected static void setGridSize(int n) {
		gridSize = n;
	}

	// --------------------- IBoard Methods ---------------------

	@Override
	public DominosBoard play(DominosMove move, DominosRole playerRole) {
		SQUARE[][] newGrid = copyGrid();
		int x = move.x;
		int y = move.y;
		if (playerRole == DominosRole.VERTICAL) {
			newGrid[x][y] = SQUARE.VERTICAL;
			newGrid[x + 1][y] = SQUARE.VERTICAL;
		} else {
			newGrid[x][y] = SQUARE.HORIZONTAL;
			newGrid[x][y + 1] = SQUARE.HORIZONTAL;
		}
		return new DominosBoard(newGrid);
	}

	@Override
	public ArrayList<DominosMove> possibleMoves(DominosRole playerRole) {
		if (playerRole == DominosRole.VERTICAL) {
			return freeVerticalMoves();
		} else {
			return freeHorizontalMoves();
		}
	}

	@Override
	public boolean isValidMove(DominosMove move, DominosRole playerRole) {
		int x = move.x;
		int y = move.y;
		return (boardGrid[x][y] == SQUARE.EMPTY)
				&& ((playerRole == DominosRole.VERTICAL) ? (boardGrid[x + 1][y] == SQUARE.EMPTY)
						: (boardGrid[x][y + 1] == SQUARE.EMPTY));

	}

	@Override
	public boolean isGameOver() {
		return (this.nbHorizontalMoves() == 0) || (this.nbVerticalMoves() == 0);
	}

	// --------------------- Other Methods ---------------------


	private ArrayList<DominosMove> freeVerticalMoves() {
		ArrayList<DominosMove> allPossibleMoves = new ArrayList<>();
		for (int i = 0; i < gridSize- 1; i++) { 			// lines
			for (int j = 0; j < gridSize ; j++) { 	// columns
				if ((boardGrid[i][j] == SQUARE.EMPTY) && (boardGrid[i + 1][j] == SQUARE.EMPTY)) // possible move
					allPossibleMoves.add(new DominosMove(i, j));
			}
		}
		return allPossibleMoves;
	}

	private ArrayList<DominosMove> freeHorizontalMoves() {
		ArrayList<DominosMove> allPossibleMoves = new ArrayList<>();
		for (int i = 0; i < gridSize; i++) { 		// lines
			for (int j = 0; j < gridSize - 1; j++) { 		// columns
				if ((boardGrid[i][j] == SQUARE.EMPTY) && (boardGrid[i ][j+ 1] == SQUARE.EMPTY)) // possible move
					allPossibleMoves.add(new DominosMove(i, j));
			}
		}
		return allPossibleMoves;
	}

	private SQUARE[][] copyGrid() {
		SQUARE[][] newGrid = new SQUARE[gridSize][gridSize];
		for (int i = 0; i < gridSize; i++)
			System.arraycopy(boardGrid[i], 0, newGrid[i], 0, gridSize);
		return newGrid;
	}

	public String toString() {
		StringBuilder retstr = new StringBuilder("");
		for (int i = 0; i < gridSize; i++) {
			for (int j = 0; j < gridSize; j++)
				if (boardGrid[i][j] == SQUARE.EMPTY)
					retstr.append("-");
				else if (boardGrid[i][j] == SQUARE.VERTICAL)
					retstr.append("V");
				else // damier[i][j] == NOIR
					retstr.append("H");
			retstr.append("\n");
		}
		return retstr.toString();
	}

	public int nbHorizontalMoves() {
		int nbMoves = 0;
		for (int i = 0; i < gridSize; i++) {
			for (int j = 0; j < gridSize - 1; j++) {
				if (boardGrid[i][j] == SQUARE.EMPTY && boardGrid[i][j + 1] == SQUARE.EMPTY)
					nbMoves++;
			}
		}
		return nbMoves;
	}

	public int nbVerticalMoves() {
		int nbMoves = 0;
		for (int i = 0; i < gridSize; i++) {
			for (int j = 0; j < gridSize - 1; j++) {
				if (boardGrid[j][i] == SQUARE.EMPTY && boardGrid[j + 1][i] == SQUARE.EMPTY)
					nbMoves++;
			}
		}
		return nbMoves;
	}

	@Override
	public ArrayList<Score<DominosRole>> getScores() {
		ArrayList<Score<DominosRole>> scores = new ArrayList<>();
		if(this.isGameOver()) {
			if (nbHorizontalMoves() == 0) {
				scores.add(new Score<>(DominosRole.HORIZONTAL,Score.Status.LOOSE,0));
				scores.add(new Score<>(DominosRole.VERTICAL,Score.Status.WIN,1));
			}
			else {
				scores.add(new Score<>(DominosRole.HORIZONTAL,Score.Status.WIN,1));
				scores.add(new Score<>(DominosRole.VERTICAL,Score.Status.LOOSE,0));
			}			
		}
		return scores;
	}

}
