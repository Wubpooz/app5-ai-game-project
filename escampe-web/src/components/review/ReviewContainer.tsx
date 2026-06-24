import React, { useEffect } from 'react';
import { useGameStore } from '../../stores/game-store';
import { Board } from '../board/Board';
import { EvalGraph } from './EvalGraph';
import { MoveClassification } from './MoveClassification';
import { EngineLines } from './EngineLines';
import { MoveHistory } from '../game/MoveHistory';
import { GameControls } from '../game/GameControls';
import { egnToString } from '../../engine/egn';
import { gameStateToEPN } from '../../engine/epn';

export const ReviewContainer: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const reviewIndex = useGameStore((state) => state.reviewIndex);
  const gameState = useGameStore((state) => state.gameState);
  const setReviewMode = useGameStore((state) => state.setReviewMode);

  // Force review mode active when mounting review page
  useEffect(() => {
    setReviewMode(true);
    return () => {
      setReviewMode(false);
    };
  }, [setReviewMode]);

  const copyEGN = () => {
    if (moveRecords.length === 0) return;
    
    // Construct EGNGame object
    const whitePlacement = moveRecords.slice(0, 1).map(r => r.notation)[0] || '';
    const blackPlacement = moveRecords.slice(1, 2).map(r => r.notation)[0] || '';
    const moves = moveRecords.slice(2).map(r => r.notation);
    
    let result: 'white' | 'black' | 'draw' | '*' = '*';
    if (gameState.winner === 'white') result = 'white';
    else if (gameState.winner === 'black') result = 'black';
    else if (gameState.winner) result = 'draw';

    const egnStr = egnToString({
      whitePlacement,
      blackPlacement,
      moves,
      result,
    });

    navigator.clipboard.writeText(egnStr);
    alert('Game EGN copied to clipboard!');
  };

  const copyEPN = () => {
    const epnStr = gameStateToEPN(gameState);
    navigator.clipboard.writeText(epnStr);
    alert('Position EPN copied to clipboard!');
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
        width: '100%',
        alignItems: 'center',
      }}
    >
      {/* Header Banner */}
      <div
        className="glass"
        style={{
          width: '100%',
          maxWidth: '890px',
          padding: '0.6rem 1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderLeft: '4px solid var(--accent)',
        }}
      >
        <span className="text-xs font-bold text-accent">Game Review & Post-Analysis</span>
        <button
          onClick={() => useGameStore.setState({ reviewMode: false })}
          className="btn btn-secondary text-xs"
          style={{ padding: '0.3rem 0.8rem' }}
        >
          ↩️ Return to Live Board
        </button>
      </div>

      {/* Analysis Grid Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr',
          gap: '1.5rem',
          width: '100%',
          maxWidth: '890px',
        }}
        className="review-desktop-layout"
      >
        {/* Playfield: Review Board & Graph */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1.2rem',
            alignItems: 'center',
            width: '100%',
          }}
        >
          {/* Historical Board */}
          <Board />

          {/* Eval Graph */}
          <EvalGraph />
          
          {/* EGN / EPN Utilities */}
          <div className="flex gap-2 w-full justify-center" style={{ maxWidth: '560px' }}>
            <button
              onClick={copyEGN}
              disabled={moveRecords.length === 0}
              className="btn btn-secondary text-xs flex-1"
              style={{ padding: '0.5rem 1rem', opacity: moveRecords.length === 0 ? 0.5 : 1 }}
            >
              📋 Copy Game EGN
            </button>
            <button
              onClick={copyEPN}
              className="btn btn-secondary text-xs flex-1"
              style={{ padding: '0.5rem 1rem' }}
            >
              📋 Copy Current EPN
            </button>
          </div>
        </div>

        {/* Analysis sidebar panels */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            width: '100%',
          }}
        >
          {/* Move Log */}
          <MoveHistory />

          {/* Nav Controls */}
          <GameControls />

          {/* Engine suggestion lines */}
          <EngineLines />

          {/* Move classifications stats */}
          <MoveClassification />
        </div>
      </div>

      <style jsx global>{`
        @media (min-width: 820px) {
          .review-desktop-layout {
            grid-template-columns: 1fr 300px !important;
          }
        }
        @media (max-width: 819px) {
          .review-desktop-layout {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};
export default ReviewContainer;
