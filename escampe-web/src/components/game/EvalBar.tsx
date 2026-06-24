import React from 'react';
import { useGameStore } from '../../stores/game-store';

export const EvalBar: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const isSearching = useGameStore((state) => state.isSearching);

  // Get current evaluation from the last move record (from White's perspective)
  let score = 0;
  if (moveRecords.length > 0) {
    score = moveRecords[moveRecords.length - 1].evalAfter;
  }

  // Check if checkmate/win is detected (clamped to win/loss limits)
  const isWin = score >= 50000;
  const isLoss = score <= -50000;

  // Formatting score to display (e.g., +1.5 or -0.8)
  // For standard play, a unit of 100 points is roughly equivalent to a pawn (1.0)
  const formatScore = () => {
    if (isWin) return 'M';
    if (isLoss) return '-M';
    const val = (score / 100).toFixed(1);
    return score >= 0 ? `+${val}` : val;
  };

  // Convert raw score into a fill percentage (0% = Black winning, 50% = equal, 100% = White winning)
  const getPercentage = () => {
    if (isWin) return 100;
    if (isLoss) return 0;
    
    // Clamp score between -1500 and +1500
    const clamped = Math.max(-1500, Math.min(1500, score));
    // Scale to 0 - 100%
    return ((clamped + 1500) / 3000) * 100;
  };

  const percentage = getPercentage();

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        height: '100%',
        width: '28px',
        background: 'linear-gradient(180deg, #1b2030 0%, #0d0f17 100%)',
        border: '1px solid var(--border-default)',
        borderRadius: 'var(--radius-sm)',
        overflow: 'hidden',
        position: 'relative',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      {/* White portion of the bar */}
      <div
        style={{
          width: '100%',
          height: `${percentage}%`,
          background: 'linear-gradient(180deg, #ffffff 0%, #d8c191 100%)',
          transition: 'height var(--transition-slow) cubic-bezier(0.4, 0, 0.2, 1)',
          position: 'absolute',
          bottom: 0,
          left: 0,
        }}
      />

      {/* 3D cylindrical reflection sheen overlay */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(90deg, rgba(255,255,255,0.08) 0%, rgba(255,255,255,0) 30%, rgba(0,0,0,0.2) 100%)',
          zIndex: 10,
          pointerEvents: 'none',
        }}
      />

      {/* Numerical score label */}
      <span
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          textAlign: 'center',
          fontSize: '0.65rem',
          fontWeight: 800,
          zIndex: 20,
          pointerEvents: 'none',
          fontFamily: 'var(--font-mono)',
          // Position score at top or bottom depending on who is leading
          top: percentage > 50 ? '8px' : 'auto',
          bottom: percentage <= 50 ? '8px' : 'auto',
          color: percentage > 50 ? 'var(--bg-base)' : 'var(--text-primary)',
        }}
      >
        {isSearching ? '...' : formatScore()}
      </span>
    </div>
  );
};
export default EvalBar;
