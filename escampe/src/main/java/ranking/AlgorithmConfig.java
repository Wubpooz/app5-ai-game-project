package games.dominos.ranking;

import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.algorithms.AlphaBeta;
import iialib.games.algs.algorithms.MiniMax;
import iialib.games.algs.IHeuristic;
import games.dominos.DominosBoard;
import games.dominos.DominosMove;
import games.dominos.DominosRole;
import games.dominos.DominosHeuristics;

/**
 * Represents a configuration of an AI algorithm for ranking and comparison.
 * Encapsulates algorithm type, search depth, and heuristic function.
 */
public class AlgorithmConfig {
    
    public enum AlgorithmType {
        ALPHABETA("AlphaBeta"),
        MINIMAX("MiniMax");
        
        private final String displayName;
        
        AlgorithmType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final AlgorithmType type;
    private final int depth;
    private final String heuristicName;
    
    public AlgorithmConfig(AlgorithmType type, int depth, String heuristicName) {
        this.type = type;
        this.depth = depth;
        this.heuristicName = heuristicName;
    }
    
    /**
     * Creates a new algorithm instance for the given player role
     */
    public GameAlgorithm<DominosMove, DominosRole, DominosBoard> createInstance(
            DominosRole playerRole, DominosRole opponentRole) {
        
        switch (type) {
            case ALPHABETA:
                return new AlphaBeta<>(
                    playerRole, 
                    opponentRole, 
                    selectHeuristic(playerRole), 
                    depth
                );
            case MINIMAX:
                return new MiniMax<>(
                    playerRole,
                    opponentRole,
                    selectHeuristic(playerRole),
                    depth
                );
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + type);
        }
    }
    
    /**
     * Selects appropriate heuristic based on player role
     */
    private IHeuristic<DominosBoard, DominosRole> selectHeuristic(DominosRole role) {
        if ("VERTICAL".equalsIgnoreCase(heuristicName)) {
            return DominosHeuristics.hVertical;
        } else if ("HORIZONTAL".equalsIgnoreCase(heuristicName)) {
            return DominosHeuristics.hHorizontal;
        }
        // Default heuristic based on role
        return role == DominosRole.VERTICAL ? 
            DominosHeuristics.hVertical : 
            DominosHeuristics.hHorizontal;
    }
    
    public AlgorithmType getType() {
        return type;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public String getHeuristicName() {
        return heuristicName;
    }
    
    @Override
    public String toString() {
        return type.getDisplayName() + " (d=" + depth + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmConfig that = (AlgorithmConfig) o;
        return depth == that.depth && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() * 31 + depth;
    }
}
