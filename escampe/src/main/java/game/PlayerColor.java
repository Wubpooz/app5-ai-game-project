package game;

public enum PlayerColor implements interfaces.IRole {
  BLACK(-1), WHITE(1);

  private final int value;

  PlayerColor(int value) {
    if(value != -1 && value != 1) {
      throw new IllegalArgumentException("PlayerColor value must be either -1 (BLACK) or 1 (WHITE)");
    }
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
