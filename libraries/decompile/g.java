package escampe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;

public class g {
  private static final int[][] a;
  
  private static final int[][] b;
  
  private static final int[][] c;
  
  private static final int[][] d;
  
  private static final int[][] e;
  
  private static final int[][] f;
  
  private static final f g;
  
  private static final LinkedList h;
  
  private boolean i;
  
  private int[][] j = new int[6][6];
  
  private f[][] k;
  
  private ArrayList l;
  
  private boolean m = true;
  
  private boolean n = false;
  
  private int o = 0;
  
  private int p;
  
  private int q = -1;
  
  private static char[] r;
  
  static {
    a = new int[][] { { 1, 2, 2, 3, 1, 2 }, { 3, 1, 3, 1, 3, 2 }, { 2, 3, 1, 2, 1, 3 }, { 2, 1, 3, 2, 3, 1 }, { 1, 3, 1, 3, 1, 2 }, { 3, 2, 2, 1, 3, 2 } };
    b = new int[][] { 
        { -3 }, { -2, 1 }, { -1, 2 }, { 0, 3 }, { 1, 2 }, { 2, 1 }, { 3 }, { 2, -1 }, { 1, -2 }, { 0, -3 }, 
        { -1, -2 }, { -2, -1 }, { -1 }, { 0, 1 }, { 1 }, { 0, -1 } };
    c = new int[][] { 
        new int[1], { 0, 1 }, { 1, 2 }, { 2 }, { 2, 3 }, { 3, 4 }, { 4 }, { 4, 5 }, { 5, 6 }, { 6 }, 
        { 6, 7 }, { 7 }, { 7, 1 }, { 1, 3 }, { 3, 5 }, { 5, 7 } };
    d = new int[][] { { -2 }, { -1, 1 }, { 0, 2 }, { 1, 1 }, { 2 }, { 1, -1 }, { 0, -2 }, { -1, -1 } };
    e = new int[][] { new int[1], { 0, 1 }, { 1 }, { 1, 2 }, { 2 }, { 2, 3 }, { 3 }, { 3 } };
    f = new int[][] { { -1 }, { 0, 1 }, { 1 }, { 0, -1 } };
    g = f.a;
    h = new LinkedList();
    r = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' };
  }
  
  private static f b(int paramInt1, int paramInt2) {
    return f.valueOf(String.valueOf(r[paramInt2]) + Integer.toString(paramInt1 + 1));
  }
  
  private static boolean c(int paramInt1, int paramInt2) {
    return (paramInt1 >= 0 && paramInt1 < 6 && paramInt2 >= 0 && paramInt2 < 6);
  }
  
  final void a() {
    DecimalFormat decimalFormat;
    (decimalFormat = new DecimalFormat()).setMinimumIntegerDigits(2);
    String str = new String("   ABCDEF\n");
    for (byte b = 0; b < 6; b++) {
      str = String.valueOf(str) + decimalFormat.format((b + 1)) + " ";
      for (byte b1 = 0; b1 < 6; b1++) {
        if (this.j[b][b1] == 0) {
          str = String.valueOf(str) + "-";
        } else if (this.j[b][b1] == -2) {
          str = String.valueOf(str) + "B";
        } else if (this.j[b][b1] == -1) {
          str = String.valueOf(str) + "b";
        } else if (this.j[b][b1] == 2) {
          str = String.valueOf(str) + "N";
        } else if (this.j[b][b1] == 1) {
          str = String.valueOf(str) + "n";
        } else {
          str = String.valueOf(str) + " ";
        } 
      } 
      str = String.valueOf(str) + " " + decimalFormat.format((b + 1)) + "\n";
    } 
    str = String.valueOf(str) + "   ABCDEF\n";
    System.out.println(str);
  }
  
  g() {
    for (byte b = 0; b < 6; b++) {
      for (byte b1 = 0; b1 < 6; b1++)
        this.j[b][b1] = 0; 
    } 
    this.k = new f[2][6];
    this.p = 1;
  }
  
  public final boolean b() {
    return this.i;
  }
  
  final int[][] c() {
    int[][] arrayOfInt = new int[6][6];
    for (byte b = 0; b < 6; b++) {
      for (byte b1 = 0; b1 < 6; b1++)
        arrayOfInt[b][b1] = this.j[b][b1]; 
    } 
    return arrayOfInt;
  }
  
  final int d() {
    return this.p;
  }
  
  public final boolean e() {
    return this.m;
  }
  
  final void a(int paramInt1, int paramInt2) {
    String str1;
    String str2;
    this.n = true;
    if (paramInt1 == -1) {
      this.o = 1;
      str1 = "BLANC";
      str2 = "NOIR";
    } else if (str1 == '\001') {
      this.o = -1;
      str1 = "NOIR";
      str2 = "BLANC";
    } else {
      this.o = 0;
      str1 = "Egalité: personne ne";
      str2 = "EGALITE";
    } 
    if (paramInt2 == 0) {
      System.out.println(String.valueOf(str1) + " perd avec un coup illégal.");
      str1 = "ILLEGAL-MOVE";
    } else if (paramInt2 == 1) {
      System.out.println(String.valueOf(str1) + " perd suivant les règles.");
      str1 = "FAIR-PLAY";
    } else if (paramInt2 == 2) {
      System.out.println("Egalité.");
      str1 = "DEUCE";
    } else if (paramInt2 == 10) {
      System.out.println("Timeout.");
      str1 = "TIMEOUT";
    } else {
      System.out.println("Fin de type " + paramInt2 + ".");
      str1 = "UNKNOWN";
    } 
    System.out.println("[REGLES] " + str2 + " GAGNE. RAISON: " + str1);
  }
  
  final boolean f() {
    return this.n;
  }
  
  final int g() {
    return (this.o == 0) ? this.o : ((this.o + 1) / 2 + 1);
  }
  
  private void b(int paramInt) {
    g g1;
    f[] arrayOfF = this.k[(paramInt + 1) / 2];
    int i = paramInt;
    ArrayList arrayList = new ArrayList();
    boolean bool = false;
  }
  
  private boolean a(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
    int[] arrayOfInt = e[paramInt1];
    for (byte b = 0; b < arrayOfInt.length; b++) {
      int k = arrayOfInt[b];
      int i = paramInt3 + f[k][0];
      int j = paramInt4 + f[k][1];
      if (this.j[i][j] == 0 && paramInt2 != k + 12)
        return true; 
    } 
    return false;
  }
  
  private boolean a(int paramInt1, int paramInt2, int paramInt3) {
    return a(paramInt1, -1, paramInt2, paramInt3);
  }
  
  private boolean b(int paramInt1, int paramInt2, int paramInt3) {
    int[] arrayOfInt = c[paramInt1];
    for (byte b = 0; b < arrayOfInt.length; b++) {
      int k = arrayOfInt[b];
      int i = paramInt2 + d[k][0];
      int j = paramInt3 + d[k][1];
      if (c(i, j) && this.j[i][j] == 0 && a(k, paramInt1, paramInt2, paramInt3))
        return true; 
    } 
    return false;
  }
  
  private ArrayList a(int paramInt, f paramf) {
    int i = paramf.b;
    int j = paramf.c;
    ArrayList<LinkedList<f>> arrayList = new ArrayList();
    if (a[paramf.b][paramf.c] == this.q || this.q == -1) {
      System.out.println("Case : (" + paramf.b + ", " + paramf.c + ")");
      if (a[paramf.b][paramf.c] == 3) {
        for (byte b = 0; b < 16; b++) {
          int k = i + b[b][0];
          int m = j + b[b][1];
          if (c(k, m) && (this.j[k][m] == 0 || (this.j[k][m] == paramInt * -2 && this.j[i][j] == 1 * paramInt)) && b(b, i, j)) {
            LinkedList<f> linkedList;
            (linkedList = new LinkedList<f>()).addLast(paramf);
            linkedList.addLast(b(k, m));
            arrayList.add(linkedList);
          } 
        } 
      } else if (a[paramf.b][paramf.c] == 2) {
        for (byte b = 0; b < 8; b++) {
          int k = i + d[b][0];
          int m = j + d[b][1];
          if (c(k, m) && (this.j[k][m] == 0 || (this.j[k][m] == paramInt * -2 && this.j[i][j] == 1 * paramInt)) && a(b, i, j)) {
            LinkedList<f> linkedList;
            (linkedList = new LinkedList<f>()).addLast(paramf);
            linkedList.addLast(b(k, m));
            arrayList.add(linkedList);
          } 
        } 
      } else {
        for (byte b = 0; b < 4; b++) {
          int k = i + f[b][0];
          int m = j + f[b][1];
          if (c(k, m) && (this.j[k][m] == 0 || (this.j[k][m] == paramInt * -2 && this.j[i][j] == 1 * paramInt))) {
            LinkedList<f> linkedList;
            (linkedList = new LinkedList<f>()).addLast(paramf);
            linkedList.addLast(b(k, m));
            arrayList.add(linkedList);
          } 
        } 
      } 
    } 
    return arrayList;
  }
  
  final String[] a(int paramInt) {
    if (this.n)
      return arrayOfString = new String[0]; 
    b(arrayOfString);
    String[] arrayOfString = new String[this.l.size()];
    if (this.l.contains(h)) {
      arrayOfString[0] = "E";
    } else {
      for (byte b = 0; b < this.l.size(); b++) {
        arrayOfString[b] = ((LinkedList)this.l.get(b)).toString().replaceAll(", ", "-");
        arrayOfString[b] = arrayOfString[b].substring(1, arrayOfString[b].length() - 1);
      } 
    } 
    return arrayOfString;
  }
  
  private void c(int paramInt, String paramString) {
    System.out.println("[ARBITRE] Coup illegal " + paramString + " pour " + ((paramInt == -1) ? "BLANC" : "NOIR"));
    a(this.p, 0);
  }
  
  final boolean a(int paramInt, String paramString) {
    boolean bool = false;
    if (!s && paramInt != this.p)
      throw new AssertionError(); 
    if (paramString != null) {
      if (this.m) {
        String[] arrayOfString1 = paramString.split("/");
        ArrayList<String> arrayList = new ArrayList();
        try {
          f f1 = f.valueOf(arrayOfString1[0]);
          bool = (paramInt == 1) ? ((f1.b > 3) ? true : false) : this.i;
          for (byte b1 = 0; b1 < 6; b1++) {
            f1 = f.valueOf(arrayOfString1[b1]);
            boolean bool1 = bool ? ((f1.b > 3) ? true : false) : ((f1.b < 2) ? true : false);
            if (arrayList.contains(arrayOfString1[b1]) || f1.c == 8 || !bool1)
              throw new IllegalArgumentException(); 
            arrayList.add(arrayOfString1[b1]);
          } 
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
          c(paramInt, paramString);
          return false;
        } catch (IllegalArgumentException illegalArgumentException) {
          c(paramInt, paramString);
          return false;
        } 
        return true;
      } 
      b(this.p);
      if (paramString.equals("E")) {
        if (this.l.contains(h))
          return true; 
        c(paramInt, paramString);
        return false;
      } 
      LinkedList<f> linkedList = new LinkedList();
      String[] arrayOfString = paramString.split("-");
      byte b;
      for (b = 0; b < arrayOfString.length; b++) {
        try {
          linkedList.add(f.valueOf(arrayOfString[b]));
        } catch (IllegalArgumentException illegalArgumentException) {
          c(paramInt, paramString);
        } 
      } 
      for (b = 0; b < this.l.size() && !bool; b++) {
        if (((LinkedList)this.l.get(b)).toString().equals(linkedList.toString())) {
          bool = true;
          System.out.println("[ARBITRE] Coup valide pour " + ((paramInt == -1) ? "BLANC" : "NOIR"));
        } 
      } 
      if (!bool) {
        c(paramInt, paramString);
        return bool;
      } 
      return bool;
    } 
    c(paramInt, paramString);
    return bool;
  }
  
  final void b(int paramInt, String paramString) {
    f f1;
    int i = (paramInt + 1) / 2;
    int j = (-1 * paramInt + 1) / 2;
    if (this.m) {
      String[] arrayOfString;
      f1 = f.valueOf((arrayOfString = paramString.split("/"))[0]);
      this.j[f1.b][f1.c] = 2 * paramInt;
      this.k[i][0] = f1;
      for (byte b = 1; b < 6; b++) {
        f1 = f.valueOf(arrayOfString[b]);
        this.j[f1.b][f1.c] = 1 * paramInt;
        this.k[i][b] = f1;
      } 
      if (paramInt == 1)
        this.i = (f1.b < 2); 
    } else {
      if (!f1.equals("E")) {
        LinkedList<f> linkedList = new LinkedList();
        String[] arrayOfString = f1.split("-");
        for (byte b = 0; b < arrayOfString.length; b++)
          linkedList.add(f.valueOf(arrayOfString[b])); 
        a(paramInt, linkedList);
      } else {
        this.q = -1;
      } 
      if (this.k[j][0] == g) {
        int k = -1 * paramInt;
        System.out.println("[ARBITRE] Licorne capturée par " + ((paramInt == -1) ? "BLANC" : "NOIR"));
        a(k, 1);
      } 
    } 
    g g1;
    if ((g1 = this).p == -1) {
      if (g1.m) {
        g1.m = false;
        return;
      } 
      g1.p = 1;
      return;
    } 
    g1.p = -1;
  }
  
  private void a(int paramInt, LinkedList<f> paramLinkedList) {
    int i = (paramInt + 1) / 2;
    paramInt = (-1 * paramInt + 1) / 2;
    f f2 = paramLinkedList.getFirst();
    f f1 = paramLinkedList.getLast();
    byte b;
    for (b = 0; b < 6; b++) {
      if (this.k[i][b].equals(f2)) {
        this.k[i][b] = f1;
        this.j[f1.b][f1.c] = this.j[f2.b][f2.c];
        this.j[f2.b][f2.c] = 0;
        break;
      } 
    } 
    for (b = 0; b < 6; b++) {
      if (this.k[paramInt][b].equals(f1)) {
        this.k[paramInt][b] = g;
        break;
      } 
    } 
    this.q = a[f1.b][f1.c];
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\g.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */