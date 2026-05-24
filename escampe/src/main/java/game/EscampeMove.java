package game;

import interfaces.IMove;

public class EscampeMove implements IMove {
  private int fromRow, fromCol, toRow, toCol;
  private String move; // format "A1-B2" or "PASSE" or "C6/A6/B5/D5/E6/F5"
  // private final int moveType; // 0 = normal move, 1 = placement, 2 = pass

  public EscampeMove(String move) {
    this.move = move.trim();
    parseMove(this.move);
  }

  public EscampeMove(int fromRow, int fromCol, int toRow, int toCol) {
    this.fromRow = fromRow;
    this.fromCol = fromCol;
    this.toRow = toRow;
    this.toCol = toCol;
    this.move = moveToString();
  }
  

  // Getters
  public String getMove() {
    return move;
  }

  public int getFromRow() {
    return fromRow;
  }

  public int getFromCol() {
    return fromCol;
  }

  public int getToRow() {
    return toRow;
  }

  public int getToCol() {
    return toCol;
  }



  @Override
  public String toString() {
    return move;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EscampeMove that = (EscampeMove) o;
    return move.equals(that.move);
  }

  @Override
  public int hashCode() {
    return move.hashCode();
  }



  // Helper methods
  /**
   * Parse a move string in the format "A1-B2" and set the corresponding fields.
   * @param move
   */
  public void parseMove(String move) {
    String m = move.trim();

    if (m.equals("PASSE") || m.contains("/")) {
      this.fromRow = this.fromCol = this.toRow = this.toCol = -1; // Indicate a pass or placement
      return;
    }
    String[] parts = m.split("-");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid move format: " + m);
    }
    String from = parts[0];
    String to = parts[1];
    this.fromCol = from.charAt(0) - 'A'; // Convert 'A' to 0, 'B' to 1, etc.
    this.fromRow = Integer.parseInt(from.substring(1)) - 1; // Convert "1" to 0, "2" to 1, etc.
    this.toCol = to.charAt(0) - 'A';
    this.toRow = Integer.parseInt(to.substring(1)) - 1;
  }

  public String moveToString() {
    char cFromCol = (char) ('A' + this.fromCol);
    char cFromRow = (char) ('1' + this.fromRow);
    char cToCol = (char) ('A' + this.toCol);
    char cToRow = (char) ('1' + this.toRow);    
    return "" + cFromCol + cFromRow + "-" + cToCol + cToRow;
  }



  //TODO move validation logic here? Or keep it in EscampeBoard?
  // Static move validation
  // public static boolean isValidMove(EscampeMove move, PlayerColor playerRole) {
  //   if (playerRole == null || move == null) return false;

  //   // Pass move
  //   if (move.getMove().equals("PASSE")) {
  //     return true; // Pass is always valid
  //   }

  //   // Initial placement: "C6/A6/B5/D5/E6/F5"
  //   if (move.getMove().contains("/")) {
  //     return isValidPlacement(move, playerRole);
  //   }

  //   // Normal move: "B1-D1"
  //   if (move.getMove().contains("-")) {
  //     return isValidNormalMove(move, playerRole);
  //   }

  //   return false;
  // }

  //   private boolean isValidNormalMove(EscampeMove move, PlayerColor playerRole) {
  //   if (!isPlacementDone()) return false;
  //   EscampeMove m = new EscampeMove(move.getMove());

  //   char piece = board[m.getFromRow()][m.getFromCol()];
  //   if (!belongsTo(piece, playerRole)) return false;

  //   // Liseret constraint: piece must start from a cell whose liseret matches
  //   // the opponent's last destination liseret
  //   if (lastMoveRow >= 0) {
  //     int requiredLiseret = LISERET[lastMoveRow][lastMoveCol];
  //     if (LISERET[m.getFromRow()][m.getFromCol()] != requiredLiseret) return false;
  //   }

  //   // Destination must be empty or enemy unicorn (only capture allowed)
  //   char target = board[m.getToRow()][m.getToCol()];
  //   if (target != EMPTY && belongsTo(target, pc)) return false;
  //   if (target != EMPTY && !isUnicorn(target)) return false;

  //   int dist = LISERET[m.getFromRow()][m.getFromCol()];
  //   return canReach(m.getFromRow(), m.getFromCol(), m.getToRow(), m.getToCol(), dist, piece);
  // }


    
  // private boolean isValidPlacement(String move, PlayerColor pc) {
  //   // Can't place if already placed or white didn't place before black
  //   if (pc == PlayerColor.WHITE && whitePlaced) return false;
  //   if (pc == PlayerColor.BLACK && blackPlaced) return false;
  //   if (pc == PlayerColor.WHITE && blackPlaced) return false;
  //   if (pc == PlayerColor.BLACK && !whitePlaced) return false;

  //   String[] parts = move.split("/");
  //   if (parts.length != 6) return false;

  //   int[][] coords = new int[SIZE][2];
  //   Set<String> seen = new HashSet<>();
  //   for (int i = 0; i < SIZE; i++) {
  //     coords[i] = parseCell(parts[i]);
  //     if (coords[i] == null) return false;
  //     int r = coords[i][0];
  //     int c = coords[i][1];
  //     if (!seen.add(r + "," + c)) return false; // duplicate
  //     if (board[r][c] != EMPTY) return false; // occupied
  //     // White places on rows 01-02 (indices 0-1), Black on rows 05-06 (indices 4-5)
  //     if (pc == PlayerColor.WHITE && r > 1) return false;
  //     if (pc == PlayerColor.BLACK && r < 4) return false;
  //   }
  //   return true;
  // }
}
