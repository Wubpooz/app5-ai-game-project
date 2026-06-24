import React, { useState, useEffect } from 'react';
import { useGameStore } from '../../stores/game-store';
import { useSettingsStore } from '../../stores/settings-store';
import { Piece } from './Piece';
import { LISERE_MAP, BOARD_SIZE } from '../../engine/constants';
import { getLegalMoves } from '../../engine/moves';
import type { Square, Cell, Move } from '../../engine/types';

export const Board: React.FC = () => {
  // Store state
  const gameState = useGameStore((state) => state.gameState);
  const selectedSquare = useGameStore((state) => state.selectedSquare);
  const legalMovesForSelected = useGameStore((state) => state.legalMovesForSelected);
  const selectSquare = useGameStore((state) => state.selectSquare);
  const makePlayerMove = useGameStore((state) => state.makePlayerMove);
  const handlePlacement = useGameStore((state) => state.handlePlacement);
  const isFlipped = useGameStore((state) => state.isFlipped);
  const reviewMode = useGameStore((state) => state.reviewMode);
  const reviewIndex = useGameStore((state) => state.reviewIndex);
  const moveRecords = useGameStore((state) => state.moveRecords);

  // Settings
  const coordinatesVisible = useSettingsStore((state) => state.coordinatesVisible);
  const boardTheme = useSettingsStore((state) => state.boardTheme);
  const highlightLegal = useSettingsStore((state) => state.highlightLegal);

  // Local placement state
  // White places on rows 0-1 (Row 1-2). Black places on rows 4-5 (Row 5-6).
  const [placementUnicorn, setPlacementUnicorn] = useState<Square | null>(null);
  const [placementPaladins, setPlacementPaladins] = useState<Square[]>([]);
  const [selectedPlacementType, setSelectedPlacementType] = useState<'unicorn' | 'paladin'>('unicorn');

  // Reset local placement when game state changes phase or resets
  useEffect(() => {
    if (gameState.phase === 'placement') {
      setPlacementUnicorn(null);
      setPlacementPaladins([]);
    }
  }, [gameState.phase, gameState.currentSide]);

  // Determine current active board to show (if in review mode, show historical board)
  const currentBoard: Cell[][] = reviewMode && reviewIndex >= 0 && moveRecords[reviewIndex]
    ? JSON.parse(moveRecords[reviewIndex].boardBefore)
    : gameState.board;

  // Get all starting squares that have legal moves in the current turn
  const legalFromSquares = !reviewMode && gameState.phase === 'playing'
    ? getLegalMoves(gameState).map(m => `${m.from.row}-${m.from.col}`)
    : [];

  // Last move highlights
  let lastMove: Move | null = null;
  if (reviewMode && reviewIndex >= 0 && moveRecords[reviewIndex]) {
    lastMove = moveRecords[reviewIndex].move;
  } else if (moveRecords.length > 0) {
    lastMove = moveRecords[moveRecords.length - 1].move;
  }

  // Row and Column arrays depending on flip
  const rowIndices = isFlipped 
    ? Array.from({ length: BOARD_SIZE }, (_, i) => i) // 0 to 5
    : Array.from({ length: BOARD_SIZE }, (_, i) => BOARD_SIZE - 1 - i); // 5 down to 0

  const colIndices = isFlipped
    ? Array.from({ length: BOARD_SIZE }, (_, i) => BOARD_SIZE - 1 - i) // 5 down to 0
    : Array.from({ length: BOARD_SIZE }, (_, i) => i); // 0 to 5

  const handleSquareClick = (row: number, col: number) => {
    if (reviewMode) return;

    if (gameState.phase === 'placement') {
      handlePlacementClick(row, col);
      return;
    }

    const cell = gameState.board[row][col];
    const isWhitePiece = cell === 'B' || cell === 'b';
    const isBlackPiece = cell === 'N' || cell === 'n';
    
    // Check if the clicked square contains a piece of the current player
    const isOwnPiece = (gameState.currentSide === 'white' && isWhitePiece) || 
                       (gameState.currentSide === 'black' && isBlackPiece);

    if (selectedSquare) {
      if (selectedSquare.row === row && selectedSquare.col === col) {
        // Clicking the selected square deselects it
        selectSquare(null);
      } else if (isOwnPiece) {
        // Clicking another of own pieces selects it instead of moving
        selectSquare({ row, col });
      } else {
        // Clicking an empty square or opponent piece attempts a move
        makePlayerMove(selectedSquare, { row, col });
      }
    } else {
      if (isOwnPiece) {
        selectSquare({ row, col });
      }
    }
  };

  const handlePlacementClick = (row: number, col: number) => {
    const side = gameState.currentSide;
    // Placement limits: White rows 0-1, Black rows 4-5
    const isRowAllowed = side === 'white' 
      ? (row === 0 || row === 1) 
      : (row === 4 || row === 5);

    if (!isRowAllowed) return;

    const isUnicornClick = placementUnicorn && placementUnicorn.row === row && placementUnicorn.col === col;
    const paladinIdx = placementPaladins.findIndex(sq => sq.row === row && sq.col === col);

    if (selectedPlacementType === 'unicorn') {
      if (isUnicornClick) {
        setPlacementUnicorn(null);
      } else {
        setPlacementUnicorn({ row, col });
        if (paladinIdx !== -1) {
          const updated = [...placementPaladins];
          updated.splice(paladinIdx, 1);
          setPlacementPaladins(updated);
        }
        setSelectedPlacementType('paladin'); // switch to paladin for convenience
      }
    } else {
      // Placing paladin
      if (paladinIdx !== -1) {
        const updated = [...placementPaladins];
        updated.splice(paladinIdx, 1);
        setPlacementPaladins(updated);
      } else {
        if (isUnicornClick) {
          setPlacementUnicorn(null);
        }
        if (placementPaladins.length < 5) {
          setPlacementPaladins([...placementPaladins, { row, col }]);
        }
      }
    }
  };

  const confirmPlacement = () => {
    if (!placementUnicorn || placementPaladins.length !== 5) return;
    handlePlacement([placementUnicorn, ...placementPaladins]);
  };

  // Helper to check if a square is highlighted as legal target
  const isLegalTarget = (row: number, col: number) => {
    return highlightLegal && legalMovesForSelected.some(
      (m) => m.to.row === row && m.to.col === col
    );
  };

  // Helper to check if a square is part of last move
  const isLastMoveSquare = (row: number, col: number) => {
    if (!lastMove || lastMove.isPass) return false;
    return (
      (lastMove.from.row === row && lastMove.from.col === col) ||
      (lastMove.to.row === row && lastMove.to.col === col)
    );
  };

  // Helper to get placement cell rendering info
  const getPlacementPieceType = (row: number, col: number): 'B' | 'b' | 'N' | 'n' | null => {
    const side = gameState.currentSide;
    if (placementUnicorn && placementUnicorn.row === row && placementUnicorn.col === col) {
      return side === 'white' ? 'B' : 'N';
    }
    const idx = placementPaladins.findIndex(sq => sq.row === row && sq.col === col);
    if (idx !== -1) {
      return side === 'white' ? 'b' : 'n';
    }
    return null;
  };

  // Board background styles based on theme
  const getBoardBgStyle = () => {
    switch (boardTheme) {
      case 'wood':
        return {
          background: '#5c3d10',
          border: '12px solid #3d2508',
          boxShadow: 'var(--shadow-lg), 0 0 40px rgba(0, 0, 0, 0.6)',
        };
      case 'glass':
        return {
          background: 'rgba(255, 255, 255, 0.03)',
          border: '2px solid rgba(255, 255, 255, 0.1)',
          boxShadow: 'var(--shadow-lg), var(--shadow-glow)',
          backdropFilter: 'blur(10px)',
        };
      case 'cyberpunk':
        return {
          background: '#090a0f',
          border: '4px solid var(--accent)',
          boxShadow: '0 0 20px var(--accent-glow), var(--shadow-lg)',
        };
      default: // classic
        return {
          background: '#1b1b1b',
          border: '8px solid #2e2e2e',
          boxShadow: 'var(--shadow-lg)',
        };
    }
  };

  // Square background styles based on theme
  const getSquareColor = (row: number, col: number, isDark: boolean) => {
    const base = isDark ? 'var(--board-dark)' : 'var(--board-light)';
    
    switch (boardTheme) {
      case 'wood':
        return isDark ? '#a87c32' : '#dfc593';
      case 'glass':
        return isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(255, 255, 255, 0.12)';
      case 'cyberpunk':
        return isDark ? '#141824' : '#1e2436';
      default: // classic
        return isDark ? '#5c6370' : '#d1d5db';
    }
  };

  return (
    <div className="flex flex-col items-center gap-4" style={{ width: '100%' }}>
      {/* Placement Toolbar */}
      {gameState.phase === 'placement' && !reviewMode && (
        <div className="glass flex items-center justify-between p-4 w-full gap-4 animate-fade-in placement-toolbar">
          <div className="flex items-center gap-3 placement-group">
            <span className="text-sm font-semibold text-accent">Setup:</span>
            <button
              onClick={() => setSelectedPlacementType('unicorn')}
              className={`btn ${selectedPlacementType === 'unicorn' ? 'btn-primary' : 'btn-secondary'} text-xs`}
              style={{ padding: '0.5rem 1rem' }}
            >
              🦄 Unicorn ({placementUnicorn ? '1/1' : '0/1'})
            </button>
            <button
              onClick={() => setSelectedPlacementType('paladin')}
              className={`btn ${selectedPlacementType === 'paladin' ? 'btn-primary' : 'btn-secondary'} text-xs`}
              style={{ padding: '0.5rem 1rem' }}
            >
              🛡️ Paladins ({placementPaladins.length}/5)
            </button>
          </div>
          
          <div className="flex items-center gap-3 placement-actions">
            <span className="text-xs text-secondary placement-instruction">
              Place on {gameState.currentSide === 'white' ? 'Rows 1-2' : 'Rows 5-6'}
            </span>
            <button
              disabled={!placementUnicorn || placementPaladins.length !== 5}
              onClick={confirmPlacement}
              className="btn btn-primary confirm-setup-btn"
              style={{
                padding: '0.8rem 1.8rem',
                fontSize: '0.8rem',
                fontWeight: 'bold',
                letterSpacing: '0.05em',
                textTransform: 'uppercase',
                opacity: (placementUnicorn && placementPaladins.length === 5) ? 1 : 0.5,
                transition: 'all var(--transition-base)',
              }}
            >
              Confirm Setup
            </button>
          </div>
        </div>
      )}

      {/* Main Board Container */}
      <div
        className="relative overflow-hidden board-container"
        style={{
          width: '100%',
          aspectRatio: '1 / 1',
          borderRadius: 'var(--radius-lg)',
          transition: 'all var(--transition-base)',
          ...getBoardBgStyle(),
        }}
      >
        {/* 6x6 Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateRows: 'repeat(6, minmax(0, 1fr))',
            gridTemplateColumns: 'repeat(6, minmax(0, 1fr))',
            width: '100%',
            height: '100%',
          }}
        >
          {rowIndices.map((row) =>
            colIndices.map((col) => {
              const isDark = (row + col) % 2 === 0;
              const squareName = String.fromCharCode(65 + col) + (row + 1);
              const pieceType = gameState.phase === 'placement' && !reviewMode
                ? getPlacementPieceType(row, col)
                : currentBoard[row][col];
              
              const isSelected = selectedSquare?.row === row && selectedSquare?.col === col;
              const isLegal = isLegalTarget(row, col);
              const isLastMove = isLastMoveSquare(row, col);
              const rings = LISERE_MAP[row][col];

              // Constraint highlighting (Must move from this liseré class)
              const hasBandConstraint = gameState.phase === 'playing' && 
                                        gameState.requiredBand !== null && 
                                        !reviewMode;
              const isContrainedSquare = hasBandConstraint && 
                                         rings === gameState.requiredBand && 
                                         pieceType !== '-' && 
                                         ((gameState.currentSide === 'white' && (pieceType === 'B' || pieceType === 'b')) ||
                                          (gameState.currentSide === 'black' && (pieceType === 'N' || pieceType === 'n')));

              return (
                <div
                  key={`${row}-${col}`}
                  onClick={() => handleSquareClick(row, col)}
                  className="relative flex items-center justify-center"
                  style={{
                    backgroundColor: getSquareColor(row, col, isDark),
                    cursor: reviewMode ? 'default' : 'pointer',
                    transition: 'background-color var(--transition-fast)',
                    boxShadow: isSelected 
                      ? 'inset 0 0 0 3px var(--board-selected)' 
                      : isContrainedSquare 
                      ? 'inset 0 0 15px rgba(212,168,83,0.8)' 
                      : isLastMove 
                      ? 'inset 0 0 0 2px var(--board-last-move)' 
                      : 'none',
                  }}
                >
                  {/* Liseré Rings rendering */}
                  <div
                    style={{
                      position: 'absolute',
                      inset: '4px',
                      pointerEvents: 'none',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <svg viewBox="0 0 100 100" width="100%" height="100%">
                      {/* Ring 1 (Teal) */}
                      {rings >= 1 && (
                        <circle
                          cx="50"
                          cy="50"
                          r="18"
                          fill="none"
                          stroke="var(--lisere-1)"
                          strokeWidth="2"
                          strokeDasharray={boardTheme === 'cyberpunk' ? '3 1' : 'none'}
                          style={{ opacity: 0.7 }}
                        />
                      )}
                      {/* Ring 2 (Terracotta) */}
                      {rings >= 2 && (
                        <circle
                          cx="50"
                          cy="50"
                          r="28"
                          fill="none"
                          stroke="var(--lisere-2)"
                          strokeWidth="2"
                          strokeDasharray={boardTheme === 'cyberpunk' ? '4 2' : 'none'}
                          style={{ opacity: 0.75 }}
                        />
                      )}
                      {/* Ring 3 (Violet) */}
                      {rings >= 3 && (
                        <circle
                          cx="50"
                          cy="50"
                          r="38"
                          fill="none"
                          stroke="var(--lisere-3)"
                          strokeWidth="2"
                          strokeDasharray={boardTheme === 'cyberpunk' ? '5 3' : 'none'}
                          style={{ opacity: 0.8 }}
                        />
                      )}
                    </svg>
                  </div>

                  {/* Chess.com style legal move dot/ring */}
                  {isLegal && (
                    <div
                      style={{
                        position: 'absolute',
                        width: pieceType === '-' ? '30%' : '80%',
                        height: pieceType === '-' ? '30%' : '80%',
                        borderRadius: '50%',
                        backgroundColor: pieceType === '-' ? 'rgba(0,0,0,0.18)' : 'transparent',
                        border: pieceType === '-' ? 'none' : '4px solid rgba(0,0,0,0.22)',
                        boxShadow: '0 0 10px rgba(0,0,0,0.15)',
                        zIndex: 10,
                        pointerEvents: 'none',
                      }}
                    />
                  )}

                  {/* Render Game Piece */}
                  {pieceType !== '-' && pieceType !== null && (
                    <Piece
                      type={pieceType}
                      size="80%"
                      isSelected={isSelected}
                      isMovable={legalFromSquares.includes(`${row}-${col}`)}
                    />
                  )}

                  {/* Coordinate Labels */}
                  {coordinatesVisible && (
                    <>
                      {/* Columns (A-F) - only show on bottom edge rows */}
                      {((!isFlipped && row === 0) || (isFlipped && row === 5)) && (
                        <span
                          className="absolute text-xs font-bold"
                          style={{
                            bottom: '2px',
                            right: '4px',
                            color: isDark ? 'var(--board-light)' : 'var(--board-dark)',
                            opacity: 0.8,
                            fontSize: '0.65rem',
                          }}
                        >
                          {String.fromCharCode(65 + col)}
                        </span>
                      )}
                      {/* Rows (1-6) - only show on left edge columns */}
                      {((!isFlipped && col === 0) || (isFlipped && col === 5)) && (
                        <span
                          className="absolute text-xs font-bold"
                          style={{
                            top: '2px',
                            left: '4px',
                            color: isDark ? 'var(--board-light)' : 'var(--board-dark)',
                            opacity: 0.8,
                            fontSize: '0.65rem',
                          }}
                        >
                          {row + 1}
                        </span>
                      )}
                    </>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
      <style jsx>{`
        .placement-toolbar {
          width: 100% !important;
          flex-direction: row;
          justify-content: space-between;
          align-items: center;
        }
        @media (max-width: 580px) {
          .placement-toolbar {
            flex-direction: column !important;
            gap: 1.25rem !important;
            align-items: stretch !important;
            padding: 1.25rem !important;
          }
          .placement-group {
            flex-wrap: wrap;
            justify-content: center;
            width: 100%;
          }
          .placement-actions {
            flex-direction: column !important;
            align-items: center !important;
            gap: 0.75rem !important;
            width: 100%;
          }
          .confirm-setup-btn {
            width: 100%;
            padding: 0.9rem 2rem !important;
            font-size: 0.85rem !important;
          }
          .placement-instruction {
            text-align: center;
          }
        }
      `}</style>
    </div>
  );
};
export default Board;
