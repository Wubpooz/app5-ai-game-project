import React, { useState } from 'react';
import { useGameStore } from '../../stores/game-store';
import { Modal } from '../ui/Modal';
import { getMoveClassification } from '../review/MoveClassification';

interface GameOverModalProps {
  onNewGame: () => void;
}

export const GameOverModal: React.FC<GameOverModalProps> = ({ onNewGame }) => {
  const isGameOver = useGameStore((state) => state.isGameOver);
  const gameStatusText = useGameStore((state) => state.gameStatusText);
  const setReviewMode = useGameStore((state) => state.setReviewMode);
  const moveRecords = useGameStore((state) => state.moveRecords);

  const [activeTab, setActiveTab] = useState<'result' | 'card'>('result');
  const [copiedType, setCopiedType] = useState<'text' | 'link' | null>(null);

  // Close helper
  const handleClose = () => {
    useGameStore.setState({ isGameOver: false });
  };

  const handleReview = () => {
    handleClose();
    setReviewMode(true);
  };

  // ──────────────────────────────────────────────────────────────────────────
  // STATS & ACCURACY CALCULATIONS
  // ──────────────────────────────────────────────────────────────────────────
  
  const whiteStats = {
    cplSum: 0,
    cplCount: 0,
    stats: { brilliant: 0, great: 0, best: 0, good: 0, book: 0, inaccuracy: 0, mistake: 0, blunder: 0 },
  };

  const blackStats = {
    cplSum: 0,
    cplCount: 0,
    stats: { brilliant: 0, great: 0, best: 0, good: 0, book: 0, inaccuracy: 0, mistake: 0, blunder: 0 },
  };

  moveRecords.forEach((rec, idx) => {
    const side = idx % 2 === 0 ? 'white' : 'black';
    const isPlacement = idx < 2;
    const classification = getMoveClassification(rec.evalBefore, rec.evalAfter, side, isPlacement);

    if (side === 'white') {
      whiteStats.stats[classification]++;
    } else {
      blackStats.stats[classification]++;
    }

    if (!isPlacement) {
      const delta = side === 'white' ? (rec.evalAfter - rec.evalBefore) : (rec.evalBefore - rec.evalAfter);
      const cpl = Math.max(0, -delta);
      if (side === 'white') {
        whiteStats.cplSum += cpl;
        whiteStats.cplCount++;
      } else {
        blackStats.cplSum += cpl;
        blackStats.cplCount++;
      }
    }
  });

  const calculateAccuracy = (cplSum: number, cplCount: number) => {
    if (cplCount === 0) return 100;
    const acpl = cplSum / cplCount;
    // Formula: Accuracy = 100 * e^(-0.008 * ACPL)
    const acc = 100 * Math.exp(-0.008 * acpl);
    return Math.max(0, Math.min(100, Math.round(acc * 10) / 10));
  };

  const whiteAccuracy = calculateAccuracy(whiteStats.cplSum, whiteStats.cplCount);
  const blackAccuracy = calculateAccuracy(blackStats.cplSum, blackStats.cplCount);

  // Copy Stats text
  const handleCopyText = () => {
    const text = `🏆 ESCAMPE.AI MATCH CARD 🏆
─────────────────────────────
White Accuracy: ${whiteAccuracy}%
Black Accuracy: ${blackAccuracy}%

Move Quality breakdown (White / Black):
💎 Brilliant:   ${whiteStats.stats.brilliant} / ${blackStats.stats.brilliant}
🎯 Great Find:   ${whiteStats.stats.great} / ${blackStats.stats.great}
⭐ Best Moves:   ${whiteStats.stats.best} / ${blackStats.stats.best}
👍 Good Moves:   ${whiteStats.stats.good} / ${blackStats.stats.good}
📖 Book Setup:   ${whiteStats.stats.book} / ${blackStats.stats.book}
Inaccuracies: ${whiteStats.stats.inaccuracy} / ${blackStats.stats.inaccuracy}
Mistakes:     ${whiteStats.stats.mistake} / ${blackStats.stats.mistake}
Blunders:     ${whiteStats.stats.blunder} / ${blackStats.stats.blunder}

Result: ${gameStatusText}
Total Moves: ${moveRecords.length}
─────────────────────────────
Play Escampe at https://escampe.ai`;

    navigator.clipboard.writeText(text);
    setCopiedType('text');
    setTimeout(() => setCopiedType(null), 2000);
  };

  // Copy mock share link
  const handleCopyLink = () => {
    const link = `https://escampe.ai/game/review?moves=${moveRecords.length}&wAcc=${whiteAccuracy}&bAcc=${blackAccuracy}`;
    navigator.clipboard.writeText(link);
    setCopiedType('link');
    setTimeout(() => setCopiedType(null), 2000);
  };

  const radialRadius = 32;
  const radialCircumference = 2 * Math.PI * radialRadius;

  const getRadialOffset = (accuracy: number) => {
    return radialCircumference - (accuracy / 100) * radialCircumference;
  };

  const rows = [
    { label: 'Brilliant', key: 'brilliant' as const, color: 'var(--quality-brilliant)', icon: '💎' },
    { label: 'Great Find', key: 'great' as const, color: 'var(--quality-good)', icon: '🎯' },
    { label: 'Best Move', key: 'best' as const, color: 'var(--quality-best)', icon: '⭐' },
    { label: 'Good', key: 'good' as const, color: '#6ac97a', icon: '👍' },
    { label: 'Book Setup', key: 'book' as const, color: '#c8b080', icon: '📖' },
    { label: 'Inaccuracy', key: 'inaccuracy' as const, color: 'var(--quality-inaccuracy)', icon: '🟡' },
    { label: 'Mistake', key: 'mistake' as const, color: 'var(--quality-mistake)', icon: '🟠' },
    { label: 'Blunder', key: 'blunder' as const, color: 'var(--quality-blunder)', icon: '🔴' },
  ];

  return (
    <Modal isOpen={isGameOver} onClose={handleClose} title="Game Over">
      <div className="flex flex-col items-center gap-4 text-center py-2 animate-fade-in" style={{ width: '100%', maxWidth: '380px', margin: '0 auto' }}>
        
        {/* Tab Buttons */}
        <div style={{
          display: 'flex',
          background: 'rgba(0,0,0,0.15)',
          borderRadius: 'var(--radius-md)',
          padding: '0.2rem',
          width: '100%',
          border: '1px solid var(--border-subtle)',
          marginBottom: '0.5rem',
        }}>
          <button
            onClick={() => setActiveTab('result')}
            className={`flex-1 text-xs font-bold`}
            style={{
              padding: '0.5rem 0',
              borderRadius: '6px',
              border: 'none',
              background: activeTab === 'result' ? 'var(--accent)' : 'transparent',
              color: activeTab === 'result' ? 'var(--text-inverse)' : 'var(--text-secondary)',
              cursor: 'pointer',
              transition: 'all var(--transition-fast)',
            }}
          >
            🏆 Result
          </button>
          <button
            onClick={() => setActiveTab('card')}
            className={`flex-1 text-xs font-bold`}
            style={{
              padding: '0.5rem 0',
              borderRadius: '6px',
              border: 'none',
              background: activeTab === 'card' ? 'var(--accent)' : 'transparent',
              color: activeTab === 'card' ? 'var(--text-inverse)' : 'var(--text-secondary)',
              cursor: 'pointer',
              transition: 'all var(--transition-fast)',
            }}
          >
            📊 Match Card
          </button>
        </div>

        {activeTab === 'result' ? (
          // Tab 1: Game Over Result Panel
          <>
            <div style={{ fontSize: '3rem', margin: '0.5rem 0' }}>🏆</div>
            <h4 className="text-lg font-bold text-primary">{gameStatusText}</h4>
            <p className="text-sm text-secondary" style={{ maxWidth: '320px' }}>
              Excellent match! You can now analyze the game move-by-move, inspect your accuracy stats, or start a new round.
            </p>
          </>
        ) : (
          // Tab 2: Match Card details
          <div style={{
            width: '100%',
            background: 'linear-gradient(135deg, rgba(30,34,53,0.9), rgba(15,17,23,0.95))',
            border: '2px solid var(--accent)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.2rem',
            boxShadow: 'var(--shadow-lg), 0 0 20px var(--accent-glow)',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 900, textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--accent)' }}>
              ⚔️ Match Accuracy ⚔️
            </div>

            {/* Circular dials side by side */}
            <div style={{ display: 'flex', justifyContent: 'space-around', alignItems: 'center', margin: '0.4rem 0' }}>
              {/* White stats */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.3rem' }}>
                <svg width="78" height="78" viewBox="0 0 78 78">
                  <circle cx="39" cy="39" r={radialRadius} fill="transparent" stroke="rgba(255,255,255,0.06)" strokeWidth="6" />
                  <circle
                    cx="39"
                    cy="39"
                    r={radialRadius}
                    fill="transparent"
                    stroke="#e8e0c8"
                    strokeWidth="6"
                    strokeDasharray={radialCircumference}
                    strokeDashoffset={getRadialOffset(whiteAccuracy)}
                    strokeLinecap="round"
                    transform="rotate(-90 39 39)"
                    style={{ transition: 'stroke-dashoffset 0.8s ease-in-out', filter: 'drop-shadow(0 0 3px rgba(232,224,200,0.4))' }}
                  />
                  <text x="39" y="44" textAnchor="middle" fill="#f0f2f8" fontSize="12px" fontWeight="900">{whiteAccuracy}%</text>
                </svg>
                <span style={{ fontSize: '0.7rem', fontWeight: 800, color: 'var(--text-secondary)' }}>White Accuracy</span>
              </div>

              {/* Black stats */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.3rem' }}>
                <svg width="78" height="78" viewBox="0 0 78 78">
                  <circle cx="39" cy="39" r={radialRadius} fill="transparent" stroke="rgba(255,255,255,0.06)" strokeWidth="6" />
                  <circle
                    cx="39"
                    cy="39"
                    r={radialRadius}
                    fill="transparent"
                    stroke="var(--quality-good)"
                    strokeWidth="6"
                    strokeDasharray={radialCircumference}
                    strokeDashoffset={getRadialOffset(blackAccuracy)}
                    strokeLinecap="round"
                    transform="rotate(-90 39 39)"
                    style={{ transition: 'stroke-dashoffset 0.8s ease-in-out', filter: 'drop-shadow(0 0 3px rgba(77,184,232,0.4))' }}
                  />
                  <text x="39" y="44" textAnchor="middle" fill="#f0f2f8" fontSize="12px" fontWeight="900">{blackAccuracy}%</text>
                </svg>
                <span style={{ fontSize: '0.7rem', fontWeight: 800, color: 'var(--text-secondary)' }}>Black Accuracy</span>
              </div>
            </div>

            {/* Quality Comparison list */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem', padding: '0.4rem 0', background: 'rgba(0,0,0,0.15)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 1fr', fontSize: '0.65rem', fontWeight: 900, textTransform: 'uppercase', color: 'var(--text-muted)', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.3rem', marginBottom: '0.2rem' }}>
                <span>White</span>
                <span>Move Quality</span>
                <span>Black</span>
              </div>

              {rows.map((row) => {
                const wCount = whiteStats.stats[row.key];
                const bCount = blackStats.stats[row.key];
                return (
                  <div key={row.key} style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 1fr', alignItems: 'center', fontSize: '0.7rem', padding: '0.25rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <span style={{ fontWeight: wCount > 0 ? 800 : 400, opacity: wCount > 0 ? 1 : 0.4 }}>{wCount}</span>
                    <span style={{ color: row.color, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.2rem' }}>
                      <span>{row.icon}</span> {row.label}
                    </span>
                    <span style={{ fontWeight: bCount > 0 ? 800 : 400, opacity: bCount > 0 ? 1 : 0.4 }}>{bCount}</span>
                  </div>
                );
              })}
            </div>

            {/* Share options */}
            <div className="flex gap-2" style={{ marginTop: '0.2rem' }}>
              <button
                onClick={handleCopyText}
                className="btn btn-secondary flex-1 text-xs"
                style={{ padding: '0.5rem 0' }}
              >
                {copiedType === 'text' ? '📋 Copied!' : '📋 Copy Stats Card'}
              </button>
              <button
                onClick={handleCopyLink}
                className="btn btn-secondary flex-1 text-xs"
                style={{ padding: '0.5rem 0' }}
              >
                {copiedType === 'link' ? '🔗 Copied Link!' : '🔗 Copy Share Link'}
              </button>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3 w-full" style={{ marginTop: '1rem' }}>
          <button
            onClick={handleReview}
            className="btn btn-secondary flex-1 text-xs"
            style={{ padding: '0.6rem 1rem' }}
          >
            📊 Review Move Log
          </button>
          <button
            onClick={() => {
              handleClose();
              onNewGame();
            }}
            className="btn btn-primary flex-1 text-xs"
            style={{ padding: '0.6rem 1rem' }}
          >
            ⚔️ Play Rematch
          </button>
        </div>
      </div>
    </Modal>
  );
};
export default GameOverModal;
