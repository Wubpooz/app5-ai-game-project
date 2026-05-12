bitboards

| Operation                   | Naive (array loops)     | Bitboard                                   |
| --------------------------- | ----------------------- | ------------------------------------------ |
| "Is square X occupied?"     | Array lookup + branch   | (bb >> x) & 1L — 1 instruction             |
| "All my pieces"             | Loop over piece list    | Single long read                           |
| "Legal moves from square X" | Loop + collision checks | Precomputed table lookup + AND/mask        |
| "Count legal moves"         | Loop counter            | Long.bitCount(bb) — 1 instruction (POPCNT) |
| "Iterate over all pieces"   | Loop over array         | Long.numberOfTrailingZeros loop            |
| Move generation             | Nested loops            | Bitwise AND with precomputed ray tables    |
| Position hashing (Zobrist)  | Loop over board         | XOR precomputed table per set bit          |
| Transposition table key     | Full board copy         | XOR of piece bitboards                     |


public class EscampeBitboard {

    // One bitboard per piece type per color
    long whitePaladins;   // 4 bits set
    long whiteUnicorn;    // 1 bit set
    long blackPaladins;   // 4 bits set
    long blackUnicorn;    // 1 bit set

    // Derived (recompute when pieces move — cheap)
    long whitePieces() { return whitePaladins | whiteUnicorn; }
    long blackPieces() { return blackPaladins | blackUnicorn; }
    long allPieces()   { return whitePieces() | blackPieces(); }

    // Current required band (1, 2, or 3; 0 = free turn)
    int requiredBand;

    // Zobrist hash (maintained incrementally)
    long zobristKey;
}

public class EscampeTables {

    // Band map: bandOf[bit] = 1, 2, or 3
    static final int[] BAND = new int[36];

    // BAND_MASK[b] = all squares with band value b
    static final long[] BAND_MASK = new long[4]; // indices 1,2,3

    // RAY tables: RAYS[dir][sq] = all squares reachable from sq
    // in direction dir ignoring blockers, at distance 1,2,3
    // We need per-distance rays because move distance = band of departure square
    // RAY_DIST[sq][dir][dist] = target square bitmask (0 if off board)
    static final long[][][] RAY_DIST = new long[36][4][4]; // 4 dirs, dist 1-3

    // Direction encoding: 0=N, 1=S, 2=E, 3=W (rook moves only, no diagonals)
    static final int[] DR = {-1, +1,  0,  0};
    static final int[] DC = { 0,  0, +1, -1};

    // Zobrist random keys
    // ZOBRIST[piece_type][sq], piece_type: 0=wPal,1=wUni,2=bPal,3=bUni
    static final long[][] ZOBRIST = new long[4][36];
    static final long[] ZOBRIST_BAND = new long[4]; // for required band

    static {
        initBands();
        initRays();
        initZobrist();
    }

    private static void initBands() {
        // The fixed Escampe band pattern
        int[][] bandGrid = {
            {3,1,2,1,3,1},
            {2,3,1,3,2,3},
            {1,2,3,2,1,2},
            {3,1,2,1,3,1},
            {2,3,1,3,2,3},
            {1,2,3,2,1,2}
        };
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 6; c++) {
                int sq = r*6+c;
                BAND[sq] = bandGrid[r][c];
                BAND_MASK[bandGrid[r][c]] |= 1L << sq;
            }
    }

    private static void initRays() {
        for (int sq = 0; sq < 36; sq++) {
            int r = sq/6, c = sq%6;
            for (int dir = 0; dir < 4; dir++) {
                for (int dist = 1; dist <= 3; dist++) {
                    int nr = r + DR[dir]*dist;
                    int nc = c + DC[dir]*dist;
                    if (nr >= 0 && nr < 6 && nc >= 0 && nc < 6)
                        RAY_DIST[sq][dir][dist] = 1L << (nr*6+nc);
                    // else stays 0 (off board)
                }
            }
        }
    }

    private static void initZobrist() {
        java.util.Random rng = new java.util.Random(0xDEADBEEFCAFEL);
        for (int p = 0; p < 4; p++) {
            for (int sq = 0; sq < 36; sq++)
                ZOBRIST[p][sq] = rng.nextLong();
            ZOBRIST_BAND[p] = rng.nextLong(); // p reused as band index 1-3
        }
    }
}


int generateMoves(EscampeBitboard pos, boolean whiteToMove, int[] moveBuf) {
    int count = 0;
    long occupied = pos.allPieces();

    // Which of my pieces can depart? (band must match requiredBand, or free turn)
    long myPieces = whiteToMove ? pos.whitePieces() : pos.blackPieces();
    long myPaladins = whiteToMove ? pos.whitePaladins : pos.blackPaladins;

    long canDepart = (pos.requiredBand == 0)
        ? myPieces
        : myPieces & BAND_MASK[pos.requiredBand];

    // Iterate over departing pieces
    long depart = canDepart;
    while (depart != 0) {
        int from = Long.numberOfTrailingZeros(depart);
        depart &= depart - 1; // clear lowest bit

        int dist = BAND[from]; // move distance = band of departure square
        // Paladins can't be captured, so landing on them is illegal
        // Unicorns can be captured (that's winning), so landing on enemy unicorn IS legal
        long myAll = whiteToMove ? pos.whitePieces() : pos.blackPieces();
        long enemyUnicorn = whiteToMove ? pos.blackUnicorn : pos.whiteUnicorn;
        // Can't land on any piece EXCEPT enemy unicorn (capture = win)
        long blocked = occupied & ~enemyUnicorn;

        for (int dir = 0; dir < 4; dir++) {
            long target = RAY_DIST[from][dir][dist];
            if (target == 0) continue;               // off board
            if ((target & blocked) != 0) continue;  // blocked by piece

            // Also check intermediate squares aren't blocked (can't jump)
            boolean pathClear = true;
            for (int d = 1; d < dist; d++) {
                if ((RAY_DIST[from][dir][d] & occupied) != 0) {
                    pathClear = false;
                    break;
                }
            }
            if (!pathClear) continue;

            // Encode move as int: bits 0-5 = from, bits 6-11 = to
            int to = Long.numberOfTrailingZeros(target);
            moveBuf[count++] = (from) | (to << 6);
        }
    }
    return count; // number of legal moves found
}

// Returns captured piece type (-1 if none) for unmake
int makeMove(EscampeBitboard pos, int move, boolean whiteToMove) {
    int from = move & 0x3F;
    int to   = (move >> 6) & 0x3F;
    long fromBit = 1L << from;
    long toBit   = 1L << to;
    int captured = -1;

    // Update Zobrist: remove piece from 'from'
    int myPieceType = whiteToMove ? 0 : 2; // 0=wPal,1=wUni,2=bPal,3=bUni
    // Determine exact piece type
    if (whiteToMove) {
        if ((pos.whiteUnicorn & fromBit) != 0) myPieceType = 1;
    } else {
        if ((pos.blackUnicorn & fromBit) != 0) myPieceType = 3;
    }
    pos.zobristKey ^= EscampeTables.ZOBRIST[myPieceType][from];
    pos.zobristKey ^= EscampeTables.ZOBRIST[myPieceType][to];

    // Check capture (landing on enemy unicorn)
    long enemyUnicorn = whiteToMove ? pos.blackUnicorn : pos.whiteUnicorn;
    if ((toBit & enemyUnicorn) != 0) {
        captured = whiteToMove ? 3 : 1;
        pos.zobristKey ^= EscampeTables.ZOBRIST[captured][to];
        if (whiteToMove) pos.blackUnicorn = 0;
        else             pos.whiteUnicorn = 0;
    }

    // Move piece
    if (whiteToMove) {
        if ((pos.whitePaladins & fromBit) != 0) {
            pos.whitePaladins ^= fromBit; pos.whitePaladins |= toBit;
        } else {
            pos.whiteUnicorn = toBit;
        }
    } else {
        if ((pos.blackPaladins & fromBit) != 0) {
            pos.blackPaladins ^= fromBit; pos.blackPaladins |= toBit;
        } else {
            pos.blackUnicorn = toBit;
        }
    }

    // Update required band: next player must move pieces on band = BAND[to]
    pos.zobristKey ^= EscampeTables.ZOBRIST_BAND[pos.requiredBand];
    pos.requiredBand = EscampeTables.BAND[to];
    pos.zobristKey ^= EscampeTables.ZOBRIST_BAND[pos.requiredBand];

    return captured;
}

void unmakeMove(EscampeBitboard pos, int move, boolean whiteToMove,
                int captured, int prevBand) {
    int from = move & 0x3F;
    int to   = (move >> 6) & 0x3F;
    long fromBit = 1L << from;
    long toBit   = 1L << to;

    // Reverse move
    if (whiteToMove) {
        if ((pos.whitePaladins & toBit) != 0) {
            pos.whitePaladins ^= toBit; pos.whitePaladins |= fromBit;
        } else {
            pos.whiteUnicorn = fromBit;
        }
    } else {
        if ((pos.blackPaladins & toBit) != 0) {
            pos.blackPaladins ^= toBit; pos.blackPaladins |= fromBit;
        } else {
            pos.blackUnicorn = fromBit;
        }
    }

    // Restore captured piece
    if (captured == 3) pos.blackUnicorn = toBit;
    if (captured == 1) pos.whiteUnicorn = toBit;

    // Restore required band (passed from caller's stack)
    pos.requiredBand = prevBand;

    // Recompute Zobrist fully (or maintain stack — easier for unmake)
    // For simplicity: recompute from scratch (still O(12) piece count)
    pos.zobristKey = computeZobrist(pos);
}



// Count legal moves for mobility heuristic — replaces your w4 term
int mobilityCount(EscampeBitboard pos, boolean white) {
    int[] buf = new int[256];
    return generateMoves(pos, white, buf);
}

// Distance of closest paladin to enemy unicorn (your w1 term)
int closestPaladinDist(EscampeBitboard pos, boolean white) {
    long paladins = white ? pos.whitePaladins : pos.blackPaladins;
    long unicorn  = white ? pos.blackUnicorn  : pos.whiteUnicorn;
    int unicornSq = Long.numberOfTrailingZeros(unicorn);
    int uRow = unicornSq/6, uCol = unicornSq%6;
    int minDist = Integer.MAX_VALUE;
    while (paladins != 0) {
        int sq = Long.numberOfTrailingZeros(paladins);
        paladins &= paladins - 1;
        int dist = Math.abs(sq/6 - uRow) + Math.abs(sq%6 - uCol);
        minDist = Math.min(minDist, dist);
    }
    return minDist;
}

// Band control score — your w5 term
int bandControlScore(EscampeBitboard pos, boolean white) {
    long myPieces = white ? pos.whitePieces() : pos.blackPieces();
    // Reward pieces on the required band (can move next turn if opponent lands there)
    // Count pieces on each band
    int score = 0;
    score += Long.bitCount(myPieces & BAND_MASK[1]) * 1;
    score += Long.bitCount(myPieces & BAND_MASK[2]) * 2;
    score += Long.bitCount(myPieces & BAND_MASK[3]) * 3;
    return score;
}

// Check if opponent is forced to pass
boolean isForcedPass(EscampeBitboard pos, boolean opponentIsWhite) {
    int[] buf = new int[256];
    return generateMoves(pos, opponentIsWhite, buf) == 0;
}

// Unicorn escape routes — your Sl term
int unicornEscapeRoutes(EscampeBitboard pos, boolean white) {
    // Temporarily treat unicorn as a moving piece and count its legal destinations
    long unicorn = white ? pos.whiteUnicorn : pos.blackUnicorn;
    int sq = Long.numberOfTrailingZeros(unicorn);
    int dist = BAND[sq];
    int count = 0;
    long blocked = pos.allPieces() & ~unicorn;
    for (int dir = 0; dir < 4; dir++) {
        long target = RAY_DIST[sq][dir][dist];
        if (target == 0) continue;
        if ((target & blocked) != 0) continue;
        boolean clear = true;
        for (int d = 1; d < dist; d++)
            if ((RAY_DIST[sq][dir][d] & blocked) != 0) { clear=false; break; }
        if (clear) count++;
    }
    return count;
}



@SuppressWarnings("unchecked")
static void precomputePaths() {
    PATHS = new List[36][4];
    for (int sq = 0; sq < 36; sq++)
        for (int d = 1; d <= 3; d++)
            PATHS[sq][d] = new ArrayList<>();

    int[] DR = {-1, +1, 0, 0};
    int[] DC = { 0,  0, +1, -1};

    for (int from = 0; from < 36; from++) {
        for (int dist = 1; dist <= 3; dist++) {
            // DFS: find all walks of exactly `dist` steps from `from`
            // State: current square, visited mask (including from), steps taken
            dfsWalk(from, dist, from, 1L << from, 0L, PATHS[from][dist], DR, DC);
        }
    }
}

static void dfsWalk(int from, int dist, int cur, long visited,
                    long intermediates, List<long[]> results,
                    int[] DR, int[] DC) {
    if (Long.bitCount(visited) - 1 == dist) {
        // Reached exactly `dist` steps — `cur` is the destination
        // intermediates = all squares visited except `from` and `cur`
        results.add(new long[]{ cur, intermediates });
        return;
    }
    int steps = (int)(Long.bitCount(visited) - 1); // steps taken so far
    int row = cur / 6, col = cur % 6;
    for (int dir = 0; dir < 4; dir++) {
        int nr = row + DR[dir], nc = col + DC[dir];
        if (nr < 0 || nr >= 6 || nc < 0 || nc >= 6) continue;
        int next = nr * 6 + nc;
        long nextBit = 1L << next;
        if ((visited & nextBit) != 0) continue; // already visited this move
        long newIntermediates = (steps < dist - 1) 
            ? intermediates | nextBit  // next is intermediate
            : intermediates;           // next is destination, not intermediate
        dfsWalk(from, dist, next, visited | nextBit, newIntermediates, results, DR, DC);
    }
}



static int generateMoves(long myPaladins, long myUnicorn,
                         long oppPaladins, long oppUnicorn,
                         int requiredBand, int[] moveBuf) {
    int count = 0;
    long myPieces  = myPaladins  | myUnicorn;
    long oppPieces = oppPaladins | oppUnicorn;
    long allPieces = myPieces | oppPieces;

    // Which of my pieces sit on the required band?
    long canDepart = (requiredBand == 0)
        ? myPieces
        : myPieces & BAND_MASK[requiredBand];

    long depart = canDepart;
    while (depart != 0) {
        int from = Long.numberOfTrailingZeros(depart);
        depart &= depart - 1;

        int dist = BAND[from];
        List<long[]> paths = PATHS[from][dist];

        for (long[] path : paths) {
            int  to            = (int) path[0];
            long intermediates =       path[1];

            // 1. Intermediates must be empty (can't pass through pieces)
            if ((intermediates & allPieces) != 0) continue;

            long toBit = 1L << to;

            // 2. Can't land on own piece
            if ((toBit & myPieces) != 0) continue;

            // 3. Can't land on enemy paladin (imprenable)
            if ((toBit & oppPaladins) != 0) continue;

            // 4. Landing on enemy unicorn = capture = win (always legal if path clear)
            // Landing on empty square = normal move

            // Encode move: bits 0-5 = from, bits 6-11 = to
            moveBuf[count++] = from | (to << 6);
        }
    }
    return count;
}



// Flat storage: for each (sq, dist) block, store pairs [to, intermediates]
// Index with PATH_START[sq][dist] into PATH_DATA[]

static int[][]  PATH_START = new int[36][4];   // start index in PATH_DATA
static int[][]  PATH_LEN   = new int[36][4];   // number of paths
static long[]   PATH_DATA;                      // flat: [to0, inter0, to1, inter1, ...]
                                                // where to is stored as long for alignment

// Access pattern in move generator (zero allocation):
int start = PATH_START[from][dist];
int len   = PATH_LEN[from][dist];
for (int i = 0; i < len; i++) {
    int  to            = (int) PATH_DATA[start + i*2];
    long intermediates =       PATH_DATA[start + i*2 + 1];
    // ... collision checks ...
}





