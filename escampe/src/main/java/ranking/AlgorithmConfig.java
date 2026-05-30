package ranking;

import algorithms.GameAlgorithm;
import algorithms.search.AlphaBeta;
import algorithms.search.MiniMax;
import algorithms.search.Negamax;
import algorithms.evaluation.Heuristic;
import algorithms.evaluation.HeuristicConfig;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;

public class AlgorithmConfig {
    
    public enum AlgorithmType {
        ALPHABETA("AlphaBeta"),
        MINIMAX("MiniMax"),
        NEGAMAX("Negamax");
        
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
    private final HeuristicConfig heuristicConfig;
    
    public AlgorithmConfig(AlgorithmType type, int depth, HeuristicConfig heuristicConfig) {
        this.type = type;
        this.depth = depth;
        this.heuristicConfig = heuristicConfig;
    }
    
    public GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> createInstance(
            PlayerColor playerRole, PlayerColor opponentRole) {
        Heuristic heuristic = new Heuristic(heuristicConfig);
        switch (type) {
            case ALPHABETA:
                return new AlphaBeta<>(playerRole, opponentRole, heuristic, depth);
            case MINIMAX:
                return new MiniMax<>(playerRole, opponentRole, heuristic, depth);
            case NEGAMAX:
                return new Negamax<>(playerRole, opponentRole, heuristic, depth);
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + type);
        }
    }
    
    public AlgorithmType getType() {
        return type;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public HeuristicConfig getHeuristicConfig() {
        return heuristicConfig;
    }
    
    @Override
    public String toString() {
        return type.getDisplayName() + " (d=" + depth + ", h=" + heuristicConfig.name + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmConfig that = (AlgorithmConfig) o;
        return depth == that.depth && type == that.type && heuristicConfig.name.equals(that.heuristicConfig.name);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + depth;
        result = 31 * result + heuristicConfig.name.hashCode();
        return result;
    }
}
