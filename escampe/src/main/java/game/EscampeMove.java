package game;

import interfaces.IMove;

public class EscampeMove implements IMove {
  private int fromRow, fromCol, toRow, toCol;
  private String move; // format "A1-B2" or "PASSE"

  public EscampeMove(String move) {
    this.move = move;
    parseMove(move);
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
    if (move.equals("PASSE")) {
      this.fromRow = this.fromCol = this.toRow = this.toCol = -1; // Indicate a pass
      return;
    }
    String[] parts = move.split("-");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid move format: " + move);
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
}
