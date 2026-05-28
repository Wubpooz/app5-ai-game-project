package algorithms.evaluation;

public class HeuristicConfig {
    public final String name;
    public final int weightMinDist;
    public final int weightAvgDist;
    public final int weightUnicornDangerMinDist;
    public final int weightUnicornDangerAvgDist;
    public final int weightEscapability;
    public final int weightTrappedUnicorn;
    public final int weightLegalMoves;
    public final int weightBandControl;
    public final int weightOppBandControl;

    public HeuristicConfig(String name, int weightMinDist, int weightAvgDist, int weightUnicornDangerMinDist,
                           int weightUnicornDangerAvgDist, int weightEscapability, int weightTrappedUnicorn,
                           int weightLegalMoves, int weightBandControl, int weightOppBandControl) {
        this.name = name;
        this.weightMinDist = weightMinDist;
        this.weightAvgDist = weightAvgDist;
        this.weightUnicornDangerMinDist = weightUnicornDangerMinDist;
        this.weightUnicornDangerAvgDist = weightUnicornDangerAvgDist;
        this.weightEscapability = weightEscapability;
        this.weightTrappedUnicorn = weightTrappedUnicorn;
        this.weightLegalMoves = weightLegalMoves;
        this.weightBandControl = weightBandControl;
        this.weightOppBandControl = weightOppBandControl;
    }

    public static HeuristicConfig createDefault() {
        return new HeuristicConfig("Default", 10, 2, 5, 5, 50, 30, 10, 0, 0);
    }

    public static HeuristicConfig createSpsaFull() {
        return new HeuristicConfig("SPSA-Tuned-Full", 25, 1, 12, 0, 0, 60, 4, 0, 0);
    }

    public static HeuristicConfig createSpsaNoBand() {
        return new HeuristicConfig("SPSA-Tuned-NoBand", 27, 6, 8, 2, 1, 42, 1, 0, 0);
    }

    public static HeuristicConfig createBayesFull() {
        return new HeuristicConfig("Bayes-Tuned-Full", 48, 21, 31, 38, 29, 60, 4, -4, -39);
    }

    public static HeuristicConfig createBayesNoBand() {
        return new HeuristicConfig("Bayes-Tuned-NoBand", 2, 33, 3, 44, 176, 61, 34, 0, 0);
    }

    public static HeuristicConfig createSimplexFull() {
        return new HeuristicConfig("Simplex-Tuned-Full", 41, 0, 5, 8, 114, 69, 13, 11, -3);
    }

    public static HeuristicConfig createSimplexNoBand() {
        return new HeuristicConfig("Simplex-Tuned-NoBand", 35, 0, 4, 8, 115, 68, 12, 0, 0);
    }

    public static HeuristicConfig createAblated(String componentName) {
        int minDist = 10, avgDist = 2, dangerMin = 5, dangerAvg = 5, escapability = 50, trapped = 30, legal = 10, band = 0, oppBand = 0;
        switch (componentName.toLowerCase()) {
            case "distance":
                minDist = 0;
                avgDist = 0;
                break;
            case "unicorndanger":
                dangerMin = 0;
                dangerAvg = 0;
                break;
            case "escapability":
                escapability = 0;
                break;
            case "trapped":
                trapped = 0;
                break;
            case "mobility":
                legal = 0;
                break;
            case "bandcoverage":
                band = 80;
                oppBand = 90;
                break;
            default:
                throw new IllegalArgumentException("Unknown ablation/variant component: " + componentName);
        }
        String prefix = componentName.equalsIgnoreCase("bandcoverage") ? "With-" : "No-";
        return new HeuristicConfig(prefix + componentName, minDist, avgDist, dangerMin, dangerAvg, escapability, trapped, legal, band, oppBand);
    }

    @Override
    public String toString() {
        return name;
    }
}
