package game;

import interfaces.IMove;

public class EscampeMove implements IMove {
    private String move;

    public EscampeMove(String move) {
        this.move = move;
    }

    public String getMove() {
        return move;
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
}
