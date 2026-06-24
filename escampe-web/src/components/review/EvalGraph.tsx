import React from 'react';
import { useGameStore } from '../../stores/game-store';

export const EvalGraph: React.FC = () => {
  const moveRecords = useGameStore((state) => state.moveRecords);
  const reviewIndex = useGameStore((state) => state.reviewIndex);
  const goToMove = useGameStore((state) => state.goToMove);

  if (moveRecords.length === 0) {
    return (
      <div className="glass flex items-center justify-center p-6 text-center text-xs text-muted" style={{ height: '120px' }}>
        No evaluation data available yet
      </div>
    );
  }

  // Dimensions of SVG
  const width = 600;
  const height = 120;
  const padding = 15;

  // Map scores between -1500 and +1500. Clamp to fit
  const getCoordinates = (index: number, score: number) => {
    const totalMoves = moveRecords.length;
    const x = padding + (index / (totalMoves - 1 || 1)) * (width - 2 * padding);
    
    // Clamp score
    const clampedScore = Math.max(-1500, Math.min(1500, score));
    // Normalize to Y: +1500 is top (padding), -1500 is bottom (height - padding)
    const y = height / 2 - (clampedScore / 1500) * (height / 2 - padding);
    
    return { x, y };
  };

  // Generate SVG path string
  const points = moveRecords.map((rec, i) => getCoordinates(i, rec.evalAfter));
  
  // Starting point (index -1, eval 0)
  const startCoord = getCoordinates(0, 0);
  
  let linePath = `M ${startCoord.x} ${startCoord.y}`;
  points.forEach((pt) => {
    linePath += ` L ${pt.x} ${pt.y}`;
  });

  // Area under/above curve fill path
  const fillPath = `${linePath} L ${points[points.length - 1].x} ${height / 2} L ${startCoord.x} ${height / 2} Z`;

  const handleGraphClick = (e: React.MouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const percentX = (clickX - padding) / (rect.width - 2 * padding);
    
    // Find closest move index
    const totalMoves = moveRecords.length;
    const targetIndex = Math.max(0, Math.min(totalMoves - 1, Math.round(percentX * (totalMoves - 1))));
    goToMove(targetIndex);
  };

  return (
    <div className="glass p-3 flex flex-col gap-2 w-full animate-fade-in">
      <div className="flex items-center justify-between text-xs">
        <span className="font-bold text-accent">Evaluation History Graph</span>
        <span className="text-muted">Click graph to navigate</span>
      </div>

      <div className="relative w-full" style={{ background: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-sm)' }}>
        <svg
          viewBox={`0 0 ${width} ${height}`}
          width="100%"
          height="100%"
          style={{ cursor: 'pointer', overflow: 'visible' }}
          onClick={handleGraphClick}
        >
          <defs>
            {/* Linear gradient for line fill */}
            <linearGradient id="graph-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.15" />
              <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* Dotted Zero Line (Balance of power) */}
          <line
            x1={padding}
            y1={height / 2}
            x2={width - padding}
            y2={height / 2}
            stroke="var(--border-default)"
            strokeWidth="1.5"
            strokeDasharray="4 4"
          />

          {/* Shaded Area fill under graph line */}
          <path d={fillPath} fill="url(#graph-fill)" />

          {/* Main Evaluation Line */}
          <path
            d={linePath}
            fill="none"
            stroke="var(--accent)"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Draw nodes */}
          {points.map((pt, i) => {
            const isCurrent = i === reviewIndex;
            return (
              <g key={i}>
                <circle
                  cx={pt.x}
                  cy={pt.y}
                  r={isCurrent ? 5 : 2.5}
                  fill={isCurrent ? 'var(--accent-bright)' : 'var(--text-muted)'}
                  stroke={isCurrent ? 'var(--bg-base)' : 'none'}
                  strokeWidth={isCurrent ? 1.5 : 0}
                  style={{
                    filter: isCurrent ? 'drop-shadow(0 0 4px var(--accent))' : 'none',
                    transition: 'all 0.15s ease',
                  }}
                />
              </g>
            );
          })}

          {/* Glowing indicator line for the selected index */}
          {reviewIndex >= 0 && points[reviewIndex] && (
            <line
              x1={points[reviewIndex].x}
              y1={padding}
              x2={points[reviewIndex].x}
              y2={height - padding}
              stroke="var(--accent-glow)"
              strokeWidth="1"
              pointerEvents="none"
            />
          )}
        </svg>
      </div>
    </div>
  );
};
export default EvalGraph;
