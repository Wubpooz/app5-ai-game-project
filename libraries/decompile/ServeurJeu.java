package escampe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;

public class ServeurJeu {
  public static boolean a = true;
  
  private static boolean b = true;
  
  public static void main(String[] paramArrayOfString) {
    ServerSocket serverSocket = null;
    Date date = new Date();
    System.out.println("Serveur démarré le ".concat(String.valueOf(date)));
    if (paramArrayOfString.length <= 0) {
      System.err.println("Usage : ServeurJeu PortNumber [1|0]");
      System.err.println("[1|0] 1 si vous voulez une applet graphique (ou non)");
      System.exit(1);
    } 
    int i = Integer.parseInt(paramArrayOfString[0]);
    if (paramArrayOfString.length > 1) {
      int j = b ? 1 : 0;
      try {
        j = Integer.parseInt(paramArrayOfString[1]);
      } catch (NumberFormatException numberFormatException) {
        System.err.println("[ARBITRE] Erreur dans le premier argument (" + paramArrayOfString[1] + ")");
        System.exit(1);
      } 
      if (j != 0 && j != 1) {
        System.err.println("[ARBITRE] Erreur de format (0 ou 1) dans le second argument (" + paramArrayOfString[1] + ")");
        System.exit(1);
      } 
      b = (j == 1);
      System.out.println("[ARBITRE] Applet Graphique configurée à " + b);
    } 
    try {
      serverSocket = new ServerSocket(i);
    } catch (IOException iOException) {
      System.err.println("[ERREUR] Le serveur ne peut pas écouter le port ".concat(String.valueOf(i)));
      System.exit(1);
    } 
    serverSocket.setSoTimeout(30000);
    try {
      while (a) {
        System.out.println("[INFO] Serveur OK. Ecoute sur le port ".concat(String.valueOf(i)));
        System.out.println("[INFO] Le serveur attend les deux joueurs pour commencer...");
        Socket socket2 = serverSocket.accept();
        String str1 = (new BufferedReader(new InputStreamReader(socket2.getInputStream()))).readLine();
        System.out.println("[ARBITRE] JOUEUR 1 " + str1 + " OK\n[INFO] Le serveur attend le second joueur...");
        Socket socket1 = serverSocket.accept();
        String str2 = (new BufferedReader(new InputStreamReader(socket1.getInputStream()))).readLine();
        System.out.println("[ARBITRE] JOUEUR 2 " + str2 + " OK\n[INFO] Les deux joueurs sont prêts. Le serveur lance la partie!");
        h h;
        (h = new h(socket2, socket1, b)).start();
        a = false;
      } 
    } catch (SocketTimeoutException socketTimeoutException) {
      System.err.println("[ARBITRE] SOCKETTIMEOUT : attente 30 secondes pour les deux clients en vain.");
      System.exit(1);
    } 
    serverSocket.close();
  }
}


/* Location:              C:\Users\mathi\Documents\GitHub\app5-ai-game-project\libraries\escampeobf.jar!\escampe\ServeurJeu.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */