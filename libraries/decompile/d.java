package escampe;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

final class d extends JPanel {
  private int[][] a;
  
  private String b;
  
  private int c = -1;
  
  private int d = -1;
  
  private int e = -1;
  
  private int f = -1;
  
  public d(a parama, String paramString, int[][] paramArrayOfint) {
    this.a = paramArrayOfint;
    this.b = paramString;
    if (paramString.length() == 5) {
      String[] arrayOfString = paramString.split("-");
      this.c = arrayOfString[0].charAt(0) - 65;
      this.d = Integer.parseInt(arrayOfString[0].substring(1)) - 1;
      this.e = arrayOfString[1].charAt(0) - 65;
      this.f = Integer.parseInt(arrayOfString[1].substring(1)) - 1;
    } 
  }
  
  private void a(Graphics paramGraphics) {
    paramGraphics.setColor(new Color(0, 0, 0));
    byte b1;
    for (b1 = 1; b1 <= 6; b1++)
      paramGraphics.drawString((char)(b1 + 65 - 1), 30 + (int)((b1 - 0.5D) * 100.0D), 20); 
    for (b1 = 1; b1 <= 6; b1++)
      paramGraphics.drawString(String.valueOf(b1), 10, 30 + (int)((b1 - 0.5D) * 100.0D)); 
    Color color1 = this.g.a;
    paramGraphics.setColor(color1);
    paramGraphics.fillRect(30, 30, 600, 600);
    byte b2;
    for (b2 = 0; b2 < 6; b2++) {
      for (byte b = 0; b < 6; b++) {
        int i = 30 + b2 * 100;
        int j = 30 + b * 100;
        int k = a.c()[b][b2];
        Color color = (b == this.d && b2 == this.c) ? this.g.e : this.g.b;
        paramGraphics.setColor(color);
        paramGraphics.fillOval(i, j, this.g.g, this.g.g);
        paramGraphics.setColor(color1);
        paramGraphics.fillOval(i + this.g.n, j + this.g.n, this.g.h, this.g.h);
        if (k > 1) {
          paramGraphics.setColor(color);
          paramGraphics.fillOval(i + this.g.o, j + this.g.o, this.g.i, this.g.i);
          paramGraphics.setColor(color1);
          paramGraphics.fillOval(i + this.g.p, j + this.g.p, this.g.j, this.g.j);
          if (k > 2) {
            paramGraphics.setColor(color);
            paramGraphics.fillOval(i + this.g.q, j + this.g.q, this.g.k, this.g.k);
            paramGraphics.setColor(color1);
            paramGraphics.fillOval(i + this.g.r, j + this.g.r, this.g.l, this.g.l);
          } 
        } 
      } 
    } 
    color1 = this.g.c;
    Color color2 = this.g.d;
    for (b2 = 0; b2 < 6; b2++) {
      for (byte b = 0; b < 6; b++) {
        int i = this.g.f + 30 + b2 * 100;
        int j = this.g.f + 30 + b * 100;
        switch (this.a[b][b2]) {
          case -2:
            paramGraphics.setColor(color1);
            paramGraphics.fillRect(i, j, 60, 60);
            break;
          case -1:
            paramGraphics.setColor(color1);
            paramGraphics.fillOval(i, j, 60, 60);
            break;
          case 2:
            paramGraphics.setColor(color2);
            paramGraphics.fillRect(i, j, 60, 60);
            break;
          case 1:
            paramGraphics.setColor(color2);
            paramGraphics.fillOval(i, j, 60, 60);
            break;
        } 
        if (b == this.f && b2 == this.e) {
          paramGraphics.setColor(this.g.e);
          paramGraphics.fillOval(i + 20, j + 20, 20, 20);
        } 
      } 
    } 
  }
  
  public final void paint(Graphics paramGraphics) {
    a(paramGraphics);
  }
  
  public final void update(Graphics paramGraphics) {
    a(paramGraphics);
  }
  
  public final String toString() {
    return this.b;
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\d.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */