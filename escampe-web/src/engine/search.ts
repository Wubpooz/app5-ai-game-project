import type { GameState, Move, SearchResult, Side } from './types';
import { EVAL_WEIGHTS, BOT_LEVELS } from './constants';
import { getLegalMoves } from './moves';
import { applyMove, applyPass } from './board';
import { evaluate, evaluateSimple } from './evaluation';

const TT_SIZE = 1 << 20; // 1M entries

interface TTEntry {
  hash: bigint;
  depth: number;
  score: number;
  flag: 'exact' | 'lower' | 'upper';
  bestMove: Move | null;
}

const transpositionTable: (TTEntry | null)[] = new Array(TT_SIZE).fill(null);

let killerMoves: (Move | null)[][] = [];
let historyTable: number[][][][] = []; // [from_row][from_col][to_row][to_col]
let nodesSearched = 0;
let stopSearch = false;

export function search(
  state: GameState,
  level: number,
  timeLimitMs: number,
  onProgress?: (depth: number, score: number, pv: Move[]) => void,
): SearchResult {
  const config = BOT_LEVELS[level - 1];
  stopSearch = false;
  nodesSearched = 0;

  // Handle randomness for lower levels
  const moves = getLegalMoves(state);
  if (moves.length === 0 || (moves.length === 1 && moves[0].isPass)) {
    return { move: moves[0] ?? null, score: 0, depth: 0, pv: [], nodesSearched: 0, timeMs: 0 };
  }

  if (config.randomness > 0 && Math.random() < config.randomness) {
    const randomMove = moves[Math.floor(Math.random() * moves.length)];
    return { move: randomMove, score: 0, depth: 0, pv: [], nodesSearched: 1, timeMs: 0 };
  }

  const startTime = Date.now();
  const maxDepth = config.maxDepth;
  const useKM = config.useKillerMoves;
  const useHH = config.useHistoryHeuristic;
  const useTT = config.useTranspositionTable;
  const evalFn = config.useFullEval ? evaluate : evaluateSimple;

  // Initialize tables
  if (useKM) killerMoves = Array.from({ length: maxDepth + 1 }, () => [null, null]);
  if (useHH) historyTable = Array.from({ length: 6 }, () =>
    Array.from({ length: 6 }, () =>
      Array.from({ length: 6 }, () => Array(6).fill(0))
    )
  );

  let bestMove: Move | null = null;
  let bestScore = -Infinity;
  let bestPV: Move[] = [];

  // Iterative deepening
  for (let depth = 1; depth <= maxDepth; depth++) {
    if (Date.now() - startTime > timeLimitMs) break;
    if (stopSearch) break;

    const pv: Move[] = [];
    const score = negamax(state, depth, -EVAL_WEIGHTS.WIN * 2, EVAL_WEIGHTS.WIN * 2, state.currentSide, pv, evalFn, useKM, useHH, useTT, startTime, timeLimitMs);

    if (!stopSearch && pv.length > 0) {
      bestMove = pv[0];
      bestScore = score;
      bestPV = pv;
      onProgress?.(depth, score, pv);
    }
  }

  return {
    move: bestMove,
    score: bestScore,
    depth: maxDepth,
    pv: bestPV.map(m => m.isPass ? 'E' : `${String.fromCharCode(65 + m.from.col)}${m.from.row + 1}-${String.fromCharCode(65 + m.to.col)}${m.to.row + 1}`),
    nodesSearched,
    timeMs: Date.now() - startTime,
  };
}

function negamax(
  state: GameState,
  depth: number,
  alpha: number,
  beta: number,
  perspective: Side,
  pv: Move[],
  evalFn: (state: GameState, side: Side) => number,
  useKM: boolean,
  useHH: boolean,
  useTT: boolean,
  startTime: number,
  timeLimitMs: number,
): number {
  nodesSearched++;

  if (Date.now() - startTime > timeLimitMs) {
    stopSearch = true;
    return evalFn(state, perspective);
  }

  if (state.phase === 'finished') {
    return state.winner === perspective ? EVAL_WEIGHTS.WIN : EVAL_WEIGHTS.LOSS;
  }

  // TT lookup
  if (useTT) {
    const ttIdx = Number(state.hash % BigInt(TT_SIZE));
    const ttEntry = transpositionTable[ttIdx];
    if (ttEntry && ttEntry.hash === state.hash && ttEntry.depth >= depth) {
      if (ttEntry.flag === 'exact') { pv.push(ttEntry.bestMove!); return ttEntry.score; }
      if (ttEntry.flag === 'lower') alpha = Math.max(alpha, ttEntry.score);
      if (ttEntry.flag === 'upper') beta = Math.min(beta, ttEntry.score);
      if (alpha >= beta) { pv.push(ttEntry.bestMove!); return ttEntry.score; }
    }
  }

  if (depth === 0) {
    return evalFn(state, perspective);
  }

  const moves = getLegalMoves(state);

  // Move ordering
  const orderedMoves = orderMoves(moves, depth, useKM, useHH);

  let bestScore = -Infinity;
  let bestMove: Move | null = null;
  let ttFlag: 'exact' | 'lower' | 'upper' = 'upper';
  const childPV: Move[] = [];

  for (const move of orderedMoves) {
    if (stopSearch) break;

    const nextState = move.isPass
      ? applyPass(state)
      : applyMove(state, move.from, move.to);

    // Negate because we're switching perspectives
    const score = -negamax(nextState, depth - 1, -beta, -alpha, perspective === 'white' ? 'black' : 'white', childPV, evalFn, useKM, useHH, useTT, startTime, timeLimitMs);

    if (score > bestScore) {
      bestScore = score;
      bestMove = move;
      pv.length = 0;
      pv.push(move, ...childPV);
    }
    childPV.length = 0;

    if (score > alpha) {
      alpha = score;
      ttFlag = 'exact';
    }

    if (alpha >= beta) {
      // Beta cutoff
      if (useKM && killerMoves[depth]) {
        killerMoves[depth][1] = killerMoves[depth][0];
        killerMoves[depth][0] = move;
      }
      if (useHH && !move.isPass) {
        historyTable[move.from.row][move.from.col][move.to.row][move.to.col] += depth * depth;
      }
      ttFlag = 'lower';
      break;
    }
  }

  // Store in TT
  if (useTT && bestMove && !stopSearch) {
    const ttIdx = Number(state.hash % BigInt(TT_SIZE));
    transpositionTable[ttIdx] = {
      hash: state.hash,
      depth,
      score: bestScore,
      flag: ttFlag,
      bestMove,
    };
  }

  return bestScore;
}

function orderMoves(moves: Move[], depth: number, useKM: boolean, useHH: boolean): Move[] {
  if (!useKM && !useHH) return moves;

  return [...moves].sort((a, b) => {
    if (a.isPass) return 1;
    if (b.isPass) return -1;

    // Killer move bonus
    if (useKM && killerMoves[depth]) {
      const aIsKiller = killerMoves[depth].some(
        k => k && !k.isPass && k.from.row === a.from.row && k.from.col === a.from.col && k.to.row === a.to.row && k.to.col === a.to.col
      );
      const bIsKiller = killerMoves[depth].some(
        k => k && !k.isPass && k.from.row === b.from.row && k.from.col === b.from.col && k.to.row === b.to.row && k.to.col === b.to.col
      );
      if (aIsKiller && !bIsKiller) return -1;
      if (!aIsKiller && bIsKiller) return 1;
    }

    // History heuristic
    if (useHH) {
      const aH = historyTable[a.from.row][a.from.col][a.to.row][a.to.col];
      const bH = historyTable[b.from.row][b.from.col][b.to.row][b.to.col];
      return bH - aH;
    }

    return 0;
  });
}

export function stopSearching(): void {
  stopSearch = true;
}

export function clearTranspositionTable(): void {
  transpositionTable.fill(null);
}
