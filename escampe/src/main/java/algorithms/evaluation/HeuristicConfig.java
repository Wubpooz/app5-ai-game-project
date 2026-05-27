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
        return new HeuristicConfig("Default", 10, 2, 5, 5, 50, 30, 10, 80, 90);
    }

    public static HeuristicConfig createAblated(String componentName) {
        int minDist = 10, avgDist = 2, dangerMin = 5, dangerAvg = 5, escapability = 50, trapped = 30, legal = 10, band = 80, oppBand = 90;
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
            case "bandcontrol":
                band = 0;
                oppBand = 0;
                break;
            default:
                throw new IllegalArgumentException("Unknown ablation component: " + componentName);
        }
        return new HeuristicConfig("No-" + componentName, minDist, avgDist, dangerMin, dangerAvg, escapability, trapped, legal, band, oppBand);
    }

    @Override
    public String toString() {
        return name;
    }
}
