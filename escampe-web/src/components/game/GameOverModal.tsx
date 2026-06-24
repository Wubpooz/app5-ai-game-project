import React, { useState, useRef } from 'react';
import { useGameStore } from '../../stores/game-store';
import { Modal } from '../ui/Modal';
import { getMoveClassification } from '../review/MoveClassification';
import * as htmlToImage from 'html-to-image';

interface GameOverModalProps {
  onNewGame: () => void;
}

// Convert base64 Data URL to Blob (synchronous & offline fallback)
const dataURLtoBlob = (dataurl: string): Blob => {
  const arr = dataurl.split(',');
  const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/png';
  const bstr = atob(arr[1]);
  let n = bstr.length;
  const u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }
  return new Blob([u8arr], { type: mime });
};

export const GameOverModal: React.FC<GameOverModalProps> = ({ onNewGame }) => {
  const isGameOver = useGameStore((state) => state.isGameOver);
  const gameStatusText = useGameStore((state) => state.gameStatusText);
  const setReviewMode = useGameStore((state) => state.setReviewMode);
  const moveRecords = useGameStore((state) => state.moveRecords);

  const [activeTab, setActiveTab] = useState<'result' | 'card'>('result');
  const [copiedType, setCopiedType] = useState<'text' | 'link' | null>(null);
  
  const cardRef = useRef<HTMLDivElement>(null);

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

  // Save Card as PNG (supports iOS direct share sheet -> Save to Photos, and desktop Save As)
  const handleSaveAsPNG = async () => {
    if (!cardRef.current) return;
    try {
      console.log('[GAMEOVER] Generating PNG image of the Match Card...');
      
      const dataUrl = await htmlToImage.toPng(cardRef.current, {
        quality: 0.95,
        backgroundColor: '#181c27', // Use surface background
        style: {
          transform: 'scale(1)',
          borderRadius: '12px',
        }
      });
      
      const blob = dataURLtoBlob(dataUrl);
      const file = new File([blob], 'escampe-match-card.png', { type: 'image/png' });
      
      // Check for iOS/macOS Safari touch device to use Web Share Sheet
      const isIOS = typeof navigator !== 'undefined' && (
        /iPad|iPhone|iPod/.test(navigator.userAgent) || 
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
      );

      if (isIOS && navigator.share) {
        if (navigator.canShare && navigator.canShare({ files: [file] })) {
          await navigator.share({
            files: [file],
            title: 'Escampe Match Card',
            text: 'Check out my Escampe match accuracy card!',
          });
          console.log('[GAMEOVER] iOS Web Share sheet triggered successfully.');
          return;
        }
      }

      // Modern desktop Chrome/Edge/Opera native "Save As" file picker
      if (typeof window !== 'undefined' && 'showSaveFilePicker' in window) {
        try {
          const handle = await (window as any).showSaveFilePicker({
            suggestedName: `escampe-match-card-${whiteAccuracy}-${blackAccuracy}.png`,
            types: [{
              description: 'PNG Image',
              accept: { 'image/png': ['.png'] }
            }]
          });
          const writable = await handle.createWritable();
          await writable.write(blob);
          await writable.close();
          console.log('[GAMEOVER] Desktop Save File Picker used successfully.');
          return;
        } catch (err) {
          if ((err as Error).name === 'AbortError') {
            console.log('[GAMEOVER] User cancelled Save File Picker.');
            return;
          }
          console.error('[GAMEOVER] Save File Picker failed, falling back to <a> tag.', err);
        }
      }

      // Default desktop "Save As" download link fallback (Firefox/Safari)
      const link = document.createElement('a');
      link.download = `escampe-match-card-${whiteAccuracy}-${blackAccuracy}.png`;
      link.href = dataUrl;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      console.log('[GAMEOVER] Desktop Save As download triggered.');
    } catch (error) {
      console.error('Error generating card image:', error);
      alert('Could not generate the image. Please try again.');
    }
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

  // Hardcode color strings to prevent html-to-image styling glitches with CSS variables
  const rows = [
    { label: 'Brilliant', key: 'brilliant' as const, color: '#44e8c8', icon: '💎' },
    { label: 'Great Find', key: 'great' as const, color: '#4db8e8', icon: '🎯' },
    { label: 'Best Move', key: 'best' as const, color: '#6ac97a', icon: '⭐' },
    { label: 'Good', key: 'good' as const, color: '#6ac97a', icon: '👍' },
    { label: 'Book Setup', key: 'book' as const, color: '#c8b080', icon: '📖' },
    { label: 'Inaccuracy', key: 'inaccuracy' as const, color: '#e8c84a', icon: '🟡' },
    { label: 'Mistake', key: 'mistake' as const, color: '#e87a44', icon: '🟠' },
    { label: 'Blunder', key: 'blunder' as const, color: '#e84444', icon: '🔴' },
  ];

  return (
    <Modal isOpen={isGameOver} onClose={handleClose} title="Game Over">
      <div className="flex flex-col items-center gap-4 text-center py-2 animate-fade-in" style={{ width: '100%', maxWidth: '380px', margin: '0 auto' }}>
        
        {/* Tab Buttons */}
        <div style={{
          display: 'flex',
          background: 'rgba(0,0,0,0.15)',
          borderRadius: '8px',
          padding: '0.2rem',
          width: '100%',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          marginBottom: '0.5rem',
        }}>
          <button
            onClick={() => setActiveTab('result')}
            className="flex-1 text-xs font-bold"
            style={{
              padding: '0.5rem 0',
              borderRadius: '6px',
              border: 'none',
              background: activeTab === 'result' ? '#d4a853' : 'transparent',
              color: activeTab === 'result' ? '#10131e' : '#9ca3af',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            🏆 Result
          </button>
          <button
            onClick={() => setActiveTab('card')}
            className="flex-1 text-xs font-bold"
            style={{
              padding: '0.5rem 0',
              borderRadius: '6px',
              border: 'none',
              background: activeTab === 'card' ? '#d4a853' : 'transparent',
              color: activeTab === 'card' ? '#10131e' : '#9ca3af',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            📊 Match Card
          </button>
        </div>

        {activeTab === 'result' ? (
          // Tab 1: Game Over Result Panel
          <>
            <div style={{ fontSize: '3rem', margin: '0.5rem 0' }}>🏆</div>
            <h4 className="text-lg font-bold" style={{ color: '#f0f2f8' }}>{gameStatusText}</h4>
            <p className="text-sm" style={{ maxWidth: '320px', color: '#9ca3af' }}>
              Excellent match! You can now analyze the game move-by-move, inspect your accuracy stats, or start a new round.
            </p>
          </>
        ) : (
          // Tab 2: Match Card details (styled with hardcoded colors for clean PNG conversion)
          <div 
            ref={cardRef}
            style={{
              width: '100%',
              background: 'linear-gradient(135deg, rgba(30,34,53,0.95), rgba(15,17,23,0.98))',
              border: '2px solid #d4a853',
              borderRadius: '12px',
              padding: '1.2rem',
              boxShadow: '0 10px 15px -3px rgba(0,0,0,0.3), 0 0 20px rgba(212,168,83,0.25)',
              display: 'flex',
              flexDirection: 'column',
              gap: '1rem',
            }}
          >
            <div style={{ fontSize: '0.8rem', fontWeight: 900, textTransform: 'uppercase', letterSpacing: '0.1em', color: '#d4a853' }}>
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
                <span style={{ fontSize: '0.7rem', fontWeight: 800, color: '#9ca3af' }}>White Accuracy</span>
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
                    stroke="#4db8e8"
                    strokeWidth="6"
                    strokeDasharray={radialCircumference}
                    strokeDashoffset={getRadialOffset(blackAccuracy)}
                    strokeLinecap="round"
                    transform="rotate(-90 39 39)"
                    style={{ transition: 'stroke-dashoffset 0.8s ease-in-out', filter: 'drop-shadow(0 0 3px rgba(77,184,232,0.4))' }}
                  />
                  <text x="39" y="44" textAnchor="middle" fill="#f0f2f8" fontSize="12px" fontWeight="900">{blackAccuracy}%</text>
                </svg>
                <span style={{ fontSize: '0.7rem', fontWeight: 800, color: '#9ca3af' }}>Black Accuracy</span>
              </div>
            </div>

            {/* Quality Comparison list */}
            <div style={{ 
              display: 'flex', 
              flexDirection: 'column', 
              gap: '0.2rem', 
              padding: '0.4rem 0', 
              background: 'rgba(0,0,0,0.15)', 
              borderRadius: '4px', 
              border: '1px solid rgba(255, 255, 255, 0.08)' 
            }}>
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: '1fr 2fr 1fr', 
                fontSize: '0.65rem', 
                fontWeight: 900, 
                textTransform: 'uppercase', 
                color: '#6b7280', 
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)', 
                paddingBottom: '0.3rem', 
                marginBottom: '0.2rem' 
              }}>
                <span>White</span>
                <span>Move Quality</span>
                <span>Black</span>
              </div>

              {rows.map((row) => {
                const wCount = whiteStats.stats[row.key];
                const bCount = blackStats.stats[row.key];
                return (
                  <div key={row.key} style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 1fr', alignItems: 'center', fontSize: '0.7rem', padding: '0.25rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <span style={{ fontWeight: wCount > 0 ? 800 : 400, opacity: wCount > 0 ? 1 : 0.4, color: '#f0f2f8' }}>{wCount}</span>
                    <span style={{ color: row.color, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.2rem' }}>
                      <span>{row.icon}</span> {row.label}
                    </span>
                    <span style={{ fontWeight: bCount > 0 ? 800 : 400, opacity: bCount > 0 ? 1 : 0.4, color: '#f0f2f8' }}>{bCount}</span>
                  </div>
                );
              })}
            </div>

            {/* Share options */}
            <div className="flex flex-col gap-2" style={{ marginTop: '0.2rem' }}>
              <div className="flex gap-2">
                <button
                  onClick={handleCopyText}
                  className="btn btn-secondary flex-1 text-xs"
                  style={{ padding: '0.5rem 0' }}
                >
                  {copiedType === 'text' ? '📋 Copied!' : '📋 Copy Text'}
                </button>
                <button
                  onClick={handleSaveAsPNG}
                  className="btn btn-secondary flex-1 text-xs"
                  style={{ padding: '0.5rem 0' }}
                >
                  🖼️ Save as PNG
                </button>
              </div>
              <button
                onClick={handleCopyLink}
                className="btn btn-secondary w-full text-xs"
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
