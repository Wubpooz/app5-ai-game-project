package algorithms;

public class TimeManager {
    private final long softBoundMs;
    private final long hardBoundMs;
    private final long startTimeNs;

    public TimeManager(long remainingMs, int legalMoveCount) {
        int estimatedMovesLeft = estimateMovesLeft(remainingMs);
        double complexityFactor = complexityFactor(legalMoveCount);

        long baseTime = remainingMs / estimatedMovesLeft;
        this.softBoundMs = (long) (baseTime * complexityFactor);
        this.hardBoundMs = Math.min(softBoundMs * 3L, remainingMs / 5);
        this.startTimeNs = System.nanoTime();
    }

    private int estimateMovesLeft(long remainingMs) {
        if (remainingMs > 200_000) return 40;
        if (remainingMs > 60_000)  return 25;
        if (remainingMs > 10_000)  return 15;
        return 8;  // fin de partie, jouer vite
    }

    private double complexityFactor(int legalMoveCount) {
        // 1-2 coups légaux (contrainte de bande forcée) -> rien à chercher
        if (legalMoveCount <= 2) return 0.3;
        // Beaucoup de coups -> position complexe, dépenser plus
        if (legalMoveCount >= 20) return 1.5;
        return 1.0;
    }

    public boolean shouldStopSoft() {
        long elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000;
        return elapsedMs >= softBoundMs;
    }

    public boolean shouldStopHard() {
        long elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000;
        return elapsedMs >= hardBoundMs;
    }
}