import type { Board, Cell, GameState, Side } from './types';
import { computeHash } from './board';

// EPN format: board string + metadata
// Board string: 36 chars, row 6 to row 1, A to F
// B=White Unicorn, b=White Paladin, N=Black Unicorn, n=Black Paladin, -=Empty

export function gameStateToEPN(state: GameState): string {
  const rows: string[] = [];
  for (let r = 5; r >= 0; r--) {
    rows.push(state.board[r].join(''));
  }
  const boardStr = rows.join('/');
  const side = state.currentSide === 'white' ? 'w' : 'b';
  const band = state.requiredBand ?? '-';
  return `${boardStr} ${side} ${band}`;
}

export function epnToGameState(epn: string): Partial<GameState> {
  const parts = epn.trim().split(' ');
  if (parts.length < 1) throw new Error('Invalid EPN');

  const rows = parts[0].split('/');
  const board: Board = Array.from({ length: 6 }, () => Array(6).fill('-') as Cell[]);

  for (let i = 0; i < 6; i++) {
    const row = rows[i];
    const boardRow = 5 - i;
    for (let c = 0; c < 6; c++) {
      board[boardRow][c] = (row[c] ?? '-') as Cell;
    }
  }

  const currentSide: Side = parts[1] === 'b' ? 'black' : 'white';
  const bandStr = parts[2];
  const requiredBand = bandStr && bandStr !== '-' ? parseInt(bandStr) : null;

  return {
    board,
    currentSide,
    requiredBand,
    hash: computeHash(board, currentSide, requiredBand),
    phase: 'playing',
    whitePlaced: true,
    blackPlaced: true,
    winner: null,
    moveCount: 0,
    moveHistory: [],
  };
}
