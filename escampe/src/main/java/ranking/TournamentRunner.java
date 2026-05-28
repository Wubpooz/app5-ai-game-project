package ranking;

import algorithms.evaluation.HeuristicConfig;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TournamentRunner {
    private static final Logger LOGGER = Logger.getLogger(TournamentRunner.class.getName());
    
    public static void main(String[] args) {
        LOGGER.info("\n╔════════════════════════════════════════════════════════╗");
        LOGGER.info("║         ESCAMPE AI TOURNAMENT RANKING SYSTEM           ║");
        LOGGER.info("╚════════════════════════════════════════════════════════╝\n");
        
        // Parse tournament configuration from command-line arguments
        TournamentConfig config = TournamentConfig.fromArgs(args);
        LOGGER.info(() -> "Tournament Configuration: " + config);
        
        AIRankingSystem ranking = new AIRankingSystem(config);
        
        // Configure standard algorithms and ablated heuristic variants
        configureAlgorithms(ranking);
        
        // Run the tournament
        ranking.executeFullTournament();
        
        // Output results to terminal and HTML report
        displayTerminalRankings(ranking);
        generateHTMLReport(ranking);
    }
    
    private static void configureAlgorithms(AIRankingSystem ranking) {
        LOGGER.info("Configuring algorithms and heuristic variants...\n");
        
        HeuristicConfig hDefault = HeuristicConfig.createDefault();
        HeuristicConfig hNoDistance = HeuristicConfig.createAblated("distance");
        HeuristicConfig hBandCoverage = HeuristicConfig.createAblated("bandcoverage");
        HeuristicConfig hSpsaFull = HeuristicConfig.createSpsaFull();
        HeuristicConfig hSpsaNoBand = HeuristicConfig.createSpsaNoBand();
        HeuristicConfig hBayesFull = HeuristicConfig.createBayesFull();
        HeuristicConfig hBayesNoBand = HeuristicConfig.createBayesNoBand();
        HeuristicConfig hSimplexFull = HeuristicConfig.createSimplexFull();
        HeuristicConfig hSimplexNoBand = HeuristicConfig.createSimplexNoBand();
        
        // 1. Search Depth and Algorithm configurations
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.MINIMAX, 1, hDefault));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 1, hDefault));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hDefault));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 3, hDefault));
        
        // 2. Heuristic Ablation (using AlphaBeta at depth 2)
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hNoDistance));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hBandCoverage));
        
        // 3. Tuned Heuristics (using AlphaBeta at depth 2)
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hSpsaFull));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hSpsaNoBand));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hBayesFull));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hBayesNoBand));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hSimplexFull));
        ranking.addAlgorithm(new AlgorithmConfig(AlgorithmConfig.AlgorithmType.ALPHABETA, 2, hSimplexNoBand));

        int n = ranking.getAlgorithms().size();
        int pairings = n * (n - 1) / 2;
        LOGGER.info(() -> "✓ " + n + " configurations registered");
        LOGGER.info(() -> "✓ Total pairings: " + pairings);
        LOGGER.info(() -> "✓ Total games: " + (pairings * 2 * ranking.getMatchesPerPairing()) + "\n");
    }
    
    private static void displayTerminalRankings(AIRankingSystem ranking) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL TOURNAMENT RANKINGS (by Converged Elo)");
        System.out.println("=".repeat(80));
        
        List<AlgorithmConfig> sortedAlgos = new ArrayList<>(ranking.getAlgorithms());
        sortedAlgos.sort((a, b) -> Double.compare(ranking.getEloRating(b.toString()), ranking.getEloRating(a.toString())));
        
        System.out.format("%-45s | %-12s | %-12s\n", "Algorithm Configuration", "Converged Elo", "Rank");
        System.out.println("-".repeat(80));
        
        for (int i = 0; i < sortedAlgos.size(); i++) {
            AlgorithmConfig config = sortedAlgos.get(i);
            System.out.format("%-45s | %-12.1f | #%d\n", 
                config.toString(), 
                ranking.getEloRating(config.toString()), 
                i + 1
            );
        }
        System.out.println("=".repeat(80) + "\n");
    }
    
    private static void generateHTMLReport(AIRankingSystem ranking) {
        List<AlgorithmConfig> sortedAlgos = new ArrayList<>(ranking.getAlgorithms());
        sortedAlgos.sort((a, b) -> Double.compare(ranking.getEloRating(b.toString()), ranking.getEloRating(a.toString())));
        
        // Calculate overall stats for each config
        Map<String, Integer> algoWins = new HashMap<>();
        Map<String, Integer> algoDraws = new HashMap<>();
        Map<String, Integer> algoLosses = new HashMap<>();
        Map<String, Integer> algoGames = new HashMap<>();
        
        for (AlgorithmConfig config : ranking.getAlgorithms()) {
            String name = config.toString();
            algoWins.put(name, 0);
            algoDraws.put(name, 0);
            algoLosses.put(name, 0);
            algoGames.put(name, 0);
        }
        
        for (MatchResult res : ranking.getResults()) {
            String name1 = res.getAlgorithm1().toString();
            String name2 = res.getAlgorithm2().toString();
            
            algoWins.put(name1, algoWins.get(name1) + res.getWins1());
            algoDraws.put(name1, algoDraws.get(name1) + res.getDraws());
            algoLosses.put(name1, algoLosses.get(name1) + res.getWins2());
            algoGames.put(name1, algoGames.get(name1) + res.getTotalGames());
            
            algoWins.put(name2, algoWins.get(name2) + res.getWins2());
            algoDraws.put(name2, algoDraws.get(name2) + res.getDraws());
            algoLosses.put(name2, algoLosses.get(name2) + res.getWins1());
            algoGames.put(name2, algoGames.get(name2) + res.getTotalGames());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("  <meta charset=\"utf-8\">\n");
        sb.append("  <meta content=\"width=device-width, initial-scale=1.0\" name=\"viewport\">\n");
        sb.append("  <title>Escampe AI Tournament Report</title>\n");
        sb.append("  <script src=\"https://cdn.tailwindcss.com?plugins=forms,container-queries\"></script>\n");
        sb.append("  <link href=\"https://fonts.googleapis.com\" rel=\"preconnect\">\n");
        sb.append("  <link crossorigin=\"\" href=\"https://fonts.gstatic.com\" rel=\"preconnect\">\n");
        sb.append("  <link href=\"https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&amp;family=IBM+Plex+Sans:wght@300;400;600&amp;display=swap\" rel=\"stylesheet\">\n");
        sb.append("  <style>\n");
        sb.append("    :root {\n");
        sb.append("      --colors-canvas: #ffffff;\n");
        sb.append("      --colors-surface-1: #f4f4f4;\n");
        sb.append("      --colors-surface-2: #e0e0e0;\n");
        sb.append("      --colors-hairline: #e0e0e0;\n");
        sb.append("      --colors-ink: #161616;\n");
        sb.append("      --colors-ink-muted: #525252;\n");
        sb.append("      --colors-ink-subtle: #8c8c8c;\n");
        sb.append("      --colors-primary: #0f62fe;\n");
        sb.append("      --colors-primary-hover: #0353e9;\n");
        sb.append("      --colors-inverse-canvas: #161616;\n");
        sb.append("      --colors-inverse-surface-1: #262626;\n");
        sb.append("      --colors-inverse-ink: #ffffff;\n");
        sb.append("      --colors-inverse-ink-muted: #c6c6c6;\n");
        sb.append("      --colors-border-variant: #e0e0e0;\n");
        sb.append("    }\n");
        sb.append("    @media (prefers-color-scheme: dark) {\n");
        sb.append("      :root:not(.light-mode) {\n");
        sb.append("        --colors-canvas: #161616;\n");
        sb.append("        --colors-surface-1: #262626;\n");
        sb.append("        --colors-surface-2: #353535;\n");
        sb.append("        --colors-hairline: #353535;\n");
        sb.append("        --colors-ink: #f4f4f4;\n");
        sb.append("        --colors-ink-muted: #a8a8a8;\n");
        sb.append("        --colors-ink-subtle: #757575;\n");
        sb.append("        --colors-primary: #4589ff;\n");
        sb.append("        --colors-primary-hover: #0f62fe;\n");
        sb.append("        --colors-inverse-canvas: #ffffff;\n");
        sb.append("        --colors-inverse-surface-1: #e0e0e0;\n");
        sb.append("        --colors-inverse-ink: #161616;\n");
        sb.append("        --colors-inverse-ink-muted: #525252;\n");
        sb.append("        --colors-border-variant: #353535;\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("    html.dark {\n");
        sb.append("      --colors-canvas: #161616;\n");
        sb.append("      --colors-surface-1: #262626;\n");
        sb.append("      --colors-surface-2: #353535;\n");
        sb.append("      --colors-hairline: #353535;\n");
        sb.append("      --colors-ink: #f4f4f4;\n");
        sb.append("      --colors-ink-muted: #a8a8a8;\n");
        sb.append("      --colors-ink-subtle: #757575;\n");
        sb.append("      --colors-primary: #4589ff;\n");
        sb.append("      --colors-primary-hover: #0f62fe;\n");
        sb.append("      --colors-inverse-canvas: #ffffff;\n");
        sb.append("      --colors-inverse-surface-1: #e0e0e0;\n");
        sb.append("      --colors-inverse-ink: #161616;\n");
        sb.append("      --colors-inverse-ink-muted: #525252;\n");
        sb.append("      --colors-border-variant: #353535;\n");
        sb.append("    }\n");
        sb.append("    body {\n");
        sb.append("      font-family: 'IBM Plex Sans', -apple-system, sans-serif;\n");
        sb.append("      letter-spacing: 0.16px;\n");
        sb.append("    }\n");
        sb.append("  </style>\n");
        sb.append("  <script id=\"tailwind-config\">\n");
        sb.append("    tailwind.config = {\n");
        sb.append("      darkMode: \"class\",\n");
        sb.append("      theme: {\n");
        sb.append("        extend: {\n");
        sb.append("          \"colors\": {\n");
        sb.append("            \"surface\": \"var(--colors-canvas)\",\n");
        sb.append("            \"on-surface\": \"var(--colors-ink)\",\n");
        sb.append("            \"on-surface-variant\": \"var(--colors-ink-muted)\",\n");
        sb.append("            \"secondary\": \"var(--colors-ink-muted)\",\n");
        sb.append("            \"outline-variant\": \"var(--colors-hairline)\",\n");
        sb.append("            \"surface-dim\": \"var(--colors-hairline)\",\n");
        sb.append("            \"surface-container-lowest\": \"var(--colors-surface-1)\",\n");
        sb.append("            \"surface-container-high\": \"var(--colors-surface-2)\",\n");
        sb.append("            \"surface-container-low\": \"var(--colors-inverse-canvas)\",\n");
        sb.append("            \"primary\": \"var(--colors-primary)\",\n");
        sb.append("            \"error\": \"#da1e28\",\n");
        sb.append("            \"success\": \"#24a148\",\n");
        sb.append("            \"warning\": \"#f1c21b\"\n");
        sb.append("          },\n");
        sb.append("          \"borderRadius\": {\n");
        sb.append("            \"DEFAULT\": \"0px\",\n");
        sb.append("            \"lg\": \"0px\",\n");
        sb.append("            \"xl\": \"0px\",\n");
        sb.append("            \"full\": \"0px\"\n");
        sb.append("          },\n");
        sb.append("          \"spacing\": {\n");
        sb.append("            \"grid-columns\": \"16\",\n");
        sb.append("            \"inset-squish\": \"8px 16px\",\n");
        sb.append("            \"margin\": \"48px\",\n");
        sb.append("            \"section-gap\": \"128px\",\n");
        sb.append("            \"gutter\": \"24px\",\n");
        sb.append("            \"component-gap\": \"16px\"\n");
        sb.append("          },\n");
        sb.append("          \"fontFamily\": {\n");
        sb.append("            \"headline-xl\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"data-mono\": [\"IBM Plex Mono\", \"monospace\"],\n");
        sb.append("            \"body-md\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"headline-lg\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"display-lg\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"label-caps\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"headline-md\": [\"IBM Plex Sans\", \"sans-serif\"],\n");
        sb.append("            \"body-lg\": [\"IBM Plex Sans\", \"sans-serif\"]\n");
        sb.append("          },\n");
        sb.append("          \"fontSize\": {\n");
        sb.append("            \"headline-xl\": [\"48px\", {\"lineHeight\": \"56px\", \"letterSpacing\": \"-0.01em\", \"fontWeight\": \"300\"}],\n");
        sb.append("            \"data-mono\": [\"14px\", {\"lineHeight\": \"20px\", \"fontWeight\": \"400\"}],\n");
        sb.append("            \"body-md\": [\"16px\", {\"lineHeight\": \"24px\", \"letterSpacing\": \"0.16px\", \"fontWeight\": \"400\"}],\n");
        sb.append("            \"headline-lg\": [\"32px\", {\"lineHeight\": \"40px\", \"letterSpacing\": \"0.16px\", \"fontWeight\": \"300\"}],\n");
        sb.append("            \"display-lg\": [\"60px\", {\"lineHeight\": \"72px\", \"letterSpacing\": \"-0.02em\", \"fontWeight\": \"300\"}],\n");
        sb.append("            \"label-caps\": [\"12px\", {\"lineHeight\": \"16px\", \"letterSpacing\": \"0.08em\", \"fontWeight\": \"600\"}],\n");
        sb.append("            \"headline-md\": [\"24px\", {\"lineHeight\": \"32px\", \"letterSpacing\": \"0.16px\", \"fontWeight\": \"400\"}],\n");
        sb.append("            \"body-lg\": [\"18px\", {\"lineHeight\": \"28px\", \"letterSpacing\": \"0.16px\", \"fontWeight\": \"400\"}]\n");
        sb.append("          }\n");
        sb.append("        }\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  </script>\n");
        sb.append("</head>\n");
        
        sb.append("<script>\n");
        sb.append("  const savedTheme = localStorage.getItem('theme');\n");
        sb.append("  if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {\n");
        sb.append("    document.documentElement.classList.add('dark');\n");
        sb.append("  } else {\n");
        sb.append("    document.documentElement.classList.remove('dark');\n");
        sb.append("  }\n");
        sb.append("</script>\n");
        sb.append("<body class=\"bg-surface text-on-surface antialiased min-h-screen flex flex-col selection:bg-surface-container-high selection:text-on-surface\">\n");
        sb.append("  <main class=\"flex-grow w-full max-w-[1584px] mx-auto px-12 pt-8 pb-section-gap flex flex-col gap-section-gap\">\n");
        sb.append("    <div class=\"flex justify-end\">\n");
        sb.append("      <button id=\"theme-toggle\" class=\"border border-outline-variant px-4 py-2 text-[12px] font-label-caps tracking-wider text-on-surface bg-surface hover:bg-surface-container-lowest transition-colors focus:outline-none\">\n");
        sb.append("        TOGGLE THEME\n");
        sb.append("      </button>\n");
        sb.append("    </div>\n");
        sb.append("    <section class=\"max-w-4xl mx-auto text-center space-y-6\">\n");
        sb.append("      <h1 class=\"font-display-lg text-display-lg text-on-surface font-light\">Escampe Global AI Rankings</h1>\n");
        sb.append("      <p class=\"font-body-lg text-body-lg text-secondary max-w-3xl mx-auto\">\n");
        sb.append("        Technical Deep Dive, Heuristic Ablation, and SPSA Parameter Optimization Report.\n");
        sb.append("      </p>\n");
        sb.append("    </section>\n");
        
        // --- 2. Global Rankings Panel ---
        sb.append("    <section class=\"w-full\">\n");
        sb.append("      <div class=\"flex justify-between items-baseline mb-8 border-b border-outline-variant pb-4\">\n");
        sb.append("        <h2 class=\"font-headline-lg text-headline-lg text-on-surface font-light\">Global Rankings</h2>\n");
        sb.append("        <span class=\"font-label-caps text-label-caps text-secondary\">Updated: May 2026</span>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"w-full\">\n");
        // Header
        sb.append("        <div class=\"grid grid-cols-12 gap-4 font-label-caps text-label-caps text-secondary pb-4 border-b border-outline-variant uppercase\">\n");
        sb.append("          <div class=\"col-span-1\">Rank</div>\n");
        sb.append("          <div class=\"col-span-4\">Agent Identifier</div>\n");
        sb.append("          <div class=\"col-span-2 text-right\">Win Rate</div>\n");
        sb.append("          <div class=\"col-span-2 text-right\">Matches</div>\n");
        sb.append("          <div class=\"col-span-3 text-right\">Elo Rating</div>\n");
        sb.append("        </div>\n");
        
        // Rows
        for (int i = 0; i < sortedAlgos.size(); i++) {
            AlgorithmConfig config = sortedAlgos.get(i);
            String name = config.toString();
            double winRate = algoGames.get(name) > 0 ? (algoWins.get(name) + 0.5 * algoDraws.get(name)) / algoGames.get(name) : 0.5;
            int totalGames = algoGames.get(name);
            double elo = ranking.getEloRating(name);
            String rankStr = String.format("%02d", i + 1);
            String rankColorClass = (i == 0) ? "text-primary" : "text-on-surface-variant";
            
            // Build tooltip data for global rankings row
            String globalTooltipDataJson = String.format(Locale.US,
                "{\"Rank\": \"%s\", \"Agent\": \"%s\", \"Wins / Draws / Losses\": \"%d / %d / %d\", \"Win Rate\": \"%.1f%%\", \"Total Matches\": \"%d\", \"Elo Rating\": \"%.1f\"}",
                rankStr, name, algoWins.get(name), algoDraws.get(name), algoLosses.get(name),
                winRate * 100, totalGames, elo
            );
            
            sb.append("        <div class=\"grid grid-cols-12 gap-4 items-center py-6 border-b border-surface-dim hover:bg-surface-container-lowest transition-colors cursor-pointer\" data-tooltip-data='")
              .append(globalTooltipDataJson).append("'>\n");
            sb.append("          <div class=\"col-span-1 font-headline-md text-headline-md ").append(rankColorClass).append("\">").append(rankStr).append("</div>\n");
            sb.append("          <div class=\"col-span-4 font-body-md text-body-md text-on-surface\">").append(name).append("</div>\n");
            sb.append("          <div class=\"col-span-2 text-right font-data-mono text-data-mono text-on-surface-variant\">")
              .append(String.format(Locale.US, "%.1f%%", winRate * 100)).append("</div>\n");
            sb.append("          <div class=\"col-span-2 text-right font-data-mono text-data-mono text-on-surface-variant\">")
              .append(String.format(Locale.US, "%,d", totalGames)).append("</div>\n");
            sb.append("          <div class=\"col-span-3 text-right font-headline-md text-headline-md text-on-surface\">")
              .append(String.format(Locale.US, "%.1f", elo)).append("</div>\n");
            sb.append("        </div>\n");
        }
        sb.append("      </div>\n");
        sb.append("    </section>\n");
        
        // --- 3. Elo Strength Hierarchy ---
        sb.append("    <section class=\"w-full\">\n");
        sb.append("      <div class=\"mb-8 border-b border-outline-variant pb-4\">\n");
        sb.append("        <h2 class=\"font-headline-lg text-headline-lg text-on-surface font-light\">Elo Strength Hierarchy</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"w-full flex flex-col gap-6\">\n");
        sb.append("        <div class=\"w-full space-y-8\">\n");
        
        double maxElo = sortedAlgos.isEmpty() ? 1500 : ranking.getEloRating(sortedAlgos.get(0).toString());
        double minElo = sortedAlgos.isEmpty() ? 1500 : ranking.getEloRating(sortedAlgos.get(sortedAlgos.size() - 1).toString());
        double range = maxElo - minElo;
        
        for (int i = 0; i < sortedAlgos.size(); i++) {
            AlgorithmConfig config = sortedAlgos.get(i);
            String name = config.toString();
            double elo = ranking.getEloRating(name);
            double pct = (range == 0) ? 100.0 : 15.0 + 85.0 * (elo - minElo) / range;
            String barColorClass = (i == 0) ? "bg-primary" : "bg-on-surface-variant";
            
            sb.append("          <div class=\"space-y-2\">\n");
            sb.append("            <div class=\"flex justify-between font-data-mono text-data-mono\">\n");
            sb.append("              <span class=\"text-on-surface\">").append(name).append("</span>\n");
            sb.append("              <span class=\"text-on-surface-variant\">").append(String.format(Locale.US, "%.1f", elo)).append("</span>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"w-full bg-surface-container-high h-4\">\n");
            sb.append("              <div class=\"").append(barColorClass).append(" h-full\" style=\"width: ")
              .append(String.format(Locale.US, "%.1f", pct)).append("%\"></div>\n");
            sb.append("            </div>\n");
            sb.append("          </div>\n");
        }
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");
        
        // --- 4. Detailed Matchup Matrix ---
        sb.append("    <section class=\"w-full\">\n");
        sb.append("      <div class=\"mb-8 border-b border-outline-variant pb-4\">\n");
        sb.append("        <h2 class=\"font-headline-lg text-headline-lg text-on-surface font-light\">Detailed Matchup Matrix</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"w-full overflow-x-auto\">\n");
        sb.append("        <table class=\"w-full text-left border-collapse\">\n");
        sb.append("          <thead class=\"border-b border-outline-variant font-label-caps text-label-caps text-secondary uppercase\">\n");
        sb.append("            <tr>\n");
        sb.append("              <th class=\"py-4\">Matchup</th>\n");
        sb.append("              <th class=\"py-4 text-center\">Score (W/D/L)</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Win Rate</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Elo Diff</th>\n");
        sb.append("              <th class=\"py-4 text-right\">nElo Diff</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Pairs Ratio</th>\n");
        sb.append("              <th class=\"py-4 text-right\">SPRT LLR</th>\n");
        sb.append("              <th class=\"py-4 text-center\">SPRT Status</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Homogeneity (&chi;<sup>2</sup>, p)</th>\n");
        sb.append("            </tr>\n");
        sb.append("          </thead>\n");
        sb.append("          <tbody class=\"font-body-md text-on-surface-variant divide-y divide-surface-dim\">\n");
        
        for (MatchResult res : ranking.getResults()) {
            double elo0 = 0.0;
            double elo1 = 35.0;
            String status = res.getSPRTStatus(elo0, elo1, 0.05, 0.05);
            String badgeClass = status.toUpperCase().contains("ACCEPTED") ? "border-primary text-primary" : 
                               (status.toUpperCase().contains("REJECTED") ? "border-error text-error" : 
                                "bg-surface-container-high text-on-surface-variant");
            
            // Build tooltip data
            String nEloVal = res.getNElo() >= 0.0 ? "+" + String.format(Locale.US, "%.1f", res.getNElo()) : String.format(Locale.US, "%.1f", res.getNElo());
            String nEloStr = String.format(Locale.US, "%s &plusmn; %.1f", nEloVal, res.getNEloErrorMargin());
            String pairsRatioStr = String.format(Locale.US, "%.2f", res.getPairsRatio());
            String llrStr = String.format(Locale.US, "%.2f", res.getSPRT_LLR(elo0, elo1));
            String homogStr = String.format(Locale.US, "(&chi;&sup2; = %.1f, p = %.4f)", res.getChi2(), res.getPValue());
            String eloDiffStr = String.format(Locale.US, "%+.1f &plusmn; %.1f", res.getEloDifference(), res.getEloErrorMargin());
            
            String tooltipDataJson = String.format(Locale.US,
                "{\"Matchup\": \"%s vs %s\", \"Score (W/D/L)\": \"%d / %d / %d\", \"Win Rate\": \"%.1f%%\", \"Elo Difference\": \"%s\", \"nElo Diff\": \"%s\", \"Pairs Ratio\": \"%s\", \"SPRT LLR (0 vs 35)\": \"%s\", \"SPRT Status\": \"%s\", \"Homogeneity\": \"%s\"}",
                res.getAlgorithm1().toString(), res.getAlgorithm2().toString(),
                res.getWins1(), res.getDraws(), res.getWins2(),
                res.getWinRate1() * 100, eloDiffStr, nEloStr, pairsRatioStr, llrStr, status, homogStr
            );
            
            sb.append("            <tr class=\"hover:bg-surface-container-lowest transition-colors cursor-pointer\" data-tooltip-data='")
              .append(tooltipDataJson).append("'>\n");
            
            sb.append("              <td class=\"py-6\"><strong>").append(res.getAlgorithm1().toString())
              .append("</strong><br><span class=\"text-secondary text-[11px] font-label-caps\">vs</span><br><strong>")
              .append(res.getAlgorithm2().toString()).append("</strong></td>\n");
            
            sb.append("              <td class=\"py-6 text-center font-data-mono text-data-mono\">")
              .append(res.getWins1()).append(" / ").append(res.getDraws()).append(" / ").append(res.getWins2()).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono\">")
              .append(String.format(Locale.US, "%.1f%%", res.getWinRate1() * 100)).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono text-primary font-semibold\">")
              .append(String.format(Locale.US, "%+.1f &plusmn; %.1f", res.getEloDifference(), res.getEloErrorMargin())).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono\">")
              .append(String.format(Locale.US, "%+.1f", res.getNElo())).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono\">")
              .append(String.format(Locale.US, "%.2f", res.getPairsRatio())).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono\">")
              .append(String.format(Locale.US, "%.2f", res.getSPRT_LLR(elo0, elo1))).append("</td>\n");
            
            sb.append("              <td class=\"py-6 text-center\"><span class=\"border ").append(badgeClass)
              .append(" text-[10px] px-2 py-1 font-label-caps\">").append(status.toUpperCase()).append("</span></td>\n");
            
            sb.append("              <td class=\"py-6 text-right font-data-mono text-data-mono\">&chi;&sup2; = ")
              .append(String.format(Locale.US, "%.1f", res.getChi2())).append(" (dof=").append(res.getDof()).append(")<br>p = ")
              .append(String.format(Locale.US, "%.4f", res.getPValue())).append("</td>\n");
            
            sb.append("            </tr>\n");
        }
        
        sb.append("          </tbody>\n");
        sb.append("        </table>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");
        
        // --- 5. Heuristic Ablation & Tuning Insights Panel ---
        sb.append("    <section class=\"w-full\">\n");
        sb.append("      <div class=\"mb-8 border-b border-outline-variant pb-4\">\n");
        sb.append("        <h2 class=\"font-headline-lg text-headline-lg text-on-surface font-light\">Heuristic Performance (Ablation &amp; Tuning)</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <p class=\"font-body-md text-body-md text-on-surface-variant mb-6 max-w-3xl\">\n");
        sb.append("        Ablation studies isolate individual heuristic components to measure their exact contribution to the playing engine. Tuning optimizes all parameters simultaneously via SPSA to find the global optimum.\n");
        sb.append("      </p>\n");
        sb.append("      <div class=\"w-full overflow-x-auto\">\n");
        sb.append("        <table class=\"w-full text-left border-collapse\">\n");
        sb.append("          <thead class=\"border-b border-outline-variant font-label-caps text-label-caps text-secondary uppercase\">\n");
        sb.append("            <tr>\n");
        sb.append("              <th class=\"py-4\">Heuristic Component / Variant</th>\n");
        sb.append("              <th class=\"py-4 text-center\">Elo Rating</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Elo Difference vs Baseline (AlphaBeta d=2)</th>\n");
        sb.append("              <th class=\"py-4 text-right\">Impact Status</th>\n");
        sb.append("            </tr>\n");
        sb.append("          </thead>\n");
        sb.append("          <tbody class=\"font-body-md text-on-surface-variant divide-y divide-surface-dim\">\n");
        
        double baseElo = ranking.getEloRating("AlphaBeta (d=2, h=Default)");
        double noDistElo = ranking.getEloRating("AlphaBeta (d=2, h=No-distance)");
        double withBandElo = ranking.getEloRating("AlphaBeta (d=2, h=With-bandcoverage)");
        double spsaFullElo = ranking.getEloRating("AlphaBeta (d=2, h=SPSA-Tuned-Full)");
        double spsaNoBandElo = ranking.getEloRating("AlphaBeta (d=2, h=SPSA-Tuned-NoBand)");
        double bayesFullElo = ranking.getEloRating("AlphaBeta (d=2, h=Bayes-Tuned-Full)");
        double bayesNoBandElo = ranking.getEloRating("AlphaBeta (d=2, h=Bayes-Tuned-NoBand)");
        double simplexFullElo = ranking.getEloRating("AlphaBeta (d=2, h=Simplex-Tuned-Full)");
        double simplexNoBandElo = ranking.getEloRating("AlphaBeta (d=2, h=Simplex-Tuned-NoBand)");
        
        appendAblationRow(sb, "Distance (Paladin to Unicorn)", noDistElo, baseElo - noDistElo);
        appendAblationRow(sb, "Band Coverage (Variant)", withBandElo, baseElo - withBandElo);
        appendAblationRow(sb, "SPSA-Tuned-Full (All Parameters)", spsaFullElo, baseElo - spsaFullElo);
        appendAblationRow(sb, "SPSA-Tuned-NoBand", spsaNoBandElo, baseElo - spsaNoBandElo);
        appendAblationRow(sb, "Bayesian-Tuned-Full", bayesFullElo, baseElo - bayesFullElo);
        appendAblationRow(sb, "Bayesian-Tuned-NoBand", bayesNoBandElo, baseElo - bayesNoBandElo);
        appendAblationRow(sb, "Simplex-Tuned-Full", simplexFullElo, baseElo - simplexFullElo);
        appendAblationRow(sb, "Simplex-Tuned-NoBand", simplexNoBandElo, baseElo - simplexNoBandElo);
        
        sb.append("          </tbody>\n");
        sb.append("        </table>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");
        
        // --- 6. Methodology & Explanation Note ---
        sb.append("    <section class=\"w-full pt-16 border-t border-outline-variant\">\n");
        sb.append("      <h2 class=\"font-headline-md text-headline-md text-on-surface mb-8 font-light\">Methodology &amp; Mathematical Explanations</h2>\n");
        sb.append("      <div class=\"grid grid-cols-1 md:grid-cols-3 gap-12 font-body-md text-body-md text-on-surface-variant\">\n");
        sb.append("        <div>\n");
        sb.append("          <h3 class=\"font-label-caps text-label-caps text-primary mb-3 uppercase\">1. Sequential Probability Ratio Test (SPRT)</h3>\n");
        sb.append("          <p class=\"text-sm leading-relaxed mb-4\">\n");
        sb.append("            The SPRT evaluates whether we can accept the alternative hypothesis <strong>H<sub>1</sub>: Elo difference &ge; 35</strong> versus the null hypothesis <strong>H<sub>0</sub>: Elo difference &le; 0</strong>.\n");
        sb.append("          </p>\n");
        sb.append("          <p class=\"text-sm leading-relaxed\">\n");
        sb.append("            We use a pentanomial (5-outcome pair) model with error bounds &alpha; = &beta; = 0.05. A Wald normal approximation is used to estimate the log-likelihood ratio (LLR), preventing inconclusive results caused by observed-mean bias.\n");
        sb.append("          </p>\n");
        sb.append("        </div>\n");
        sb.append("        <div>\n");
        sb.append("          <h3 class=\"font-label-caps text-label-caps text-primary mb-3 uppercase\">2. Chi-Squared (&chi;<sup>2</sup>) Homogeneity</h3>\n");
        sb.append("          <p class=\"text-sm leading-relaxed mb-4\">\n");
        sb.append("            The homogeneity test measures whether win, loss, and draw rates are independent of the opening played. Because search engines are entirely deterministic, playing the same opening multiple times always results in the identical outcome (zero variance per opening).\n");
        sb.append("          </p>\n");
        sb.append("          <p class=\"text-sm leading-relaxed\">\n");
        sb.append("            When outcomes vary across openings, the trials perfectly cluster, driving &chi;<sup>2</sup> to its mathematical maximum (500) and yielding a <strong>p-value of 0.0000</strong>. When outcomes are identical, observed equals expected perfectly, yielding <strong>p-value = 1.0000</strong>.\n");
        sb.append("          </p>\n");
        sb.append("        </div>\n");
        sb.append("        <div>\n");
        sb.append("          <h3 class=\"font-label-caps text-label-caps text-primary mb-3 uppercase\">3. SPSA Parameter Optimization</h3>\n");
        sb.append("          <p class=\"text-sm leading-relaxed mb-4\">\n");
        sb.append("            Simultaneous Perturbation Stochastic Approximation (SPSA) optimizes the 9 heuristic weights simultaneously. SPSA perturbs all parameters by a random vector &Delta; &in; {-1, +1}<sup>9</sup> and runs parallel game matches to estimate the performance gradient.\n");
        sb.append("          </p>\n");
        sb.append("          <p class=\"text-sm leading-relaxed\">\n");
        sb.append("            Tuning proved that standing on 1-band squares restricts the engine's own mobility too much (reversing <code>weightBandControl</code> to -34), while restricting the opponent on 1-band squares remains critical (<code>weightOppBandControl</code> = 85).\n");
        sb.append("          </p>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");
        
        sb.append("  </main>\n");
        
        // --- Footer ---
        sb.append("  <footer class=\"bg-surface-container-low w-full mt-section-gap border-t border-outline-variant py-12 text-center text-xs text-secondary\">\n");
        sb.append("    &copy; May 2026 Escampe AI Lab. All rights reserved.\n");
        sb.append("  </footer>\n");
        
        // --- Global Floating Tooltip ---
        sb.append("  <div id=\"global-tooltip\" class=\"fixed hidden bg-surface text-on-surface p-4 shadow-2xl border border-outline-variant z-50 pointer-events-none w-80 font-data-mono\">\n");
        sb.append("    <div id=\"tooltip-content\" class=\"divide-y divide-outline-variant\">\n");
        sb.append("      <!-- Dynamic rows will be inserted here -->\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        
        // --- Tooltip Script & Theme Toggle Script ---
        sb.append("  <script>\n");
        sb.append("    // Theme Toggle Handler\n");
        sb.append("    const toggle = document.getElementById('theme-toggle');\n");
        sb.append("    toggle.addEventListener('click', () => {\n");
        sb.append("      document.documentElement.classList.toggle('dark');\n");
        sb.append("      const isDark = document.documentElement.classList.contains('dark');\n");
        sb.append("      if (isDark) {\n");
        sb.append("        document.documentElement.classList.remove('light-mode');\n");
        sb.append("        localStorage.setItem('theme', 'dark');\n");
        sb.append("      } else {\n");
        sb.append("        document.documentElement.classList.add('light-mode');\n");
        sb.append("        localStorage.setItem('theme', 'light');\n");
        sb.append("      }\n");
        sb.append("    });\n");
        sb.append("\n");
        sb.append("    // Tooltip handler\n");
        sb.append("    const tooltip = document.getElementById('global-tooltip');\n");
        sb.append("    const tooltipContent = document.getElementById('tooltip-content');\n");
        sb.append("    document.querySelectorAll('[data-tooltip-data]').forEach(el => {\n");
        sb.append("      el.addEventListener('mouseenter', (e) => {\n");
        sb.append("        const data = JSON.parse(el.getAttribute('data-tooltip-data'));\n");
        sb.append("        tooltipContent.innerHTML = '';\n");
        sb.append("        \n");
        sb.append("        Object.entries(data).forEach(([key, val]) => {\n");
        sb.append("          const row = document.createElement('div');\n");
        sb.append("          row.className = 'flex justify-between items-center py-2';\n");
        sb.append("          \n");
        sb.append("          const labelSpan = document.createElement('span');\n");
        sb.append("          labelSpan.className = 'text-on-surface-variant text-[11px] uppercase font-sans tracking-wider mr-2';\n");
        sb.append("          labelSpan.innerText = key;\n");
        sb.append("          \n");
        sb.append("          const valSpan = document.createElement('span');\n");
        sb.append("          \n");
        sb.append("          if (key.toUpperCase().includes('STATUS')) {\n");
        sb.append("            valSpan.className = 'font-sans text-[10px] px-2 py-0.5 border font-semibold';\n");
        sb.append("            valSpan.innerText = val.toUpperCase();\n");
        sb.append("            if (val.toUpperCase().includes('ACCEPTED') || val.toUpperCase().includes('PASS')) {\n");
        sb.append("              valSpan.classList.add('border-success', 'text-success');\n");
        sb.append("            } else if (val.toUpperCase().includes('REJECTED') || val.toUpperCase().includes('FAIL')) {\n");
        sb.append("              valSpan.classList.add('border-error', 'text-error');\n");
        sb.append("            } else {\n");
        sb.append("              valSpan.classList.add('border-outline-variant', 'text-on-surface-variant');\n");
        sb.append("            }\n");
        sb.append("          } else {\n");
        sb.append("            valSpan.className = 'text-on-surface font-semibold font-data-mono';\n");
        sb.append("            valSpan.innerHTML = val;\n");
        sb.append("          }\n");
        sb.append("          \n");
        sb.append("          row.appendChild(labelSpan);\n");
        sb.append("          row.appendChild(valSpan);\n");
        sb.append("          tooltipContent.appendChild(row);\n");
        sb.append("        });\n");
        sb.append("        \n");
        sb.append("        tooltip.classList.remove('hidden');\n");
        sb.append("      });\n");
        sb.append("      \n");
        sb.append("      el.addEventListener('mousemove', (e) => {\n");
        sb.append("        const x = e.clientX + 15;\n");
        sb.append("        const y = e.clientY + 15;\n");
        sb.append("        const tooltipWidth = tooltip.offsetWidth;\n");
        sb.append("        const tooltipHeight = tooltip.offsetHeight;\n");
        sb.append("        const viewportWidth = window.innerWidth;\n");
        sb.append("        const viewportHeight = window.innerHeight;\n");
        sb.append("        let posX = x;\n");
        sb.append("        let posY = y;\n");
        sb.append("        if (x + tooltipWidth > viewportWidth) {\n");
        sb.append("          posX = e.clientX - tooltipWidth - 15;\n");
        sb.append("        }\n");
        sb.append("        if (y + tooltipHeight > viewportHeight) {\n");
        sb.append("          posY = e.clientY - tooltipHeight - 15;\n");
        sb.append("        }\n");
        sb.append("        tooltip.style.left = posX + 'px';\n");
        sb.append("        tooltip.style.top = posY + 'px';\n");
        sb.append("      });\n");
        sb.append("      \n");
        sb.append("      el.addEventListener('mouseleave', () => {\n");
        sb.append("        tooltip.classList.add('hidden');\n");
        sb.append("      });\n");
        sb.append("    });\n");
        sb.append("  </script>\n");
        sb.append("</body>\n</html>");
        
        String htmlPath = Paths.get(".").toAbsolutePath().resolve("ranking_results.html").normalize().toString();
        try (FileWriter fw = new FileWriter(htmlPath)) {
            fw.write(sb.toString());
            LOGGER.info(() -> "✓ HTML ranking report generated successfully at: " + htmlPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write HTML report", e);
        }
    }
    
    private static void appendAblationRow(StringBuilder sb, String name, double rating, double loss) {
        sb.append("          <tr class=\"hover:bg-surface-container-lowest transition-colors\">\n");
        sb.append("            <td class=\"py-6\"><strong>").append(name).append("</strong></td>\n");
        sb.append("            <td class=\"py-6 text-center font-data-mono text-data-mono text-on-surface\">")
          .append(String.format(Locale.US, "%.1f", rating)).append("</td>\n");
        if (loss >= 0.0) {
            sb.append("            <td class=\"py-6 text-right font-data-mono text-data-mono text-error font-semibold\">-")
              .append(String.format(Locale.US, "%.1f", loss)).append("</td>\n");
            String status = loss > 100.0 ? "Critical Loss" : (loss > 20.0 ? "Significant Loss" : "Minor Loss");
            sb.append("            <td class=\"py-6 text-right\"><span class=\"border border-error text-error text-[10px] px-2 py-1 font-label-caps\">\n");
            sb.append("              ").append(status.toUpperCase()).append("\n");
            sb.append("            </span></td>\n");
        } else {
            sb.append("            <td class=\"py-6 text-right font-data-mono text-data-mono text-primary font-semibold\">+")
              .append(String.format(Locale.US, "%.1f", -loss)).append("</td>\n");
            String status = (-loss) > 100.0 ? "Critical Gain" : ((-loss) > 20.0 ? "Significant Gain" : "Minor Gain");
            sb.append("            <td class=\"py-6 text-right\"><span class=\"border border-primary text-primary text-[10px] px-2 py-1 font-label-caps\">\n");
            sb.append("              ").append(status.toUpperCase()).append("\n");
            sb.append("            </span></td>\n");
        }
        sb.append("          </tr>\n");
    }
}
