# TODO
## Search
- [x] Minimax with alpha-beta pruning

- [x] [Time Management](https://www.chessprogramming.org/Time_Management): Une hard limit et soft limit pour éviter les dépassements de temps. La soft limit est un seuil de temps auquel on arrête l'itération en cours et on retourne le meilleur coup trouvé jusqu'à présent, tandis que la hard limit est un seuil de temps auquel on arrête immédiatement toute recherche et retourne le meilleur coup trouvé jusqu'à présent, même si l'itération en cours n'est pas terminée. Pour estimer le budget du coup actuel, on peut utiliser une formule qui prend en compte le temps restant, le nombre de coups légaux (complexité de la position) et la phase de jeu (ouverture, milieu, fin). Par exemple:
    ```java
    public class TimeManager {
        private final long softBoundMs;
        private final long hardBoundMs;
        private final long startTimeNs;

        public TimeManager(long remainingMs, int legalMoveCount) {
            int estimatedMovesLeft = estimateMovesLeft(remainingMs);

            // (peu de coups légaux) -> dépenser moins
            double complexityFactor = complexityFactor(legalMoveCount);

            long baseTime = remainingMs / estimatedMovesLeft;
            this.softBoundMs = (long)(baseTime * complexityFactor);
            this.hardBoundMs = Math.min(softBoundMs * 3L, remainingMs / 5);
            this.startTimeNs = System.nanoTime();
        }

        private int estimateMovesLeft(long remainingMs) {
            // Plus de temps restant = on suppose plus de coups à jouer
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
            long elapsed = (System.nanoTime() - startTimeNs) / 1_000_000;
            return elapsed >= softBoundMs;
        }

        public boolean shouldStopHard() {
            long elapsed = (System.nanoTime() - startTimeNs) / 1_000_000;
            return elapsed >= hardBoundMs;
        }
    }

    public String bestMove(EscampeBoard board, String player, long remainingMs) {
        String[] moves = board.possiblesMoves(player);

        // Cas trivial : 0 ou 1 coup
        if (moves.length == 0) return "E";
        if (moves.length == 1) return moves[0];

        TimeManager tm = new TimeManager(remainingMs, moves.length);
        String bestMove = moves[0];  // fallback garanti
        int bestScore = Integer.MIN_VALUE;

        // Iterative deepening
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            if (tm.shouldStopSoft()) break;

            SearchResult result = negamax(board, player, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, tm);

            // Ne mettre à jour que si l'itération s'est terminée proprement
            if (!result.wasAborted()) {
                bestMove = result.move();
                bestScore = result.score();
            }
        }
        return bestMove;
    }

    private SearchResult negamax(EscampeBoard board, String player, int depth, int alpha, int beta, TimeManager tm) {
        // à chaque noeud
        if (tm.shouldStopHard()) return SearchResult.aborted();
        
        if (depth == 0 || board.gameOver()) 
            return SearchResult.of(evaluate(board, player), null);

        // ... 
    }
    ```


- [x] Negamax with alpha-beta pruning: since the game is zero-sum, we can simplify the implementation by using a single function that returns the score from the perspective of the current player. The opponent's best score is just the negative of our best score.

- [ ] [Iterative Deepening](https://www.chessprogramming.org/Iterative_Deepening): start with a shallow depth and increase it until time runs out, always keeping track of the best move found so far.
  - [ ] [Aspiration Windows](https://www.chessprogramming.org/Aspiration_Windows): when doing iterative deepening, instead of starting each deeper search with alpha=-inf and beta=+inf, we can start with a narrow window around the previous iteration's score (e.g., alpha=prev_score-50, beta=prev_score+50). This can lead to faster cutoffs if the score doesn't change much between iterations. If the search fails low or high, we can widen the window and re-search. Be careful with the choice of window size to balance between speed and the risk of re-searching.

- [ ] Repetition detection using the transposition table: if we encounter the same board position again, we can detect a draw by repetition.

- [ ] [Move ordering](https://www.chessprogramming.org/Move_Ordering)
  - [ ] TT move first
  - [ ] Captures and immediate tactial mpves sorted by MVV/LVA (most valuable victim/least valuable attacker)
  - [ ] History heuristic: moves that have caused beta cutoffs in the past are tried earlier.
  - [ ] Killer moves: moves that caused cutoffs at the same depth in other branches are tried earlier.
  - [ ] Remaining moves

- [ ] [Selective pruning](https://www.chessprogramming.org/Selectivity)
  - [ ] [Null-move pruning](https://www.chessprogramming.org/Null_Move_Pruning) (not the pass move, only for regular moves): assume that if we skip our turn, the opponent will get a chance to move. If the opponent's best response is still worse than our current best score, we can prune this branch.
  - [ ] [Late Move Reduction](https://www.chessprogramming.org/Late_Move_Reduction): if we are deep in the search and we have already found a good move, we can reduce the depth of search for less promising moves (e.g., non-captures, non-checks) to save time.
  - [ ] [Futility pruning](https://www.chessprogramming.org/Futility_Pruning): if we are near the end of the search and the current position is already much worse than our best score, we can prune this branch since it's unlikely to improve.

- [ ] [Quiescence search](https://www.chessprogramming.org/Quiescence_Search): extend search in "noisy" positions (captures, checks) to avoid horizon effect.

- [ ] [Killer heuristic](https://www.chessprogramming.org/Killer_Heuristic): keep track of the best moves that caused beta cutoffs at each depth and try them first in future searches at the same depth.

## Heuristics
- [x] Evaluation function: $\displaystyle{-w_1\min_{p\in P}{(d^{\text{atk}}_{p})} - w_2\,\text{avg}_{p\in P}(d^{\text{atk}}_{p}) + w_3\min_{e\in E}{(d^{\text{def}}_{e})} + w_4\,\text{avg}_{e\in E}(d^{\text{def}}_{e}) + w_5\,\mathcal{E_{\text{us}}} - w_6\,\mathcal{E_{\text{opp}}} + w_7 \mathcal{BC} + w_8 \mathcal{T}}$
  - Attack distance: Manhattan distance from our paladins to the opponent's unicorn (min and avg, closer is better)
  - Defense distance: Manhattan distance from opponent's paladins to our unicorn (min and avg, farther is better)
  - $\mathcal{E_{\text{us}}}$: unicorn escapability — number of legal moves for our unicorn (more = safer)
  - $\mathcal{E_{\text{opp}}}$: opponent unicorn escapability — number of legal moves for opponent's unicorn (fewer = better, opponent trapped)
  - $\mathcal{BC}$: band control — our paladins on 1-band squares is good, opponent's paladins on 3-band squares is good for us
  - $\mathcal{T}$: mobility/pass pressure — reward for our legal moves, penalty for opponent's legal moves, large penalty when we must pass, reward when opponent must pass
  - TODO: Band-aware distance (BFS ply distance) instead of Manhattan distance

- [x] Time management: 
  - Spend more time in complex midgame positions (high branching factor, many legal moves).
  - Spend less time in forced/obvious positions (only 1–2 legal moves due to band constraint).
  ```java
  long timeForMove = Math.min(
      remainingTime / estimatedMovesLeft,
      remainingTime * 0.1  // never use more than 10% on one move
  );
  ```

- [ ] (Optional) NN-based evaluation function **for move ordering** (export visualisation from [NN-SVG](https://alexlenail.me/NN-SVG/LeNet.html))
  - Analysis 
    The eval function takes a board state and returns a scalar in [-1, +1]. The signal it must capture:
    - Which pieces can legally move right now (band constraint)
    - How many moves each side has (mobility asymmetry)
    - Proximity of attackers to the opponent's unicorn, weighted by band alignment
    - Unicorn escape routes
    - Structural patterns (corridor control, band-type distribution)
    Key property: the board is a 6×6 grid with fixed non-uniform topology (the band map). The band constraint creates strong local dependencies between adjacent squares.
  - Possible models:
    - MLP: fast but no spatial awareness and relational reasoning => avoid
    - CNN: exploits local spatial patterns, translation equivariance for pattern recognition, fast enough BUT translation equivariance possible wrong => ok but overkill
    - [NNUE](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html): fast because of incremental updates, dual-perspective design, ClippedReLU (-1 to 1) activation function BUT designed for large input sizes and material changes => doesn't fit but dual-perspective and ClippedReLU are interesting ideas to borrow.
    - ResNet (MLP variant): good if baseline evaluation exists BUT no spatial awareness => good
    - GNN: movement graph, invariant to symetries BUT graph changes every turn, complex to implement and slower => avoid
    => Dual-perspective ResNet with ClippedReLU activations and CNN spatial encoder.
  
  - Export to HuggingFace (with .pth)



## Optimization
- [ ] Java optimizations:
  - [x] Use `System.nanoTime()` for more accurate time measurement instead of `System.currentTimeMillis()`.
  - [ ] Moves as int primitives (no objects)
  - [x] Avoid `instanceof` checks in move generation and evaluation by using separate methods for different piece types.
  - [ ] Flat long[] transposition table
  - [ ] Make-unmake instead of board copy
  - [ ] Move ordering (TT/killers/history)
  - [ ] Partial selection sort in move list
  - [ ] final methods + avoid virtual dispatch
  - [x] Use `System.arraycopy` for board copying instead of manual loops.
  - [x] Avoid unnecessary object creation in the search loop (e.g., reuse move lists, transposition table entries).
  - [x] Preallocate arrays
  - [ ] Sort moves in place using partial selection sort to find the best move without fully sorting the list.
  - [ ] Partial selection sort: only sort the top N moves (e.g., 4) instead of the entire move list, since we only care about the best move for alpha-beta pruning.

- [ ] Zobrist Hashing + [Transposition Table](https://www.chessprogramming.org/Transposition_Table): assign a random 64-bit integer to each piece type and board square. The hash of the board is the XOR of the values for all pieces on the board. When we make a move, we can update the hash by XORing out the piece from its old square and XORing in the piece at its new square. Store evaluated positions in a transposition table (hash map) keyed by the Zobrist hash to avoid redundant calculations.
  - store hash, depth, node type (exact, lower bound, upper bound), score, best move

- [ ] Optional threading (lazy SMP): start multiple search threads with the same root position but different random seeds for move ordering. Each thread shares the transposition table, so they can benefit from each other's work. When time runs out, return the best move found by any thread.

- [x] Bitboard board representation: represent the board using bitboards (64-bit integers, `long`) for each piece type and player. This allows for very fast move generation and board evaluation using bitwise operations. (https://www.chessprogramming.org/Bitboards & https://www.chessprogramming.org/0x88)
  - `Long.bitCount(bitboard)` to count pieces, `Long.numberOfTrailingZeros(bitboard)` to find the index of the least significant piece, etc.
- [ ] [Principal variation search](https://www.chessprogramming.org/Principal_Variation_Search) (PVS / NegaScout): an optimization of alpha-beta that assumes the first move is the best and searches it with a full window, while subsequent moves are searched with a null window (alpha, alpha+1). If a move fails high, we re-search it with a full window.

- [x] (Optional) Openning book
  - Cover all three band types with your paladins => if all your pieces are on band-2 squares, opponent landing on band-1 forces you to pass immediately.
  - Unicorn on a double band (D1, C2) => not too mobile (triple = exposed), not too restricted (single = easy to trap).
  - Don't wall the unicorn => if paladins surround the unicorn on all 4 sides, they block its escape routes.
  - Spread paladins along the file axis => you want paladins in multiple columns so that regardless of what band the opponent imposes, at least one paladin is both on that band AND has a clear attacking line toward the opponent's unicorn.
  - Avoid clustering => two paladins in the same column block each other's rook-like movement.
  - Candidate Openings (for White, mirror for Black):
    - "Balanced" (C1/A1/B2/D2/E2/F1)
      Unicorn on C1 (double)
      Paladins: A1(triple), B2(triple), D2(triple), E2(double), F1(single)
      Covers all 3 bands: single(F1), double(E2), triple(A1,B2,D2)
      Unicorn has escape via C1→C3 (1 step) or C1→A1 (blocked by A1 paladin, adjust)

    - "Central Pressure" (D1/B1/C2/E1/A2/F2)
      Unicorn on D1 (single, risky but central)
      Paladins spread across columns A–F in rows 1–2
      Very wide coverage, central paladins can reach enemy unicorn quickly

    - "Triple Dominance" (C1/A1/B2/D2/F2/E2)
      5 paladins on triple squares → high mobility
      Unicorn on double → safe
      Risk: if opponent always lands on non-triple, you have fewer choices
    ```java
    public static final String[] OPENING_BOOK_WHITE = {
      "C1/A1/B2/D2/E2/F1", // Balanced (all bands covered, unicorn central on double)
      "C1/A2/B1/D2/E1/F2", // Widespread (paladins in every column, unicorn on single)
      "C1/A1/B2/D2/F2/E2", // Triple-heavy (max mobility, aggressive)
      "D1/A1/B2/C2/E2/F1", // Central pressure (unicorn in center, paladins spread)
      "B1/A2/C1/D2/E1/F2", // Defensive (unicorn tucked at B1, paladins spread to control center and escape routes)
      "C1/D1/E1/A2/B2/F2", // Flanking (unicorn on double, paladins clustered on one side to create a strong attack vector, but riskier if opponent lands on the other side)
      "A2/B1/C2/D1/E2/F2", // Defensive edge (unicorn on double at edge, paladins spread to control center and escape routes, but unicorn has fewer escape options)
      "C1/A1/F1/B2/E2/D2", // Symmetric (paladins mirrored around center, unicorn on double)
    };
    ```
- [ ] (Optional) Endgame tablebases: precompute perfect play for all positions with a small number of pieces (e.g., unicorn + 1 paladin vs unicorn) and store the results in a database. During the game, if we reach a position that is in the tablebase, we can play the perfect move instantly.


## Bonus
- [ ] Study the impact of each heuristic components
- [ ] Study the impact of each optimization
- [ ] Create levels of AI for each optimization
- [x] Elo rating for bots: update with $R_A' = R_A + K(S_A - E_A)$ where $E_A = \frac{1}{1 + 10^{(R_B - R_A)/400}}$ and $K=32$, $S_A=1$ for win, $0.5$ for draw, $0$ for loss.
    Sequential Probability Ratio Test (SPRT) for engine evaluation:
    https://tests.stockfishchess.org/tests/view/696a9e83cec152c6220c1d1d :
    60000 games - master vs e0bfc4b69bbe928d6f474a46560bcc3b3f6709aa diff finished
    Elo: 55.03 ± 1.4 (95%) LOS: 100.0%
    Total: 60000 W: 18810 L: 9386 D: 31804
    Ptnml(0-2): 48, 3203, 14470, 11835, 444
    nElo: 112.79 ± 3.0 (95%) PairsRatio: 3.78
    Stats
    chi^2	234.30
    dof	178
    p-value	0.30%
- [x] use compute time at startup for other stuff since we'll use opening book and [Pondering](https://www.chessprogramming.org/Pondering)
- [ ] Add a bar like in chess to see which side is winning
- [ ] Mark moves as brilliant?  
- [ ] Manim video explainer
