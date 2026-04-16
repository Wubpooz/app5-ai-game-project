package interfaces;

import interfaces.IBoard;
import interfaces.IRole;

@FunctionalInterface
public interface IHeuristic<B extends IBoard<?,R, B>, R extends IRole> {
	
	public static int MIN_VALUE = java.lang.Integer.MIN_VALUE;
	public static int MAX_VALUE = java.lang.Integer.MAX_VALUE;
		
	int eval(B board, R role);
}
 