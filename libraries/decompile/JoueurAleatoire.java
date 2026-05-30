package escampe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class JoueurAleatoire implements e {
  private String[] a = new String[] { 
      "Bugs Bunny", "Daffy Duck", "Porky Pig", "Elmer Fudd", "Tweety", "Sylvester", "Road Runner", "Wile E. Coyote", "The Tasmanian Devil", "Yosemite Sam", 
      "Pepe Le Pew", "Marvin the Martian", "Foghorn Leghorn", "Speedy Gonzales", "Bosko", "Buddy", "Egghead", "Sniffles", "Cecil Turtle", "Mac 'n Tosh", 
      "The Three Bears", "Henery Hawk", "Beaky Buzzard", "Witch Hazel", "Gossamer", "Cool Cat", "Merlin the Magic Mouse" };
  
  private String b = this.a[this.g.nextInt(this.a.length)];
  
  private g c = new g();
  
  private int d;
  
  private int e;
  
  private int f = 0;
  
  private Random g = new Random();
  
  public final String b() {
    String[] arrayOfString;
    String str;
    System.out.println("Ah, c'est a moi, le joueur aleatoire " + ((this.d == -1) ? "BLANC" : "NOIR") + " de jouer... Je réfléchis...");
    System.out.println("Voici mon plateau de jeu avant de choisir mon coup :");
    this.c.a();
    if (this.c.e()) {
      boolean bool;
      String[] arrayOfString1 = { 
          "A1", "B1", "C1", "D1", "E1", "F1", "A2", "B2", "C2", "D2", 
          "E2", "F2" };
      String[] arrayOfString2 = { 
          "A5", "B5", "C5", "D5", "E5", "F5", "A6", "B6", "C6", "D6", 
          "E6", "F6" };
      if (this.d == 1) {
        bool = (Math.random() < 0.5D);
      } else {
        bool = this.c.b();
      } 
      System.out.println("Mon bord est celui du " + (bool ? "bas." : "haut."));
      ArrayList<String> arrayList;
      String str1 = (arrayList = new ArrayList<String>(Arrays.asList(bool ? (Object[])arrayOfString2 : (Object[])arrayOfString1))).get(this.g.nextInt(arrayList.size()));
      arrayList.remove(str1);
      for (byte b = 1; b < 6; b++) {
        str1 = arrayList.get(this.g.nextInt(arrayList.size()));
        str = String.valueOf(str) + "/" + str1;
        arrayList.remove(str1);
      } 
      this.c.b(this.d, str);
      System.out.println("Voici mon plateau de jeu apres mon coup :");
      this.c.a();
      return str;
    } 
    if (!this.c.f()) {
      arrayOfString = this.c.a(this.d);
    } else {
      arrayOfString = new String[0];
    } 
    if (arrayOfString.length > 0) {
      if (arrayOfString.length != 1) {
        System.out.print(String.valueOf(arrayOfString.length) + " Coups : ");
        for (byte b = 0; b < arrayOfString.length - 1; b++)
          System.out.print(String.valueOf(arrayOfString[b]) + " | "); 
        System.out.println(arrayOfString[arrayOfString.length - 1]);
      } else {
        System.out.print(String.valueOf(arrayOfString.length) + " Coup : ");
        System.out.println(arrayOfString[arrayOfString.length - 1]);
      } 
      str = arrayOfString[this.g.nextInt(arrayOfString.length)];
      System.out.println("Je choisi de jouer ".concat(String.valueOf(str)));
      this.c.b(this.d, str);
      System.out.println("Voici mon plateau de jeu apres mon coup :");
      this.c.a();
      return str;
    } 
    System.out.println(String.valueOf(str.length) + " Coups");
    return "xxxxx";
  }
  
  public final void b(int paramInt) {
    this.f = paramInt;
    if (this.f == this.d)
      System.out.println(String.valueOf(this.b) + " FTW!"); 
  }
  
  public final void a(int paramInt) {
    this.d = paramInt;
    if (paramInt == -1) {
      this.e = 1;
      return;
    } 
    this.e = -1;
  }
  
  public final int a() {
    return this.d;
  }
  
  public final void a(String paramString) {
    this.c.b(this.e, paramString);
  }
  
  public final String c() {
    return this.b;
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\JoueurAleatoire.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */