package escampe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class JoueurHumain implements e {
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
    System.out.println("Ah, c'est à moi, le joueur humain " + ((this.d == -1) ? "BLANC" : "NOIR") + " de jouer... Je réfléchis...");
    System.out.println("Voici mon plateau de jeu avant de choisir mon coup :");
    this.c.a();
    String str = null;
    if (this.c.e()) {
      boolean bool;
      String[] arrayOfString1 = { 
          "A1", "B1", "C1", "D1", "E1", "F1", "A2", "B2", "C2", "D2", 
          "E2", "F2" };
      String[] arrayOfString2 = { 
          "A5", "B5", "C5", "D5", "E5", "F5", "A6", "B6", "C6", "D6", 
          "E6", "F6" };
      if (this.d == 1) {
        System.out.println("Choisir le bord haut ou bas (H/B) :");
        String str2;
        bool = (str2 = (new Scanner(System.in)).nextLine()).equals("B");
      } else {
        bool = this.c.b();
      } 
      System.out.println("Mon bord est celui du " + (bool ? "bas." : "haut."));
      ArrayList arrayList = new ArrayList(Arrays.asList(bool ? (Object[])arrayOfString2 : (Object[])arrayOfString1));
      String str1 = bool ? "5 ou 6 :" : "1 ou 2 :";
      while (str == null) {
        System.out.println("Choisir la case de la licorne, sur la ligne ".concat(String.valueOf(str1)));
        String str2 = (new Scanner(System.in)).nextLine();
        if (arrayList.contains(str2)) {
          str = str2;
          arrayList.remove(str2);
          continue;
        } 
        System.out.println("Erreur : case non valide.");
      } 
      byte b = 1;
      while (b < 6) {
        System.out.println("Choisir la case du paladin " + b + ", sur la ligne " + str1);
        String str2 = (new Scanner(System.in)).nextLine();
        if (arrayList.contains(str2)) {
          str = String.valueOf(str) + "/" + str2;
          arrayList.remove(str2);
          b++;
          continue;
        } 
        System.out.println("Erreur : case non valide.");
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
      while (str == null) {
        System.out.println("Entrer un coup :");
        String str1 = (new Scanner(System.in)).nextLine();
        if (Arrays.<String>asList(arrayOfString).contains(str1)) {
          str = str1;
          continue;
        } 
        System.out.println("Erreur : coup non reconnu.");
      } 
      this.c.b(this.d, str);
      System.out.println("Voici mon plateau de jeu apres mon coup :");
      this.c.a();
      return str;
    } 
    System.out.println(String.valueOf(arrayOfString.length) + " Coups");
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


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\JoueurHumain.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */