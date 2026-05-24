package game;

import java.util.*;

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
    //TODO generate initial positions? look at what the engine expects
  }

  public char getPieceAt(int row, int col) {
    if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return EMPTY;
    return board[row][col];
  }


  // Play
  public void play(EscampeMove move, PlayerColor playerRole) {
    if (playerRole == null) return;

    // Pass, reset lastMove so the opponent can freely choose their piece
    if (move.getMove().equals("PASSE")) {
      lastMoveRow = -1;
      lastMoveCol = -1;
    } else if (move.getMove().contains("/")) { // Initial placement
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
        board[move.getToRow()][move.getToCol()] = board[move.getFromRow()][move.getFromCol()];
        board[move.getFromRow()][move.getFromCol()] = EMPTY;
        lastMoveRow = move.getToRow();
        lastMoveCol = move.getToCol();
      } catch (IllegalArgumentException e) {
        System.err.println("Invalid move format: " + move);
      }
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
      List<EscampeMove> placements = new ArrayList<>();
      if (playerRole == PlayerColor.WHITE) {
        // Recommend some solid placements from the opening book
        placements.add(new EscampeMove("C1/A1/B2/D2/E2/F1"));
        placements.add(new EscampeMove("C1/A2/B1/D2/E1/F2"));
        placements.add(new EscampeMove("C1/A1/B2/D2/F2/E2"));
      } else {
        // Black placements mirrored to rows 5-6
        placements.add(new EscampeMove("C6/A6/B5/D5/E5/F6"));
        placements.add(new EscampeMove("C6/A5/B6/D5/E6/F5"));
        placements.add(new EscampeMove("C6/A6/B5/D5/F5/E5"));
      }
      return placements;
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
      char piece = board[fr][fc];
      int dist = LISERET[fr][fc];

      // Try all destination cells: empty or enemy unicorn, reachable in `dist` steps
      //TODO optimize by only checking cells within manhattan distance `dist` from (fr,fc)
      for (int tr = 0; tr < SIZE; tr++) {
        for (int tc = 0; tc < SIZE; tc++) {
          if (tr == fr && tc == fc) continue;
          char target = board[tr][tc];
          if ((target == EMPTY || (isUnicorn(target) && !belongsTo(target, playerRole)))
              && canReach(fr, fc, tr, tc, dist, piece)) {
            moves.add(new EscampeMove(fr, fc, tr, tc));
          }
        }
      }
    }

    if (moves.isEmpty()) {
      moves.add(new EscampeMove("PASSE"));
    }
    return moves;
  }


  // DFS: can we reach (tr,tc) from (fr,fc) in exactly `dist` orthogonal steps,
  // without revisiting cells, without jumping over pieces (path must be clear except destination)
  private boolean canReach(int fr, int fc, int tr, int tc, int dist, char piece) {
    // Temporarily remove the moving piece so it doesn't block its own path
    char saved = board[fr][fc];
    board[fr][fc] = EMPTY;
    boolean result = dfsReach(fr, fc, tr, tc, dist, new boolean[SIZE][SIZE], piece);
    board[fr][fc] = saved;
    return result;
  }

  private boolean dfsReach(int r, int c, int tr, int tc, int steps, boolean[][] visited, char piece) {
    if (steps == 0) return r == tr && c == tc;
    // Pruning: Manhattan distance must be reachable in remaining steps with same parity
    int manhattan = Math.abs(tr - r) + Math.abs(tc - c);
    if (manhattan > steps || (steps - manhattan) % 2 != 0) return false;

    visited[r][c] = true;
    for (int[] dir : DIRECTIONS) {
      int nr = r + dir[0];
      int nc = c + dir[1];
      if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;
      if (visited[nr][nc]) continue;
      // Intermediate cells must be empty; destination can be empty or enemy unicorn
      if (steps > 1 && board[nr][nc] != EMPTY) continue;
      if (steps == 1 && board[nr][nc] != EMPTY && (!isUnicorn(board[nr][nc]) || sameColor(board[nr][nc], piece))) continue;

      if (dfsReach(nr, nc, tr, tc, steps - 1, visited, piece)) {
        visited[r][c] = false;
        return true;
      }
    }
    visited[r][c] = false;
    return false;
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

  private boolean sameColor(char a, char b) {
    boolean aWhite = (a == WHITE_UNICORN || a == WHITE_PALADIN);
    boolean bWhite = (b == WHITE_UNICORN || b == WHITE_PALADIN);
    return aWhite == bWhite;
  }

  private boolean hasPieces(PlayerColor pc) {
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        if (belongsTo(board[r][c], pc)) return true;
      }
    }
    return false;
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
