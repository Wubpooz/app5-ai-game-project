import type { GameState, Side } from './types';
import { LISERE_MAP, BOARD_SIZE, EVAL_WEIGHTS, isWhite, isBlack } from './constants';
import { getLegalMoves } from './moves';

function manhattanDistance(r1: number, c1: number, r2: number, c2: number): number {
  return Math.abs(r1 - r2) + Math.abs(c1 - c2);
}

interface PieceInfo {
  row: number;
  col: number;
  isUnicorn: boolean;
}

function getPieces(board: string[][], side: 'white' | 'black'): PieceInfo[] {
  const pieces: PieceInfo[] = [];
  const check = side === 'white' ? isWhite : isBlack;
  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      if (check(board[r][c])) {
        pieces.push({ row: r, col: c, isUnicorn: board[r][c] === 'B' || board[r][c] === 'N' });
      }
    }
  }
  return pieces;
}

export function evaluate(state: GameState, perspective: Side): number {
  const { board, winner, phase } = state;

  if (phase === 'finished') {
    return winner === perspective ? EVAL_WEIGHTS.WIN : EVAL_WEIGHTS.LOSS;
  }

  const myPieces = getPieces(board as string[][], perspective);
  const oppPieces = getPieces(board as string[][], perspective === 'white' ? 'black' : 'white');

  const myUnicorn = myPieces.find(p => p.isUnicorn);
  const oppUnicorn = oppPieces.find(p => p.isUnicorn);

  if (!myUnicorn) return EVAL_WEIGHTS.LOSS;
  if (!oppUnicorn) return EVAL_WEIGHTS.WIN;

  let score = 0;

  // Distance of opponent attackers to my unicorn
  const myUDists = oppPieces.map(p => manhattanDistance(p.row, p.col, myUnicorn.row, myUnicorn.col));
  const minAtk = Math.min(...myUDists);
  const avgAtk = myUDists.reduce((a, b) => a + b, 0) / myUDists.length;

  // Distance of my pieces to opp unicorn
  const oppUDists = myPieces.map(p => manhattanDistance(p.row, p.col, oppUnicorn.row, oppUnicorn.col));
  const minDef = Math.min(...oppUDists);
  const avgDef = oppUDists.reduce((a, b) => a + b, 0) / oppUDists.length;

  // We want opponent far from our unicorn, and us close to theirs
  score += EVAL_WEIGHTS.dangerMin * minAtk;  // danger: higher is worse for opp
  score += EVAL_WEIGHTS.dangerAvg * avgAtk;
  score -= EVAL_WEIGHTS.minDist * minDef;    // we want small distance to opp
  score -= EVAL_WEIGHTS.avgDist * avgDef;

  // Unicorn escapability: legal moves for our unicorn
  const myUnicornState = { ...state, currentSide: perspective };
  const myUnicornMoves = getLegalMoves(myUnicornState).filter(
    m => !m.isPass && m.from.row === myUnicorn.row && m.from.col === myUnicorn.col
  );
  const oppUnicornState = { ...state, currentSide: perspective === 'white' ? 'black' as const : 'white' as const };
  const oppUnicornMoves = getLegalMoves(oppUnicornState).filter(
    m => !m.isPass && m.from.row === oppUnicorn.row && m.from.col === oppUnicorn.col
  );

  score += EVAL_WEIGHTS.escape * myUnicornMoves.length;
  score -= EVAL_WEIGHTS.escape * oppUnicornMoves.length;

  // Band coverage: count how many liseré classes we cover (prevents forced passes)
  const myBands = new Set(myPieces.map(p => LISERE_MAP[p.row][p.col]));
  const oppBands = new Set(oppPieces.map(p => LISERE_MAP[p.row][p.col]));

  score += EVAL_WEIGHTS.band * myBands.size;
  score -= EVAL_WEIGHTS.oppBand * (3 - oppBands.size); // penalize opponent's missing bands

  // Mobility
  const myMoves = getLegalMoves(state);
  const oppState = { ...state, currentSide: perspective === 'white' ? 'black' as const : 'white' as const, requiredBand: null };
  const oppMoves = getLegalMoves(oppState);
  score += EVAL_WEIGHTS.legal * (myMoves.length - oppMoves.length);

  return score;
}

export function evaluateSimple(state: GameState, perspective: Side): number {
  const { board, winner, phase } = state;
  if (phase === 'finished') {
    return winner === perspective ? EVAL_WEIGHTS.WIN : EVAL_WEIGHTS.LOSS;
  }
  // Simple: count pieces
  let score = 0;
  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      const cell = board[r][c];
      if (cell === 'B' || cell === 'b') score += (cell === 'B' ? 10 : 1) * (perspective === 'white' ? 1 : -1);
      if (cell === 'N' || cell === 'n') score += (cell === 'N' ? 10 : 1) * (perspective === 'black' ? 1 : -1);
    }
  }
  return score;
}
