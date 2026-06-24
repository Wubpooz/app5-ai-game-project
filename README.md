# Escampe AI
> Escampe AI is a class project to conceive and build an AI capable of playing the Escampe game.  

&nbsp;  

**Table of Contents**
- [Escampe AI](#escampe-ai)
  - [Game Rules](#game-rules)
  - [Versioning plan](#versioning-plan)
  - [Releases](#releases)
  - [References](#references)

---

&nbsp;  
## Game Rules
This is a 2-player game. Each player has 5 paladins and 1 unicorn, either white or black.  
The goal is to capture the opponent's unicorn by moving a paladin to its position.  
The pieces are placed on a 6 by 6 grid. Each circle of the grid is either a simple, double, or triple-banded circle.  
![game board](docs/images/board.png)

The game starts with the black player choosing a side (top or bottom) of the board and placing their pieces in the first two rows as he pleases. The white player places their pieces and starts the game.

Each turn, a player chooses one of their pieces that is sitting on a band count equal to the number on the ending position of the opponent's previous turn.  
The pieces move in straight lines (like a rook in chess) by a number of circles equal to the band of the circle on which the piece is currently standing. No piece can "jump" over another one or pass through the same circle twice in a turn. The paladins can't be captured.    
If a player can't move any of their pieces, they pass their turn and the opponent can move any of their pieces on the next turn.  


---

&nbsp;  




## Versioning plan
- Model versions: Use semantic experiment IDs like `v0.1-inputs`, `v0.2-loss`, `v0.3-bootstrap1`, `v0.4-se`, `v0.5-dwconv`, `v0.6-q8`, `v0.7-distilled`. A simple naming convention could be:  
  - `banddper-v0.1-base`
  - `banddper-v0.2-extra-channels`
  - `banddper-v0.3-weighted-loss`
  - `banddper-v0.4-bootstrap-round1`
  - `banddper-v0.5-se`
  - `banddper-v0.6-dwconv`
  - `banddper-v0.7-int8`
  - `banddper-v0.8-distilled`
- Checkpoints: Save `model.pt`, `config.json`, `metrics.json`, `train_manifest.json` together
- Paper versions: Match paper revision to architecture revision, e.g. `paper-r3` corresponding to `model-v0.4`
- Changelog: Maintain a short changelog with architecture changes, training data changes, and benchmark changes

## Releases

We publish runnable releases as tagged versions. The repository includes a GitHub Actions workflow that builds a fat JAR for the `escampe` Java project and publishes it as a release asset whenever a tag matching `v*` is pushed.

Quick steps to create a release:

```bash
# update CHANGELOG.md and commit
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

The workflow will run and attach the jar to the GitHub release.


---

&nbsp;  
## References
- [Escampe Game Rules](http://jeuxstrategieter.free.fr/Escampe_complet.php)
- [Mana (2nd version de Escampe)](https://fr.wikipedia.org/wiki/Mana_(jeu))
- [Mana Rules](https://regle.escaleajeux.fr/mana__rg.pdf)
- [I Solved Connect 4 - 2swap](https://youtube.com/watch?v=KaljD3Q3ct0)
- [I Improved the Strongest Chess AI | My Best Idea Yet - Daniel Monroe](https://youtube.com/watch?v=geHcAS1fFg8)
- [NNUE](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html)
- [AlphaZero Chess](https://arxiv.org/abs/1712.01815)
- [AlphaZero Network Architecture](https://www.chessprogramming.org/AlphaZero#Network_Architecture)
- [MuZero](https://arxiv.org/abs/1911.08265)
- [NN-SVG](https://alexlenail.me/NN-SVG/LeNet.html)
- [I Ran a Chess Programming Tournament, Here's How it Went!](https://youtu.be/Ne40a5LkK6A?si=YQ0QKJNYBpj5fA3l)