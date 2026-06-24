import React from 'react';
import { Modal } from './Modal';
import { useSettingsStore, BoardTheme, PieceTheme } from '../../stores/settings-store';
import { Piece } from '../board/Piece';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({ isOpen, onClose }) => {
  const {
    soundEnabled,
    coordinatesVisible,
    boardTheme,
    pieceTheme,
    highlightLegal,
    showThreats,
    toggleSound,
    toggleCoordinates,
    setBoardTheme,
    setPieceTheme,
    toggleHighlightLegal,
    toggleShowThreats,
  } = useSettingsStore();

  const boardThemesList: { id: BoardTheme; label: string; desc: string; colors: [string, string] }[] = [
    { id: 'classic', label: 'Classic', desc: 'Standard gray/silver checkers', colors: ['#d1d5db', '#5c6370'] },
    { id: 'wood', label: 'Wood', desc: 'Traditional wood grains', colors: ['#dfc593', '#a87c32'] },
    { id: 'glass', label: 'Glass', desc: 'Frosted semi-transparent', colors: ['rgba(255,255,255,0.12)', 'rgba(255,255,255,0.05)'] },
    { id: 'cyberpunk', label: 'Cyberpunk', desc: 'Futuristic neon blueprint', colors: ['#1e2436', '#141824'] },
  ];

  const pieceThemesList: { id: PieceTheme; label: string; desc: string; type: 'B' | 'b' | 'N' | 'n' }[] = [
    { id: 'classic', label: 'Classic Vector', desc: 'Majestic detailed crowns & shields', type: 'B' },
    { id: 'minimalist', label: 'Minimalist', desc: 'Clean geometric lines', type: 'B' },
    { id: 'neon', label: 'Cyber Neon', desc: 'Glow rings & cyber design', type: 'B' },
  ];

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Customize Experience">
      <div className="flex flex-col gap-6 pt-2 pr-1" style={{ paddingBottom: '1.5rem' }}>
        
        {/* General Toggles */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-accent mb-3">Toggles & Assist</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.8rem' }}>
            {/* Sound Toggle */}
            <button
              onClick={toggleSound}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.8rem 1rem',
                background: soundEnabled ? 'rgba(212,168,83,0.1)' : 'rgba(0,0,0,0.2)',
                border: soundEnabled ? '1px solid var(--accent)' : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                color: soundEnabled ? 'var(--accent-bright)' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
                textAlign: 'left',
              }}
            >
              <div className="flex flex-col">
                <span className="text-xs font-bold">Sound Effects</span>
                <span style={{ fontSize: '0.65rem', opacity: 0.7 }}>Move & capture sounds</span>
              </div>
              <span style={{ fontSize: '1.2rem' }}>{soundEnabled ? '🔊' : '🔇'}</span>
            </button>

            {/* Coordinates Toggle */}
            <button
              onClick={toggleCoordinates}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.8rem 1rem',
                background: coordinatesVisible ? 'rgba(212,168,83,0.1)' : 'rgba(0,0,0,0.2)',
                border: coordinatesVisible ? '1px solid var(--accent)' : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                color: coordinatesVisible ? 'var(--accent-bright)' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
                textAlign: 'left',
              }}
            >
              <div className="flex flex-col">
                <span className="text-xs font-bold">Board Coordinates</span>
                <span style={{ fontSize: '0.65rem', opacity: 0.7 }}>Show A-F and 1-6 guides</span>
              </div>
              <span style={{ fontSize: '1.2rem' }}>{coordinatesVisible ? '🔢' : '🙈'}</span>
            </button>

            {/* Legal Highlight Toggle */}
            <button
              onClick={toggleHighlightLegal}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.8rem 1rem',
                background: highlightLegal ? 'rgba(212,168,83,0.1)' : 'rgba(0,0,0,0.2)',
                border: highlightLegal ? '1px solid var(--accent)' : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                color: highlightLegal ? 'var(--accent-bright)' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
                textAlign: 'left',
              }}
            >
              <div className="flex flex-col">
                <span className="text-xs font-bold">Legal Moves</span>
                <span style={{ fontSize: '0.65rem', opacity: 0.7 }}>Highlight valid landing spots</span>
              </div>
              <span style={{ fontSize: '1.2rem' }}>{highlightLegal ? '✨' : '⚪'}</span>
            </button>

            {/* Threat Highlight Toggle */}
            <button
              onClick={toggleShowThreats}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.8rem 1rem',
                background: showThreats ? 'rgba(212,168,83,0.1)' : 'rgba(0,0,0,0.2)',
                border: showThreats ? '1px solid var(--accent)' : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                color: showThreats ? 'var(--accent-bright)' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
                textAlign: 'left',
              }}
            >
              <div className="flex flex-col">
                <span className="text-xs font-bold">Threat Detection</span>
                <span style={{ fontSize: '0.65rem', opacity: 0.7 }}>Show endangered pieces</span>
              </div>
              <span style={{ fontSize: '1.2rem' }}>{showThreats ? '⚠️' : '🛡️'}</span>
            </button>
          </div>
        </div>

        {/* Board Theme Selector */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-accent mb-3">Board Themes</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.8rem' }}>
            {boardThemesList.map((theme) => {
              const isSelected = boardTheme === theme.id;
              return (
                <div
                  key={theme.id}
                  onClick={() => setBoardTheme(theme.id)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    padding: '0.6rem',
                    background: 'rgba(0,0,0,0.2)',
                    border: isSelected ? '2px solid var(--accent)' : '1px solid var(--border-default)',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer',
                    transition: 'all var(--transition-fast)',
                    boxShadow: isSelected ? '0 0 12px var(--accent-glow)' : 'none',
                  }}
                >
                  {/* Grid Preview */}
                  <div
                    style={{
                      height: '60px',
                      borderRadius: 'var(--radius-sm)',
                      marginBottom: '0.5rem',
                      display: 'grid',
                      gridTemplateColumns: 'repeat(2, 1fr)',
                      overflow: 'hidden',
                      background: theme.id === 'glass' ? 'linear-gradient(45deg, #181c27, #252a3d)' : '#000',
                      border: theme.id === 'glass' ? '1px dashed rgba(255,255,255,0.1)' : 'none',
                    }}
                  >
                    <div style={{ background: theme.colors[0], display: 'flex', alignItems: 'center', justifyContent: 'center' }} />
                    <div style={{ background: theme.colors[1] }} />
                    <div style={{ background: theme.colors[1] }} />
                    <div style={{ background: theme.colors[0] }} />
                  </div>
                  <span className="text-xs font-bold text-primary">{theme.label}</span>
                  <span style={{ fontSize: '0.6rem', color: 'var(--text-secondary)' }}>{theme.desc}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Piece Theme Selector */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-accent mb-3">Piece Designs</h4>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.8rem' }}>
            {pieceThemesList.map((pt) => {
              const isSelected = pieceTheme === pt.id;
              
              // We'll construct a little preview piece by injecting styles
              return (
                <div
                  key={pt.id}
                  onClick={() => setPieceTheme(pt.id)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    padding: '0.8rem 0.4rem',
                    background: 'rgba(0,0,0,0.2)',
                    border: isSelected ? '2px solid var(--accent)' : '1px solid var(--border-default)',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer',
                    transition: 'all var(--transition-fast)',
                    textAlign: 'center',
                    boxShadow: isSelected ? '0 0 12px var(--accent-glow)' : 'none',
                  }}
                >
                  <div
                    style={{
                      width: '44px',
                      height: '44px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginBottom: '0.5rem',
                      borderRadius: '50%',
                      background: 'rgba(255,255,255,0.02)',
                    }}
                  >
                    {/* Render standard Piece. It will update as pieceTheme changes, but for selection we show it. */}
                    {/* We mock a Piece props by passing it to reflect the setting. Since the store updates globally, all pieces render pt.id */}
                    <div style={{ transform: 'scale(0.85)' }}>
                      <PiecePreview theme={pt.id} type={pt.type} />
                    </div>
                  </div>
                  <span className="text-xs font-bold text-primary">{pt.label}</span>
                  <span style={{ fontSize: '0.55rem', color: 'var(--text-secondary)', lineHeight: '1.2' }}>{pt.desc}</span>
                </div>
              );
            })}
          </div>
        </div>

      </div>
    </Modal>
  );
};

// Internal mini-component to render static piece previews regardless of active store setting
const PiecePreview: React.FC<{ theme: PieceTheme; type: 'B' | 'b' | 'N' | 'n' }> = ({ theme, type }) => {
  const isWhite = type === 'B' || type === 'b';
  const gradId = `preview-grad-${theme}-${type}`;

  const isNeon = theme === 'neon';
  const isMinimalist = theme === 'minimalist';

  const neonGlowColor = isWhite ? '#ffd700' : '#00ffff';
  const strokeColor = isNeon 
    ? neonGlowColor
    : isMinimalist
      ? (isWhite ? '#8a6a2e' : '#4e5569')
      : (isWhite ? '#4e3b1c' : '#f8f9fa');

  let fillColor = `url(#${gradId})`;
  let hornFill = 'url(#grad-gold-prev)';
  let hornStroke = 'var(--accent)';
  let eyeColor = isWhite ? 'var(--accent-dim)' : 'var(--accent)';
  let crownStroke = 'var(--accent)';
  let shadowFilter = '';

  if (isNeon) {
    fillColor = '#090a0f';
    hornFill = 'none';
    hornStroke = strokeColor;
    eyeColor = strokeColor;
    crownStroke = strokeColor;
    shadowFilter = `drop-shadow(0 0 4px ${neonGlowColor})`;
  } else if (isMinimalist) {
    fillColor = isWhite ? '#fcfaf2' : '#1e2230';
    hornFill = isWhite ? '#d4a853' : '#5b6380';
    hornStroke = strokeColor;
    eyeColor = strokeColor;
    crownStroke = 'none';
  }

  return (
    <svg
      viewBox="0 0 100 100"
      width="36"
      height="36"
      style={{ overflow: 'visible', filter: shadowFilter }}
    >
      <defs>
        <linearGradient id="grad-gold-prev" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="var(--accent-bright)" />
          <stop offset="100%" stopColor="var(--accent)" />
        </linearGradient>

        <linearGradient id={gradId} x1="0%" y1="0%" x2="100%" y2="100%">
          {isWhite ? (
            <>
              <stop offset="0%" stopColor="#ffffff" />
              <stop offset="100%" stopColor="#d8c191" />
            </>
          ) : (
            <>
              <stop offset="0%" stopColor="#31374a" />
              <stop offset="100%" stopColor="#10131e" />
            </>
          )}
        </linearGradient>
      </defs>

      <g>
        <path
          d="M 50 15 L 56 35 L 48 37 Z"
          fill={hornFill}
          stroke={hornStroke}
          strokeWidth="2.5"
          strokeLinejoin="round"
          style={{
            filter: isNeon ? `drop-shadow(0 0 3px ${strokeColor})` : 'drop-shadow(0 0 3px var(--accent-bright))',
            transformOrigin: '50px 35px',
          }}
        />
        <path
          d="M 28 85 C 28 65, 34 50, 42 40 C 46 35, 54 32, 58 35 C 64 39, 66 48, 62 54 C 58 60, 50 63, 50 68 C 50 75, 56 82, 58 85 Z"
          fill={fillColor}
          stroke={strokeColor}
          strokeWidth="3.5"
          strokeLinejoin="round"
        />
        <circle
          cx="54"
          cy="46"
          r="3.5"
          fill={eyeColor}
        />
        {crownStroke !== 'none' && (
          <path
            d="M 40 55 L 46 51 L 50 56 L 54 50 L 58 55"
            fill="none"
            stroke={crownStroke}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
      </g>
    </svg>
  );
};
