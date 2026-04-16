package games.dominos;

import iialib.games.algs.IHeuristic;

public class DominosHeuristics {
	
  // if you have more moves than your opponent, you are in a better position, so the heuristic returns 2, in equal position it returns 1, and in a worse position it returns 0
	public static IHeuristic<DominosBoard, DominosRole>  hVertical = (board, role) -> {
    if(board.isGameOver()) {
      return role == DominosRole.VERTICAL && board.nbHorizontalMoves() == 0 ? 99 : -99;
    }
    if(board.nbHorizontalMoves() == board.nbVerticalMoves()) {
    	return 1;
    }
    if(role == DominosRole.VERTICAL) {
    	return board.nbVerticalMoves() > board.nbHorizontalMoves() ? 2 : 0;
    } else {
    	return board.nbHorizontalMoves() > board.nbVerticalMoves() ? 2 : 0;
    }
  };
    

	public static IHeuristic<DominosBoard, DominosRole> hHorizontal = (board, role) -> {
    if(board.isGameOver()) {
      return role == DominosRole.HORIZONTAL && board.nbVerticalMoves() == 0 ? 99 : -99;
    }
    if(board.nbHorizontalMoves() == board.nbVerticalMoves()) {
    	return 1;
    }
    if(role == DominosRole.HORIZONTAL) {
    	return board.nbHorizontalMoves() > board.nbVerticalMoves() ? 2 : 0;
    } else {
    	return board.nbVerticalMoves() > board.nbHorizontalMoves() ? 2 : 0;
    }
	};
}
	