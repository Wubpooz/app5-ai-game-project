import React from 'react';
import { useGameStore } from '../../stores/game-store';

interface PlayerCardProps {
  side: 'white' | 'black';
  name: string;
  rating?: number;
  isBot?: boolean;
}

export const PlayerCard: React.FC<PlayerCardProps> = ({
  side,
  name,
  rating = 1200,
  isBot = false,
}) => {
  const clocks = useGameStore((state) => state.clocks);
  const gameState = useGameStore((state) => state.gameState);
  const timeControl = useGameStore((state) => state.timeControl);
  const clockActive = useGameStore((state) => state.clockActive);
  const isSearching = useGameStore((state) => state.isSearching);

  const isCurrentTurn = gameState.currentSide === side && gameState.phase === 'playing';
  const remainingTime = side === 'white' ? clocks.white : clocks.black;

  // Format time (mm:ss or mm:ss.d if under 10 seconds)
  const formatTime = () => {
    if (!timeControl) return '∞';
    
    const minutes = Math.floor(remainingTime / 60);
    const seconds = Math.floor(remainingTime % 60);
    
    if (remainingTime < 10 && remainingTime > 0) {
      const tenths = Math.floor((remainingTime % 1) * 10);
      return `${seconds}.${tenths}`;
    }
    
    const secStr = seconds < 10 ? `0${seconds}` : seconds;
    return `${minutes}:${secStr}`;
  };

  // Get ELO badge color class
  const getRatingBadgeClass = () => {
    if (rating < 800) return 'color: var(--rating-novice)';
    if (rating < 1200) return 'color: var(--rating-beginner)';
    if (rating < 1500) return 'color: var(--rating-intermediate)';
    if (rating < 1800) return 'color: var(--rating-advanced)';
    if (rating < 2000) return 'color: var(--rating-expert)';
    if (rating < 2200) return 'color: var(--rating-master)';
    return 'color: var(--rating-grandmaster)';
  };

  const isLowTime = remainingTime <= 15 && timeControl !== null;

  return (
    <div
      className="glass flex items-center justify-between p-3 w-full"
      style={{
        borderLeft: isCurrentTurn 
          ? '4px solid var(--accent)' 
          : '1px solid var(--glass-border)',
        boxShadow: isCurrentTurn ? 'var(--shadow-glow)' : 'none',
        transition: 'all var(--transition-base)',
      }}
    >
      {/* Player Identity Info */}
      <div className="flex items-center gap-3">
        {/* Avatar */}
        <div
          style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            background: side === 'white' 
              ? 'linear-gradient(135deg, #f8f9fa 0%, #cfbfa7 100%)' 
              : 'linear-gradient(135deg, #1b2030 0%, #0d0f17 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.2rem',
            border: '1px solid var(--border-default)',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          {isBot ? '🤖' : side === 'white' ? '🏰' : '⚔️'}
        </div>

        {/* Name and Rating */}
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="font-bold text-sm text-primary">{name}</span>
            {isBot && (
              <span
                style={{
                  fontSize: '0.65rem',
                  background: 'var(--accent)',
                  color: 'var(--text-inverse)',
                  padding: '0.1rem 0.3rem',
                  borderRadius: '4px',
                  fontWeight: 800,
                }}
              >
                BOT
              </span>
            )}
          </div>
          <span
            style={{
              fontSize: '0.72rem',
              fontWeight: 700,
            }}
            className="rating-badge"
          >
            <span style={{ color: 'var(--text-muted)' }}>Rating: </span>
            <span style={{ color: 'var(--accent-bright)' }}>{rating} ELO</span>
          </span>
        </div>
      </div>

      {/* Clock Display */}
      {timeControl && (
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontWeight: 800,
            fontSize: '1.25rem',
            padding: '0.4rem 0.8rem',
            borderRadius: 'var(--radius-sm)',
            minWidth: '76px',
            textAlign: 'center',
            background: isLowTime 
              ? 'rgba(232, 68, 68, 0.15)' 
              : isCurrentTurn 
              ? 'var(--text-primary)' 
              : 'var(--bg-elevated)',
            color: isLowTime 
              ? 'var(--quality-blunder)' 
              : isCurrentTurn 
              ? 'var(--bg-base)' 
              : 'var(--text-secondary)',
            border: isLowTime 
              ? '1px solid var(--quality-blunder)' 
              : isCurrentTurn 
              ? '1px solid var(--accent)' 
              : '1px solid var(--border-subtle)',
            boxShadow: isCurrentTurn && !isLowTime ? '0 0 10px rgba(255,255,255,0.2)' : 'none',
            animation: isLowTime && isCurrentTurn ? 'pulse-glow 1s infinite alternate' : 'none',
            transition: 'all var(--transition-base)',
          }}
        >
          {isCurrentTurn && isSearching && side !== gameState.currentSide ? '...' : formatTime()}
        </div>
      )}
    </div>
  );
};
export default PlayerCard;
