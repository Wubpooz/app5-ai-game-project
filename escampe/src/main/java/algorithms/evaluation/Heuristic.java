package algorithms.evaluation;

import game.EscampeBoard;
import game.PlayerColor;
import interfaces.IHeuristic;



/**
 * Basic heuristic evaluation function for Escampe. Evaluates the board state from the perspective of the given player color.
 * $\displaystyle{w_1\min_{p\in P}{(d_{p})} + w_2\,\text{avg}_{p\in P}(d_{p}) + w_3\,\mathcal{S_l} + w_4\sum_{e \in (P \wedge l)} \text{moves}(e) + w_5 \mathcal{BC} + w_6 \mathcal{T}}$
 * The heuristic considers:
 * - The distance (min and average) of our paladins to the opponent's unicorn (closer is better)
 * - The distance of opponent's paladins to our unicorn (closer is worse) either in manhattan distance or band-aware distance (BFS ply distance)
 * - Unicorn escapability: number of legal moves for the unicorn of the opponent in 1-2 moves.
 * - $\mathcal{BC}$: landing on a 1-band square is bad for the opponent, landing on a 3-band square is good for the opponent. Low bands are good when ahead, high bands are good when behind.
 * - $\mathcal{T}$: penality when few legal moves for us, reward when few legal moves for opponent.
 * - Win/loss states (if our unicorn is captured, very bad and if opponent's unicorn is captured, very good)
 */
public class Heuristic implements IHeuristic<EscampeBoard, PlayerColor> {
  private static final int SIZE = 6;
  private static final char WHITE_UNICORN = 'B';
  private static final char WHITE_PALADIN = 'b';
  private static final char BLACK_UNICORN = 'N';
  private static final char BLACK_PALADIN = 'n';


  private static final int WEIGHT_MIN_DIST = 10; // min distance from our paladins to the opponent unicorn
  private static final int WEIGHT_AVG_DIST = 2; // average distance from our paladins to the opponent unicorn
  private static final int WEIGHT_UNICORN_DANGER_MIN_DIST = 5; // penality for each opponent paladin close to our unicorn
  private static final int WEIGHT_UNICORN_DANGER_AVG_DIST = 5; // penality for each opponent paladin close to our unicorn
  private static final int WEIGHT_ESCAPABILITY = 50; // penality when our unicorn has few escape moves
  private static final int WEIGHT_TRAPPED_UNICORN = 30; // reward when opponent unicorn has few escape moves
  private static final int WEIGHT_LEGAL_MOVES = 10; // our move count is bad, opponent move count is good
  private static final int WEIGHT_BAND_CONTROL = 80; // reward for controlling bands that restrict opponent movements
  private static final int WEIGHT_OPP_BAND_CONTROL = 90; // penality for opponent controlling bands that restrict our movements

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
    int[][] paladinPositionsBand = new int[4][3]; // max 4 paladins on the board
    int paladinCount = 0;
    int[][] oppPaladinPositionsBand = new int[4][3];
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
        } else if (piece == myPaladinChar) {
          if (paladinCount < 4) {
            paladinPositionsBand[paladinCount][0] = r;
            paladinPositionsBand[paladinCount][1] = c;
            paladinPositionsBand[paladinCount][2] = board.getLiseretAt(r, c);
            paladinCount++;
          }
        } else if (piece == oppPaladinChar) {
          if (oppPaladinCount < 4) {
            oppPaladinPositionsBand[oppPaladinCount][0] = r;
            oppPaladinPositionsBand[oppPaladinCount][1] = c;
            oppPaladinPositionsBand[oppPaladinCount][2] = board.getLiseretAt(r, c);
            oppPaladinCount++;
          }
        }
      }
    }

    // =============== Win/loss states ===============
    if (myUnicornRow == -1 || myUnicornCol == -1 || paladinCount == 0) return LOSS_SCORE;
    if (oppUnicornRow == -1 || oppUnicornCol == -1 || oppPaladinCount == 0) return WIN_SCORE;


    // =====================================================
    // =============== Heuristic calculation ===============
    // =====================================================
    int myLegalMoves = board.possibleMoves(role).size();
    int oppLegalMoves = board.possibleMoves(role.getOpponent()).size();
    int myUnicornEscapability = board.possibleMovesForUnicorn(role).size();
    int oppUnicornEscapability = board.possibleMovesForUnicorn(role.getOpponent()).size();


    // Calculate distances of paladins to opponent unicorn and opponent paladins to our unicorn
    int minMyDist = Integer.MAX_VALUE;
    int minOppDist = Integer.MAX_VALUE;
    int sumMyDist = 0;    
    int sumOppDist = 0;

    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        char piece = board.getPieceAt(r, c);
        if (piece == myPaladinChar) {
          int d = Math.abs(r - oppUnicornRow) + Math.abs(c - oppUnicornCol); // Manhattan distance, raw but faster
          sumMyDist += d;
          if (d < minMyDist) minMyDist = d;
        } else if (piece == oppPaladinChar) {
          int d = Math.abs(r - myUnicornRow) + Math.abs(c - myUnicornCol); // Manhattan distance, raw but faster
          sumOppDist += d;
          if (d < minOppDist) minOppDist = d;
        }
      }
    }


    // =============== Score calculation ===============
    // Closer our paladins are to opponent's unicorn -> better (subtract distance)
    // Closer opponent's paladins are to our unicorn -> worse (add distance)
    int score = 0;

    // Legal moves score and pass penalty
    if (myLegalMoves == 0) {
      score -= WE_PASS_PENALTY; // penality when we have to pass
    } else {
      score += myLegalMoves * WEIGHT_LEGAL_MOVES; // reward for having more legal moves
    }

    // Pass pressure
    if (oppLegalMoves == 0) {
      score += PASS_PRESSURE_REWARD; // reward when opponent has to pass
    } else {
      score -= oppLegalMoves * WEIGHT_LEGAL_MOVES; // penality for opponent having more legal moves
    }

    // Band control score
    for (int i = 0; i < paladinCount; i++) {
      int band = paladinPositionsBand[i][2];
      if (band == 1) score += WEIGHT_BAND_CONTROL; // controlling a 1-band square is good
      else if (band == 3) score -= WEIGHT_BAND_CONTROL; // controlling a 3-band square is bad
    }
    for (int i = 0; i < oppPaladinCount; i++) {
      int band = oppPaladinPositionsBand[i][2];
      if (band == 1) score -= WEIGHT_OPP_BAND_CONTROL; // opponent controlling a 1-band square is bad for us
      else if (band == 3) score += WEIGHT_OPP_BAND_CONTROL; // opponent controlling a 3-band square is good for us
    }
    //TODO maybe add the check of wheter the band control is actually restricting the opponent movements

    // Unicorn escapability score
    score -= myUnicornEscapability * WEIGHT_ESCAPABILITY; // penality when our unicorn has few escape moves
    score += oppUnicornEscapability * WEIGHT_TRAPPED_UNICORN; // reward when opponent unicorn has few escape moves

    // Unicorn danger scores
    if (minMyDist != Integer.MAX_VALUE) {
      score -= WEIGHT_MIN_DIST * minMyDist; // min distance is more important than average distance, so we multiply it by a higher weight
      score -= WEIGHT_AVG_DIST * sumMyDist / paladinCount; // average distance
    }
    if (minOppDist != Integer.MAX_VALUE) {
      score += WEIGHT_UNICORN_DANGER_MIN_DIST * minOppDist;
      score += WEIGHT_UNICORN_DANGER_AVG_DIST * sumOppDist / oppPaladinCount; // average distance
    }

    return score;
  }
}
