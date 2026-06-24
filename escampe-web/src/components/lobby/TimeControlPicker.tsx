import React, { useState } from 'react';

export interface TimePreset {
  name: string;
  limitSecs: number;
  incrementSecs: number;
  icon: string;
}

const PRESETS: (TimePreset | null)[] = [
  { name: '1 min', limitSecs: 60, incrementSecs: 0, icon: '⚡' }, // Bullet
  { name: '3 min', limitSecs: 180, incrementSecs: 0, icon: '🔥' }, // Blitz
  { name: '5 min', limitSecs: 300, incrementSecs: 0, icon: '⏱️' }, // Blitz
  { name: '10 min', limitSecs: 600, incrementSecs: 0, icon: '🕒' }, // Rapid
  { name: '30 min', limitSecs: 1800, incrementSecs: 0, icon: '🐢' }, // Classic
  null, // represent "Untimed"
];

interface TimeControlPickerProps {
  onSelectTime: (time: { limitSecs: number; incrementSecs: number } | null) => void;
}

export const TimeControlPicker: React.FC<TimeControlPickerProps> = ({ onSelectTime }) => {
  const [selectedIdx, setSelectedIdx] = useState<number>(5); // default untimed
  const [customMin, setCustomMin] = useState('15');
  const [customInc, setCustomInc] = useState('10');

  const handleSelect = (idx: number) => {
    setSelectedIdx(idx);
    if (idx === 5) {
      onSelectTime(null);
    } else if (idx === 6) { // custom
      const limit = parseInt(customMin) * 60 || 600;
      const inc = parseInt(customInc) || 0;
      onSelectTime({ limitSecs: limit, incrementSecs: inc });
    } else {
      const preset = PRESETS[idx];
      if (preset) {
        onSelectTime({ limitSecs: preset.limitSecs, incrementSecs: preset.incrementSecs });
      }
    }
  };

  const handleCustomSubmit = () => {
    const limit = parseInt(customMin) * 60 || 600;
    const inc = parseInt(customInc) || 0;
    onSelectTime({ limitSecs: limit, incrementSecs: inc });
  };

  return (
    <div className="flex flex-col gap-3 w-full animate-fade-in">
      <label className="text-sm font-bold text-accent">Select Time Control</label>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem' }}>
        {PRESETS.map((preset, idx) => {
          const isSelected = selectedIdx === idx;
          if (!preset) {
            return (
              <div
                key="untimed"
                onClick={() => handleSelect(5)}
                style={{
                  cursor: 'pointer',
                  background: isSelected ? 'var(--accent-glow)' : 'var(--bg-elevated)',
                  border: isSelected ? '2px solid var(--accent)' : '1px solid var(--border-default)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0.6rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.4rem',
                  fontWeight: 700,
                  fontSize: '0.8rem',
                  boxShadow: isSelected ? 'var(--shadow-glow)' : 'none',
                  transition: 'all var(--transition-base)',
                }}
                className="hover:scale-95"
              >
                <span>♾️</span> Untimed
              </div>
            );
          }

          return (
            <div
              key={idx}
              onClick={() => handleSelect(idx)}
              style={{
                cursor: 'pointer',
                background: isSelected ? 'var(--accent-glow)' : 'var(--bg-elevated)',
                border: isSelected ? '2px solid var(--accent)' : '1px solid var(--border-default)',
                borderRadius: 'var(--radius-md)',
                padding: '0.6rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.4rem',
                fontWeight: 700,
                fontSize: '0.8rem',
                boxShadow: isSelected ? 'var(--shadow-glow)' : 'none',
                transition: 'all var(--transition-base)',
              }}
              className="hover:scale-95"
            >
              <span>{preset.icon}</span> {preset.name}
            </div>
          );
        })}

        {/* Custom trigger */}
        <div
          onClick={() => handleSelect(6)}
          style={{
            cursor: 'pointer',
            background: selectedIdx === 6 ? 'var(--accent-glow)' : 'var(--bg-elevated)',
            border: selectedIdx === 6 ? '2px solid var(--accent)' : '1px solid var(--border-default)',
            borderRadius: 'var(--radius-md)',
            padding: '0.6rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.4rem',
            fontWeight: 700,
            fontSize: '0.8rem',
            boxShadow: selectedIdx === 6 ? 'var(--shadow-glow)' : 'none',
            transition: 'all var(--transition-base)',
          }}
          className="hover:scale-95"
        >
          ⚙️ Custom
        </div>
      </div>

      {/* Custom Inputs */}
      {selectedIdx === 6 && (
        <div
          className="glass flex items-center gap-3 p-3 animate-fade-in"
          style={{
            background: 'rgba(0,0,0,0.15)',
          }}
        >
          <div className="flex flex-col gap-1 flex-1">
            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Minutes</span>
            <input
              type="number"
              min="1"
              max="180"
              value={customMin}
              onChange={(e) => {
                setCustomMin(e.target.value);
                // Trigger auto update
                const limit = parseInt(e.target.value) * 60 || 600;
                const inc = parseInt(customInc) || 0;
                onSelectTime({ limitSecs: limit, incrementSecs: inc });
              }}
              className="input"
              style={{ padding: '0.4rem', fontSize: '0.8rem', textAlign: 'center' }}
            />
          </div>
          <div className="flex flex-col gap-1 flex-1">
            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Increment (s)</span>
            <input
              type="number"
              min="0"
              max="60"
              value={customInc}
              onChange={(e) => {
                setCustomInc(e.target.value);
                // Trigger auto update
                const limit = parseInt(customMin) * 60 || 600;
                const inc = parseInt(e.target.value) || 0;
                onSelectTime({ limitSecs: limit, incrementSecs: inc });
              }}
              className="input"
              style={{ padding: '0.4rem', fontSize: '0.8rem', textAlign: 'center' }}
            />
          </div>
        </div>
      )}
    </div>
  );
};
export default TimeControlPicker;
