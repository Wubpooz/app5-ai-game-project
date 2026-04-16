package games.dominos.ranking;

import java.util.logging.Logger;

/**
 * Entry point for running the AI Tournament Ranking System.
 * Executes comprehensive "All vs All" tournaments with multiple algorithm configurations.
 */
public class TournamentRunner {
    private static final Logger LOGGER = Logger.getLogger(TournamentRunner.class.getName());
    private static final String DEFAULT_HEURISTIC = "DEFAULT";
    
    public static void main(String[] args) {
        LOGGER.info("\n╔════════════════════════════════════════════════════════╗");
        LOGGER.info("║         DOMINOS AI TOURNAMENT RANKING SYSTEM            ║");
        LOGGER.info("╚════════════════════════════════════════════════════════╝\n");
        
        // Create ranking system: 50 matches per pairing
        AIRankingSystem ranking = new AIRankingSystem(50);
        
        // Configure algorithms to test
        configureAlgorithms(ranking);
        
        // Execute the tournament
        ranking.executeFullTournament();
    }
    
    private static void configureAlgorithms(AIRankingSystem ranking) {
        LOGGER.info("Configuring algorithms for tournament...\n");
        
        // AlphaBeta configurations
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, DEFAULT_HEURISTIC));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 4, DEFAULT_HEURISTIC));
        
        // MiniMax configurations
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.MINIMAX, 1, DEFAULT_HEURISTIC));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.MINIMAX, 2, DEFAULT_HEURISTIC));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.MINIMAX, 3, DEFAULT_HEURISTIC));
        
        LOGGER.info("✓ 5 algorithm configurations registered");
        LOGGER.info("✓ Total matchups: 10 (5 choose 2)");
        LOGGER.info("✓ Total games: 500 (10 matchups × 50 games)\n");
    }
}
