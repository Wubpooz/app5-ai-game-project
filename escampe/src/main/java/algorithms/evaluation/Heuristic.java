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
  private static final int WEIGHT_UNICORN_DANGER_DIST = 5; // penality for each opponent paladin within 2 moves of our unicorn
  private static final int WEIGHT_ESCAPABILITY = 50; // penality when our unicorn has few escape moves
  private static final int WEIGHT_TRAPPED_UNICORN = 30; // reward when opponent unicorn has few escape moves
  private static final int WEIGHT_LEGAL_MOVES = 10; // our move count is bad, opponent move count is good
  private static final int WEIGHT_BAND_CONTROL = 5; // reward for controlling bands that restrict opponent movements
  private static final int WEIGHT_OPP_BAND_CONTROL = 5; // penality for opponent controlling bands that restrict our movements

  private static final int WE_PASS_PENALTY = 20; // penality when we have to pass
  private static final int PASS_PRESSURE_REWARD = 20; // reward when opponent has to pass

  private static final int WIN_SCORE = 100000; // Imediately winning positions
  private static final int LOSS_SCORE = -100000; // Imediately losing positions


  @Override
  public int eval(EscampeBoard board, PlayerColor role) {
    // Variables to track piece positions
    int myUnicornRow = -1;
    int myUnicornCol = -1;
    int oppUnicornRow = -1;
    int oppUnicornCol = -1;
    int[][] paladinPositions = new int[4][2]; // max 4 paladins on the board
    int paladinCount = 0;
    int[][] oppPaladinPositions = new int[4][2];
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
            paladinPositions[paladinCount][0] = r;
            paladinPositions[paladinCount][1] = c;
            paladinCount++;
          }
        } else if (piece == oppPaladinChar) {
          if (oppPaladinCount < 4) {
            oppPaladinPositions[oppPaladinCount][0] = r;
            oppPaladinPositions[oppPaladinCount][1] = c;
            oppPaladinCount++;
          }
        }
      }
    }

    // =============== Win/loss states ===============
    if (myUnicornRow == -1) return LOSS_SCORE;
    if (oppUnicornRow == -1) return WIN_SCORE;


    // =====================================================
    // =============== Heuristic calculation ===============
    // =====================================================
    // TODO calculate min, avg  d(palad, opp unic)
    // TODO calculate min, avg  d(opp palad, our unic)
    // TODO calculate unicorn escapability (number of legal moves for opponent unicorn in 1-2 moves)
    // TODO calculate oppennent unicorn threat
    // TODO calculate legal moves for us and opponent => penality when few legal moves for us, reward when few legal moves for opponent and large penality when we have to pass, reward when opponent has to pass
    // TODO store band of each piece and calculate band control score => reward for controlling bands that restrict opponent movements, 
    // TODO penality for opponent controlling bands that restrict our movements

    // Basic heuristic: Threat distance
    int threatScore = 0;

    // We want our paladins close to the opponent's unicorn
    int minMyDist = Integer.MAX_VALUE;
    int sumMyDist = 0;
    
    // Opponent wants their paladins close to our unicorn
    int minOppDist = Integer.MAX_VALUE;
    int sumOppDist = 0;

    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        char piece = board.getPieceAt(r, c);
        if (piece == myPaladinChar) {
          int d = Math.abs(r - oppUnicornRow) + Math.abs(c - oppUnicornCol);
          sumMyDist += d;
          if (d < minMyDist) minMyDist = d;
        } else if (piece == oppPaladinChar) {
          int d = Math.abs(r - myUnicornRow) + Math.abs(c - myUnicornCol);
          sumOppDist += d;
          if (d < minOppDist) minOppDist = d;
        }
      }
    }

    // Score calculation
    // Closer our paladins are to opponent's unicorn -> better (subtract distance)
    // Closer opponent's paladins are to our unicorn -> worse (add distance)
    if (minMyDist != Integer.MAX_VALUE) {
      threatScore -= minMyDist * 10;
      threatScore -= sumMyDist * 2;
    }
    if (minOppDist != Integer.MAX_VALUE) {
      threatScore += minOppDist * 10;
      threatScore += sumOppDist * 2;
    }

    return threatScore;
  }
}
