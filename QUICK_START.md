## Running
```bash
# Compile the project
cd escampe
.\gradlew.bat compileJava
```

### Local Testing
```bash
# Test your AI vs. random player
java -cp "build/classes/java/main;../libraries/escampeobf.jar" io.Solo io.MyAIPlayer escampe.JoueurAleatoire

# Test your AI vs. itself
java -cp "build/classes/java/main;../libraries/escampeobf.jar" io.Solo io.MyAIPlayer io.MyAIPlayer
```

### Network Testing
```bash
# Terminal 1: Start server
java -cp libraries/escampeobf.jar escampe.ServeurJeu 1234 1

# Terminal 2: Start your AI player
java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu io.MyAIPlayer localhost 1234

# Terminal 3: Start opponent
java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234
```


&nbsp;  
&nbsp;  
### Client-Server Message Flow
Network Server (escampe.ServeurJeu); `java -cp libraries/escampeobf.jar escampe.ServeurJeu <PORT> <NUM_GAMES>`  

Network Client (io.ClientJeu): `java -cp escampe/build/classes/java/main;libraries/escampeobf.jar io.ClientJeu <PLAYER_CLASS> <HOST> <PORT>`  

#### 1. **Connection Establishment**
```
Player sends:
  <team_name>    (binoName())

Server responds:
  "Blanc" or "Noir"    (determines player color)
```

#### 2. **Game Loop Messages**
Server sends one of three message types:
**A) Request to Play**
```
Message: "JOUEUR <BLANC|NOIR>"
Our code:
  - Calls choixMouvement() on your IJoueur
  - Sends back move: "A1-B2" or "E"
```

**B) Opponent's Move Notification**
```
Message: "MOUVEMENT <MOVE>"
Our code:
  - Calls mouvementEnnemi(moveStr) to update internal state
  - Example: "MOUVEMENT A1-B2"
```

**C) Game Over**
```
Message: "FIN! <BLANC|NOIR>"
Our code:
  - Calls declareLeVainqueur(color) to handle end-game
  - Color = -1 (Blanc) or 1 (Noir) or 0 (draw?)
```
