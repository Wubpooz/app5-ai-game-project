package algorithms.evaluation;

import game.EscampeBoard;
import game.PlayerColor;
import interfaces.IHeuristic;

public class Heuristic implements IHeuristic<EscampeBoard, PlayerColor> {
  private static final int SIZE = 6;
  private static final char WHITE_UNICORN = 'B';
  private static final char WHITE_PALADIN = 'b';
  private static final char BLACK_UNICORN = 'N';
  private static final char BLACK_PALADIN = 'n';

  @Override
  public int eval(EscampeBoard board, PlayerColor role) {
    int myUnicornRow = -1, myUnicornCol = -1;
    int oppUnicornRow = -1, oppUnicornCol = -1;

    char myUnicornChar = (role == PlayerColor.WHITE) ? WHITE_UNICORN : BLACK_UNICORN;
    char oppUnicornChar = (role == PlayerColor.WHITE) ? BLACK_UNICORN : WHITE_UNICORN;
    char myPaladinChar = (role == PlayerColor.WHITE) ? WHITE_PALADIN : BLACK_PALADIN;
    char oppPaladinChar = (role == PlayerColor.WHITE) ? BLACK_PALADIN : WHITE_PALADIN;

    // Locate unicorns and paladins
    int myPaladinsCount = 0;
    int oppPaladinsCount = 0;

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
          myPaladinsCount++;
        } else if (piece == oppPaladinChar) {
          oppPaladinsCount++;
        }
      }
    }

    // Win/loss states
    if (myUnicornRow == -1) return -100000;
    if (oppUnicornRow == -1) return 100000;

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
