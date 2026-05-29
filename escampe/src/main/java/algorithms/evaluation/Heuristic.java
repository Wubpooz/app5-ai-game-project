package algorithms.evaluation;

import game.EscampeBoard;
import game.PlayerColor;
import interfaces.IHeuristic;



/**
 * Basic heuristic evaluation function for Escampe. Evaluates the board state from the perspective of the given player color.
 * $\displaystyle{-w_1\min_{p\in P}{(d^{atk}_{p})} - w_2\,\text{avg}_{p\in P}(d^{atk}_{p}) + w_3\min_{e\in E}{(d^{def}_{e})} + w_4\,\text{avg}_{e\in E}(d^{def}_{e}) + w_5\,\mathcal{E_{us}} - w_6\,\mathcal{E_{opp}} + w_7 \mathcal{BC} + w_8 \mathcal{T}}$
 * The heuristic considers:
 * - Attack distance: Manhattan distance (min and avg) of our paladins to the opponent's unicorn (closer is better)
 * - Defense distance: Manhattan distance (min and avg) of opponent's paladins to our unicorn (farther is better)
 * - Unicorn escapability: number of legal moves for our unicorn (more = safer), fewer for opponent (= trapped)
 * - $\mathcal{BC}$: band control — our paladins on 1-band squares is good, opponent's on 3-band squares is good for us
 * - $\mathcal{T}$: mobility/pass pressure — our legal moves good, opponent legal moves bad, pass penalties
 * - Win/loss states (if our unicorn is captured, very bad and if opponent's unicorn is captured, very good)
 */
public class Heuristic implements IHeuristic<EscampeBoard, PlayerColor> {
  private static final int SIZE = 6;
  private static final char WHITE_UNICORN = 'B';
  private static final char WHITE_PALADIN = 'b';
  private static final char BLACK_UNICORN = 'N';
  private static final char BLACK_PALADIN = 'n';


  private final HeuristicConfig config;

  public Heuristic() {
    this.config = HeuristicConfig.createDefault();
  }

  public Heuristic(HeuristicConfig config) {
    this.config = config;
  }

  private static final int WE_PASS_PENALTY = 500; // penality when we have to pass
  private static final int PASS_PRESSURE_REWARD = 100; // reward when opponent has to pass

  private static final int WIN_SCORE = 100000; // Imediately winning positions
  private static final int LOSS_SCORE = -100000; // Imediately losing positions


  @Override
  public int eval(EscampeBoard board, PlayerColor role) {
    // Variables to track piece positions
    int myUnicornRow = -1;
    int myUnicornCol = -1;
    int oppUnicornRow = -1;
    int oppUnicornCol = -1;
    int[][] paladinPositionsBand = new int[5][3]; // max 5 paladins on the board
    int paladinCount = 0;
    int[][] oppPaladinPositionsBand = new int[5][3];
    int oppPaladinCount = 0;

    char myUnicornChar = (role == PlayerColor.WHITE) ? WHITE_UNICORN : BLACK_UNICORN;
    char oppUnicornChar = (role == PlayerColor.WHITE) ? BLACK_UNICORN : WHITE_UNICORN;
    char myPaladinChar = (role == PlayerColor.WHITE) ? WHITE_PALADIN : BLACK_PALADIN;
    char oppPaladinChar = (role == PlayerColor.WHITE) ? BLACK_PALADIN : WHITE_PALADIN;



    // =============== Locate unicorns and paladins ===============
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        char piece = board.getPieceAt(r, c);
        if (piece == myUnicornChar) {
          myUnicornRow = r;
          myUnicornCol = c;
        } else if (piece == oppUnicornChar) {
          oppUnicornRow = r;
          oppUnicornCol = c;
        } else if (piece == myPaladinChar && paladinCount < 5) {
          paladinPositionsBand[paladinCount][0] = r;
          paladinPositionsBand[paladinCount][1] = c;
          paladinPositionsBand[paladinCount][2] = board.getLiseretAt(r, c);
          paladinCount++;
        } else if (piece == oppPaladinChar && oppPaladinCount < 5) {
          oppPaladinPositionsBand[oppPaladinCount][0] = r;
          oppPaladinPositionsBand[oppPaladinCount][1] = c;
          oppPaladinPositionsBand[oppPaladinCount][2] = board.getLiseretAt(r, c);
          oppPaladinCount++;
        }
      }
    }

    // =============== Win/loss states ===============
    if (myUnicornRow == -1 || myUnicornCol == -1 || paladinCount == 0) return LOSS_SCORE;
    if (oppUnicornRow == -1 || oppUnicornCol == -1 || oppPaladinCount == 0) return WIN_SCORE;


    // =====================================================
    // =============== Heuristic calculation ===============
    // =====================================================
    int myLegalMoves = board.countPossibleMoves(role);
    int oppLegalMoves = board.countPossibleMoves(role.getOpponent());
    int myUnicornEscapability = board.countPossibleMovesForUnicorn(role);
    int oppUnicornEscapability = board.countPossibleMovesForUnicorn(role.getOpponent());


    // Calculate distances using already-collected paladin positions (no second board scan)
    int minMyDist = Integer.MAX_VALUE;
    int minOppDist = Integer.MAX_VALUE;
    int sumMyDist = 0;    
    int sumOppDist = 0;

    for (int i = 0; i < paladinCount; i++) {
      int d = Math.abs(paladinPositionsBand[i][0] - oppUnicornRow) + Math.abs(paladinPositionsBand[i][1] - oppUnicornCol);
      sumMyDist += d;
      if (d < minMyDist) minMyDist = d;
    }
    for (int i = 0; i < oppPaladinCount; i++) {
      int d = Math.abs(oppPaladinPositionsBand[i][0] - myUnicornRow) + Math.abs(oppPaladinPositionsBand[i][1] - myUnicornCol);
      sumOppDist += d;
      if (d < minOppDist) minOppDist = d;
    }


    // =============== Score calculation ===============
    // Closer our paladins are to opponent's unicorn -> better (subtract distance)
    // Closer opponent's paladins are to our unicorn -> worse (add distance)
    int score = 0;

    // Legal moves score and pass penalty
    if (myLegalMoves == 0) {
      score -= WE_PASS_PENALTY; // penality when we have to pass
    } else {
      score += myLegalMoves * config.weightLegalMoves; // reward for having more legal moves
    }

    // Pass pressure
    if (oppLegalMoves == 0) {
      score += PASS_PRESSURE_REWARD; // reward when opponent has to pass
    } else {
      score -= oppLegalMoves * config.weightLegalMoves; // penality for opponent having more legal moves
    }

    // Band coverage: penalize missing bands (1, 2, 3) to prevent forced passes
    boolean[] myBands = new boolean[4];
    boolean[] oppBands = new boolean[4];
    for (int i = 0; i < paladinCount; i++) {
      myBands[paladinPositionsBand[i][2]] = true;
    }
    for (int i = 0; i < oppPaladinCount; i++) {
      oppBands[oppPaladinPositionsBand[i][2]] = true;
    }
    if (myUnicornRow >= 0 && myUnicornCol >= 0) {
      myBands[board.getLiseretAt(myUnicornRow, myUnicornCol)] = true;
    }
    if (oppUnicornRow >= 0 && oppUnicornCol >= 0) {
      oppBands[board.getLiseretAt(oppUnicornRow, oppUnicornCol)] = true;
    }

    int myMissingBands = 0;
    if (!myBands[1]) myMissingBands++;
    if (!myBands[2]) myMissingBands++;
    if (!myBands[3]) myMissingBands++;
    score -= myMissingBands * config.weightBandControl;

    int oppMissingBands = 0;
    if (!oppBands[1]) oppMissingBands++;
    if (!oppBands[2]) oppMissingBands++;
    if (!oppBands[3]) oppMissingBands++;
    score += oppMissingBands * config.weightOppBandControl;

    // Unicorn escapability score
    score += myUnicornEscapability * config.weightEscapability; // more escape routes for our unicorn = good
    score -= oppUnicornEscapability * config.weightTrappedUnicorn; // more escape routes for opponent unicorn = bad

    // Unicorn danger scores
    if (minMyDist != Integer.MAX_VALUE) {
      score -= config.weightMinDist * minMyDist; // min distance is more important than average distance, so we multiply it by a higher weight
      score -= config.weightAvgDist * sumMyDist / paladinCount; // average distance
    }
    if (minOppDist != Integer.MAX_VALUE) {
      score += config.weightUnicornDangerMinDist * minOppDist;
      score += config.weightUnicornDangerAvgDist * sumOppDist / oppPaladinCount; // average distance
    }

    return score;
  }
}
