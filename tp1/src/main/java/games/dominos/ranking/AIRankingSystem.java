package games.dominos.ranking;

import games.dominos.*;
import iialib.games.algs.AIPlayer;
import iialib.games.algs.GameAlgorithm;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tournament ranking system for AI algorithms.
 * Executes comprehensive "All vs All" tournament matches and ranks algorithms by win rate.
 */
public class AIRankingSystem {
    
    private static final Logger LOGGER = Logger.getLogger(AIRankingSystem.class.getName());
    private final int matchesPerPairing;
    private final List<AlgorithmConfig> algorithms;
    private final Map<String, MatchResult> results;
    private final Map<String, Integer> winCounts;
    
    /**
     * Creates ranking system with specified algorithms and matches per pairing
     */
    public AIRankingSystem(int matchesPerPairing) {
        this.matchesPerPairing = matchesPerPairing;
        this.algorithms = new ArrayList<>();
        this.results = new HashMap<>();
        this.winCounts = new HashMap<>();
    }
    
    /**
     * Adds an algorithm configuration to the tournament
     */
    public void addAlgorithm(AlgorithmConfig config) {
        if (!algorithms.contains(config)) {
            algorithms.add(config);
            winCounts.put(config.toString(), 0);
        }
    }
    
    /**
     * Executes the full "All vs All" tournament
     */
    public void executeFullTournament() {
        LOGGER.info(() -> "\n" + "=".repeat(60));
        LOGGER.info("AI TOURNAMENT RANKING SYSTEM");
        LOGGER.info(() -> "Matches per pairing: " + matchesPerPairing);
        LOGGER.info(() -> "Total algorithms: " + algorithms.size());
        LOGGER.info(() -> "=".repeat(60) + "\n");
        
        int totalMatchups = 0;
        for (int i = 0; i < algorithms.size(); i++) {
            for (int j = i + 1; j < algorithms.size(); j++) {
                totalMatchups++;
            }
        }
        
        int currentMatchup = 0;
        for (int i = 0; i < algorithms.size(); i++) {
            for (int j = i + 1; j < algorithms.size(); j++) {
                currentMatchup++;
                AlgorithmConfig algo1 = algorithms.get(i);
                AlgorithmConfig algo2 = algorithms.get(j);
                
                LOGGER.log(Level.INFO, "[{0}/{1}] {2} vs {3}", new Object[]{currentMatchup, totalMatchups, algo1, algo2});
                
                executeMatchSeries(algo1, algo2);
                LOGGER.info("");
            }
        }
        
        displayRankings();
    }
    
    /**
     * Executes a series of matches between two algorithms
     */
    private void executeMatchSeries(AlgorithmConfig algo1, AlgorithmConfig algo2) {
        MatchResult result = new MatchResult(algo1, algo2, matchesPerPairing);
        
        for (int match = 0; match < matchesPerPairing; match++) {
            // Alternate who goes first to be fair
            boolean algo1First = (match % 2 == 0);
            
            DominosRole roleAlgo1 = algo1First ? DominosRole.VERTICAL : DominosRole.HORIZONTAL;
            DominosRole roleAlgo2 = algo1First ? DominosRole.HORIZONTAL : DominosRole.VERTICAL;
            
            DominosRole winner = executeSingleMatch(algo1, algo2, roleAlgo1, roleAlgo2);
            
            if (winner == roleAlgo1) {
                result.recordWin(algo1);
                winCounts.put(algo1.toString(), winCounts.get(algo1.toString()) + 1);
            } else if (winner == roleAlgo2) {
                result.recordWin(algo2);
                winCounts.put(algo2.toString(), winCounts.get(algo2.toString()) + 1);
            } else {
                result.recordDraw();
            }
            
            // Progress indicator
            if ((match + 1) % 10 == 0) {
                LOGGER.info(() -> ".");
            }
        }
        
        LOGGER.info(() -> " Complete: " + algo1 + " " + 
            String.format("%.1f%%", result.getWinRate1() * 100) + " vs " + 
            String.format("%.1f%%", result.getWinRate2() * 100) + " " + algo2);
        
        String key = algo1 + " vs " + algo2;
        results.put(key, result);
    }
    
    /**
     * Executes a single match between two algorithms
     */
    private DominosRole executeSingleMatch(AlgorithmConfig algo1, AlgorithmConfig algo2,
                                          DominosRole roleAlgo1, DominosRole roleAlgo2) {
        try {
            // Create algorithm instances
            GameAlgorithm<DominosMove, DominosRole, DominosBoard> algInstance1 = 
                algo1.createInstance(roleAlgo1, roleAlgo2);
            GameAlgorithm<DominosMove, DominosRole, DominosBoard> algInstance2 = 
                algo2.createInstance(roleAlgo2, roleAlgo1);
            
            // Reset statistics
            resetStatistics(algInstance1);
            resetStatistics(algInstance2);
            
            // Create players
            AIPlayer<DominosMove, DominosRole, DominosBoard> player1 = 
                new AIPlayer<>(roleAlgo1, algInstance1);
            AIPlayer<DominosMove, DominosRole, DominosBoard> player2 = 
                new AIPlayer<>(roleAlgo2, algInstance2);
            
            // Create game
            ArrayList<AIPlayer<DominosMove, DominosRole, DominosBoard>> players = new ArrayList<>();
            players.add(player1);
            players.add(player2);
            
            DominosBoard initialBoard = new DominosBoard();
            
            // Silent game execution (no output)
            DominosGame game = new DominosGame(players, initialBoard, algInstance1, algInstance2, true);
            game.runGame();
            
            // Get winner from game after execution
            return game.getWinner();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in match execution", e);
            return null;
        }
    }
    
    /**
     * Resets statistics counters if algorithm supports it
     */
    private void resetStatistics(GameAlgorithm<DominosMove, DominosRole, DominosBoard> algorithm) {
        try {
            if (algorithm instanceof iialib.games.algs.algorithms.AlphaBeta<?, ?, ?>) {
                ((iialib.games.algs.algorithms.AlphaBeta<DominosMove, DominosRole, DominosBoard>) algorithm).resetStatistics();
            } else if (algorithm instanceof iialib.games.algs.algorithms.MiniMax<?, ?, ?>) {
                ((iialib.games.algs.algorithms.MiniMax<DominosMove, DominosRole, DominosBoard>) algorithm).resetStatistics();
            }
        } catch (Exception e) {
            // Silently ignore if reset not available
        }
    }
    
    /**
     * Displays final rankings in a formatted table
     */
    public void displayRankings() {
        LOGGER.info(() -> "\n" + "=".repeat(70));
        LOGGER.info("FINAL RANKINGS (by Total Wins)");
        LOGGER.info(() -> "=".repeat(70));
        
        // Sort algorithms by win count
        List<Map.Entry<String, Integer>> sorted = winCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .collect(Collectors.toList());
        
        int totalPairings = algorithms.size() * (algorithms.size() - 1) / 2;
        int maxWinsPerAlgo = totalPairings * matchesPerPairing;
        
        LOGGER.info("┌─────────────────────────────────────────────────┬─────────────────┐");
        LOGGER.info("│ Algorithm Configuration                          │ Wins (Ranking)  │");
        LOGGER.info("├─────────────────────────────────────────────────┼─────────────────┤");
        
        for (Map.Entry<String, Integer> entry : sorted) {
            String algo = entry.getKey();
            int wins = entry.getValue();
            double winPercentage = (double) wins / maxWinsPerAlgo * 100;
            
            LOGGER.info(() -> String.format("│ %-47s │ %3d/%3d (%.1f%%)  │", 
                algo, wins, maxWinsPerAlgo, winPercentage));
        }
        
        LOGGER.info("└─────────────────────────────────────────────────┴─────────────────┘");
        
        // Display detailed matchup results
        LOGGER.info("\nDETAILED MATCHUP RESULTS:");
        LOGGER.info(() -> "─".repeat(70));
        
        for (Map.Entry<String, MatchResult> entry : results.entrySet()) {
            MatchResult result = entry.getValue();
            LOGGER.info(() -> String.format("%-30s: %s  [%3d/%3d games]",
                result.getAlgorithm1(),
                String.format("%d-%d", result.getWins1(), result.getWins2()),
                result.getWins1() + result.getWins2(),
                result.getTotalMatches()
            ));
        }
        
        LOGGER.info(() -> "─".repeat(70) + "\n");
    }
    
    /**
     * Returns total wins for an algorithm
     */
    public int getTotalWins(String algorithmName) {
        return winCounts.getOrDefault(algorithmName, 0);
    }
    
    /**
     * Returns all match results
     */
    public Collection<MatchResult> getResults() {
        return results.values();
    }
}
