package game.escampe;

import interfaces.IRole;

public class EscampeRole implements IRole {
  public static final EscampeRole WHITE = new EscampeRole("White");
  public static final EscampeRole BLACK = new EscampeRole("Black");

  private String name;

  private EscampeRole(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
