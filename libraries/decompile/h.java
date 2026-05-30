package escampe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import javax.swing.JFrame;

public final class h extends Thread {
  private Socket[] a;
  
  private PrintWriter[] b;
  
  private BufferedReader[] c;
  
  private String[] d = new String[] { "Egalité", "Blanc", "Noir" };
  
  private g e = null;
  
  private long[] f;
  
  private static boolean g = true;
  
  private static a h;
  
  private static JFrame i = null;
  
  public h(Socket paramSocket1, Socket paramSocket2, boolean paramBoolean) {
    super("ServeurJeuThread");
    g = paramBoolean;
    this.a = new Socket[2];
    this.a[0] = paramSocket1;
    this.a[1] = paramSocket2;
    this.b = new PrintWriter[2];
    this.c = new BufferedReader[2];
    this.e = new g();
    if (g) {
      i = new JFrame("Vue du jeu");
      (h = new a()).a(i.getContentPane());
      i.setSize(a.b());
      h.a(i);
      i.setVisible(true);
      h.a("Départ ", this.e.c());
      h.a(i.getGraphics(), i.getInsets());
    } 
    this.f = new long[2];
    this.f[0] = 0L;
    this.f[1] = 0L;
    System.out.println("Jeu lancé");
  }
  
  public final void run() {
    try {
      byte b;
      for (b = 0; b < 2; b++) {
        this.b[b] = new PrintWriter(this.a[b].getOutputStream(), true);
        this.c[b] = new BufferedReader(new InputStreamReader(this.a[b].getInputStream()));
      } 
      for (b = 0; b < 2; b++) {
        this.b[b].println(String.valueOf(this.d[b + 1]) + Character.MIN_VALUE);
        this.b[b].flush();
      } 
      sleep(2000L);
      try {
        this.a[0].setSoTimeout(600000);
        this.a[1].setSoTimeout(600000);
      } catch (SocketException socketException) {
        System.out.println(socketException);
      } 
      do {
        int i = (this.e.d() + 1) / 2 + 1;
        for (b = 0; b < 2; b++) {
          this.b[b].println("JOUEUR " + this.d[i] + Character.MIN_VALUE);
          this.b[b].flush();
        } 
        try {
          String str;
          sleep(500L);
          this.e.a();
          System.out.println("[INFO] Attente du mouvement du joueur " + this.d[i]);
          this.a[i - 1].setSoTimeout(630000 - (int)this.f[i - 1]);
          long l1 = (new Date()).getTime();
          try {
            str = this.c[i - 1].readLine();
          } catch (SocketException socketException) {
            this.e.a(i, 10);
            System.out.println("[ARBITRE] Dépassement de temps sur lecture de socket.");
            break;
          } 
          long l2;
          long l3;
          if ((l3 = (l2 = (new Date()).getTime()) - l1) == 0L)
            l3 = 1L; 
          this.f[i - 1] = this.f[i - 1] + l3;
          if (this.f[i - 1] / 1000L > 600L) {
            this.e.a((i - 1 << 1) - 1, 10);
            System.out.println("[ARBITRE] Dépassement du temps pour  " + i + ": " + (this.f[i - 1] / 1000L) + " secondes de réflexion !");
            break;
          } 
          if (!this.e.a((i - 1 << 1) - 1, str))
            break; 
          System.out.println("[MOUVEMENT] " + l3 + " " + this.f[i - 1] + " " + this.d[i] + " " + str);
          this.e.b((i - 1 << 1) - 1, str);
          if (i == 1) {
            this.b[1].println("MOUVEMENT ".concat(String.valueOf(str)));
            this.b[1].flush();
          } else {
            this.b[0].println("MOUVEMENT ".concat(String.valueOf(str)));
            this.b[0].flush();
          } 
          if (g) {
            h.a(str, this.e.c());
            h.a(i.getGraphics(), i.getInsets());
          } 
        } catch (InterruptedIOException interruptedIOException) {
          this.e.a((i - 1 << 1) - 1, 10);
          System.out.println("[ARBITRE] TIMEOUT " + i + Character.MIN_VALUE);
        } catch (InterruptedException interruptedException) {
          System.out.println(interruptedException);
        } 
      } while (!this.e.f());
      for (b = 0; b < 2; b++) {
        this.b[b].println("FIN! " + this.d[this.e.g()] + "\000");
        this.b[b].flush();
      } 
      System.out.println("[ARBITRE] FIN! " + this.d[this.e.g()] + "\000");
      sleep(100L);
      ServeurJeu.a = false;
      return;
    } catch (IOException iOException) {
      System.out.println("La communication ne passe pas !");
      return;
    } catch (InterruptedException interruptedException) {
      System.out.println(interruptedException);
      return;
    } 
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\h.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */