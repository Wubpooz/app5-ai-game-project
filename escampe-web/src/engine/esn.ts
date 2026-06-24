import type { Move, Square } from './types';
import { squareToAlgebraic, algebraicToSquare } from './board';

// ESN format: "P c2>d2>d3" (Paladin from c2, through d2, to d3)
// or "L c2>d2>d3>d4" for Licorne (Unicorn)
// Pass: "E"

export function moveToESN(move: Move, pieceChar: string): string {
  if (move.isPass) return 'E';
  const prefix = pieceChar === 'B' || pieceChar === 'N' ? 'L' : 'P';
  const path = move.path.map(sq => squareToAlgebraic(sq).toLowerCase()).join('>');
  return `${prefix} ${path}`;
}

export function esnToMove(esn: string): { from: Square; to: Square; path: Square[] } | null {
  if (esn === 'E' || esn === 'e') return null; // pass
  const parts = esn.trim().split(' ');
  if (parts.length < 2) return null;
  const squares = parts[1].split('>').map(s => algebraicToSquare(s.toUpperCase()));
  if (squares.length < 2) return null;
  return {
    from: squares[0],
    to: squares[squares.length - 1],
    path: squares,
  };
}
