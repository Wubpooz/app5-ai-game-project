# Escampe AI
> Escampe AI is a class project to conceive and build an AI capable of playing the Escampe game.  

&nbsp;  

**Table of Contents**
- [Escampe AI](#escampe-ai)
  - [Game Rules](#game-rules)
  - [Analysis](#analysis)
  - [References](#references)

---

&nbsp;  
## Game Rules
This is a 2 player game. Each payer has 4 paladins and 1 unicorn either white or black.  
The goals is to capture the opponent's unicorn by moving a paladin to its position.  
The pieces are placed on a 6 by 6 grid. Each circle of the grid is either a simple, double or tripple banded circle.  
![alt text](image.png)

The game starts with the black player choosing a side (top or bottom) of the board and placing their pieces in the first two rows as he pleases. The white player places their and starts the game.

Each turn, a player chooses one of their pieces from those who are sitting on the same band count as the number on the ending position of the piece of the last opponent's turn.  
The pieces moves in straigh lines (like a rook in chess) of the number of circles corresponding to the band of the circle on which the piece is currently standing. No piece can "jump" over another one or pass twice by the same circle in a turn. The paladins can't be captured.    
If a player can't move any of their pieces, they pass their turn and the opponent can move any of their pieces on the next turn.  


&nbsp;  

## Analysis
1. Comment modéliser un état du jeu (plateau et pièces restantes) ? Préciser les avantages/inconvénients de votre représentation.
2. Comment d éterminer si une configuration correspond à une fin de partie ?
3. Essayez d’identifier les paramètres source de difficulté dans ce jeu. Quel est le facteur de branchement maximal de ce jeu pour chaque action ?
4. Existe-t-il dans ce jeu des coups imparables, permettant la victoire à coup sûr d’un des joueurs ?
5. Quels sont les critères que vous envisagez de prendre en compte pour concevoir des heuristiques d’estimation de configuration de jeu (donner au moins 3 critères) ?
6. Est-il souhaitable pour ce jeu d’adopter une stratégie particulière en début, milieu ou fin de partie ?
7. Donnez un majorant du nombre de coups dans une partie. Détaillez les techniques que vous comptez mettre en oeuvre pour respecter une contrainte de temps imposée sur la durée totale d’une partie


---

&nbsp;  


## References
- [Escampe Game Rules](http://jeuxstrategieter.free.fr/Escampe_complet.php)