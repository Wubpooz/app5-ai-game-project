## **Player Interface Mapping**

**Obfuscated:** `escampe.e` → **Your source:** `io.IJoueur`

```
Obfuscated Method          → Actual Method
───────────────────────────────────────────────────────
void a(int)               → void initJoueur(int mycolour)
int a()                   → int getNumJoueur()
String b()                → String choixMouvement()
void b(int)               → void declareLeVainqueur(int colour)
void a(String)            → void mouvementEnnemi(String coup)
String c()                → String binoName()
```

## **Other Key Classes**

| Obfuscated | Your Source | Purpose |
|-----------|-----------|---------|
| `escampe.Solo` | `io.Solo` | Local game runner (for debugging without network) |
| `escampe.ClientJeu` | `io.ClientJeu` | Network client that connects to game server |
| `escampe.Applet` | `io.Applet` | GUI board display |
| `escampe.JoueurAleatoire` | — | Reference random player (for testing) |
| `escampe.JoueurHumain` | — | Human player with GUI input |
| `escampe.ServeurJeu` | — | Game server (runs tournament/matches) |

## **To Create Your AI Player:**

1. **Implement `IJoueur` interface** in your own class (e.g., `MyAIPlayer.java`)
2. **Implement all 6 methods**, especially:
   - `choixMouvement()` - returns your AI's move as a string like `"A1-B2"` or `"E"`
   - `mouvementEnnemi()` - updates your internal board state when opponent moves
3. **Launch with:**
   ```bash
   java -cp escampe/build/classes/java/main;libraries/escampeobf.jar io.ClientJeu mypackage.MyAIPlayer localhost 1234
   ```
