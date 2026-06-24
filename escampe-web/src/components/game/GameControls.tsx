import React from 'react';
import { useGameStore } from '../../stores/game-store';

export const GameControls: React.FC = () => {
  const isGameOver = useGameStore((state) => state.isGameOver);
  const gameState = useGameStore((state) => state.gameState);
  const gameMode = useGameStore((state) => state.gameMode);
  const playerColor = useGameStore((state) => state.playerColor);
  const resignGame = useGameStore((state) => state.resignGame);
  const triggerDraw = useGameStore((state) => state.triggerDraw);
  const moveRecords = useGameStore((state) => state.moveRecords);

  const reviewMode = useGameStore((state) => state.reviewMode);
  const reviewIndex = useGameStore((state) => state.reviewIndex);
  const goToMove = useGameStore((state) => state.goToMove);
  const setReviewMode = useGameStore((state) => state.setReviewMode);

  const isFlipped = useGameStore((state) => state.isFlipped);
  const toggleFlipped = () => useGameStore.setState({ isFlipped: !isFlipped });

  const handleResign = () => {
    if (confirm('Are you sure you want to resign?')) {
      const activeSide = gameState.currentSide;
      resignGame(activeSide);
    }
  };

  const handleDraw = () => {
    if (confirm('Are you sure you want to offer a draw?')) {
      triggerDraw();
    }
  };

  return (
    <div
      className="glass flex flex-col gap-3 p-4 w-full"
      style={{
        justifyContent: 'center',
      }}
    >
      {reviewMode ? (
        /* Navigation Controls for Game Review */
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-accent">Review Navigator:</span>
            <span className="text-xs text-muted font-mono">
              Move {reviewIndex + 1} / {moveRecords.length}
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.5rem' }}>
            <button
              onClick={() => goToMove(-1)}
              disabled={reviewIndex === -1}
              className="btn btn-secondary text-sm"
              style={{ padding: '0.4rem 0.2rem', minWidth: '40px' }}
              title="First Position"
            >
              ⏮️
            </button>
            <button
              onClick={() => goToMove(reviewIndex - 1)}
              disabled={reviewIndex === -1}
              className="btn btn-secondary text-sm"
              style={{ padding: '0.4rem 0.2rem', minWidth: '40px' }}
              title="Previous Move"
            >
              ◀️
            </button>
            <button
              onClick={() => goToMove(reviewIndex + 1)}
              disabled={reviewIndex === moveRecords.length - 1}
              className="btn btn-secondary text-sm"
              style={{ padding: '0.4rem 0.2rem', minWidth: '40px' }}
              title="Next Move"
            >
              ▶️
            </button>
            <button
              onClick={() => goToMove(moveRecords.length - 1)}
              disabled={reviewIndex === moveRecords.length - 1}
              className="btn btn-secondary text-sm"
              style={{ padding: '0.4rem 0.2rem', minWidth: '40px' }}
              title="Current Position"
            >
              ⏭️
            </button>
          </div>

          <button
            onClick={() => setReviewMode(false)}
            className="btn btn-primary text-xs w-full"
            style={{ padding: '0.5rem 1rem' }}
          >
            Return to Game
          </button>
        </div>
      ) : (
        /* Live Game Action Controls */
        <div className="flex flex-col gap-3">
          <div className="flex justify-between gap-2">
            <button
              onClick={toggleFlipped}
              className="btn btn-secondary text-xs flex-1"
              style={{ padding: '0.5rem 1rem' }}
            >
              🔄 Flip Board
            </button>

            {gameState.phase === 'playing' && !isGameOver && (
              <>
                <button
                  onClick={handleDraw}
                  className="btn btn-secondary text-xs flex-1"
                  style={{ padding: '0.5rem 1rem' }}
                >
                  🤝 Draw
                </button>
                <button
                  onClick={handleResign}
                  className="btn btn-danger text-xs flex-1"
                  style={{ padding: '0.5rem 1rem' }}
                >
                  🏳️ Resign
                </button>
              </>
            )}
          </div>

          {isGameOver && moveRecords.length > 0 && (
            <button
              onClick={() => setReviewMode(true)}
              className="btn btn-primary text-xs w-full"
              style={{ padding: '0.6rem 1rem' }}
            >
              📊 Game Review Analysis
            </button>
          )}
        </div>
      )}
    </div>
  );
};
export default GameControls;
