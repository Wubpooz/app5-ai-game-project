package games.dominos.ranking;

/**
 * Records the results of multiple matches between two algorithm configurations.
 */
public class MatchResult {
    
    private final AlgorithmConfig algorithm1;
    private final AlgorithmConfig algorithm2;
    private int wins1;
    private int wins2;
    private int draws;
    private final int totalMatches;
    
    public MatchResult(AlgorithmConfig algo1, AlgorithmConfig algo2, int totalMatches) {
        this.algorithm1 = algo1;
        this.algorithm2 = algo2;
        this.totalMatches = totalMatches;
        this.wins1 = 0;
        this.wins2 = 0;
        this.draws = 0;
    }
    
    public void recordWin(AlgorithmConfig winner) {
        if (winner.equals(algorithm1)) {
            wins1++;
        } else if (winner.equals(algorithm2)) {
            wins2++;
        }
    }
    
    public void recordDraw() {
        draws++;
    }
    
    public double getWinRate1() {
        return totalMatches > 0 ? (double) wins1 / totalMatches : 0.0;
    }
    
    public double getWinRate2() {
        return totalMatches > 0 ? (double) wins2 / totalMatches : 0.0;
    }
    
    public int getWins1() {
        return wins1;
    }
    
    public int getWins2() {
        return wins2;
    }
    
    public int getDraws() {
        return draws;
    }
    
    public AlgorithmConfig getAlgorithm1() {
        return algorithm1;
    }
    
    public AlgorithmConfig getAlgorithm2() {
        return algorithm2;
    }
    
    public int getTotalMatches() {
        return totalMatches;
    }
    
    /**
     * Returns comparison string: "Algo1 vs Algo2"
     */
    public String getMatchupString() {
        return algorithm1 + " vs " + algorithm2;
    }
    
    /**
     * Returns result summary: "3W-7L" format for algo1's perspective
     */
    public String getSummary() {
        return wins1 + "W-" + wins2 + "L";
    }
}
