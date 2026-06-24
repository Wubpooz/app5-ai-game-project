import React from 'react';
import { BOT_LEVELS } from '../../engine/constants';

interface BotSelectorProps {
  selectedLevel: number;
  onSelectLevel: (level: number) => void;
}

export const BotSelector: React.FC<BotSelectorProps> = ({
  selectedLevel,
  onSelectLevel,
}) => {
  return (
    <div className="flex flex-col gap-3 w-full animate-fade-in">
      <label className="text-sm font-bold text-accent">Select Bot Difficulty</label>
      
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))',
          gap: '0.6rem',
          maxHeight: '260px',
          overflowY: 'auto',
          paddingRight: '4px',
        }}
      >
        {BOT_LEVELS.map((bot) => {
          const isSelected = selectedLevel === bot.level;
          
          return (
            <div
              key={bot.level}
              onClick={() => onSelectLevel(bot.level)}
              style={{
                cursor: 'pointer',
                background: isSelected ? 'var(--accent-glow)' : 'var(--bg-elevated)',
                border: isSelected 
                  ? '2px solid var(--accent)' 
                  : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                padding: '0.6rem',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                boxShadow: isSelected ? 'var(--shadow-glow)' : 'none',
                transition: 'all var(--transition-base)',
              }}
              className="hover:scale-95"
            >
              <span className="text-xs text-muted" style={{ fontWeight: 700 }}>
                Level {bot.level}
              </span>
              <span className="text-sm font-bold text-primary" style={{ marginTop: '0.1rem' }}>
                {bot.name}
              </span>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 800,
                  color: 'var(--accent-bright)',
                  marginTop: '0.2rem',
                  background: 'rgba(0,0,0,0.2)',
                  padding: '0.1rem 0.4rem',
                  borderRadius: '4px',
                }}
              >
                {bot.elo} ELO
              </span>
              <span className="text-xs text-muted" style={{ fontSize: '0.65rem', marginTop: '0.4rem' }}>
                Depth: {bot.maxDepth} ply
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
export default BotSelector;
