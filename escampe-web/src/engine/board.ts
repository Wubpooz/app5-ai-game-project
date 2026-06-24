import type { Board, Cell, GameState, GamePhase, PlacementMove, Side } from './types';
import { LISERE_MAP, BOARD_SIZE, PIECE_INDEX, ZOBRIST_PIECES, ZOBRIST_SIDE, ZOBRIST_BAND, isWhite, isBlack, isEmpty } from './constants';

export function createEmptyBoard(): Board {
  return Array.from({ length: BOARD_SIZE }, () => Array(BOARD_SIZE).fill('-') as Cell[]);
}

export function createInitialGameState(): GameState {
  return {
    board: createEmptyBoard(),
    phase: 'placement',
    currentSide: 'white',
    requiredBand: null,
    moveHistory: [],
    whitePlaced: false,
    blackPlaced: false,
    winner: null,
    moveCount: 0,
    hash: computeHash(createEmptyBoard(), 'white', null),
  };
}

export function computeHash(board: Board, side: Side, requiredBand: number | null): bigint {
  let h = 0n;
  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      const cell = board[r][c];
      if (cell !== '-') {
        const pi = PIECE_INDEX[cell];
        h ^= ZOBRIST_PIECES[pi][r][c];
      }
    }
  }
  if (side === 'black') h ^= ZOBRIST_SIDE;
  if (requiredBand !== null) h ^= ZOBRIST_BAND[requiredBand];
  return h;
}

export function applyPlacement(state: GameState, placement: PlacementMove): GameState {
  const board = state.board.map(row => [...row]) as Board;
  const side = state.currentSide;
  const [unicornSq, ...paladinSqs] = placement.squares;

  board[unicornSq.row][unicornSq.col] = side === 'white' ? 'B' : 'N';
  for (const sq of paladinSqs) {
    board[sq.row][sq.col] = side === 'white' ? 'b' : 'n';
  }

  const whitePlaced = state.whitePlaced || side === 'white';
  const blackPlaced = state.blackPlaced || side === 'black';
  const phase: GamePhase = whitePlaced && blackPlaced ? 'playing' : 'placement';
  const nextSide: Side = side === 'white' ? 'black' : 'white';

  return {
    ...state,
    board,
    phase,
    currentSide: nextSide,
    whitePlaced,
    blackPlaced,
    hash: computeHash(board, nextSide, null),
  };
}

export function applyMove(state: GameState, from: { row: number; col: number }, to: { row: number; col: number }): GameState {
  const board = state.board.map(row => [...row]) as Board;
  const piece = board[from.row][from.col];
  board[to.row][to.col] = piece;
  board[from.row][from.col] = '-';

  // Check if unicorn was captured (should not happen in normal play, but safety check)
  const winner = detectWinner(board);
  const phase: GamePhase = winner ? 'finished' : 'playing';

  const landedLisere = LISERE_MAP[to.row][to.col];
  const nextSide: Side = state.currentSide === 'white' ? 'black' : 'white';

  const newState: GameState = {
    ...state,
    board,
    phase,
    currentSide: nextSide,
    requiredBand: landedLisere,
    winner,
    moveCount: state.moveCount + 1,
    hash: computeHash(board, nextSide, landedLisere),
  };

  return newState;
}

export function applyPass(state: GameState): GameState {
  const nextSide: Side = state.currentSide === 'white' ? 'black' : 'white';
  return {
    ...state,
    currentSide: nextSide,
    // Keep requiredBand since pass doesn't change the last landing
    moveCount: state.moveCount + 1,
    hash: computeHash(state.board, nextSide, state.requiredBand),
  };
}

function detectWinner(board: Board): Side | null {
  let whiteUnicorn = false;
  let blackUnicorn = false;
  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      if (board[r][c] === 'B') whiteUnicorn = true;
      if (board[r][c] === 'N') blackUnicorn = true;
    }
  }
  if (!blackUnicorn) return 'white';
  if (!whiteUnicorn) return 'black';
  return null;
}

export function cloneState(state: GameState): GameState {
  return {
    ...state,
    board: state.board.map(row => [...row]) as Board,
    moveHistory: [...state.moveHistory],
  };
}

export function squareToAlgebraic(sq: { row: number; col: number }): string {
  return String.fromCharCode(65 + sq.col) + (sq.row + 1);
}

export function algebraicToSquare(alg: string): { row: number; col: number } {
  const col = alg.charCodeAt(0) - 65;
  const row = parseInt(alg[1]) - 1;
  return { row, col };
}

export function placementToString(placement: PlacementMove): string {
  return placement.squares.map(squareToAlgebraic).join('/');
}

export function stringToPlacement(s: string): PlacementMove {
  const squares = s.split('/').map(algebraicToSquare);
  return { squares };
}
