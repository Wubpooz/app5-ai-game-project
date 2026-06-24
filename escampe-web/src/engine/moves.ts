import type { GameState, Move, Square } from './types';
import { LISERE_MAP, BOARD_SIZE, DIRECTIONS, isWhite, isBlack, isEmpty } from './constants';
import { PASS_MOVE } from './types';

export function getLegalMoves(state: GameState): Move[] {
  if (state.phase !== 'playing') return [];
  if (state.phase === 'finished' as string) return [];

  const { board, currentSide, requiredBand } = state;
  const moves: Move[] = [];

  const isCurrentSide = currentSide === 'white' ? isWhite : isBlack;

  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      const cell = board[r][c];
      if (!isCurrentSide(cell)) continue;

      // Check band constraint
      if (requiredBand !== null && LISERE_MAP[r][c] !== requiredBand) continue;

      const stepsRequired = LISERE_MAP[r][c];
      const fromSq: Square = { row: r, col: c };
      const movingPiece = board[r][c];
      const movingIsUnicorn = movingPiece === 'B' || movingPiece === 'N';

      // DFS to find all paths of exactly stepsRequired steps
      const visited = Array.from({ length: BOARD_SIZE }, () => Array(BOARD_SIZE).fill(false));
      visited[r][c] = true;

      const path: Square[] = [fromSq];

      generatePaths(board, visited, path, fromSq, stepsRequired, 0, moves, isCurrentSide, currentSide, movingIsUnicorn);
    }
  }

  // If no moves, must pass
  if (moves.length === 0) {
    moves.push({ ...PASS_MOVE });
  }

  return moves;
}

function generatePaths(
  board: string[][],
  visited: boolean[][],
  path: Square[],
  current: Square,
  totalSteps: number,
  stepsTaken: number,
  result: Move[],
  isCurrentSide: (c: string) => boolean,
  currentSide: string,
  movingIsUnicorn: boolean,
): void {
  if (stepsTaken === totalSteps) {
    // Valid destination reached
    const from = path[0];
    const to = path[path.length - 1];
    result.push({
      from,
      to,
      path: [...path],
      isPass: false,
    });
    return;
  }

  for (const [dr, dc] of DIRECTIONS) {
    const nr = current.row + dr;
    const nc = current.col + dc;
    if (nr < 0 || nr >= BOARD_SIZE || nc < 0 || nc >= BOARD_SIZE) continue;
    if (visited[nr][nc]) continue;

    const cell = board[nr][nc];
    const isLastStep = stepsTaken === totalSteps - 1;

    if (isLastStep) {
      // Last step: can land if empty, or if we are a Paladin and target is opponent's Unicorn
      const isOpponentUnicorn = (cell === 'B' || cell === 'N') && !isCurrentSide(cell);
      const canCapture = !movingIsUnicorn && isOpponentUnicorn;
      const canLand = cell === '-' || canCapture;
      if (!canLand) continue;
    } else {
      // Intermediate step: must be empty
      if (cell !== '-') continue;
    }

    visited[nr][nc] = true;
    path.push({ row: nr, col: nc });

    generatePaths(board, visited, path, { row: nr, col: nc }, totalSteps, stepsTaken + 1, result, isCurrentSide, currentSide, movingIsUnicorn);

    path.pop();
    visited[nr][nc] = false;
  }
}

export function countLegalMoves(state: GameState): number {
  return getLegalMoves(state).length;
}

export function isLegalMove(state: GameState, from: Square, to: Square): boolean {
  const moves = getLegalMoves(state);
  return moves.some(m => !m.isPass && m.from.row === from.row && m.from.col === from.col &&
    m.to.row === to.row && m.to.col === to.col);
}

export function findMove(state: GameState, from: Square, to: Square): Move | null {
  const moves = getLegalMoves(state);
  return moves.find(m => !m.isPass && m.from.row === from.row && m.from.col === from.col &&
    m.to.row === to.row && m.to.col === to.col) ?? null;
}
