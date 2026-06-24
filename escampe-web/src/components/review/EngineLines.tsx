import React, { useState, useEffect } from 'react';
import { useGameStore } from '../../stores/game-store';
import { createInitialGameState, computeHash, applyMove, applyPass, applyPlacement } from '../../engine/board';
import { getLegalMoves } from '../../engine/moves';
import { LISERE_MAP } from '../../engine/constants';
import type { GameState, Move, SearchResult } from '../../engine/types';

export const EngineLines: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const reviewMode = useGameStore((state) => state.reviewMode);
  const reviewIndex = useGameStore((state) => state.reviewIndex);

  const [analysisResult, setAnalysisResult] = useState<SearchResult | null>(null);
  const [analyzing, setAnalyzing] = useState(false);

  useEffect(() => {
    if (!reviewMode || reviewIndex < 1 || moveRecords.length === 0) {
      setAnalysisResult(null);
      setAnalyzing(false);
      return;
    }

    // Reconstruct the exact GameState at reviewIndex
    const record = moveRecords[reviewIndex];
    const board = JSON.parse(record.boardBefore);
    
    // Determine active player color for this move
    const currentSide = reviewIndex % 2 === 0 ? 'white' : 'black';
    
    // Determine required liseré constraint
    // For move N, the constraint was set by the landing square of move N-1
    let requiredBand: number | null = null;
    if (reviewIndex >= 2 && moveRecords[reviewIndex - 1]) {
      const prevMove = moveRecords[reviewIndex - 1].move;
      if (!prevMove.isPass) {
        requiredBand = LISERE_MAP[prevMove.to.row][prevMove.to.col];
      }
    }

    const state: GameState = {
      board,
      phase: 'playing',
      currentSide,
      requiredBand,
      moveHistory: moveRecords.slice(0, reviewIndex).map(r => r.notation),
      whitePlaced: true,
      blackPlaced: true,
      winner: null,
      moveCount: reviewIndex,
      hash: computeHash(board, currentSide, requiredBand),
    };

    // Trigger analysis
    setAnalyzing(true);
    setAnalysisResult(null);

    const worker = new Worker(new URL('../../engine/worker.ts', import.meta.url), { type: 'module' });

    worker.onmessage = (event) => {
      const res = event.data;
      if (res.type === 'result') {
        setAnalysisResult(res.result);
        setAnalyzing(false);
        worker.terminate();
      } else if (res.type === 'error') {
        setAnalyzing(false);
        worker.terminate();
      }
    };

    // Run at Level 7 (Specialist) for depth 5 search
    worker.postMessage({
      type: 'search',
      state,
      maxDepth: 5,
      timeLimitMs: 1500,
      level: 7,
    });

    return () => {
      worker.terminate();
    };
  }, [reviewIndex, reviewMode, moveRecords]);

  if (!reviewMode || reviewIndex < 1) return null;

  return (
    <div className="glass p-4 flex flex-col gap-3 w-full animate-fade-in">
      <div className="flex items-center justify-between text-xs">
        <span className="font-bold text-accent">Engine Analysis Engine</span>
        {analyzing ? (
          <span className="text-accent animate-pulse-glow" style={{ fontSize: '0.7rem' }}>
            ⚡ Calculating line...
          </span>
        ) : (
          <span className="text-muted" style={{ fontSize: '0.7rem' }}>
            Ready
          </span>
        )}
      </div>

      {analyzing && (
        <div className="flex items-center justify-center p-4">
          <div
            style={{
              width: '20px',
              height: '20px',
              border: '2px solid var(--border-default)',
              borderTopColor: 'var(--accent)',
              borderRadius: '50%',
              animation: 'spin 0.8s linear infinite',
            }}
          />
        </div>
      )}

      {!analyzing && analysisResult && (
        <div className="flex flex-col gap-2">
          {/* Best Line header */}
          <div
            style={{
              background: 'rgba(0,0,0,0.15)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-sm)',
              padding: '0.6rem 0.8rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.2rem',
            }}
          >
            <div className="flex justify-between items-center text-xs">
              <span className="font-bold text-primary">Best Move:</span>
              <span className="font-mono text-accent" style={{ fontWeight: 800 }}>
                {analysisResult.move?.isPass ? 'E (Pass)' : analysisResult.pv[0]}
              </span>
            </div>

            <div className="flex justify-between items-center text-xs" style={{ marginTop: '0.2rem' }}>
              <span className="text-muted">Evaluation:</span>
              <span className="font-mono font-bold" style={{ color: analysisResult.score >= 0 ? 'var(--quality-best)' : 'var(--quality-blunder)' }}>
                {(analysisResult.score / 100).toFixed(2)}
              </span>
            </div>
          </div>

          {/* Principal Variation Line */}
          {analysisResult.pv.length > 0 && (
            <div className="flex flex-col gap-1 text-xs">
              <span className="text-muted" style={{ fontWeight: 600 }}>Principal Line:</span>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  background: 'rgba(255,255,255,0.02)',
                  padding: '0.5rem',
                  borderRadius: '4px',
                  border: '1px dashed var(--border-subtle)',
                  color: 'var(--text-secondary)',
                  wordBreak: 'break-all',
                }}
              >
                {analysisResult.pv.join(' ➔ ')}
              </div>
            </div>
          )}
        </div>
      )}

      {!analyzing && !analysisResult && (
        <div className="text-xs text-muted" style={{ textAlign: 'center', padding: '1rem 0' }}>
          Navigate to playing moves to activate engine analysis
        </div>
      )}
    </div>
  );
};
export default EngineLines;
