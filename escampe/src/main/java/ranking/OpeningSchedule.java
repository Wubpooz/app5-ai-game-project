package ranking;

import java.util.Random;

/**
 * Generates deterministic opening schedules for tournament games.
 */
public class OpeningSchedule {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private OpeningSchedule() {
        // Utility class, not meant to be instantiated
    }

    /**
     * Builds a deterministic opening schedule for tournament games.
     *
     * @param pairs       number of game pairs in the tournament (must be > 0)
     * @param numOpenings number of different opening positions available (must be > 0)
     * @param random      seeded Random instance for deterministic generation (must not be null)
     * @return array of opening indices, one per pair, each in range [0, numOpenings)
     * @throws IllegalArgumentException if pairs or numOpenings are not positive
     * @throws NullPointerException if random is null
     */
    public static int[] buildSchedule(int pairs, int numOpenings, Random random) {
        if (pairs <= 0) {
            throw new IllegalArgumentException("pairs must be positive, got: " + pairs);
        }
        if (numOpenings <= 0) {
            throw new IllegalArgumentException("numOpenings must be positive, got: " + numOpenings);
        }
        if (random == null) {
            throw new NullPointerException("random cannot be null");
        }

        int[] schedule = new int[pairs];
        for (int i = 0; i < pairs; i++) {
            schedule[i] = random.nextInt(numOpenings);
        }
        return schedule;
    }
}
