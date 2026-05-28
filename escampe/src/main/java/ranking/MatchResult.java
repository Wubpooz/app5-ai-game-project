package ranking;

public class MatchResult {
    
    private final AlgorithmConfig algorithm1;
    private final AlgorithmConfig algorithm2;
    private int wins1;
    private int wins2;
    private int draws;
    private int totalGames;
    
    // Pentanomial stats for pairs: [0: 0 pts, 1: 0.5 pts, 2: 1.0 pts, 3: 1.5 pts, 4: 2.0 pts]
    private final int[] ptnml = new int[5];
    private int totalPairs;
    
    // Outcomes per opening to compute Chi-Squared homogeneity test
    // We assume a maximum of 64 openings
    private final int[][] openingPtnml = new int[64][5];
    
    public MatchResult(AlgorithmConfig algo1, AlgorithmConfig algo2) {
        this.algorithm1 = algo1;
        this.algorithm2 = algo2;
        this.wins1 = 0;
        this.wins2 = 0;
        this.draws = 0;
        this.totalGames = 0;
        this.totalPairs = 0;
    }
    
    public synchronized void recordPairResult(int openingIndex, double score1, double score2) {
        // score1: score of Bot 1 in Game 1 (Bot 1 is White)
        // score2: score of Bot 1 in Game 2 (Bot 1 is Black)
        // Individual games updates
        if (score1 == 1.0) wins1++;
        else if (score1 == 0.0) wins2++;
        else draws++;
        
        if (score2 == 1.0) wins1++;
        else if (score2 == 0.0) wins2++;
        else draws++;
        
        totalGames += 2;
        
        // Pentanomial bin update
        double pairScore = score1 + score2;
        int bin;
        if (pairScore == 0.0) bin = 0;      // 0-2 (both lost)
        else if (pairScore == 0.5) bin = 1; // 0.5-1.5 (one draw, one loss)
        else if (pairScore == 1.0) bin = 2; // 1-1 (two draws, or win-loss)
        else if (pairScore == 1.5) bin = 3; // 1.5-0.5 (one win, one draw)
        else bin = 4;                       // 2-0 (both won)
        
        ptnml[bin]++;
        totalPairs++;
        
        if (openingIndex >= 0 && openingIndex < openingPtnml.length) {
            openingPtnml[openingIndex][bin]++;
        }
    }
    
    public AlgorithmConfig getAlgorithm1() {
        return algorithm1;
    }
    
    public AlgorithmConfig getAlgorithm2() {
        return algorithm2;
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
    
    public int getTotalGames() {
        return totalGames;
    }
    
    public int[] getPtnml() {
        return ptnml;
    }
    
    public int getTotalPairs() {
        return totalPairs;
    }
    
    // ----------- Statistical Calculations -----------
    
    public double getWinRate1() {
        return totalGames > 0 ? (wins1 + 0.5 * draws) / totalGames : 0.5;
    }
    
    public double getEloDifference() {
        double r = getWinRate1();
        if (r <= 0.0) r = 0.001;
        if (r >= 1.0) r = 0.999;
        return 400.0 * Math.log10(r / (1.0 - r));
    }
    
    public double getEloErrorMargin() {
        if (totalGames == 0) return 0.0;
        double mu = getWinRate1();
        double wPercent = (double) wins1 / totalGames;
        double dPercent = (double) draws / totalGames;
        double lPercent = (double) wins2 / totalGames;
        
        double variance = wPercent * Math.pow(1.0 - mu, 2) + dPercent * Math.pow(0.5 - mu, 2) + lPercent * Math.pow(0.0 - mu, 2);
        double stdErr = Math.sqrt(variance / totalGames);
        double dEloDS = 400.0 / (Math.log(10.0) * mu * (1.0 - mu));
        return 1.96 * stdErr * dEloDS;
    }
    
    public double getNElo() {
        if (totalPairs == 0) return 0.0;
        double mean = 0.0;
        for (int i = 0; i <= 4; i++) {
            mean += (i / 4.0) * ptnml[i];
        }
        mean /= totalPairs;
        
        double variance = 0.0;
        for (int i = 0; i <= 4; i++) {
            variance += ptnml[i] * Math.pow((i / 4.0) - mean, 2);
        }
        variance /= totalPairs;
        if (variance == 0.0) variance = 1e-6;
        
        return ((mean - 0.5) / Math.sqrt(variance)) * (400.0 / Math.log(10.0));
    }
    
    public double getNEloErrorMargin() {
        if (totalPairs == 0) return 0.0;
        // nElo error is independent of variance in first approximation: 1.96 * 400 / ln(10) / sqrt(N_pairs)
        return 1.96 * (400.0 / Math.log(10.0)) * (1.0 / Math.sqrt(totalPairs));
    }
    
    public double getPairsRatio() {
        double favorable = (double) ptnml[3] + (double) ptnml[4];
        double unfavorable = (double) ptnml[0] + (double) ptnml[1];
        if (unfavorable == 0.0) {
            return favorable > 0.0 ? Double.POSITIVE_INFINITY : 1.0;
        }
        return favorable / unfavorable;
    }
    
    public double getSPRT_LLR(double elo0, double elo1) {
        if (totalPairs == 0) return 0.0;
        
        // Target expected scores
        double mu0 = 1.0 / (1.0 + Math.pow(10.0, -elo0 / 400.0));
        double mu1 = 1.0 / (1.0 + Math.pow(10.0, -elo1 / 400.0));
        
        // Observed mean (expected score) of the normalized pair results
        double muObs = 0.0;
        for (int i = 0; i <= 4; i++) {
            muObs += (i / 4.0) * ptnml[i];
        }
        muObs /= totalPairs;
        
        // Observed variance of the normalized pair results
        double variance = 0.0;
        for (int i = 0; i <= 4; i++) {
            variance += ptnml[i] * Math.pow((i / 4.0) - muObs, 2);
        }
        variance /= totalPairs;
        
        // Clamp variance to avoid division by zero
        if (variance < 1e-4) {
            variance = 1e-4;
        }
        
        // Wald's SPRT normal approximation
        return totalPairs * (mu1 - mu0) / variance * (muObs - (mu0 + mu1) / 2.0);
    }
    
    public String getSPRTStatus(double elo0, double elo1, double alpha, double beta) {
        double llr = getSPRT_LLR(elo0, elo1);
        double lowerBound = Math.log(beta / (1.0 - alpha));
        double upperBound = Math.log((1.0 - beta) / alpha);
        
        if (llr >= upperBound) return "Accepted (Pass)";
        if (llr <= lowerBound) return "Rejected (Fail)";
        return "Inconclusive";
    }
    
    // ----------- Chi-Squared Homogeneity Test -----------
    public double getChi2() {
        int numOpenings = openingPtnml.length;
        
        // Compute column totals (sum over openings for each bin)
        double[] colTotals = new double[5];
        double grandTotal = 0.0;
        for (int c = 0; c < 5; c++) {
            for (int r = 0; r < numOpenings; r++) {
                colTotals[c] += openingPtnml[r][c];
            }
            grandTotal += colTotals[c];
        }
        
        if (grandTotal == 0.0) return 0.0;
        
        // Compute row totals (sum over bins for each opening)
        double[] rowTotals = new double[numOpenings];
        for (int r = 0; r < numOpenings; r++) {
            for (int c = 0; c < 5; c++) {
                rowTotals[r] += openingPtnml[r][c];
            }
        }
        
        double chi2 = 0.0;
        for (int r = 0; r < numOpenings; r++) {
            if (rowTotals[r] == 0.0) continue;
            for (int c = 0; c < 5; c++) {
                double expected = (rowTotals[r] * colTotals[c]) / grandTotal;
                if (expected > 0.0) {
                    double observed = openingPtnml[r][c];
                    chi2 += Math.pow(observed - expected, 2) / expected;
                }
            }
        }
        
        return chi2;
    }
    
    public int getDof() {
        int numOpenings = openingPtnml.length;
        
        int activeRows = 0;
        for (int r = 0; r < numOpenings; r++) {
            double sum = 0.0;
            for (int c = 0; c < 5; c++) sum += openingPtnml[r][c];
            if (sum > 0.0) activeRows++;
        }
        
        int activeCols = 0;
        for (int c = 0; c < 5; c++) {
            double sum = 0.0;
            for (int r = 0; r < numOpenings; r++) sum += openingPtnml[r][c];
            if (sum > 0.0) activeCols++;
        }
        
        if (activeRows <= 1 || activeCols <= 1) return 0;
        return (activeRows - 1) * (activeCols - 1);
    }
    
    public double getPValue() {
        double chi2 = getChi2();
        int dof = getDof();
        if (dof <= 0 || chi2 <= 0.0) return 1.0;
        return regularizedGammaQ(dof / 2.0, chi2 / 2.0);
    }
    
    // ----------- Incomplete Gamma Function Approximation -----------
    private static double regularizedGammaPSeries(double a, double x) {
        double sum = 1.0 / a;
        double term = sum;
        for (int i = 1; i <= 100; i++) {
            term *= x / (a + i);
            sum += term;
            if (Math.abs(term) < Math.abs(sum) * 1e-12) break;
        }
        return sum * Math.exp(-x + a * Math.log(x) - lanczosGamma(a));
    }
    
    private static double regularizedGammaQContinuedFraction(double a, double x) {
        double b = x + 1.0 - a;
        double c = 1.0 / 1e-30;
        double d = 1.0 / b;
        double h = d;
        for (int i = 1; i <= 100; i++) {
            double an = -i * (i - a);
            b += 2.0;
            d = an * d + b;
            if (Math.abs(d) < 1e-30) d = 1e-30;
            c = b + an / c;
            if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1.0 / d;
            double del = c * d;
            h *= del;
            if (Math.abs(del - 1.0) < 1e-12) break;
        }
        return h * Math.exp(-x + a * Math.log(x) - lanczosGamma(a));
    }
    
    private static double regularizedGammaQ(double a, double x) {
        if (x < 0.0 || a <= 0.0) return 1.0;
        if (x < a + 1.0) {
            return 1.0 - regularizedGammaPSeries(a, x);
        } else {
            return regularizedGammaQContinuedFraction(a, x);
        }
    }
    
    private static double lanczosGamma(double x) {
        // Lanczos approximation log-gamma function
        double[] p = {
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.505632730003659e-7
        };
        double g = 607.0 / 128.0;
        double sum = p[0];
        for (int i = 1; i < p.length; i++) {
            sum += p[i] / (x + i - 1);
        }
        double tmp = x + g - 0.5;
        return 0.5 * Math.log(2.0 * Math.PI) + (x - 0.5) * Math.log(tmp) - tmp + Math.log(sum);
    }
}
