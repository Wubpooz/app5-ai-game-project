package escampe;

public enum f {
  a, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, A, B, C, D, E, F, G, H, I, J, K, L, M;
  
  public int b;
  
  public int c;
  
  f() {
    this$enum$name = toString();
    this.c = this$enum$name.charAt(0) - 65;
    this.b = Integer.parseInt(this$enum$name.substring(1)) - 1;
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\f.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */