# Escampe Standards
> v1.0 (2026-09-17) of the Escampe Standards reference document.
> Standards family for the board game Escampe, inspired by chess protocols (PGN, FEN, CECP/XBoard).
> Designed for interoperability between Java engines, TypeScript servers, and TypeScript/web clients.

---

## Overview
| Standard | Full name                 | Chess analog | Role                        |
|----------|---------------------------|--------------|-----------------------------|
| **ESN**  | Escampe Standard Notation | SAN          | Canonical move notation     |
| **EPN**  | Escampe Position Notation | FEN          | Full position snapshot      |
| **EGN**  | Escampe Game Notation     | PGN          | Human-readable game archive |
| **EEI**  | Escampe Engine Interface  | UCI          | Primary engine–GUI protocol |

---

## 1. Game vocabulary
| Term               | Definition                                                                                                                  |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------|
| **Licorne**        | The "royal" piece. One per side. Captured by an opposing paladin to end the game. It can't capture other pieces.            |
| **Paladin**        | The five non-royal pieces per side. Cannot be captured.                                                                     |
| **Liseré class**   | Each square belongs to class `s` (simple=1 step), `d` (double=2 steps), or `t` (triple=3 steps).                            |
| **Required class** | The class of the square the previous move *arrived on*. The current player **must** move a piece that starts on that class. |
| **Pass**           | When no piece of the required class can legally move, the player passes.                                                    |
| **Broken move**    | A multi-step move whose path changes direction at least once.                                                               |


### 1.1 Board coordinates
The board is 6 columns × 6 rows.
```
     a   b    c    d    e    f
  ┌────┬────┬────┬────┬────┬────┐
6 │ t  │ d  │ t  │ d  │ t  │ d  │ ← Black back row (row 6)
  ├────┼────┼────┼────┼────┼────┤
5 │ d  │ s  │ d  │ s  │ d  │ s  │
  ├────┼────┼────┼────┼────┼────┤
4 │ t  │ d  │ t  │ d  │ t  │ d  │
  ├────┼────┼────┼────┼────┼────┤
3 │ d  │ s  │ d  │ s  │ d  │ s  │
  ├────┼────┼────┼────┼────┼────┤
2 │ t  │ d  │ t  │ d  │ t  │ d  │
  ├────┼────┼────┼────┼────┼────┤
1 │ d  │ s  │ d  │ s  │ d  │ s  │ ← White back row (row 1)
  └────┴────┴────┴────┴────┴────┘
```
Coordinate format: column letter `a–f` + row digit `1–6`, e.g. `c4`.


### 1.2 Liseré class map
| Square pattern                                        | Class        |
|-------------------------------------------------------|--------------|
| Column `a,c,e` + even row OR column `b,d,f` + odd row | `t` (triple) |
| Column `a,c,e` + odd row OR column `b,d,f` + even row | `d` (double) |
| … (same as `d` but with `s` offset)                   | `s` (simple) |

Formal rule: `class(col, row) = classTable[(colIndex + row) % 3]` where `colIndex = {a:0, b:1, c:2, d:3, e:4, f:5}` and `classTable = ['t', 'd', 's']`.

---


## 2. ESN — Escampe Standard Notation
### 2.1 Grammar (EBNF)
```ebnf
move       ::= piece_move | pass
piece_move ::= piece_id " " square (">" square)+ capture?
piece_id   ::= "P" | "L"
square     ::= col row
col        ::= "a" | "b" | "c" | "d" | "e" | "f"
row        ::= "1" | "2" | "3" | "4" | "5" | "6"
capture    ::= "xL"
pass       ::= "pass"
```

### 2.2 Rules
1. The full traversal path is mandatory (every intermediate square must appear in the `>` chain).
2. `P` denotes a paladin; `L` denotes the licorne.
3. No square may appear twice in a single move's path.
4. The path may not cross an occupied square.
5. `xL` is appended to the final square when the move captures the opposing licorne.
6. `pass` is the entire move when no legal move exists from the required class.

### 2.3 Examples
| ESN             | Meaning                                       |
|-----------------|-----------------------------------------------|
| `P c2>d2>d3`    | Paladin moves c2→d2→d3 (broken path, 2 steps) |
| `L e5>d5`       | Licorne moves e5→d5 (straight, 1 step)        |
| `P b4>c4>d4xL`  | Paladin captures the opposing licorne at d4   |
| `P a3>a4>a5>a6` | Paladin moves straight north 3 steps          |
| `pass`          | No legal move from the required class         |


---


## 3. EPN — Escampe Position Notation
### 3.1 Grammar (EBNF)
```ebnf
epn      ::= board " " side " " req " " fullmove " " ply
board    ::= row6 "/" row5 "/" row4 "/" row3 "/" row2 "/" row1
row      ::= cell+
cell     ::= piece | empty_run
piece    ::= "p" | "l" | "P" | "L"
empty_run ::= "1".."6"
side     ::= "w" | "b"
req      ::= "s" | "d" | "t" | "*"
fullmove ::= [1-9][0-9]*
ply      ::= [0-9]+
```

### 3.2 Piece encoding
| Symbol | Meaning       |
|--------|---------------|
| `P`    | White paladin |
| `L`    | White licorne |
| `p`    | Black paladin |
| `l`    | Black licorne |
Row 6 (Black's back row) is written first; row 1 (White's back row) is written last.

### 3.3 Examples
Initial setup (example): `p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0`
Mid-game: `2l3/1p1p2/6/2P3/1P2P1/3L2 b t 14 27`

### 3.4 Notes
- `req = *` at game start and immediately after a `pass`.
- `ply` resets to `0` only on licorne capture (which ends the game).


---

## 4. EGN — Escampe Game Notation
### 4.1 Mandatory headers
| Tag               | Example                               |
|-------------------|---------------------------------------|
| `Variant`         | `"Escampe"`                           |
| `InitialPosition` | `"p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0"` |
| `White`           | `"Alice"`                             |
| `Black`           | `"Bob"`                               |
| `ToMove`          | `"White"`                             |
| `Date`            | `"2026.06.17"`                        |
| `Result`          | `"1-0"`                               |

### 4.2 Movetext grammar (EBNF)
```ebnf
movetext   ::= move_pair* result
move_pair  ::= move_number white_move black_move?
move_number ::= [1-9][0-9]* "."
result     ::= "1-0" | "0-1" | "1/2-1/2" | "*"
```
Annotations: `{comment}` inline, `$n` NAG glyphs, `(variation)` sub-lines.

### 4.3 Full example
```pgn
[Variant "Escampe"]
[InitialPosition "p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0"]
[White "Alice"]
[Black "Bob"]
[ToMove "White"]
[Date "2026.06.17"]
[Result "1-0"]

1. P c2>d2>d3 L e5>d5
2. pass {White has no 's'-class piece, forced pass} P b4>c4>d4xL
1-0
```

---

## 5. EEI — Escampe Engine Interface
### 5.1 GUI → Engine commands
| Command     | Syntax                                                     |
|-------------|------------------------------------------------------------|
| `isready`   | `isready`                                                  |
| `setoption` | `setoption name <n> value <v>`                             |
| `newgame`   | `newgame`                                                  |
| `position`  | `position epn <EPN> [moves <ESN>...]`                      |
| `go`        | `go [depth <n>] [movetime <ms>] [wtime <ms>] [btime <ms>]` |
| `stop`      | `stop`                                                     |
| `quit`      | `quit`                                                     |

Other optional commands:
| Command     | Syntax                                                     |
|-------------|------------------------------------------------------------|
| `benchmark` | `benchmark [testSize] [threads] [limit]`                   |
| `speedtest` | `speedtest [threads] [hash (MiB)] [runtime (s)]`           |
| `print`     | `print`                                                    |
| `eval`      | `eval`                                                     |
| `help`      | `help`                                                     |
| `info`      | `info`                                                     |


### 5.2 Engine → GUI responses
| Response     | Syntax                                                              |
|--------------|---------------------------------------------------------------------|
| `id`         | `id name <str>` / `id author <str>`                                 |
| `variant`    | `variant escampe`                                                   |
| `option`     | `option name <n> type <t> default <d> [min <m> max <x>]`            |
| `readyok`    | `readyok`                                                           |
| `info`       | `info depth <n> score cp <n> [nodes <n>] [time <ms>] [pv <ESN>...]` |
| `bestmove`   | `bestmove <ESN>`                                                    |
| `legalmoves` | `legalmoves <ESN>...`                                               |
| `result`     | `result <1-0\|0-1\|1/2-1/2> reason <str>`                           |



### 5.3 Full session
```
ENG → id name EscampeNN v1
ENG → id author Mathieu
ENG → option name Hash type spin default 64 min 1 max 4096
ENG → variant escampe

GUI → isready
ENG → readyok

GUI → newgame
GUI → position epn p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0
GUI → go depth 8

ENG → info depth 1 score cp 5 nodes 320 time 4 pv P c2>d2>d3
ENG → info depth 8 score cp 31 nodes 241000 time 580 pv P c2>d2>d3
ENG → bestmove P c2>d2>d3

GUI → position epn p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0 moves P c2>d2>d3 L e5>d5
GUI → go movetime 3000
ENG → bestmove P b1>b2>b3

ENG → bestmove P b4>c4>d4xL
ENG → result 1-0 reason licorne-captured
```
