package ranking;

import algorithms.GameAlgorithm;
import algorithms.search.AlphaBeta;
import algorithms.search.AlphaBetaKH;
import algorithms.search.MiniMax;
import algorithms.search.Negamax;
import algorithms.search.NegamaxKH;
import algorithms.search.PonderingGameAlgorithm;
import algorithms.evaluation.Heuristic;
import algorithms.evaluation.HeuristicConfig;
import game.EscampeBoard;
import game.EscampeMove;
import game.PlayerColor;

public class AlgorithmConfig {
    
    public enum AlgorithmType {
        ALPHABETA("AlphaBeta"),
        MINIMAX("MiniMax"),
        NEGAMAX("Negamax"),
        ALPHABETA_KH("AlphaBeta+KH"),
        NEGAMAX_KH("Negamax+KH");
        
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
    private final boolean iterativeDeepening;
    private final boolean ponder;
    
    public AlgorithmConfig(AlgorithmType type, int depth, HeuristicConfig heuristicConfig) {
        this.type = type;
        this.depth = depth;
        this.heuristicConfig = heuristicConfig;
        this.iterativeDeepening = false;
        this.ponder = false;
    }

    public AlgorithmConfig(AlgorithmType type, int depth, HeuristicConfig heuristicConfig, boolean iterativeDeepening) {
        this.type = type;
        this.depth = depth;
        this.heuristicConfig = heuristicConfig;
        this.iterativeDeepening = iterativeDeepening;
        this.ponder = false;
    }

    public AlgorithmConfig(AlgorithmType type, int depth, HeuristicConfig heuristicConfig, boolean iterativeDeepening, boolean ponder) {
        this.type = type;
        this.depth = depth;
        this.heuristicConfig = heuristicConfig;
        this.iterativeDeepening = iterativeDeepening;
        this.ponder = ponder;
    }
    
    public GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> createInstance(
            PlayerColor playerRole, PlayerColor opponentRole) {
        Heuristic heuristic = new Heuristic(heuristicConfig);
        GameAlgorithm<EscampeMove, PlayerColor, EscampeBoard> baseInstance;
        switch (type) {
            case ALPHABETA:
                baseInstance = new AlphaBeta<>(playerRole, opponentRole, heuristic, depth);
                break;
            case MINIMAX:
                baseInstance = new MiniMax<>(playerRole, opponentRole, heuristic, depth);
                break;
            case NEGAMAX:
                baseInstance = new Negamax<>(playerRole, opponentRole, heuristic, depth, iterativeDeepening);
                break;
            case ALPHABETA_KH:
                baseInstance = new AlphaBetaKH(playerRole, opponentRole, heuristic, depth);
                break;
            case NEGAMAX_KH:
                baseInstance = new NegamaxKH(playerRole, opponentRole, heuristic, depth, iterativeDeepening);
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + type);
        }
        if (ponder) {
            return new PonderingGameAlgorithm(baseInstance, playerRole, opponentRole);
        }
        return baseInstance;
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

    public boolean isPonder() {
        return ponder;
    }
    
    @Override
    public String toString() {
        return type.getDisplayName() + (ponder ? "+Ponder" : "") + " (d=" + depth + ", h=" + heuristicConfig.name + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmConfig that = (AlgorithmConfig) o;
        return depth == that.depth && type == that.type && heuristicConfig.name.equals(that.heuristicConfig.name) && ponder == that.ponder;
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + depth;
        result = 31 * result + heuristicConfig.name.hashCode();
        result = 31 * result + (ponder ? 1 : 0);
        return result;
    }
}
