package escampe;

import java.util.Date;

public class Solo {
  private static e a;
  
  private static e b;
  
  private static int c = 0;
  
  private static e a(String paramString) {
    System.out.println(String.valueOf(paramString) + " : defaultPlayer");
    return new JoueurAleatoire();
  }
  
  private static e a(String paramString1, String paramString2) {
    System.out.print(String.valueOf(paramString2) + " : Chargement de la classe joueur " + paramString1 + "... ");
    try {
      Class<?> clazz;
      e e1 = (e)(clazz = Class.forName(paramString1)).newInstance();
    } catch (Exception exception) {
      System.out.println("Erreur de chargement");
      System.out.println(exception);
      return null;
    } 
    System.out.println("Ok");
    return (e)exception;
  }
  
  private static void a(e parame1, e parame2) {
    boolean bool = false;
    e e1 = parame2;
    while (!bool) {
      c++;
      System.out.println("\n*********\nOn demande à " + e1.c() + " de jouer...");
      long l1 = (new Date()).getTime();
      String str = e1.b();
      long l2;
      long l3 = (l2 = (new Date()).getTime()) - l1 + 1L;
      System.out.println("Le joueur " + e1.c() + " a joué le coup " + str + " en " + l3 + "s.");
      try {
        Thread.sleep(1L);
      } catch (InterruptedException interruptedException) {}
      if (str.compareTo("xxxxx") == 0) {
        bool = true;
        continue;
      } 
      if (c == 2) {
        parame2.a(str);
        continue;
      } 
      if (e1.a() == -1) {
        e1 = parame2;
      } else {
        e1 = parame1;
      } 
      e1.a(str);
    } 
    System.out.println("Partie finie en " + c + " coups.\n");
  }
  
  public static void main(String[] paramArrayOfString) {
    System.out.println("Partie solo ...");
    if (paramArrayOfString.length == 0) {
      a = a("Blanc");
      b = a("Noir");
    } else if (paramArrayOfString.length == 2) {
      a = a("Blanc");
      b = a("Noir");
    } else if (paramArrayOfString.length == 3) {
      a = a(paramArrayOfString[0], "Blanc");
      b = a(paramArrayOfString[0], "Noir");
    } else if (paramArrayOfString.length == 4) {
      a = a(paramArrayOfString[0], "Blanc");
      b = a(paramArrayOfString[1], "Noir");
    } 
    a.a(-1);
    System.out.println("Joueur Blanc : " + a.c());
    b.a(1);
    System.out.println("Joueur Noir : " + b.c());
    System.out.println("Initialisation des deux joueurs ok.");
    a(a, b);
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\Solo.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */