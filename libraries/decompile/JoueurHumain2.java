package escampe;

import java.util.Random;
import java.util.Scanner;

public class JoueurHumain2 implements e {
  private String[] a = new String[] { 
      "Astérix", "Obélix", "Lucky Luke", "Spirou", "Fantasio", "Boule", "Bill", "Tintin", "Archibald Haddock", "Valérian", 
      "Laureline", "Thorgal Aegirsson", "Aaricia Gandalfsdottir", "Achille Talon", "Hilarion Lefuneste", "Iznogoud", "Dilat Laraht", "Gaston Lagaffe", "Prunelle", "Melle Jeanne", 
      "Bob Morane", "Bill Ballantine", "Lone Sloane", "Cornélius M. Chesterfield", "Blutch", "Léonard da Vinci", "Basile Landouye" };
  
  private String b = this.a[this.f.nextInt(this.a.length)];
  
  private int c;
  
  private int d;
  
  private int e = 0;
  
  private Random f = new Random();
  
  public final String b() {
    System.out.println("Ah, c'est à moi, le joueur humain " + ((this.c == -1) ? "BLANC" : "NOIR") + " de jouer... Je réfléchis...");
    System.out.println("Entrer un coup :");
    String str = (new Scanner(System.in)).nextLine();
    System.out.println("Je choisis de jouer ".concat(String.valueOf(str)));
    return str;
  }
  
  public final void b(int paramInt) {
    this.e = paramInt;
    if (this.e == this.c)
      System.out.println(String.valueOf(this.b) + " FTW!"); 
  }
  
  public final void a(int paramInt) {
    this.c = paramInt;
    if (paramInt == -1) {
      this.d = 1;
      return;
    } 
    this.d = -1;
  }
  
  public final int a() {
    return this.c;
  }
  
  public final void a(String paramString) {}
  
  public final String c() {
    return this.b;
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\JoueurHumain2.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */