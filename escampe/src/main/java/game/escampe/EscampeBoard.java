package game.escampe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Board model for the Escampe game.
 * Supported move formats:
 * - Regular move: {@code B1-D1}
 * - Initial placement: {@code C6/A6/B5/D5/E6/F5}
 * - Pass: {@code E}
 */
public class EscampeBoard {

  private static final int SIZE = 6;
  private static final int FREE_BAND = -1;

  private static final char EMPTY = '-';
  private static final char BLACK_UNICORN = 'N';
  private static final char BLACK_PALADIN = 'n';
  private static final char WHITE_UNICORN = 'B';
  private static final char WHITE_PALADIN = 'b';

  private static final Pattern FILE_ROW_PATTERN = Pattern.compile("^(\\d{1,2})\\s+([NnBb-]{6})\\s+(\\d{1,2})$");
  private static final Pattern REGULAR_MOVE_PATTERN = Pattern.compile("^([A-Fa-f][1-6])\\s*-\\s*([A-Fa-f][1-6])$");
  private static final Pattern COORD_PATTERN = Pattern.compile("^([A-Fa-f])([1-6])$");

  // Band map indexed by [row-1][column], with row 1 at the bottom.
  private static final int[][] BAND_MAP = {
    { 1, 2, 2, 3, 1, 2 }, // row 1
    { 3, 1, 3, 1, 3, 2 }, // row 2
    { 2, 3, 1, 2, 1, 3 }, // row 3
    { 2, 1, 3, 2, 3, 1 }, // row 4
    { 1, 3, 1, 3, 1, 2 }, // row 5
    { 3, 2, 2, 1, 3, 2 }  // row 6
  };

  private static final int[][] DIRECTIONS = {
    { 1, 0 },
    { -1, 0 },
    { 0, 1 },
    { 0, -1 }
  };

  private final char[][] board;

  // Required starting band for each player on their next turn.
  // FREE_BAND means no constraint (typically after a pass or initial state).
  private int requiredBandForBlack;
  private int requiredBandForWhite;

  private boolean blackPlaced;
  private boolean whitePlaced;

  private enum PlayerColor {
    BLACK,
    WHITE
  }

  private record Position(int row, int col) {}

  public EscampeBoard() {
    this.board = new char[SIZE][SIZE];
    clearBoard();
    requiredBandForBlack = FREE_BAND;
    requiredBandForWhite = FREE_BAND;
    blackPlaced = false;
    whitePlaced = false;
  }

  /**
   * Initialise un plateau a partir d'un fichier texte.
   *
   * @param fileName le nom du fichier a lire
   */
  public void setFromFile(String fileName) {
    clearBoard();

    Integer metaNextBlack = null;
    Integer metaNextWhite = null;
    Boolean metaPlacedBlack = null;
    Boolean metaPlacedWhite = null;

    Map<Integer, String> rows = new HashMap<>();
    List<String> lines;
    try {
      lines = Files.readAllLines(Path.of(fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read board file: " + fileName, e);
    }

    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }

      if (line.startsWith("%")) {
        String comment = line.substring(1).trim();
        Integer nextBlack = parseBandMetadata(comment, "NEXT_NOIR");
        if (nextBlack != null) {
          metaNextBlack = nextBlack;
        }
        Integer nextWhite = parseBandMetadata(comment, "NEXT_BLANC");
        if (nextWhite != null) {
          metaNextWhite = nextWhite;
        }
        Boolean placedBlack = parseBooleanMetadata(comment, "PLACED_NOIR");
        if (placedBlack != null) {
          metaPlacedBlack = placedBlack;
        }
        Boolean placedWhite = parseBooleanMetadata(comment, "PLACED_BLANC");
        if (placedWhite != null) {
          metaPlacedWhite = placedWhite;
        }
        continue;
      }

      Matcher matcher = FILE_ROW_PATTERN.matcher(line);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid board line: " + rawLine);
      }

      int leftIndex = Integer.parseInt(matcher.group(1));
      int rightIndex = Integer.parseInt(matcher.group(3));
      if (leftIndex != rightIndex || leftIndex < 1 || leftIndex > SIZE) {
        throw new IllegalArgumentException("Invalid row indices in line: " + rawLine);
      }

      if (rows.put(leftIndex, matcher.group(2)) != null) {
        throw new IllegalArgumentException("Duplicated row index in file: " + leftIndex);
      }
    }

    if (rows.size() < SIZE) {
      throw new IllegalArgumentException("Board file must contain at least 6 board rows");
    }

    for (int rowNumber = 1; rowNumber <= SIZE; rowNumber++) {
      String row = rows.get(rowNumber);
      if (row == null) {
        throw new IllegalArgumentException("Missing board row " + rowNumber + " in file");
      }
      for (int col = 0; col < SIZE; col++) {
        board[rowNumber - 1][col] = row.charAt(col);
      }
    }

    validatePieceCounts();

    requiredBandForBlack = (metaNextBlack != null) ? metaNextBlack : FREE_BAND;
    requiredBandForWhite = (metaNextWhite != null) ? metaNextWhite : FREE_BAND;
    blackPlaced = (metaPlacedBlack != null) ? metaPlacedBlack : hasAnyPiece(PlayerColor.BLACK);
    whitePlaced = (metaPlacedWhite != null) ? metaPlacedWhite : hasAnyPiece(PlayerColor.WHITE);
  }

  /**
   * Sauve la configuration courante (plateau et etat de tour) dans un fichier.
   *
   * @param fileName le nom du fichier a sauvegarder
   */
  public void saveToFile(String fileName) {
    List<String> lines = new ArrayList<>();
    lines.add("% Escampe board");
    lines.add("% NEXT_NOIR=" + bandToText(requiredBandForBlack));
    lines.add("% NEXT_BLANC=" + bandToText(requiredBandForWhite));
    lines.add("% PLACED_NOIR=" + blackPlaced);
    lines.add("% PLACED_BLANC=" + whitePlaced);
    lines.add("% ABCDEF");

    for (int row = 1; row <= SIZE; row++) {
      String content = new String(board[row - 1]);
      lines.add(String.format(Locale.ROOT, "%02d %s %02d", row, content, row));
    }

    lines.add("% ABCDEF");

    Path path = Path.of(fileName);
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.write(path, lines, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to save board file: " + fileName, e);
    }
  }





  /**
   * Indique si le coup est valide pour le joueur sur le plateau courant.
   */
  public boolean isValidMove(String move, String player) {
    PlayerColor playerColor = parsePlayer(player);
    if (playerColor == null || move == null || move.trim().isEmpty()) {
      return false;
    }

    String normalizedMove = move.trim();

    if ("E".equalsIgnoreCase(normalizedMove)) {
      if (!isPlaced(playerColor) || gameOver()) {
        return false;
      }
      return generateRegularMoves(playerColor).isEmpty();
    }

    if (normalizedMove.contains("/")) {
      return isValidPlacementMove(normalizedMove, playerColor);
    }

    return isValidRegularMove(normalizedMove, playerColor);
  }

  /**
   * Calcule les coups possibles pour le joueur donne.
   */
  public String[] possiblesMoves(String player) {
    PlayerColor playerColor = parsePlayer(player);
    if (playerColor == null || gameOver() || !isPlaced(playerColor)) {
      return new String[0];
    }

    List<String> moves = generateRegularMoves(playerColor);
    if (moves.isEmpty()) {
      return new String[] { "E" };
    }
    return moves.toArray(String[]::new);
  }

  /**
   * Modifie le plateau en jouant le coup move pour le joueur donne.
   */
  public void play(String move, String player) {
    if (!isValidMove(move, player)) {
      throw new IllegalArgumentException("Invalid move '" + move + "' for player '" + player + "'");
    }

    PlayerColor playerColor = parsePlayer(player);
    String normalizedMove = move.trim();

    if ("E".equalsIgnoreCase(normalizedMove)) {
      setRequiredBand(opponentOf(playerColor), FREE_BAND);
      return;
    }

    if (normalizedMove.contains("/")) {
      applyPlacementMove(normalizedMove, playerColor);
      return;
    }

    Matcher matcher = REGULAR_MOVE_PATTERN.matcher(normalizedMove);
    matcher.matches();

    Position from = requireCoordinate(matcher.group(1));
    Position to = requireCoordinate(matcher.group(2));

    char movingPiece = board[from.row][from.col];
    board[from.row][from.col] = EMPTY;
    board[to.row][to.col] = movingPiece;

    setRequiredBand(opponentOf(playerColor), bandAt(to));
  }

  /**
   * Vrai lorsque le plateau correspond a une fin de partie.
   */
  public boolean gameOver() {
    if (!blackPlaced || !whitePlaced) {
      return false;
    }
    return !containsPiece(BLACK_UNICORN) || !containsPiece(WHITE_UNICORN);
  }

  /**
   * Utility method useful for tests and demos.
   */
  public int getRequiredBand(String player) {
    PlayerColor playerColor = parsePlayer(player);
    if (playerColor == null) {
      throw new IllegalArgumentException("Unknown player: " + player);
    }
    return requiredBandFor(playerColor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("  ABCDEF\n");
    for (int row = SIZE; row >= 1; row--) {
      sb.append(row);
      sb.append(' ');
      for (int col = 0; col < SIZE; col++) {
        sb.append(board[row - 1][col]);
      }
      sb.append(' ');
      sb.append(row);
      sb.append('\n');
    }
    sb.append("  ABCDEF\n");
    sb.append("next noir=").append(bandToText(requiredBandForBlack));
    sb.append(", next blanc=").append(bandToText(requiredBandForWhite));
    return sb.toString();
  }






  public static void main(String[] args) {
    try {
      Path startFile = resolveExamplePath("start_standard.txt");
      Path midgameFile = resolveExamplePath("midgame_req.txt");
      Path saveTarget = resolveExamplePath("demo_saved_position.txt");

      EscampeBoard board = new EscampeBoard();
      board.setFromFile(startFile.toString());
      System.out.println("=== Loaded starting board ===");
      System.out.println(board);
      String[] whiteMoves = board.possiblesMoves("blanc");
      int previewCount = Math.min(8, whiteMoves.length);
      System.out.println("Sample white moves: " + Arrays.toString(Arrays.copyOf(whiteMoves, previewCount)));

      EscampeBoard placementDemo = new EscampeBoard();
      String blackPlacement = "C6/A6/B5/D5/E6/F5";
      String whitePlacement = "C1/A3/C2/C5/F1/F4";
      System.out.println("\n=== Placement demo ===");
      System.out.println("Black placement valid? " + placementDemo.isValidMove(blackPlacement, "noir"));
      placementDemo.play(blackPlacement, "noir");
      System.out.println("White placement valid? " + placementDemo.isValidMove(whitePlacement, "blanc"));
      placementDemo.play(whitePlacement, "blanc");
      placementDemo.saveToFile(saveTarget.toString());
      System.out.println("Saved placed board to: " + saveTarget);

      EscampeBoard tactical = new EscampeBoard();
      tactical.setFromFile(midgameFile.toString());
      System.out.println("\n=== Tactical scenario from requirement ===");
      System.out.println(tactical);
      System.out.println("C2-C1 valid for white now? " + tactical.isValidMove("C2-C1", "blanc"));
      System.out.println("F6-E5 valid for white now? " + tactical.isValidMove("F6-E5", "blanc"));
      tactical.play("F6-E5", "blanc");
      System.out.println("A1-A2 valid for black now? " + tactical.isValidMove("A1-A2", "noir"));
      tactical.play("A1-A2", "noir");
      System.out.println("C2-C1 valid for white now? " + tactical.isValidMove("C2-C1", "blanc"));
      tactical.play("C2-C1", "blanc");
      System.out.println("Game over after capture? " + tactical.gameOver());
    } catch (RuntimeException e) {
      System.err.println("EscampeBoard demo failed: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private void clearBoard() {
    for (int row = 0; row < SIZE; row++) {
      Arrays.fill(board[row], EMPTY);
    }
  }

  private Integer parseBandMetadata(String comment, String key) {
    String prefix = key + "=";
    if (!comment.regionMatches(true, 0, prefix, 0, prefix.length())) {
      return null;
    }

    String value = comment.substring(prefix.length()).trim();
    if (value.equalsIgnoreCase("ANY") || value.equalsIgnoreCase("LIBRE") || value.equals("*")) {
      return FREE_BAND;
    }

    try {
      int parsed = Integer.parseInt(value);
      if (parsed >= 1 && parsed <= 3) {
        return parsed;
      }
    } catch (NumberFormatException ignored) {
      return null;
    }
    return null;
  }

  private Boolean parseBooleanMetadata(String comment, String key) {
    String prefix = key + "=";
    if (!comment.regionMatches(true, 0, prefix, 0, prefix.length())) {
      return null;
    }

    String value = comment.substring(prefix.length()).trim();
    if (value.equalsIgnoreCase("true")) {
      return Boolean.TRUE;
    }
    if (value.equalsIgnoreCase("false")) {
      return Boolean.FALSE;
    }
    return null;
  }

  private void validatePieceCounts() {
    int blackUnicornCount = countPiece(BLACK_UNICORN);
    int whiteUnicornCount = countPiece(WHITE_UNICORN);
    int blackPaladinCount = countPiece(BLACK_PALADIN);
    int whitePaladinCount = countPiece(WHITE_PALADIN);

    if (blackUnicornCount > 1 || whiteUnicornCount > 1) {
      throw new IllegalArgumentException("A board cannot contain more than one unicorn per side");
    }
    if (blackPaladinCount > 5 || whitePaladinCount > 5) {
      throw new IllegalArgumentException("A board cannot contain more than five paladins per side");
    }
  }

  private int countPiece(char piece) {
    int count = 0;
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        if (board[row][col] == piece) {
          count++;
        }
      }
    }
    return count;
  }

  private boolean isValidPlacementMove(String move, PlayerColor playerColor) {
    if (isPlaced(playerColor) || gameOver()) {
      return false;
    }

    String[] tokens = move.split("/");
    if (tokens.length != 6) {
      return false;
    }

    Set<Position> used = new HashSet<>();
    for (String token : tokens) {
      Position position = parseCoordinate(token.trim());
      if (position == null || board[position.row][position.col] != EMPTY || !used.add(position)) {
        return false;
      }
    }
    return true;
  }

  private void applyPlacementMove(String move, PlayerColor playerColor) {
    String[] tokens = move.split("/");
    Position unicornPosition = requireCoordinate(tokens[0].trim());
    board[unicornPosition.row][unicornPosition.col] = (playerColor == PlayerColor.BLACK) ? BLACK_UNICORN : WHITE_UNICORN;
    for (int i = 1; i < tokens.length; i++) {
      Position paladinPosition = requireCoordinate(tokens[i].trim());
      board[paladinPosition.row][paladinPosition.col] = (playerColor == PlayerColor.BLACK) ? BLACK_PALADIN : WHITE_PALADIN;
    }

    if (playerColor == PlayerColor.BLACK) {
      blackPlaced = true;
    } else {
      whitePlaced = true;
    }
  }

  private boolean isValidRegularMove(String move, PlayerColor playerColor) {
    if (!isPlaced(playerColor) || gameOver()) {
      return false;
    }

    Matcher matcher = REGULAR_MOVE_PATTERN.matcher(move);
    if (!matcher.matches()) {
      return false;
    }

    Position from = parseCoordinate(matcher.group(1));
    Position to = parseCoordinate(matcher.group(2));
    if (from == null || to == null || from.equals(to)) {
      return false;
    }

    char movingPiece = board[from.row][from.col];
    if (!isPlayersPiece(movingPiece, playerColor)) {
      return false;
    }

    int requiredBand = requiredBandFor(playerColor);
    if (requiredBand != FREE_BAND && bandAt(from) != requiredBand) {
      return false;
    }

    char destination = board[to.row][to.col];
    if (isPlayersPiece(destination, playerColor)) {
      return false;
    }

    boolean captureAllowed = isOpponentUnicorn(destination, playerColor) && isPaladin(movingPiece);
    if (destination != EMPTY && !captureAllowed) {
      return false;
    }

    return canReachWithExactSteps(from, to, bandAt(from), captureAllowed);
  }

  private List<String> generateRegularMoves(PlayerColor playerColor) {
    Set<String> moves = new TreeSet<>();
    int requiredBand = requiredBandFor(playerColor);

    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        char piece = board[row][col];
        if (!isPlayersPiece(piece, playerColor)) {
          continue;
        }

        Position from = new Position(row, col);
        if (requiredBand != FREE_BAND && bandAt(from) != requiredBand) {
          continue;
        }

        Set<Position> targets = collectReachableTargets(from, bandAt(from), piece, playerColor);
        for (Position target : targets) {
          moves.add(formatCoordinate(from) + "-" + formatCoordinate(target));
        }
      }
    }

    return new ArrayList<>(moves);
  }

  private Set<Position> collectReachableTargets(Position from, int steps, char movingPiece, PlayerColor playerColor) {
    Set<Position> targets = new HashSet<>();
    Set<Position> visited = new HashSet<>();
    visited.add(from);
    dfsCollectTargets(from, from, steps, movingPiece, playerColor, visited, targets);
    return targets;
  }

  private void dfsCollectTargets(Position origin, Position current, int remainingSteps, char movingPiece, PlayerColor playerColor,
      Set<Position> visited, Set<Position> targets) {
    if (remainingSteps == 0) {
      if (!current.equals(origin)) {
        targets.add(current);
      }
      return;
    }

    for (int[] direction : DIRECTIONS) {
      Position next = new Position(current.row + direction[0], current.col + direction[1]);
      if (!isInBounds(next) || visited.contains(next)) {
        continue;
      }

      char occupant = board[next.row][next.col];
      boolean isLastStep = remainingSteps == 1;

      if (!isLastStep) {
        if (occupant != EMPTY) {
          continue;
        }
      } else {
        if (occupant != EMPTY) {
          boolean captureAllowed = isOpponentUnicorn(occupant, playerColor) && isPaladin(movingPiece);
          if (!captureAllowed) {
            continue;
          }
        }
      }

      visited.add(next);
      dfsCollectTargets(origin, next, remainingSteps - 1, movingPiece, playerColor, visited, targets);
      visited.remove(next);
    }
  }

  private boolean canReachWithExactSteps(Position from, Position to, int steps, boolean captureAllowed) {
    Set<Position> visited = new HashSet<>();
    visited.add(from);
    return dfsReach(from, to, steps, captureAllowed, visited);
  }

  private boolean dfsReach(Position current, Position target, int remainingSteps, boolean captureAllowed,
      Set<Position> visited) {
    if (remainingSteps == 0) {
      return current.equals(target);
    }

    for (int[] direction : DIRECTIONS) {
      Position next = new Position(current.row + direction[0], current.col + direction[1]);
      if (!isInBounds(next) || visited.contains(next)) {
        continue;
      }

      boolean isTarget = next.equals(target);
      char occupant = board[next.row][next.col];

      if (!isTarget) {
        if (occupant != EMPTY) {
          continue;
        }
      } else {
        if (remainingSteps != 1) {
          continue;
        }
        if (occupant != EMPTY && !captureAllowed) {
          continue;
        }
      }

      visited.add(next);
      if (dfsReach(next, target, remainingSteps - 1, captureAllowed, visited)) {
        return true;
      }
      visited.remove(next);
    }

    return false;
  }

  private PlayerColor parsePlayer(String player) {
    if (player == null) {
      return null;
    }

    String normalized = player.trim().toLowerCase(Locale.ROOT);
    if (normalized.equals("noir") || normalized.equals("black")) {
      return PlayerColor.BLACK;
    }
    if (normalized.equals("blanc") || normalized.equals("white")) {
      return PlayerColor.WHITE;
    }
    return null;
  }

  private Position parseCoordinate(String coordinate) {
    if (coordinate == null) {
      return null;
    }

    Matcher matcher = COORD_PATTERN.matcher(coordinate.trim());
    if (!matcher.matches()) {
      return null;
    }

    int col = Character.toUpperCase(matcher.group(1).charAt(0)) - 'A';
    int row = matcher.group(2).charAt(0) - '1';
    return new Position(row, col);
  }

  private Position requireCoordinate(String coordinate) {
    Position position = parseCoordinate(coordinate);
    if (position == null) {
      throw new IllegalArgumentException("Invalid coordinate: " + coordinate);
    }
    return position;
  }

  private String formatCoordinate(Position position) {
    char file = (char) ('A' + position.col);
    char rank = (char) ('1' + position.row);
    return "" + file + rank;
  }

  private int bandAt(Position position) {
    return BAND_MAP[position.row][position.col];
  }

  private boolean isInBounds(Position position) {
    return position.row >= 0 && position.row < SIZE && position.col >= 0 && position.col < SIZE;
  }

  private boolean isPlaced(PlayerColor color) {
    return color == PlayerColor.BLACK ? blackPlaced : whitePlaced;
  }

  private boolean hasAnyPiece(PlayerColor color) {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        if (isPlayersPiece(board[row][col], color)) {
          return true;
        }
      }
    }
    return false;
  }

  private int requiredBandFor(PlayerColor color) {
    return color == PlayerColor.BLACK ? requiredBandForBlack : requiredBandForWhite;
  }

  private void setRequiredBand(PlayerColor color, int band) {
    if (color == PlayerColor.BLACK) {
      requiredBandForBlack = band;
    } else {
      requiredBandForWhite = band;
    }
  }

  private boolean isPlayersPiece(char piece, PlayerColor color) {
    if (color == PlayerColor.BLACK) {
      return piece == BLACK_UNICORN || piece == BLACK_PALADIN;
    }
    return piece == WHITE_UNICORN || piece == WHITE_PALADIN;
  }

  private boolean isOpponentUnicorn(char piece, PlayerColor color) {
    return (color == PlayerColor.BLACK) ? piece == WHITE_UNICORN : piece == BLACK_UNICORN;
  }

  private boolean isPaladin(char piece) {
    return piece == BLACK_PALADIN || piece == WHITE_PALADIN;
  }

  private boolean containsPiece(char piece) {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        if (board[row][col] == piece) {
          return true;
        }
      }
    }
    return false;
  }

  private String bandToText(int band) {
    return (band == FREE_BAND) ? "ANY" : Integer.toString(band);
  }

  private PlayerColor opponentOf(PlayerColor color) {
    return color == PlayerColor.BLACK ? PlayerColor.WHITE : PlayerColor.BLACK;
  }

  private static Path resolveExamplePath(String fileName) {
    List<Path> candidates = List.of(
        Path.of("examples", fileName),
        Path.of("escampe", "examples", fileName));

    for (Path candidate : candidates) {
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    return candidates.get(0);
  }
}