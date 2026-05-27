package algorithms;

import java.util.List;

import game.EscampeMove;

public class Opening {
  public static final String[] WHITE_OPENINGS = {
    "C1/A1/B2/D2/E2/F1", // Balanced (all bands covered, unicorn central on double)
    "C1/A2/B1/D2/E1/F2", // Widespread (paladins in every column, unicorn on single)
    "C1/A1/B2/D2/F2/E2", // Triple-heavy (max mobility, aggressive)
    "D1/A1/B2/C2/E2/F1", // Central pressure (unicorn in center, paladins spread)
    "B1/A2/C1/D2/E1/F2", // Defensive (unicorn tucked at B1, paladins spread to control center and escape routes)
    "C1/D1/E1/A2/B2/F2", // Flanking (unicorn on double, paladins clustered on one side)
    "A2/B1/C2/D1/E2/F2", // Defensive edge (unicorn on double at edge, paladins spread)
    "C1/A1/F1/B2/E2/D2"  // Symmetric (paladins mirrored around center, unicorn on double)
  };

  public static final String[] BLACK_OPENINGS = {
    "C6/A6/B5/D5/E5/F6",
    "C6/A5/B6/D5/E6/F5",
    "C6/A6/B5/D5/F5/E5",
    "D6/A6/B5/C5/E5/F6",
    "B6/A5/C6/D5/E6/F5",
    "C6/D6/E6/A5/B5/F5",
    "A5/B6/C5/D6/E5/F5",
    "C6/A6/F6/B5/E5/D5"
  };

  private Opening() {/* Default constructor */}


  public static String getOpening(boolean isWhite, int index) {
    if (index < 0 || index >= WHITE_OPENINGS.length) {
      throw new IllegalArgumentException("Invalid opening index: " + index);
    }
    return isWhite ? WHITE_OPENINGS[index] : BLACK_OPENINGS[index];
  }

  public static List<String> getOpenings(boolean isWhite) {
    return List.of(isWhite ? WHITE_OPENINGS : BLACK_OPENINGS);
  }

  public static List<EscampeMove> getOpeningsMoves(boolean isWhite) {
    String[] openings = isWhite ? WHITE_OPENINGS : BLACK_OPENINGS;
    return List.of(openings).stream()
      .map(EscampeMove::new)
      .toList();
  }
}
