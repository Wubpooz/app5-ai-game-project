package escampe;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.DefaultListModel;
import javax.swing.JApplet;
import javax.swing.JList;
import javax.swing.JScrollPane;

public final class a extends JApplet {
  private static final int[][] s = new int[][] { { 1, 2, 2, 3, 1, 2 }, { 3, 1, 3, 1, 3, 2 }, { 2, 3, 1, 2, 1, 3 }, { 2, 1, 3, 2, 3, 1 }, { 1, 3, 1, 3, 1, 2 }, { 3, 2, 2, 1, 3, 2 } };
  
  Color a = new Color(155, 102, 95);
  
  Color b = new Color(239, 210, 158);
  
  Color c = new Color(255, 255, 255);
  
  Color d = new Color(0, 0, 0);
  
  Color e = new Color(255, 0, 0);
  
  private static final Dimension t = new Dimension(860, 660);
  
  private JList u;
  
  private d v;
  
  private JScrollPane w;
  
  private DefaultListModel x;
  
  private Frame y;
  
  private static int z = 0;
  
  int f = 20;
  
  private int A = 10;
  
  private int B = 5;
  
  int g = 100;
  
  int h = this.g - this.A;
  
  int i = this.h - this.B;
  
  int j = this.i - this.A;
  
  int k = this.j - this.B;
  
  int l = this.k - this.A;
  
  int m = 0;
  
  int n = (100 - this.h) / 2;
  
  int o = (100 - this.i) / 2;
  
  int p = (100 - this.j) / 2;
  
  int q = (100 - this.k) / 2;
  
  int r = (100 - this.l) / 2;
  
  public final void init() {
    System.out.println("Initialisation BoardApplet" + z++);
    a(getContentPane());
  }
  
  public final void a(Container paramContainer) {
    setBackground(Color.white);
    int[][] arrayOfInt = new int[6][6];
    for (byte b = 0; b < 6; b++) {
      for (byte b1 = 0; b1 < 6; b1++)
        arrayOfInt[b][b1] = 0; 
    } 
    this.v = new d(this, "Coups :", arrayOfInt);
    this.x = new DefaultListModel();
    this.x.addElement(this.v);
    this.u = new JList(this.x);
    this.u.setSelectionMode(0);
    this.u.setSelectedIndex(0);
    this.w = new JScrollPane(this.u);
    Dimension dimension = this.w.getSize();
    this.w.setPreferredSize(new Dimension(200, dimension.height));
    this.u.addKeyListener(new b(this));
    this.u.addMouseListener(new c(this));
    paramContainer.add(this.v, "Center");
    paramContainer.add(this.w, "East");
  }
  
  public final void a(Graphics paramGraphics, Insets paramInsets) {
    paramInsets = paramInsets;
    paramGraphics.translate(paramInsets.left, paramInsets.top);
    paint(paramGraphics);
  }
  
  public final void paint(Graphics paramGraphics) {
    this.v.paint(paramGraphics);
  }
  
  public final void a(String paramString, int[][] paramArrayOfint) {
    d d1 = new d(this, paramString, paramArrayOfint);
    this.x.addElement(new d(this, paramString, paramArrayOfint));
    this.u.setSelectedIndex(this.x.getSize() - 1);
    this.u.ensureIndexIsVisible(this.x.getSize() - 1);
    this.v = d1;
    a(this.y.getGraphics(), this.y.getInsets());
  }
  
  public final void a(Frame paramFrame) {
    this.y = paramFrame;
  }
  
  final void a(KeyEvent paramKeyEvent) {
    int i = this.u.getSelectedIndex();
    if (paramKeyEvent.getKeyCode() == 38 && i > 0)
      this.v = this.x.getElementAt(i - 1); 
    if (paramKeyEvent.getKeyCode() == 40 && i < this.x.getSize() - 1)
      this.v = this.x.getElementAt(i + 1); 
    a(this.y.getGraphics(), this.y.getInsets());
  }
  
  final void a() {
    this.v = this.x.getElementAt(this.u.getSelectedIndex());
    a(this.y.getGraphics(), this.y.getInsets());
  }
  
  public static Dimension b() {
    return t;
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\a.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */