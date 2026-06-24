import type { BotLevel } from './types';

// The fixed asymmetric liseré pattern for the 6x6 Escampe board
// LISERE_MAP[row][col] = liseré class (1, 2, or 3)
// row 0 = row 1 in chess notation (white back row)
// row 5 = row 6 in chess notation (black back row)
export const LISERE_MAP: number[][] = [
  [1, 2, 2, 3, 1, 2], // Row 1 (white back row)
  [3, 1, 3, 1, 3, 2], // Row 2
  [2, 3, 1, 2, 1, 3], // Row 3
  [2, 1, 3, 2, 3, 1], // Row 4
  [1, 3, 1, 3, 1, 2], // Row 5
  [3, 2, 2, 1, 3, 2], // Row 6 (black back row)
];

export const BOARD_SIZE = 6;

// Directions: up, right, down, left
export const DIRECTIONS = [
  [-1, 0], [0, 1], [1, 0], [0, -1]
] as const;

// Piece identity helpers
export const isWhite = (c: string) => c === 'B' || c === 'b';
export const isBlack = (c: string) => c === 'N' || c === 'n';
export const isUnicorn = (c: string) => c === 'B' || c === 'N';
export const isPaladin = (c: string) => c === 'b' || c === 'n';
export const isEmpty = (c: string) => c === '-';

// 10 Bot difficulty levels
export const BOT_LEVELS: BotLevel[] = [
  {
    level: 1, name: 'Monkey', elo: 400,
    maxDepth: 1, randomness: 0.95,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: false, timeLimitMs: 100,
  },
  {
    level: 2, name: 'Apprentice', elo: 600,
    maxDepth: 1, randomness: 0.50,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: false, timeLimitMs: 200,
  },
  {
    level: 3, name: 'Novice', elo: 800,
    maxDepth: 2, randomness: 0.30,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: false, timeLimitMs: 300,
  },
  {
    level: 4, name: 'Hobbyist', elo: 1000,
    maxDepth: 2, randomness: 0.10,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: false, timeLimitMs: 500,
  },
  {
    level: 5, name: 'Player', elo: 1200,
    maxDepth: 3, randomness: 0,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: true, timeLimitMs: 750,
  },
  {
    level: 6, name: 'Competitor', elo: 1400,
    maxDepth: 4, randomness: 0,
    useKillerMoves: false, useHistoryHeuristic: false,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: true, timeLimitMs: 1000,
  },
  {
    level: 7, name: 'Specialist', elo: 1600,
    maxDepth: 5, randomness: 0,
    useKillerMoves: true, useHistoryHeuristic: true,
    useTranspositionTable: false, useOpeningBook: false,
    useFullEval: true, timeLimitMs: 1500,
  },
  {
    level: 8, name: 'Expert', elo: 1800,
    maxDepth: 6, randomness: 0,
    useKillerMoves: true, useHistoryHeuristic: true,
    useTranspositionTable: true, useOpeningBook: true,
    useFullEval: true, timeLimitMs: 2000,
  },
  {
    level: 9, name: 'Master', elo: 2100,
    maxDepth: 7, randomness: 0,
    useKillerMoves: true, useHistoryHeuristic: true,
    useTranspositionTable: true, useOpeningBook: true,
    useFullEval: true, timeLimitMs: 2500,
  },
  {
    level: 10, name: 'Grandmaster', elo: 2400,
    maxDepth: 8, randomness: 0,
    useKillerMoves: true, useHistoryHeuristic: true,
    useTranspositionTable: true, useOpeningBook: true,
    useFullEval: true, timeLimitMs: 3000,
  },
];

// Zobrist random values for hashing: [piece_index][row][col]
// piece_index: 0=B, 1=b, 2=N, 3=n
// Generated once at module load using a seeded LCG
function lcg(seed: bigint): () => bigint {
  let s = seed;
  return () => {
    s = (s * 6364136223846793005n + 1442695040888963407n) & 0xFFFFFFFFFFFFFFFFn;
    return s;
  };
}

const rng = lcg(123456789n);
export const ZOBRIST_PIECES: bigint[][][] = Array.from({ length: 4 }, () =>
  Array.from({ length: 6 }, () =>
    Array.from({ length: 6 }, () => rng())
  )
);
export const ZOBRIST_SIDE: bigint = rng();
export const ZOBRIST_BAND: bigint[] = [0n, rng(), rng(), rng()]; // index 0 unused

export const PIECE_INDEX: Record<string, number> = { 'B': 0, 'b': 1, 'N': 2, 'n': 3 };

// Evaluation weights ("bandcoverage" config from Java)
export const EVAL_WEIGHTS = {
  minDist: 10,
  avgDist: 2,
  dangerMin: 5,
  dangerAvg: 5,
  escape: 50,
  trapped: 30,
  legal: 10,
  band: 80,
  oppBand: 90,
  WE_PASS_PENALTY: 500,
  PASS_PRESSURE_REWARD: 100,
  WIN: 100000,
  LOSS: -100000,
};
