import { create } from 'zustand';
import type { GameState, Move, Square, Side, SearchResult, Cell } from '../engine/types';
import { createInitialGameState, applyMove, applyPass, applyPlacement, squareToAlgebraic, cloneState } from '../engine/board';
import { getLegalMoves, findMove } from '../engine/moves';
import { moveToESN } from '../engine/esn';
import { BOT_LEVELS } from '../engine/constants';
import { getOpeningPlacement } from '../engine/opening-book';
import { evaluate } from '../engine/evaluation';

export type GameMode = 'pass-and-play' | 'vs-bot' | 'bot-vs-bot' | 'online';

export interface MoveRecord {
  move: Move;
  piece: Cell;
  notation: string;
  evalBefore: number;
  evalAfter: number;
  classification?: 'brilliant' | 'great' | 'best' | 'good' | 'book' | 'inaccuracy' | 'mistake' | 'blunder';
  boardBefore: string; // EPN or simple board representation for review
}

interface GameStore {
  // Core game states
  gameState: GameState;
  gameActive: boolean;
  gameMode: GameMode;
  botLevelWhite: number; // 1-10
  botLevelBlack: number; // 1-10
  playerColor: Side | 'random';
  gameId: string | null;
  
  // Game UI states
  selectedSquare: Square | null;
  activePath: Square[];
  legalMovesForSelected: Move[];
  isSearching: boolean;
  isFlipped: boolean;
  isGameOver: boolean;
  gameStatusText: string;
  
  // Clocks
  timeControl: { limitSecs: number; incrementSecs: number } | null;
  clocks: { white: number; black: number };
  clockActive: boolean;

  // Move history with full evaluation details
  moveRecords: MoveRecord[];

  // Review & Navigation
  reviewMode: boolean;
  reviewIndex: number; // current showing move index (-1 = starting pos)
  
  // Sound trigger (state to trigger audio)
  soundEvent: 'move' | 'capture' | 'pass' | 'gameover' | 'placement' | 'tick' | null;

  // Actions
  initGame: (config: {
    mode: GameMode;
    botLevelWhite?: number;
    botLevelBlack?: number;
    playerColor?: Side | 'random';
    timeControl?: { limitSecs: number; incrementSecs: number } | null;
  }) => void;
  
  selectSquare: (sq: Square | null) => void;
  makePlayerMove: (from: Square, to: Square) => void;
  makePass: () => void;
  handlePlacement: (squares: Square[]) => void;
  
  // Bot control
  triggerBotSearch: () => void;
  applyBotMove: (result: SearchResult) => void;
  
  // Review navigation
  setReviewMode: (active: boolean) => void;
  goToMove: (index: number) => void;
  
  // Sound clear
  clearSoundEvent: () => void;
  resignGame: (side: Side) => void;
  triggerDraw: () => void;
}

// Helper to check if a piece is captured on a move
function isCapture(board: Cell[][], move: Move): boolean {
  if (move.isPass) return false;
  const target = board[move.to.row][move.to.col];
  return target !== '-';
}

export const useGameStore = create<GameStore>((set, get) => {
  let botWorker: Worker | null = null;

  const getWorker = (): Worker => {
    if (typeof window === 'undefined') throw new Error('Worker cannot be run on server');
    if (!botWorker) {
      botWorker = new Worker(new URL('../engine/worker.ts', import.meta.url), { type: 'module' });
    }
    return botWorker;
  };

  const terminateWorker = () => {
    if (botWorker) {
      botWorker.terminate();
      botWorker = null;
    }
  };

  return {
    gameState: createInitialGameState(),
    gameActive: false,
    gameMode: 'pass-and-play',
    botLevelWhite: 5,
    botLevelBlack: 5,
    playerColor: 'white',
    gameId: null,

    selectedSquare: null,
    activePath: [],
    legalMovesForSelected: [],
    isSearching: false,
    isFlipped: false,
    isGameOver: false,
    gameStatusText: 'Placement Phase',

    timeControl: null,
    clocks: { white: 600, black: 600 },
    clockActive: false,

    moveRecords: [],
    reviewMode: false,
    reviewIndex: -1,
    soundEvent: null,

    clearSoundEvent: () => set({ soundEvent: null }),

    initGame: (config) => {
      terminateWorker();
      const initial = createInitialGameState();
      const playerColor = config.playerColor === 'random' 
        ? (Math.random() < 0.5 ? 'white' : 'black') 
        : (config.playerColor ?? 'white');

      const isFlipped = playerColor === 'black' && config.mode === 'vs-bot';

      set({
        gameActive: true,
        gameState: initial,
        gameMode: config.mode,
        botLevelWhite: config.botLevelWhite ?? 5,
        botLevelBlack: config.botLevelBlack ?? 5,
        playerColor,
        selectedSquare: null,
        activePath: [],
        legalMovesForSelected: [],
        isSearching: false,
        isFlipped,
        isGameOver: false,
        gameStatusText: 'Placement Phase',
        timeControl: config.timeControl ?? null,
        clocks: {
          white: config.timeControl?.limitSecs ?? 600,
          black: config.timeControl?.limitSecs ?? 600,
        },
        clockActive: false,
        moveRecords: [],
        reviewMode: false,
        reviewIndex: -1,
        soundEvent: null,
      });

      // If Bot vs Bot, or VS Bot (and bot starts first as white), play placement immediately
      const state = get();
      if (state.gameMode === 'bot-vs-bot') {
        setTimeout(() => get().triggerBotSearch(), 200);
      } else if (state.gameMode === 'vs-bot' && state.playerColor === 'black') {
        // Bot is white and must place first
        setTimeout(() => get().triggerBotSearch(), 200);
      }
    },

    selectSquare: (sq) => {
      const { gameState, reviewMode, isSearching, isGameOver } = get();
      if (reviewMode || isSearching || isGameOver) return;
      if (gameState.phase !== 'playing') return;

      if (!sq) {
        set({ selectedSquare: null, activePath: [], legalMovesForSelected: [] });
        return;
      }

      const cell = gameState.board[sq.row][sq.col];
      const isWhitePiece = cell === 'B' || cell === 'b';
      const isBlackPiece = cell === 'N' || cell === 'n';

      // Check correct turn
      if ((gameState.currentSide === 'white' && !isWhitePiece) || 
          (gameState.currentSide === 'black' && !isBlackPiece)) {
        set({ selectedSquare: null, activePath: [], legalMovesForSelected: [] });
        return;
      }

      const legalMoves = getLegalMoves(gameState).filter(
        m => !m.isPass && m.from.row === sq.row && m.from.col === sq.col
      );

      set({
        selectedSquare: sq,
        legalMovesForSelected: legalMoves,
        activePath: [sq],
      });
    },

    makePlayerMove: (from, to) => {
      const { gameState, gameMode, playerColor, isSearching, isGameOver } = get();
      if (isSearching || isGameOver) return;

      // Validate turn in vs-bot mode
      if (gameMode === 'vs-bot' && gameState.currentSide !== playerColor) return;

      const move = findMove(gameState, from, to);
      if (!move) return;

      const piece = gameState.board[from.row][from.col];
      const notation = moveToESN(move, piece);
      
      const evalBefore = evaluate(gameState, 'white');
      const isCap = isCapture(gameState.board, move);
      
      const nextState = applyMove(gameState, from, to);
      const evalAfter = evaluate(nextState, 'white');

      // Setup EGN history move record
      const record: MoveRecord = {
        move,
        piece,
        notation,
        evalBefore,
        evalAfter,
        boardBefore: JSON.stringify(gameState.board),
      };

      // Add to move history strings
      const moveHistory = [...gameState.moveHistory, notation];
      nextState.moveHistory = moveHistory;

      // Increment clock with increment time
      const timeControl = get().timeControl;
      const currentClocks = { ...get().clocks };
      if (timeControl && timeControl.incrementSecs > 0) {
        if (gameState.currentSide === 'white') {
          currentClocks.white += timeControl.incrementSecs;
        } else {
          currentClocks.black += timeControl.incrementSecs;
        }
      }

      set({
        gameState: nextState,
        moveRecords: [...get().moveRecords, record],
        selectedSquare: null,
        legalMovesForSelected: [],
        activePath: [],
        clocks: currentClocks,
        soundEvent: isCap ? 'capture' : 'move',
      });

      // Check game over
      if (nextState.phase === 'finished') {
        const winnerName = nextState.winner === 'white' ? 'White' : 'Black';
        set({
          isGameOver: true,
          clockActive: false,
          gameStatusText: `${winnerName} wins by capture!`,
          soundEvent: 'gameover',
        });
        return;
      }

      // Check if next side has legal moves. If not, auto-pass is enforced
      const nextLegalMoves = getLegalMoves(nextState);
      if (nextLegalMoves.length === 1 && nextLegalMoves[0].isPass) {
        setTimeout(() => get().makePass(), 800);
        return;
      }

      // Start clocks on first playing move
      if (timeControl && !get().clockActive) {
        set({ clockActive: true });
      }

      // Trigger bot if vs-bot
      if (gameMode === 'vs-bot' && nextState.currentSide !== playerColor) {
        setTimeout(() => get().triggerBotSearch(), 400);
      }
    },

    makePass: () => {
      const { gameState, gameMode, playerColor, isSearching, isGameOver } = get();
      if (isSearching || isGameOver) return;

      const legalMoves = getLegalMoves(gameState);
      if (legalMoves.length !== 1 || !legalMoves[0].isPass) return;

      const move = legalMoves[0];
      const notation = 'E';
      
      const evalBefore = evaluate(gameState, 'white');
      const nextState = applyPass(gameState);
      const evalAfter = evaluate(nextState, 'white');

      const record: MoveRecord = {
        move,
        piece: '-',
        notation,
        evalBefore,
        evalAfter,
        boardBefore: JSON.stringify(gameState.board),
      };

      const moveHistory = [...gameState.moveHistory, notation];
      nextState.moveHistory = moveHistory;

      set({
        gameState: nextState,
        moveRecords: [...get().moveRecords, record],
        selectedSquare: null,
        legalMovesForSelected: [],
        activePath: [],
        soundEvent: 'pass',
      });

      // Check if next side has legal moves. If not, double pass ends the game or forces loop
      const nextLegalMoves = getLegalMoves(nextState);
      if (nextLegalMoves.length === 1 && nextLegalMoves[0].isPass) {
        // Double pass logic: usually indicates a draw or deadlock, but game continues in turns
        setTimeout(() => get().makePass(), 800);
        return;
      }

      // Trigger bot if vs-bot
      if (gameMode === 'vs-bot' && nextState.currentSide !== playerColor) {
        setTimeout(() => get().triggerBotSearch(), 400);
      } else if (gameMode === 'bot-vs-bot') {
        setTimeout(() => get().triggerBotSearch(), 400);
      }
    },

    handlePlacement: (squares) => {
      const { gameState, gameMode, playerColor } = get();
      
      // Safety placement verification
      if (gameState.phase !== 'placement') return;
      if (squares.length !== 6) return;

      // Validate turn in vs-bot mode
      if (gameMode === 'vs-bot' && gameState.currentSide !== playerColor) return;

      const placementObj = { squares };
      const notation = squares.map(sq => squareToAlgebraic(sq)).join('/');
      const nextState = applyPlacement(gameState, placementObj);

      const isWhitePlacement = gameState.currentSide === 'white';
      const moveHistory = [...gameState.moveHistory, notation];
      nextState.moveHistory = moveHistory;

      set({
        gameState: nextState,
        soundEvent: 'placement',
        gameStatusText: nextState.phase === 'playing' ? 'Game Started' : 'Black Placement Phase',
      });

      // If now playing phase, start clock if configured
      if (nextState.phase === 'playing') {
        const timeControl = get().timeControl;
        if (timeControl) {
          set({ clockActive: true });
        }
      }

      // Check if bot should move next
      if (nextState.phase === 'placement' && gameMode === 'vs-bot' && nextState.currentSide !== playerColor) {
        setTimeout(() => get().triggerBotSearch(), 500);
      } else if (nextState.phase === 'playing') {
        if (gameMode === 'vs-bot' && nextState.currentSide !== playerColor) {
          setTimeout(() => get().triggerBotSearch(), 500);
        } else if (gameMode === 'bot-vs-bot') {
          setTimeout(() => get().triggerBotSearch(), 500);
        }
      }
    },

    triggerBotSearch: () => {
      const { gameState, isGameOver, gameMode, botLevelWhite, botLevelBlack, playerColor } = get();
      if (isGameOver) return;

      const isBotWhite = gameMode === 'bot-vs-bot' || (gameMode === 'vs-bot' && playerColor === 'black');
      const isBotBlack = gameMode === 'bot-vs-bot' || (gameMode === 'vs-bot' && playerColor === 'white');

      // Check if it's currently a bot's turn
      const currentIsBot = (gameState.currentSide === 'white' && isBotWhite) || 
                           (gameState.currentSide === 'black' && isBotBlack);

      if (!currentIsBot) return;

      set({ isSearching: true });

      // If placement phase, bots use opening book
      if (gameState.phase === 'placement') {
        const side = gameState.currentSide;
        const placementStr = getOpeningPlacement(side, true);
        if (placementStr) {
          setTimeout(() => {
            const placementSqs = placementStr.split('/').map(sqStr => {
              const col = sqStr.charCodeAt(0) - 65;
              const row = parseInt(sqStr[1]) - 1;
              return { row, col };
            });
            
            const placementObj = { squares: placementSqs };
            const nextState = applyPlacement(gameState, placementObj);
            nextState.moveHistory = [...gameState.moveHistory, placementStr];

            set({
              gameState: nextState,
              isSearching: false,
              soundEvent: 'placement',
              gameStatusText: nextState.phase === 'playing' ? 'Game Started' : 'Black Placement Phase',
            });

            // Trigger next turn
            if (nextState.phase === 'playing') {
              if (gameMode === 'vs-bot' && nextState.currentSide !== playerColor) {
                get().triggerBotSearch();
              } else if (gameMode === 'bot-vs-bot') {
                get().triggerBotSearch();
              }
            } else if (nextState.phase === 'placement' && gameMode === 'bot-vs-bot') {
              get().triggerBotSearch();
            }
          }, 600);
        }
        return;
      }

      // Search phase using Web Worker
      const level = gameState.currentSide === 'white' ? botLevelWhite : botLevelBlack;
      const config = BOT_LEVELS[level - 1];

      const worker = getWorker();
      
      worker.onmessage = (event) => {
        const res = event.data;
        if (res.type === 'result') {
          set({ isSearching: false });
          get().applyBotMove(res.result);
        } else if (res.type === 'error') {
          console.error('Worker error:', res.message);
          set({ isSearching: false });
        }
      };

      worker.postMessage({
        type: 'search',
        state: gameState,
        maxDepth: config.maxDepth,
        timeLimitMs: config.timeLimitMs,
        level,
      });
    },

    applyBotMove: (result) => {
      const { move } = result;
      if (!move) return;

      const { gameState, gameMode, playerColor } = get();

      if (move.isPass) {
        const notation = 'E';
        const evalBefore = evaluate(gameState, 'white');
        const nextState = applyPass(gameState);
        const evalAfter = evaluate(nextState, 'white');

        const record: MoveRecord = {
          move,
          piece: '-',
          notation,
          evalBefore,
          evalAfter,
          boardBefore: JSON.stringify(gameState.board),
        };

        const moveHistory = [...gameState.moveHistory, notation];
        nextState.moveHistory = moveHistory;

        set({
          gameState: nextState,
          moveRecords: [...get().moveRecords, record],
          soundEvent: 'pass',
        });

        // Trigger next turn check
        const nextLegalMoves = getLegalMoves(nextState);
        if (nextLegalMoves.length === 1 && nextLegalMoves[0].isPass) {
          setTimeout(() => get().makePass(), 800);
          return;
        }

        if (gameMode === 'bot-vs-bot') {
          setTimeout(() => get().triggerBotSearch(), 500);
        }
      } else {
        const from = move.from;
        const to = move.to;
        const piece = gameState.board[from.row][from.col];
        const notation = moveToESN(move, piece);

        const evalBefore = evaluate(gameState, 'white');
        const isCap = isCapture(gameState.board, move);
        
        const nextState = applyMove(gameState, from, to);
        const evalAfter = evaluate(nextState, 'white');

        const record: MoveRecord = {
          move,
          piece,
          notation,
          evalBefore,
          evalAfter,
          boardBefore: JSON.stringify(gameState.board),
        };

        const moveHistory = [...gameState.moveHistory, notation];
        nextState.moveHistory = moveHistory;

        // Increment clock
        const timeControl = get().timeControl;
        const currentClocks = { ...get().clocks };
        if (timeControl && timeControl.incrementSecs > 0) {
          if (gameState.currentSide === 'white') {
            currentClocks.white += timeControl.incrementSecs;
          } else {
            currentClocks.black += timeControl.incrementSecs;
          }
        }

        set({
          gameState: nextState,
          moveRecords: [...get().moveRecords, record],
          clocks: currentClocks,
          soundEvent: isCap ? 'capture' : 'move',
        });

        // Check game over
        if (nextState.phase === 'finished') {
          const winnerName = nextState.winner === 'white' ? 'White' : 'Black';
          set({
            isGameOver: true,
            clockActive: false,
            gameStatusText: `${winnerName} wins by capture!`,
            soundEvent: 'gameover',
          });
          return;
        }

        // Trigger next turn check
        const nextLegalMoves = getLegalMoves(nextState);
        if (nextLegalMoves.length === 1 && nextLegalMoves[0].isPass) {
          setTimeout(() => get().makePass(), 800);
          return;
        }

        if (gameMode === 'bot-vs-bot') {
          setTimeout(() => get().triggerBotSearch(), 500);
        } else if (gameMode === 'vs-bot' && nextState.currentSide === playerColor) {
          // Player's turn now, no bot search needed
        }
      }
    },

    resignGame: (side) => {
      const winner = side === 'white' ? 'black' : 'white';
      const winnerName = winner === 'white' ? 'White' : 'Black';
      set({
        isGameOver: true,
        clockActive: false,
        gameStatusText: `${winnerName} wins by resignation!`,
        soundEvent: 'gameover',
      });
    },

    triggerDraw: () => {
      set({
        isGameOver: true,
        clockActive: false,
        gameStatusText: `Game drawn by agreement!`,
        soundEvent: 'gameover',
      });
    },

    setReviewMode: (active) => {
      const records = get().moveRecords;
      set({
        reviewMode: active,
        reviewIndex: active ? records.length - 1 : -1,
      });
    },

    goToMove: (index) => {
      const records = get().moveRecords;
      if (index < -1 || index >= records.length) return;
      set({ reviewIndex: index });
    },
  };
});
