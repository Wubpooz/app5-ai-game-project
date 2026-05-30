package game;

import java.util.*;
import algorithms.Opening;

public class EscampeBoard implements interfaces.IBoard<EscampeMove, PlayerColor, EscampeBoard> {
  private static final int SIZE = 6;
  private static final char EMPTY = '-';
  private static final char BLACK_UNICORN = 'N';
  private static final char BLACK_PALADIN = 'n';
  private static final char WHITE_UNICORN = 'B';
  private static final char WHITE_PALADIN = 'b';

  // Liseret map (row 0 = line 01 bottom, col 0 = A)
  private static final int[][] LISERET = {
    {1, 2, 2, 3, 1, 2}, // row 01
    {3, 1, 3, 1, 3, 2}, // row 02
    {2, 3, 1, 2, 1, 3}, // row 03
    {2, 1, 3, 2, 3, 1}, // row 04
    {1, 3, 1, 3, 1, 2}, // row 05
    {3, 2, 2, 1, 3, 2}  // row 06
  };

  private static final int[][] DIRECTIONS = {
    { 1, 0 },
    { -1, 0 },
    { 0, 1 },
    { 0, -1 }
  };

  // Board: board[row][col], row 0 = line 01 (bottom), col 0 = A
  // '-' = empty, 'B'/'b' = white unicorn/paladin, 'N'/'n' = black unicorn/paladin
  private char[][] board = new char[SIZE][SIZE];

  // Track last move destination for liseret constraint
  private int lastMoveRow = -1;
  private int lastMoveCol = -1;

  // History state stack for undo Move
  private int[] historyLastMoveRow = new int[128];
  private int[] historyLastMoveCol = new int[128];
  private char[] historyCapturedPiece = new char[128];
  private int historySize = 0;

  private void ensureHistoryCapacity() {
    if (historySize >= historyLastMoveRow.length) {
      int newLen = historyLastMoveRow.length * 2;
      historyLastMoveRow = Arrays.copyOf(historyLastMoveRow, newLen);
      historyLastMoveCol = Arrays.copyOf(historyLastMoveCol, newLen);
      historyCapturedPiece = Arrays.copyOf(historyCapturedPiece, newLen);
    }
  }

  // Initialization
  public EscampeBoard() {
    initializeBoard();
  }

  public void initializeBoard() { 
    for (char[] r : board) {
      Arrays.fill(r, EMPTY);
    }
    lastMoveRow = -1;
    lastMoveCol = -1;
    historySize = 0;
    // The board starts empty. Initial positions are generated dynamically
    // during the placement phase (handled by possibleMoves and Opening).
  }

  // =========== Getters ===========
  public char getPieceAt(int row, int col) {
    if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return EMPTY;
    return board[row][col];
  }

  public char[][] getBoardState() {
    char[][] copy = new char[SIZE][SIZE];
    for (int r = 0; r < SIZE; r++) {
      System.arraycopy(board[r], 0, copy[r], 0, SIZE);
    }
    return copy;
  }

  public int getLastMoveRow() {
    return lastMoveRow;
  }

  public int getLastMoveCol() {
    return lastMoveCol;
  }

  public int getLiseretAt(int row, int col) {
    if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return -1;
    return LISERET[row][col];
  }



  // =========== Play ===========
  public void play(EscampeMove move, PlayerColor playerRole) {
    if (playerRole == null) return;

    ensureHistoryCapacity();
    historyLastMoveRow[historySize] = lastMoveRow;
    historyLastMoveCol[historySize] = lastMoveCol;

    // Pass, reset lastMove so the opponent can freely choose their piece
    if (move.getMove().equals("E")) {
      historyCapturedPiece[historySize] = EMPTY;
      historySize++;
      lastMoveRow = -1;
      lastMoveCol = -1;
    } else if (move.getMove().contains("/")) { // Initial placement
      historyCapturedPiece[historySize] = EMPTY;
      historySize++;
      String[] parts = move.getMove().split("/");
      char unicorn = (playerRole == PlayerColor.BLACK) ? BLACK_UNICORN : WHITE_UNICORN;
      char paladin = (playerRole == PlayerColor.BLACK) ? BLACK_PALADIN : WHITE_PALADIN;
      int colU = Character.toUpperCase(parts[0].charAt(0)) - 'A';
      int rowU = parts[0].charAt(1) - '1';
      board[rowU][colU] = unicorn;
      int colP = -1;
      int rowP = -1;
      for (int i = 1; i < parts.length; i++) {
        colP = Character.toUpperCase(parts[i].charAt(0)) - 'A';
        rowP = parts[i].charAt(1) - '1';        
        board[rowP][colP] = paladin;
      }
      lastMoveRow = -1;
      lastMoveCol = -1;
    } else { // Normal move
      try {
        historyCapturedPiece[historySize] = board[move.getToRow()][move.getToCol()];
        historySize++;
        board[move.getToRow()][move.getToCol()] = board[move.getFromRow()][move.getFromCol()];
        board[move.getFromRow()][move.getFromCol()] = EMPTY;
        lastMoveRow = move.getToRow();
        lastMoveCol = move.getToCol();
      } catch (IllegalArgumentException e) {
        System.err.println("Invalid move format: " + move);
      }
    }
  }

  @Override
  public void undo(EscampeMove move, PlayerColor playerRole) {
    if (playerRole == null || historySize == 0) return;

    historySize--;
    lastMoveRow = historyLastMoveRow[historySize];
    lastMoveCol = historyLastMoveCol[historySize];
    char captured = historyCapturedPiece[historySize];

    if (move.getMove().equals("E")) {
      // Pass move: only lastMoveRow/lastMoveCol are restored, which we already did
    } else if (move.getMove().contains("/")) { // Initial placement
      // Remove all placed pieces
      String[] parts = move.getMove().split("/");
      for (String part : parts) {
        int col = Character.toUpperCase(part.charAt(0)) - 'A';
        int row = part.charAt(1) - '1';
        board[row][col] = EMPTY;
      }
    } else { // Normal move
      board[move.getFromRow()][move.getFromCol()] = board[move.getToRow()][move.getToCol()];
      board[move.getToRow()][move.getToCol()] = captured;
    }
  }


  // Game over: one player has no unicorn left
  @Override
  public boolean isGameOver() {
    if (!hasPieces(PlayerColor.WHITE) || !hasPieces(PlayerColor.BLACK)) {
      return false;
    }
    boolean whiteUnicorn = false;
    boolean blackUnicorn = false;
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        if (board[r][c] == WHITE_UNICORN) whiteUnicorn = true;
        if (board[r][c] == BLACK_UNICORN) blackUnicorn = true;
      }
    }
    return !whiteUnicorn || !blackUnicorn;
  }



  // =========== Possible moves ===========
  @Override
  public List<EscampeMove> possibleMoves(PlayerColor playerRole) {
    if (isGameOver()) return new ArrayList<>();
    if (playerRole == null) return new ArrayList<>();

    // Placement phase check
    if (!hasPieces(playerRole)) {
      boolean isWhite = (playerRole == PlayerColor.WHITE);
      return Opening.getOpeningsMoves(isWhite);
    }

    List<EscampeMove> moves = new ArrayList<>();
    List<int[]> pieces = getPlayerPieces(playerRole);

    // Determine required liseret (from opponent's last move)
    int requiredLiseret = -1;
    if (lastMoveRow >= 0) {
      requiredLiseret = LISERET[lastMoveRow][lastMoveCol];
    }

    // Filter pieces that can move (liseret constraint)
    List<int[]> movable = new ArrayList<>();
    if (requiredLiseret < 0) {
      movable.addAll(pieces); // no constraint, all pieces can move
    } else {
      for (int[] p : pieces) {
        if (LISERET[p[0]][p[1]] == requiredLiseret) {
          movable.add(p);
        }
      }
    }

    for (int[] p : movable) {
      int fr = p[0];
      int fc = p[1];
      int dist = LISERET[fr][fc];

      long mask = getReachableMask(fr, fc, dist, playerRole); // bitboard mask
      while (mask != 0) {
        int bitIndex = Long.numberOfTrailingZeros(mask);
        int tr = bitIndex / SIZE;
        int tc = bitIndex % SIZE;
        moves.add(new EscampeMove(fr, fc, tr, tc));
        mask &= mask - 1; // clear lowest set bit
      }
    }

    if (moves.isEmpty()) {
      moves.add(new EscampeMove("E"));
    }
    return moves;
  }

  // Generate all reachable cells in exactly `dist` orthogonal steps
  // returning a 64-bit mask of (r * SIZE + c) coordinates.
  private long getReachableMask(int fr, int fc, int dist, PlayerColor playerRole) {
    long visited = 1L << (fr * SIZE + fc); // mark starting cell as visited
    return findReachableCells(fr, fc, dist, visited, playerRole, isUnicorn(board[fr][fc]));
  }

  /**
   * Recursive DFS to find reachable cells at exact distance, respecting move rules.
   * @param r row of current cell
   * @param c col of current cell
   * @param steps remaining steps to reach destination
   * @param visited bitmask of visited cells to prevent cycles
   * @param playerRole current player's role for move legality checks
   * @return bitmask of reachable destination cells at exact distance
   */
  private long findReachableCells(int r, int c, int steps, long visited, PlayerColor playerRole, boolean pieceIsUnicorn) {
    if (steps == 0) { // base case: check if current cell is a valid destination
      char target = board[r][c];
      // valid destination if target is empty or can be captured (is a unicorn while we're not a unicorn and belongs to the opponent)
      if (target == EMPTY || (!pieceIsUnicorn && isUnicorn(target) && !belongsTo(target, playerRole))) {
        return 1L << (r * SIZE + c); // valid destination
      }
      return 0L;
    }

    long mask = 0L;
    for (int[] dir : DIRECTIONS) {
      int nr = r + dir[0];
      int nc = c + dir[1];
      if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue; // out of bounds
      long bit = 1L << (nr * SIZE + nc);
      if ((visited & bit) != 0) continue; // already visited

      if (steps > 1) {
        if (board[nr][nc] != EMPTY) continue; // Intermediate cells must be empty
      } else {
        // Final step destination check
        char target = board[nr][nc];
        // continue if target is not empty and cannot be captured (belongs to player or is not a unicorn)
        if (target != EMPTY && (!isUnicorn(target) || belongsTo(target, playerRole))) {
          continue;
        }
      }

      mask |= findReachableCells(nr, nc, steps - 1, visited | bit, playerRole, pieceIsUnicorn); // mark cell as visited with an OR operation
    }
    return mask;
  }


  //  Helper Unicorn move generator
  public List<EscampeMove> possibleMovesForUnicorn(PlayerColor playerRole) {
    if (isGameOver()) return new ArrayList<>();
    if (playerRole == null) return new ArrayList<>();

    // Placement phase check
    if (!hasPieces(playerRole)) {
      boolean isWhite = (playerRole == PlayerColor.WHITE);
      return Opening.getOpeningsMoves(isWhite);
    }

    List<EscampeMove> moves = new ArrayList<>();
    int[] unicornPos = getUnicornPosition(playerRole);
    if (unicornPos == null || unicornPos[0] == -1 || unicornPos[1] == -1) return moves; // should not happen if game is not over


    // Determine required liseret (from opponent's last move)
    int requiredLiseret = -1;
    if (lastMoveRow >= 0) {
      requiredLiseret = LISERET[lastMoveRow][lastMoveCol];
    }


    if (requiredLiseret >= 0 && LISERET[unicornPos[0]][unicornPos[1]] != requiredLiseret) {
      return moves; // unicorn cannot move due to liseret constraint
    }

    int fr = unicornPos[0];
    int fc = unicornPos[1];
    int dist = LISERET[fr][fc];

    long mask = getReachableMask(fr, fc, dist, playerRole); // bitboard mask
    while (mask != 0) {
      int bitIndex = Long.numberOfTrailingZeros(mask);
      int tr = bitIndex / SIZE;
      int tc = bitIndex % SIZE;
      moves.add(new EscampeMove(fr, fc, tr, tc));
      mask &= mask - 1; // clear lowest set bit
    }

    if (moves.isEmpty()) {
      moves.add(new EscampeMove("E"));
    }
    return moves;
}



  /**
   * Returns the number of legal moves for the given player without allocating any List or EscampeMove.
   * Use this in the heuristic instead of possibleMoves().size() to avoid object allocation on every leaf node.
   * Returns 1 when the player must pass (the pass move "E" counts as one move), 0 on game over.
   */
  public int countPossibleMoves(PlayerColor playerRole) {
    if (isGameOver()) return 0;
    if (playerRole == null) return 0;

    // Placement phase: count opening book entries
    if (!hasPieces(playerRole)) {
      return (playerRole == PlayerColor.WHITE) ? Opening.WHITE_OPENINGS.length : Opening.BLACK_OPENINGS.length;
    }

    int requiredLiseret = (lastMoveRow >= 0) ? LISERET[lastMoveRow][lastMoveCol] : -1;
    int count = 0;

    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        char piece = board[r][c];
        if (!belongsTo(piece, playerRole)) continue;
        if (requiredLiseret >= 0 && LISERET[r][c] != requiredLiseret) continue;
        count += Long.bitCount(getReachableMask(r, c, LISERET[r][c], playerRole));
      }
    }

    return (count == 0) ? 1 : count; // 1 represents the forced pass move
  }

  /**
   * Returns the number of legal moves for the unicorn of the given player without allocating any objects.
   * Use this in the heuristic instead of possibleMovesForUnicorn().size().
   * Returns 1 when the unicorn must pass (counts as one move), 0 on game over.
   */
  public int countPossibleMovesForUnicorn(PlayerColor playerRole) {
    if (isGameOver()) return 0;
    if (playerRole == null) return 0;

    // Placement phase
    if (!hasPieces(playerRole)) {
      return (playerRole == PlayerColor.WHITE) ? Opening.WHITE_OPENINGS.length : Opening.BLACK_OPENINGS.length;
    }

    int[] unicornPos = getUnicornPosition(playerRole);
    if (unicornPos[0] == -1) return 0;

    int requiredLiseret = (lastMoveRow >= 0) ? LISERET[lastMoveRow][lastMoveCol] : -1;
    if (requiredLiseret >= 0 && LISERET[unicornPos[0]][unicornPos[1]] != requiredLiseret) {
      return 0; // unicorn cannot move due to liseret constraint
    }

    int count = Long.bitCount(getReachableMask(unicornPos[0], unicornPos[1], LISERET[unicornPos[0]][unicornPos[1]], playerRole));
    return (count == 0) ? 1 : count; // 1 represents the forced pass move
  }



  // =========== Utils ===========
  private boolean belongsTo(char piece, PlayerColor pc) {
    if (pc == PlayerColor.WHITE) return piece == WHITE_UNICORN || piece == WHITE_PALADIN;
    if (pc == PlayerColor.BLACK) return piece == BLACK_UNICORN || piece == BLACK_PALADIN;
    return false;
  }

  private boolean isUnicorn(char piece) {
    return piece == WHITE_UNICORN || piece == BLACK_UNICORN;
  }

  private boolean hasPieces(PlayerColor pc) {
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        if (belongsTo(board[r][c], pc)) return true;
      }
    }
    return false;
  }

  public int[] getUnicornPosition(PlayerColor pc) {
    char unicornChar = (pc == PlayerColor.WHITE) ? WHITE_UNICORN : BLACK_UNICORN;
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        if (board[r][c] == unicornChar) {
          return new int[]{r, c};
        }
      }
    }
    return new int[]{-1, -1}; // should not happen if game is not over
  }

  private List<int[]> getPlayerPieces(PlayerColor pc) {
    List<int[]> pieces = new ArrayList<>();
    for (int r = 0; r < SIZE; r++)
      for (int c = 0; c < SIZE; c++)
        if (belongsTo(board[r][c], pc)) {
          pieces.add(new int[]{r, c});
        }
    return pieces;
  }

  @Override
  public EscampeBoard copy() {
    EscampeBoard newBoard = new EscampeBoard();
    for (int r = 0; r < SIZE; r++) {
      System.arraycopy(this.board[r], 0, newBoard.board[r], 0, SIZE);
    }
    newBoard.lastMoveRow = this.lastMoveRow;
    newBoard.lastMoveCol = this.lastMoveCol;
    return newBoard;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("  A B C D E F\n");
    for (int r = SIZE - 1; r >= 0; r--) {
      sb.append((r + 1)).append(" ");
      for (int c = 0; c < SIZE; c++) sb.append(board[r][c]).append(" ");
      sb.append("\n");
    }
    if (lastMoveRow >= 0)
      sb.append("  (last move landed on ").append("(" + lastMoveRow + "," + lastMoveCol + ")")
        .append(", liseret=").append(LISERET[lastMoveRow][lastMoveCol]).append(")\n");
    return sb.toString();
  }
}
