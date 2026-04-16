package algorithms;

import interfaces.IBoard;
import interfaces.IMove;
import interfaces.IRole;

public interface GameAlgorithm<M extends IMove, R extends IRole, B extends IBoard<M,R,B>> {
		
        M bestMove(B board, R playerRole);
}