import React from 'react';
import { useSettingsStore } from '../../stores/settings-store';

interface PieceProps {
  type: 'B' | 'b' | 'N' | 'n'; // B/b = White Unicorn/Paladin, N/n = Black Unicorn/Paladin
  size?: number | string;
  isSelected?: boolean;
  isThreatened?: boolean;
  isMovable?: boolean;
}

export const Piece: React.FC<PieceProps> = ({
  type,
  size = '80%',
  isSelected = false,
  isThreatened = false,
  isMovable = false,
}) => {
  const isUnicorn = type === 'B' || type === 'N';
  const isWhite = type === 'B' || type === 'b';
  
  const pieceTheme = useSettingsStore((state) => state.pieceTheme);

  const isNeon = pieceTheme === 'neon';
  const isMinimalist = pieceTheme === 'minimalist';

  const gradId = `grad-${type}`;

  const neonGlowColor = isThreatened 
    ? '#ff3333' 
    : isWhite 
    ? '#ffd700' 
    : '#00ffff';

  const strokeColor = isSelected 
    ? 'var(--accent-bright)' 
    : isThreatened 
    ? 'var(--quality-blunder)' 
    : isNeon 
      ? neonGlowColor
      : isMinimalist
        ? (isWhite ? '#8a6a2e' : '#4e5569')
        : (isWhite ? '#4e3b1c' : '#f8f9fa');

  let fillColor = `url(#${gradId})`;
  let hornFill = 'url(#grad-gold)';
  let hornStroke = 'var(--accent)';
  let eyeColor = isWhite ? 'var(--accent-dim)' : 'var(--accent)';
  let crownStroke = 'var(--accent)';
  let crossStroke = 'var(--accent)';
  let visorStroke = isWhite ? '#6e5631' : 'var(--text-secondary)';
  let shadowFilter = isSelected 
    ? 'drop-shadow(0 0 8px var(--accent))' 
    : '';

  if (isNeon) {
    fillColor = '#090a0f';
    hornFill = 'none';
    hornStroke = strokeColor;
    eyeColor = strokeColor;
    crownStroke = strokeColor;
    crossStroke = strokeColor;
    visorStroke = strokeColor;
    shadowFilter = `drop-shadow(0 0 5px ${neonGlowColor})`;
  } else if (isMinimalist) {
    fillColor = isWhite ? '#fcfaf2' : '#1e2230';
    hornFill = isWhite ? '#d4a853' : '#5b6380';
    hornStroke = strokeColor;
    eyeColor = strokeColor;
    crownStroke = 'none'; // hide crown neck details
    crossStroke = isWhite ? '#d4a853' : '#5b6380';
    visorStroke = 'none'; // hide helmet visor lines
  }

  return (
    <div
      style={{
        width: typeof size === 'number' ? `${size}px` : size,
        height: typeof size === 'number' ? `${size}px` : size,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'grab',
        borderRadius: '50%',
        transition: 'transform var(--transition-fast), filter var(--transition-fast), box-shadow var(--transition-fast)',
        filter: isSelected 
          ? 'drop-shadow(0 0 8px var(--accent)) drop-shadow(0 4px 10px rgba(0,0,0,0.5))' 
          : 'drop-shadow(0 3px 6px rgba(0,0,0,0.4))',
        transform: isSelected ? 'scale(1.1) translateY(-2px)' : 'scale(1)',
        animation: isMovable ? 'pulse-glow 1.5s infinite alternate' : 'none',
        boxShadow: isMovable && !isSelected ? '0 0 10px var(--accent-glow)' : 'none',
      }}
    >
      <svg
        viewBox="0 0 100 100"
        width="90%"
        height="90%"
        style={{ overflow: 'visible', filter: shadowFilter }}
      >
        <defs>
          {/* Gold gradient for Unicorn horn */}
          <linearGradient id="grad-gold" x1="0%" y1="0%" x2="100%" y2="100%">
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

        {isUnicorn ? (
          /* Majestic Unicorn SVG */
          <g>
            {/* Horn */}
            <path
              d="M 50 15 L 56 35 L 48 37 Z"
              fill={hornFill}
              stroke={hornStroke}
              strokeWidth="2.5"
              strokeLinejoin="round"
              style={{
                filter: isNeon ? `drop-shadow(0 0 4px ${strokeColor})` : 'drop-shadow(0 0 4px var(--accent-bright))',
                transformOrigin: '50px 35px',
              }}
            />
            {/* Mane & Head */}
            <path
              d="M 28 85 C 28 65, 34 50, 42 40 C 46 35, 54 32, 58 35 C 64 39, 66 48, 62 54 C 58 60, 50 63, 50 68 C 50 75, 56 82, 58 85 Z"
              fill={fillColor}
              stroke={strokeColor}
              strokeWidth="3.5"
              strokeLinejoin="round"
            />
            {/* Eye */}
            <circle
              cx="54"
              cy="46"
              r="3.5"
              fill={eyeColor}
            />
            {/* Golden Crown Base around neck */}
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
        ) : (
          /* Stylized Paladin SVG */
          <g>
            {/* Shield / Outer Crest */}
            <path
              d="M 25 32 C 25 32, 25 75, 50 88 C 75 75, 75 32, 75 32 L 50 22 Z"
              fill={fillColor}
              stroke={strokeColor}
              strokeWidth="3.5"
              strokeLinejoin="round"
            />
            {/* Helmet Visor Slit */}
            {visorStroke !== 'none' && (
              <path
                d="M 38 42 H 62 M 38 48 H 62 M 50 42 V 58"
                fill="none"
                stroke={visorStroke}
                strokeWidth="2"
                strokeLinecap="round"
              />
            )}
            {/* Cross on the shield */}
            <path
              d="M 50 30 V 70 M 35 52 H 65"
              fill="none"
              stroke={crossStroke}
              strokeWidth="2.5"
              strokeLinecap="round"
              style={{ opacity: 0.85 }}
            />
          </g>
        )}
      </svg>
    </div>
  );
};
export default Piece;
