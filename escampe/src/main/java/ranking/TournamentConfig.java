package ranking;

/**
 * Configuration for tournament execution including match counts and random seeds.
 * 
 * This class encapsulates all tournament-level configuration parameters needed
 * for running deterministic, seeded tournament games. It supports parsing
 * command-line arguments for easy configuration.
 * 
 * Default values:
 * - matchesPerPairing: 1000 (matches per pairing = 1000 games per matchup)
 * - tournamentSeed: 2026 (seed for opening schedule generation)
 * - eloSeed: 2027 (seed for Elo convergence shuffle)
 */
public class TournamentConfig {

    private final int matchesPerPairing;
    private final long tournamentSeed;
    private final long eloSeed;

    /**
     * Creates a TournamentConfig with specified parameters.
     *
     * @param matchesPerPairing number of matches per pairing (games per matchup × 2)
     * @param tournamentSeed    random seed for tournament/opening schedule generation
     * @param eloSeed           random seed for Elo convergence updates
     */
    public TournamentConfig(int matchesPerPairing, long tournamentSeed, long eloSeed) {
        this.matchesPerPairing = matchesPerPairing;
        this.tournamentSeed = tournamentSeed;
        this.eloSeed = eloSeed;
    }

    /**
     * Parses command-line arguments to create a TournamentConfig.
     *
     * Supported arguments:
     * - {@code --matches=N}: Set matchesPerPairing to N (default: 1000)
     * - {@code --tournamentSeed=N}: Set tournamentSeed to N (default: 2026)
     * - {@code --eloSeed=N}: Set eloSeed to N (default: 2027)
     *
     * Unknown arguments are silently ignored. If multiple values are provided
     * for the same parameter, the last one is used.
     *
     * @param args command-line arguments array (may be empty)
     * @return TournamentConfig with parsed or default values
     * @throws NumberFormatException if a numeric argument cannot be parsed
     */
    public static TournamentConfig fromArgs(String[] args) {
        int matches = 1000;         // Default
        long tournamentSeed = 2026L; // Default
        long eloSeed = 2027L;        // Default

        for (String arg : args) {
            if (arg.startsWith("--matches=")) {
                String value = arg.substring("--matches=".length());
                matches = Integer.parseInt(value);
            } else if (arg.startsWith("--tournamentSeed=")) {
                String value = arg.substring("--tournamentSeed=".length());
                tournamentSeed = Long.parseLong(value);
            } else if (arg.startsWith("--eloSeed=")) {
                String value = arg.substring("--eloSeed=".length());
                eloSeed = Long.parseLong(value);
            }
            // Silently ignore unknown arguments
        }

        return new TournamentConfig(matches, tournamentSeed, eloSeed);
    }

    /**
     * Returns the number of matches per pairing.
     *
     * @return matchesPerPairing value
     */
    public int getMatchesPerPairing() {
        return matchesPerPairing;
    }

    /**
     * Returns the tournament seed for opening schedule generation.
     *
     * @return tournamentSeed value
     */
    public long getTournamentSeed() {
        return tournamentSeed;
    }

    /**
     * Returns the Elo convergence seed for deterministic rating updates.
     *
     * @return eloSeed value
     */
    public long getEloSeed() {
        return eloSeed;
    }

    @Override
    public String toString() {
        return String.format("TournamentConfig(matches=%d, tournamentSeed=%d, eloSeed=%d)",
                matchesPerPairing, tournamentSeed, eloSeed);
    }
}
