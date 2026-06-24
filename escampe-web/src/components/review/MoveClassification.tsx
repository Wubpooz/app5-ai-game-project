import React from 'react';
import { useGameStore } from '../../stores/game-store';

export type ClassificationType = 'brilliant' | 'great' | 'best' | 'good' | 'book' | 'inaccuracy' | 'mistake' | 'blunder';

// Helper to classify a single move
export function getMoveClassification(
  evalBefore: number,
  evalAfter: number,
  side: 'white' | 'black',
  isBook: boolean = false
): ClassificationType {
  if (isBook) return 'book';
  
  // Difference in centipawns. Positive is good for the active side.
  const delta = side === 'white' ? (evalAfter - evalBefore) : (evalBefore - evalAfter);
  
  if (delta >= 10) {
    // If it was a critical tactical save or huge swing
    if (delta >= 250) return 'great';
    return 'best';
  }
  
  const absLoss = Math.abs(delta);
  
  if (absLoss <= 30) return 'best';       // minor swing, still top tier
  if (absLoss <= 80) return 'good';       // solid move
  if (absLoss <= 180) return 'inaccuracy'; // suboptimal
  if (absLoss <= 350) return 'mistake';    // weak play
  return 'blunder';                       // game-losing drop
}

export const MoveClassification: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const reviewIndex = useGameStore((state) => state.reviewIndex);

  const currentRecord = reviewIndex >= 0 ? moveRecords[reviewIndex] : null;

  // Aggregate stats
  const stats = {
    brilliant: 0,
    great: 0,
    best: 0,
    good: 0,
    book: 0,
    inaccuracy: 0,
    mistake: 0,
    blunder: 0,
  };

  moveRecords.forEach((rec, idx) => {
    const side = idx % 2 === 0 ? 'white' : 'black';
    // First 2 moves are placement, classified as "book"
    const isPlacement = idx < 2;
    const classification = getMoveClassification(rec.evalBefore, rec.evalAfter, side, isPlacement);
    stats[classification]++;
  });

  const getBadgeStyle = (type: ClassificationType) => {
    switch (type) {
      case 'brilliant':
        return { bg: 'rgba(68, 232, 200, 0.15)', text: 'var(--quality-brilliant)', label: 'Brilliant' };
      case 'great':
        return { bg: 'rgba(77, 184, 232, 0.15)', text: 'var(--quality-good)', label: 'Great Find' };
      case 'best':
        return { bg: 'rgba(106, 201, 122, 0.15)', text: 'var(--quality-best)', label: 'Best Move' };
      case 'good':
        return { bg: 'rgba(106, 201, 122, 0.08)', stroke: '1px solid var(--quality-best)', text: 'var(--quality-best)', label: 'Good' };
      case 'book':
        return { bg: 'rgba(180, 140, 60, 0.15)', text: '#c8b080', label: 'Book Setup' };
      case 'inaccuracy':
        return { bg: 'rgba(232, 200, 74, 0.15)', text: 'var(--quality-inaccuracy)', label: 'Inaccuracy' };
      case 'mistake':
        return { bg: 'rgba(232, 122, 68, 0.15)', text: 'var(--quality-mistake)', label: 'Mistake' };
      case 'blunder':
        return { bg: 'rgba(232, 68, 68, 0.15)', text: 'var(--quality-blunder)', label: 'Blunder' };
    }
  };

  const currentClassification = currentRecord
    ? getMoveClassification(
        currentRecord.evalBefore,
        currentRecord.evalAfter,
        reviewIndex % 2 === 0 ? 'white' : 'black',
        reviewIndex < 2
      )
    : null;

  const activeBadge = currentClassification ? getBadgeStyle(currentClassification) : null;

  return (
    <div className="glass p-4 flex flex-col gap-4 w-full animate-fade-in">
      <div className="text-sm font-bold text-accent">Move Classification & Stats</div>

      {/* Aggregate Stats grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.4rem' }}>
        {(Object.keys(stats) as Array<ClassificationType>).map((key) => {
          const style = getBadgeStyle(key);
          const count = stats[key];
          return (
            <div
              key={key}
              style={{
                background: 'rgba(0,0,0,0.15)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-sm)',
                padding: '0.4rem',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                opacity: count > 0 ? 1 : 0.4,
              }}
            >
              <span style={{ fontSize: '0.65rem', color: style.text, fontWeight: 800 }}>{style.label}</span>
              <span style={{ fontSize: '0.9rem', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>{count}</span>
            </div>
          );
        })}
      </div>

      {/* Active Move Detail Display */}
      {currentRecord && activeBadge ? (
        <div
          style={{
            background: activeBadge.bg,
            border: activeBadge.stroke || `1px solid ${activeBadge.text}33`,
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.4rem',
          }}
        >
          <div className="flex items-center justify-between">
            <span style={{ color: activeBadge.text, fontWeight: 900, fontSize: '0.95rem' }}>
              {activeBadge.label}
            </span>
            <span className="font-mono text-xs text-primary" style={{ fontWeight: 800 }}>
              Move {reviewIndex + 1}: {currentRecord.notation}
            </span>
          </div>
          
          <p className="text-xs text-secondary">
            {currentClassification === 'best' && 'This was the optimal move in this position, maintaining or consolidating the advantage.'}
            {currentClassification === 'good' && 'A solid, healthy move that develops the position without major mistakes.'}
            {currentClassification === 'great' && 'An excellent tactical find that significantly improves your position!'}
            {currentClassification === 'book' && 'A standard placement or opening sequence from the game library.'}
            {currentClassification === 'inaccuracy' && 'A suboptimal choice that slightly slips control of the board. There were better options.'}
            {currentClassification === 'mistake' && 'A weak move that gives away a noticeable portion of your advantage or increases danger.'}
            {currentClassification === 'blunder' && 'A major mistake! This significantly swings the evaluation in favor of the opponent.'}
          </p>

          <div style={{ display: 'flex', gap: '1rem', marginTop: '0.2rem', fontSize: '0.72rem', fontFamily: 'var(--font-mono)' }}>
            <span className="text-muted">
              Eval Before: <span className="text-primary">{(currentRecord.evalBefore / 100).toFixed(2)}</span>
            </span>
            <span className="text-muted">
              Eval After: <span className="text-primary">{(currentRecord.evalAfter / 100).toFixed(2)}</span>
            </span>
            <span className="text-muted">
              Swing: <span style={{ color: (currentRecord.evalAfter - currentRecord.evalBefore) * (reviewIndex % 2 === 0 ? 1 : -1) >= 0 ? 'var(--quality-best)' : 'var(--quality-blunder)' }}>
                {(((currentRecord.evalAfter - currentRecord.evalBefore) / 100) * (reviewIndex % 2 === 0 ? 1 : -1)).toFixed(2)}
              </span>
            </span>
          </div>
        </div>
      ) : (
        <div className="text-xs text-muted" style={{ textAlign: 'center', padding: '1rem 0' }}>
          Select a move in the log to inspect its quality details
        </div>
      )}
    </div>
  );
};
export default MoveClassification;
