package game.escampe;

import interfaces.IMove;

public class EscampeMove implements IMove {

  public final int x;
    public final int y;

    EscampeMove(int x, int y){
      this.x = x;
      this.y = y;
    }

    @Override
    public String toString() {
      return "Move{" + x + "," + y + "}";
    }
  
}
