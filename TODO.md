# TODO
## Search
- [ ] Minimax with alpha-beta pruning

- [ ] Negamax with alpha-beta pruning: since the game is zero-sum, we can simplify the implementation by using a single function that returns the score from the perspective of the current player. The opponent's best score is just the negative of our best score.

- [ ] Iterative deepening: start with a shallow depth and increase it until time runs out, always keeping track of the best move found so far.

- [ ] Repetition detection using the transposition table: if we encounter the same board position again, we can detect a draw by repetition.

- [ ] Move ordering
  - [ ] TT move first
  - [ ] Captures and immediate tactial mpves sorted by MVV/LVA (most valuable victim/least valuable attacker)
  - [ ] History heuristic: moves that have caused beta cutoffs in the past are tried earlier.
  - [ ] Killer moves: moves that caused cutoffs at the same depth in other branches are tried earlier.
  - [ ] Remaining moves

- [ ] Selective pruning
  - [ ] Null-move pruning (not the pass move, only for regular moves): assume that if we skip our turn, the opponent will get a chance to move. If the opponent's best response is still worse than our current best score, we can prune this branch.
  - [ ] Late Move Reduction: if we are deep in the search and we have already found a good move, we can reduce the depth of search for less promising moves (e.g., non-captures, non-checks) to save time.
  - [ ] Futility pruning: if we are near the end of the search and the current position is already much worse than our best score, we can prune this branch since it's unlikely to improve.

- [ ] Quiescence search: extend search in "noisy" positions (captures, checks) to avoid horizon effect.

## Heuristics
- [ ] Evaluation function: $\displaystyle{w_1\min_{p\in P}{(d_{p})} + w_2\,\text{avg}_{p\in P}(d_{p}) + w_3\,\mathcal{S_l} + w_4\sum_{e \in (P \wedge l)} \text{moves}(e) + w_5 \mathcal{BC} + w_6 \mathcal{T}}$
  - Band-aware distance: BFS ply distance search instead of Manhattan distance
  - Phase base weights
  - $\mathcal{BC}$: landing on a 1-band square is bad for the opponent, landing on a 3-band square is good for the opponent. Low bands are good when ahead, high bands are good when behind.
  - $\mathcal{T}$: penality when few legal moves for us, reward when few legal moves for opponent.
  - Unicorn escapability: number of legal moves for the unicorn of the opponent in 1-2 moves.

- [ ] Time management: 
  - Spend more time in complex midgame positions (high branching factor, many legal moves).
  - Spend less time in forced/obvious positions (only 1–2 legal moves due to band constraint).
  ```java
  long timeForMove = Math.min(
      remainingTime / estimatedMovesLeft,
      remainingTime * 0.1  // never use more than 10% on one move
  );
  ```

- [ ] (Optional) NN-based evaluation function (export visualisation from [NN-SVG](https://alexlenail.me/NN-SVG/LeNet.html))
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
  - [ ] Use `System.nanoTime()` for more accurate time measurement instead of `System.currentTimeMillis()`.
  - [ ] Moves as int primitives (no objects)
  - [ ] Avoid `instanceof` checks in move generation and evaluation by using separate methods for different piece types.
  - [ ] Flat long[] transposition table
  - [ ] Make-unmake instead of board copy
  - [ ] Move ordering (TT/killers/history)
  - [ ] Partial selection sort in move list
  - [ ] final methods + avoid virtual dispatch
  - [ ] Use `System.arraycopy` for board copying instead of manual loops.
  - [ ] Avoid unnecessary object creation in the search loop (e.g., reuse move lists, transposition table entries).
  - [ ] Preallocate arrays
  - [ ] Sort moves in place using partial selection sort to find the best move without fully sorting the list.
  - [ ] Partial selection sort: only sort the top N moves (e.g., 4) instead of the entire move list, since we only care about the best move for alpha-beta pruning.

- [ ] Zobrist Hashing + Transposition Table: assign a random 64-bit integer to each piece type and board square. The hash of the board is the XOR of the values for all pieces on the board. When we make a move, we can update the hash by XORing out the piece from its old square and XORing in the piece at its new square. Store evaluated positions in a transposition table (hash map) keyed by the Zobrist hash to avoid redundant calculations.
  - store hash, depth, node type (exact, lower bound, upper bound), score, best move

- [ ] Optional threading (lazy SMP): start multiple search threads with the same root position but different random seeds for move ordering. Each thread shares the transposition table, so they can benefit from each other's work. When time runs out, return the best move found by any thread.

- [ ] Bitboard board representation: represent the board using bitboards (64-bit integers, `long`) for each piece type and player. This allows for very fast move generation and board evaluation using bitwise operations.
  - `Long.bitCount(bitboard)` to count pieces, `Long.numberOfTrailingZeros(bitboard)` to find the index of the least significant piece, etc.
- [ ] Principal Variation Search (PVS / NegaScout): an optimization of alpha-beta that assumes the first move is the best and searches it with a full window, while subsequent moves are searched with a null window (alpha, alpha+1). If a move fails high, we re-search it with a full window.

- [ ] (Optional) Openning book
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
      4 paladins on triple squares → high mobility
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

## Bonus
- [ ] Study the impact of each heuristic components
- [ ] Study the impact of each optimization
- [ ] Create levels of AI for each optimization
- [ ] Elo rating for bots: update with $R_A' = R_A + K(S_A - E_A)$ where $E_A = \frac{1}{1 + 10^{(R_B - R_A)/400}}$ and $K=32$, $S_A=1$ for win, $0.5$ for draw, $0$ for loss.
- [ ] `-v` flag to print the board each turn else don't 
- [ ] Add a bar like in chess to see which side is winning
- [ ] Mark moves as brilliant?  
- [ ] Solo & Duo human play ?  