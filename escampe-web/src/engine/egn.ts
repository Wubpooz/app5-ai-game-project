import type { GameState } from './types';

// EGN: records the full game as a series of moves
// Format: [whitePlacement] [blackPlacement] move1 move2 ...
// Placement: "A1/B1/C1/D1/E1/F1"
// Move: "A1-B2" or "E" (pass)

export interface EGNGame {
  whitePlacement: string;
  blackPlacement: string;
  moves: string[];
  result: 'white' | 'black' | 'draw' | '*';
}

export function egnToString(game: EGNGame): string {
  const moves = game.moves.map((m, i) => {
    if (i % 2 === 0) return `${Math.floor(i / 2) + 1}. ${m}`;
    return m;
  }).join(' ');
  return `${game.whitePlacement} ${game.blackPlacement} ${moves} ${game.result}`;
}

export function parseEGN(egn: string): EGNGame {
  const tokens = egn.trim().split(/\s+/);
  let idx = 0;

  const whitePlacement = tokens[idx++] ?? '';
  const blackPlacement = tokens[idx++] ?? '';
  const moves: string[] = [];
  let result: 'white' | 'black' | 'draw' | '*' = '*';

  while (idx < tokens.length) {
    const tok = tokens[idx++];
    if (!tok) continue;
    if (/^\d+\./.test(tok)) continue; // move numbers
    if (tok === 'white' || tok === 'black' || tok === 'draw' || tok === '*') {
      result = tok as 'white' | 'black' | 'draw' | '*';
    } else {
      moves.push(tok);
    }
  }

  return { whitePlacement, blackPlacement, moves, result };
}
