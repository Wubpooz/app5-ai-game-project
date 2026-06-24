import React from 'react';

interface PieceProps {
  type: 'B' | 'b' | 'N' | 'n'; // B/b = White Unicorn/Paladin, N/n = Black Unicorn/Paladin
  size?: number;
  isSelected?: boolean;
  isThreatened?: boolean;
  isMovable?: boolean;
}

export const Piece: React.FC<PieceProps> = ({
  type,
  size = 50,
  isSelected = false,
  isThreatened = false,
  isMovable = false,
}) => {
  const isUnicorn = type === 'B' || type === 'N';
  const isWhite = type === 'B' || type === 'b';
  
  // Custom theme colors for pieces
  const strokeColor = isSelected 
    ? 'var(--accent-bright)' 
    : isThreatened 
    ? 'var(--quality-blunder)' 
    : isWhite 
    ? '#4e3b1c' 
    : '#f8f9fa';

  const gradId = `grad-${type}`;

  return (
    <div
      style={{
        width: size,
        height: size,
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
        style={{ overflow: 'visible' }}
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
              fill="url(#grad-gold)"
              stroke="var(--accent)"
              strokeWidth="2.5"
              strokeLinejoin="round"
              style={{
                filter: 'drop-shadow(0 0 4px var(--accent-bright))',
                transformOrigin: '50px 35px',
              }}
            />
            {/* Mane & Head */}
            <path
              d="M 28 85 C 28 65, 34 50, 42 40 C 46 35, 54 32, 58 35 C 64 39, 66 48, 62 54 C 58 60, 50 63, 50 68 C 50 75, 56 82, 58 85 Z"
              fill={`url(#${gradId})`}
              stroke={strokeColor}
              strokeWidth="3.5"
              strokeLinejoin="round"
            />
            {/* Eye */}
            <circle
              cx="54"
              cy="46"
              r="3.5"
              fill={isWhite ? 'var(--accent-dim)' : 'var(--accent)'}
            />
            {/* Golden Crown Base around neck */}
            <path
              d="M 40 55 L 46 51 L 50 56 L 54 50 L 58 55"
              fill="none"
              stroke="var(--accent)"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </g>
        ) : (
          /* Stylized Paladin SVG */
          <g>
            {/* Shield / Outer Crest */}
            <path
              d="M 25 32 C 25 32, 25 75, 50 88 C 75 75, 75 32, 75 32 L 50 22 Z"
              fill={`url(#${gradId})`}
              stroke={strokeColor}
              strokeWidth="3.5"
              strokeLinejoin="round"
            />
            {/* Helmet Visor Slit */}
            <path
              d="M 38 42 H 62 M 38 48 H 62 M 50 42 V 58"
              fill="none"
              stroke={isWhite ? '#6e5631' : 'var(--text-secondary)'}
              strokeWidth="2"
              strokeLinecap="round"
            />
            {/* Cross on the shield */}
            <path
              d="M 50 30 V 70 M 35 52 H 65"
              fill="none"
              stroke="var(--accent)"
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
