package game;

import java.io.*;
import java.util.*;

public class EscampeBoard {

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

  
  // Board: board[row][col], row 0 = line 01 (bottom), col 0 = A, '-' = empty, 'B'/'b' = white unicorn/paladin, 'N'/'n' = black unicorn/paladin
  private char[][] board = new char[SIZE][SIZE];


  // Track which player has placed pieces (initial placement phase)
  private boolean whitePlaced = false;
  private boolean blackPlaced = false;
  // Track last move destination for liseret constraint
  private int lastMoveRow = -1;
  private int lastMoveCol = -1;

  public EscampeBoard() {
    for (char[] r : board) Arrays.fill(r, '-');
  }

  // =========== File I/O ===========
  public void setFromFile(String fileName) {
    // Reset all state before loading
    for (char[] r : board) Arrays.fill(r, '-');
    lastMoveRow = -1;
    lastMoveCol = -1;
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;
      int rowIdx = 0;
      while ((line = br.readLine()) != null && rowIdx < SIZE) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("%")) continue;
        // Format: "01 nnN--- 01" or "01 -bBb-- 01"
        String[] parts = line.split("\\s+");
        if (parts.length < 2) continue;
        String boardPart = parts[1];
        if (boardPart.length() != SIZE) continue;
        for (int c = 0; c < SIZE; c++) {
          board[rowIdx][c] = boardPart.charAt(c);
        }
        rowIdx++;
      }
      // Infer placement state from board content
      whitePlaced = hasPieces(PlayerColor.WHITE);
      blackPlaced = hasPieces(PlayerColor.BLACK);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void saveToFile(String fileName) {
    try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
      pw.println("% ABCDEF");
      for (int r = 0; r < SIZE; r++) {
        String rowNum = String.format("%02d", r + 1);
        pw.println(rowNum + " " + new String(board[r]) + " " + rowNum); // board positions
      }
      pw.println("% ABCDEF");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // =========== Validation ===========
  public boolean isValidMove(String move, String player) {
    if (gameOver()) return false;
    PlayerColor pc = parsePlayer(player);
    if (pc == null || move == null) return false;
    move = move.trim();

    // Pass move
    if (move.equals("E")) {
      if (!isPlacementDone()) return false;
      String[] pm = possiblesMoves(player);
      return pm.length == 1 && pm[0].equals("E");
    }

    // Initial placement: "C6/A6/B5/D5/E6/F5"
    if (move.contains("/")) {
      return isValidPlacement(move, pc);
    }

    // Normal move: "B1-D1"
    if (move.contains("-")) {
      return isValidNormalMove(move, pc);
    }

    return false;
  }

  private boolean isValidPlacement(String move, PlayerColor pc) {
    // Can't place if already placed or white didn't place before black
    if (pc == PlayerColor.WHITE && whitePlaced) return false;
    if (pc == PlayerColor.BLACK && blackPlaced) return false;
    if (pc == PlayerColor.WHITE && blackPlaced) return false;
    if (pc == PlayerColor.BLACK && !whitePlaced) return false;
    
    String[] parts = move.split("/");
    if (parts.length != 6) return false;
    
    int[][] coords = new int[SIZE][2];
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < SIZE; i++) {
      coords[i] = parseCell(parts[i]);
      if (coords[i] == null) return false;
      int r = coords[i][0];
      int c = coords[i][1];
      if (!seen.add(r + "," + c)) return false; // duplicate
      if (board[r][c] != '-') return false; // occupied
      // White places on rows 01-02 (indices 0-1), Black on rows 05-06 (indices 4-5)
      if (pc == PlayerColor.WHITE && r > 1) return false;
      if (pc == PlayerColor.BLACK && r < 4) return false;
    }
    return true;
  }

  private boolean isValidNormalMove(String move, PlayerColor pc) {
    if (!isPlacementDone()) return false;
    String[] parts = move.split("-");
    if (parts.length != 2) return false; // must be exactly one '-' to fit "from-to" format

    int[] from = parseCell(parts[0]);
    int[] to = parseCell(parts[1]);
    if (from == null || to == null) return false;

    // from row/col, to row/col
    int fr = from[0];
    int  fc = from[1];
    int  tr = to[0];
    int  tc = to[1];
    if (fr == tr && fc == tc) return false;

    char piece = board[fr][fc];
    if (!belongsTo(piece, pc)) return false;

    // Liseret constraint: if opponent just moved, the piece we move must start
    // from a cell whose liseret matches the opponent's destination liseret
    if (lastMoveRow >= 0) {
      int requiredLiseret = LISERET[lastMoveRow][lastMoveCol];
      if (LISERET[fr][fc] != requiredLiseret) return false;
    }

    // Destination must be empty or enemy piece (capture, only for unicorn target by reaching it)
    char target = board[tr][tc];
    if (target != '-' && belongsTo(target, pc)) return false;
    // Only unicorn can be captured (landing on enemy unicorn)
    if (target != '-' && !isUnicorn(target)) return false;

    int dist = LISERET[fr][fc];
    return canReach(fr, fc, tr, tc, dist, piece);
  }

  // BFS/DFS: can we reach (tr,tc) from (fr,fc) in exactly `dist` orthogonal steps,
  // without revisiting cells, without jumping over pieces (path must be clear except destination)
  private boolean canReach(int fr, int fc, int tr, int tc, int dist, char piece) {
    // Temporarily remove the moving piece so it doesn't block its own path
    char saved = board[fr][fc];
    board[fr][fc] = '-';
    boolean result = dfsReach(fr, fc, tr, tc, dist, new boolean[SIZE][SIZE], piece);
    board[fr][fc] = saved;
    return result;
  }

  private boolean dfsReach(int r, int c, int tr, int tc, int steps, boolean[][] visited, char piece) {
    if (steps == 0) return r == tr && c == tc;
    visited[r][c] = true;
    for (int d = 0; d < DIRECTIONS.length; d++) {
      int nr = r + DIRECTIONS[d][0];
      int nc = c + DIRECTIONS[d][1];
      if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;
      if (visited[nr][nc]) continue;
      // Intermediate cells must be empty; destination can be empty or enemy unicorn
      if (steps > 1 && board[nr][nc] != EMPTY) continue;
      // Can only land on enemy unicorn and can't jump pieces
      if (steps == 1 && board[nr][nc] != '-' && (!isUnicorn(board[nr][nc]) || sameColor(board[nr][nc], piece))) continue;
      
      if (dfsReach(nr, nc, tr, tc, steps - 1, visited, piece)) {
        visited[r][c] = false;
        return true;
      }
    }
    visited[r][c] = false;
    return false;
  }

  // =========== Possible moves ===========
  public String[] possiblesMoves(String player) {
    if (gameOver()) return new String[0];
    PlayerColor pc = parsePlayer(player);
    if (pc == null) return new String[0];

    // Placement phase
    if (!isPlacementDone()) {
      if (pc == PlayerColor.WHITE && !whitePlaced) return generatePlacements(pc, 0, 1);
      if (pc == PlayerColor.BLACK && !blackPlaced && whitePlaced) return generatePlacements(pc, 4, 5);
      return new String[0];
    }

    // Normal move phase
    List<String> moves = new ArrayList<>();
    List<int[]> pieces = getPlayerPieces(pc);

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
      // Try all destination cells, retaining only those that are empty or occupied by an enemy unicorn, and reachable in `dist` steps
      for (int tr = 0; tr < 6; tr++) {
        for (int tc = 0; tc < 6; tc++) {
          if (tr == fr && tc == fc) continue;
          char target = board[tr][tc];
          if (target != EMPTY && belongsTo(target, pc)) continue;
          if (target != EMPTY && !isUnicorn(target)) continue;
          if (canReach(fr, fc, tr, tc, dist, piece)) {
            moves.add(cellToString(fr, fc) + "-" + cellToString(tr, tc));
          }
        }
      }
    }

    if (moves.isEmpty()) {
      moves.add("E");
    }
    return moves.toArray(new String[0]);
  }

  //TODO is this really usefull ??
  private String[] generatePlacements(PlayerColor pc, int minRow, int maxRow) {
    // Collect available cells in the player's two rows
    List<int[]> cells = new ArrayList<>();
    for (int r = minRow; r <= maxRow; r++) {
      for (int c = 0; c < SIZE; c++) {
        if (board[r][c] == EMPTY) cells.add(new int[]{r, c});
      }
    }
    if (cells.size() < 6) return new String[0];

    // Generate all C(n,6) combinations, first cell = unicorn, rest = paladins
    List<String> result = new ArrayList<>();
    int n = cells.size();
    int[] indices = new int[6];
    generateCombinations(cells, indices, 0, 0, n, result);
    return result.toArray(new String[0]);
  }

  private void generateCombinations(List<int[]> cells, int[] indices, int start, int depth, int n, List<String> result) {
    if (depth == 6) {
      // indices[0] = unicorn position, indices[1..5] = paladin positions
      // But any of the 6 could be the unicorn - actually the format is:
      // first cell is unicorn, remaining 5 are paladins
      // We need to try each of the 6 chosen cells as the unicorn
      for (int u = 0; u < SIZE; u++) {
        StringBuilder sb = new StringBuilder();
        sb.append(cellToString(cells.get(indices[u])[0], cells.get(indices[u])[1]));
        for (int j = 0; j < SIZE; j++) {
          if (j == u) continue;
          sb.append('/').append(cellToString(cells.get(indices[j])[0], cells.get(indices[j])[1]));
        }
        result.add(sb.toString());
      }
      return;
    }
    for (int i = start; i < n; i++) {
      indices[depth] = i;
      generateCombinations(cells, indices, i + 1, depth + 1, n, result);
    }
  }

  // =========== Play ===========
  public void play(String move, String player) {
    PlayerColor pc = parsePlayer(player);
    if (pc == null) return;
    move = move.trim();

    // Pass - no board change, but don't reset lastMove since the opponent didn't actually move to a new cell (the constraint resets)
    if (move.equals("E")) {
      lastMoveRow = -1;
      lastMoveCol = -1;
      return;
    }

    // Initial placement
    if (move.contains("/")) {
      String[] parts = move.split("/");
      char unicorn = (pc == PlayerColor.BLACK) ? BLACK_UNICORN : WHITE_UNICORN;
      char paladin = (pc == PlayerColor.BLACK) ? BLACK_PALADIN : WHITE_PALADIN;
      int[] uc = parseCell(parts[0]);
      if (uc != null) {
        board[uc[0]][uc[1]] = unicorn;
        for (int i = 1; i < parts.length; i++) {
          int[] cell = parseCell(parts[i]);
          if (cell != null) {
            board[cell[0]][cell[1]] = paladin;
          }
        }
        if (pc == PlayerColor.WHITE) whitePlaced = true;
        else blackPlaced = true;
      } else {
        // TODO Invalid placement format, do nothing
        System.out.println("Invalid placement format: " + move);
      }
      lastMoveRow = -1;
      lastMoveCol = -1;
      return;
    }

    // Normal move
    String[] parts = move.split("-");
    int[] from = parseCell(parts[0]);
    int[] to = parseCell(parts[1]);
    if (from != null && to != null) {
      board[to[0]][to[1]] = board[from[0]][from[1]];
      board[from[0]][from[1]] = EMPTY;
      lastMoveRow = to[0];
      lastMoveCol = to[1];
    } else {
      // TODO Invalid move format, do nothing
      System.out.println("Invalid move format: " + move);
    }
  }

  // =========== Game Over ===========
  public boolean gameOver() {
    if (!isPlacementDone()) return false;
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

  // =========== Utils ===========
  private PlayerColor parsePlayer(String player) {
    if (player == null) return null;
    String normalized = player.trim().toLowerCase(Locale.ROOT);
    if (normalized.equals("noir") || normalized.equals("black")) return PlayerColor.BLACK;
    if (normalized.equals("blanc") || normalized.equals("white")) return PlayerColor.WHITE;
    return null;
  }

  private int[] parseCell(String cell) {
    if (cell == null || cell.length() != 2) return null;
    int col = cell.charAt(0) - 'A';
    int row = cell.charAt(1) - '1';
    if (col < 0 || col >= SIZE || row < 0 || row >= SIZE) return null;
    return new int[]{row, col};
  }

  private String cellToString(int row, int col) {
    return "" + (char)('A' + col) + (char)('1' + row);
  }

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

  private boolean isPlacementDone() {
    return whitePlaced && blackPlaced;
  }

  private boolean hasPieces(PlayerColor pc) {
    for (int r = 0; r < SIZE; r++)
      for (int c = 0; c < SIZE; c++)
        if (belongsTo(board[r][c], pc)) return true;
    return false;
  }

  private List<int[]> getPlayerPieces(PlayerColor pc) {
    List<int[]> pieces = new ArrayList<>();
    for (int r = 0; r < SIZE; r++)
      for (int c = 0; c < SIZE; c++)
        if (belongsTo(board[r][c], pc)) pieces.add(new int[]{r, c});
    return pieces;
  }

  // =========== Main ===========
  public static void main(String[] args) {
    EscampeBoard b = new EscampeBoard();

    // 1. Initial placement
    System.out.println("=== Placement phase ===");
    String whitePlace = "C1/A1/B2/D2/E1/F2";
    System.out.println("White placement valid: " + b.isValidMove(whitePlace, "blanc"));
    b.play(whitePlace, "blanc");

    String blackPlace = "C6/A6/B5/D5/E6/F5";
    System.out.println("Black placement valid: " + b.isValidMove(blackPlace, "noir"));
    b.play(blackPlace, "noir");

    b.saveToFile("test_board.txt");
    System.out.println("Board saved.");

    // 2. Normal moves
    System.out.println("\n=== Normal moves ===");
    String[] whiteMoves = b.possiblesMoves("blanc");
    System.out.println("White possible moves: " + whiteMoves.length);
    for (String m : whiteMoves) System.out.println("  " + m);

    if (whiteMoves.length > 0) {
      String mv = whiteMoves[0];
      System.out.println("Playing: " + mv);
      b.play(mv, "blanc");
    }

    System.out.println("Game over: " + b.gameOver());

    // 3. File I/O round-trip
    System.out.println("\n=== File I/O ===");
    b.saveToFile("escampe\\examples\\test_board.txt");
    EscampeBoard b2 = new EscampeBoard();
    b2.setFromFile("escampe\\examples\\test_board2.txt");
    System.out.println("Loaded board, game over: " + b2.gameOver());
  }
}
