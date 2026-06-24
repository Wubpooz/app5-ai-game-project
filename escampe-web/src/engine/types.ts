// Board cell values
export type Cell = 'B' | 'b' | 'N' | 'n' | '-';

// 6x6 board: board[row][col], row 0 = row 1, row 5 = row 6
export type Board = Cell[][];

// Game phases
export type GamePhase = 'placement' | 'playing' | 'finished';

// Which side
export type Side = 'white' | 'black';

// A square on the board
export interface Square {
  row: number; // 0-5
  col: number; // 0-5
}

// A single normal move
export interface Move {
  from: Square;
  to: Square;
  path: Square[]; // full path including from and to
  isPass: boolean;
}

// A pass move
export const PASS_MOVE: Move = {
  from: { row: -1, col: -1 },
  to: { row: -1, col: -1 },
  path: [],
  isPass: true,
};

// Placement move: array of 6 squares [unicorn, p1, p2, p3, p4, p5]
export interface PlacementMove {
  squares: Square[];
}

// Game state
export interface GameState {
  board: Board;
  phase: GamePhase;
  currentSide: Side;
  // Which liseré class must move next (1, 2, or 3), or null if no constraint
  requiredBand: number | null;
  // Move history (in internal format)
  moveHistory: string[];
  // Whether white/black have placed
  whitePlaced: boolean;
  blackPlaced: boolean;
  // Winner if finished
  winner: Side | null;
  // Move count
  moveCount: number;
  // Zobrist hash
  hash: bigint;
}

// Search result from AI
export interface SearchResult {
  move: Move | null;
  score: number;
  depth: number;
  pv: string[]; // principal variation
  nodesSearched: number;
  timeMs: number;
}

// Web Worker message types
export type WorkerRequest =
  | { type: 'search'; state: GameState; maxDepth: number; timeLimitMs: number; level: number }
  | { type: 'stop' };

export type WorkerResponse =
  | { type: 'result'; result: SearchResult }
  | { type: 'progress'; depth: number; score: number; pv: string[] }
  | { type: 'error'; message: string };

// Bot difficulty level config
export interface BotLevel {
  level: number;
  name: string;
  elo: number;
  maxDepth: number;
  randomness: number; // 0.0 to 1.0, probability of playing random move
  useKillerMoves: boolean;
  useHistoryHeuristic: boolean;
  useTranspositionTable: boolean;
  useOpeningBook: boolean;
  useFullEval: boolean;
  timeLimitMs: number;
}
