import React, { useEffect, useRef } from 'react';
import { useGameStore } from '../../stores/game-store';

export const MoveHistory: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const reviewMode = useGameStore((state) => state.reviewMode);
  const reviewIndex = useGameStore((state) => state.reviewIndex);
  const goToMove = useGameStore((state) => state.goToMove);
  const setReviewMode = useGameStore((state) => state.setReviewMode);

  const containerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll on new moves
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [moveRecords.length]);

  // Group moves into pairs (White, Black)
  const pairedMoves: { white: string; black?: string; whiteIdx: number; blackIdx?: number }[] = [];
  for (let i = 0; i < moveRecords.length; i += 2) {
    pairedMoves.push({
      white: moveRecords[i].notation,
      whiteIdx: i,
      black: moveRecords[i + 1]?.notation,
      blackIdx: i + 1,
    });
  }

  const handleMoveClick = (idx: number) => {
    if (!reviewMode) {
      setReviewMode(true);
    }
    goToMove(idx);
  };

  return (
    <div
      className="glass"
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '240px',
        width: '100%',
        padding: '1rem',
        overflow: 'hidden',
      }}
    >
      <div className="text-sm font-bold text-accent" style={{ marginBottom: '0.75rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
        Move Log
      </div>

      <div
        ref={containerRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.4rem',
        }}
      >
        {pairedMoves.length === 0 ? (
          <div className="text-xs text-muted" style={{ textAlign: 'center', padding: '2rem 0' }}>
            No moves played yet
          </div>
        ) : (
          pairedMoves.map((pair, index) => {
            const isWhiteActive = reviewMode && reviewIndex === pair.whiteIdx;
            const isBlackActive = reviewMode && reviewIndex === pair.blackIdx;

            return (
              <div
                key={index}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 1fr 1fr',
                  fontSize: '0.825rem',
                  fontFamily: 'var(--font-mono)',
                  alignItems: 'center',
                }}
              >
                {/* Move Number */}
                <span className="text-muted">{index + 1}.</span>

                {/* White Move */}
                <span
                  onClick={() => handleMoveClick(pair.whiteIdx)}
                  style={{
                    cursor: 'pointer',
                    padding: '0.2rem 0.4rem',
                    borderRadius: '4px',
                    backgroundColor: isWhiteActive ? 'var(--accent-glow)' : 'transparent',
                    color: isWhiteActive ? 'var(--accent-bright)' : 'var(--text-primary)',
                    fontWeight: isWhiteActive ? 700 : 500,
                  }}
                  className="hover:text-accent"
                >
                  {pair.white}
                </span>

                {/* Black Move */}
                {pair.black && (
                  <span
                    onClick={() => handleMoveClick(pair.blackIdx!)}
                    style={{
                      cursor: 'pointer',
                      padding: '0.2rem 0.4rem',
                      borderRadius: '4px',
                      backgroundColor: isBlackActive ? 'var(--accent-glow)' : 'transparent',
                      color: isBlackActive ? 'var(--accent-bright)' : 'var(--text-primary)',
                      fontWeight: isBlackActive ? 700 : 500,
                    }}
                    className="hover:text-accent"
                  >
                    {pair.black}
                  </span>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
export default MoveHistory;
