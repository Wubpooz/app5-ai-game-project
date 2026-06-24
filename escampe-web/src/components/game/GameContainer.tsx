import React from 'react';
import { useGameStore } from '../../stores/game-store';
import { useClock } from '../../hooks/useClock';
import { useSound } from '../../hooks/useSound';
import { Board } from '../board/Board';
import { EvalBar } from './EvalBar';
import { PlayerCard } from './PlayerCard';
import { MoveHistory } from './MoveHistory';
import { GameControls } from './GameControls';
import { GameOverModal } from './GameOverModal';
import { BOT_LEVELS } from '../../engine/constants';

interface GameContainerProps {
  onNewGame: () => void;
}

export const GameContainer: React.FC<GameContainerProps> = ({ onNewGame }) => {
  // Start game loop clocks and sound triggers
  useClock();
  useSound();

  const gameState = useGameStore((state) => state.gameState);
  const gameMode = useGameStore((state) => state.gameMode);
  const playerColor = useGameStore((state) => state.playerColor);
  const botLevelWhite = useGameStore((state) => state.botLevelWhite);
  const botLevelBlack = useGameStore((state) => state.botLevelBlack);
  const isFlipped = useGameStore((state) => state.isFlipped);
  const gameStatusText = useGameStore((state) => state.gameStatusText);

  // Resolve player identities
  const whiteBotConfig = BOT_LEVELS[botLevelWhite - 1];
  const blackBotConfig = BOT_LEVELS[botLevelBlack - 1];

  let whitePlayerName = 'White Player';
  let whiteRating = 1200;
  let whiteIsBot = false;

  let blackPlayerName = 'Black Player';
  let blackRating = 1200;
  let blackIsBot = false;

  if (gameMode === 'vs-bot') {
    if (playerColor === 'white') {
      whitePlayerName = 'Guest Player';
      blackPlayerName = `Bot ${blackBotConfig.name}`;
      blackRating = blackBotConfig.elo;
      blackIsBot = true;
    } else {
      whitePlayerName = `Bot ${whiteBotConfig.name}`;
      whiteRating = whiteBotConfig.elo;
      whiteIsBot = true;
      blackPlayerName = 'Guest Player';
    }
  } else if (gameMode === 'bot-vs-bot') {
    whitePlayerName = `Bot ${whiteBotConfig.name}`;
    whiteRating = whiteBotConfig.elo;
    whiteIsBot = true;
    blackPlayerName = `Bot ${blackBotConfig.name}`;
    blackRating = blackBotConfig.elo;
    blackIsBot = true;
  } else {
    whitePlayerName = 'Local Player 1';
    blackPlayerName = 'Local Player 2';
  }

  // Determine top/bottom players based on flip state
  const topPlayer = isFlipped
    ? { name: whitePlayerName, rating: whiteRating, isBot: whiteIsBot, side: 'white' as const }
    : { name: blackPlayerName, rating: blackRating, isBot: blackIsBot, side: 'black' as const };

  const bottomPlayer = isFlipped
    ? { name: blackPlayerName, rating: blackRating, isBot: blackIsBot, side: 'black' as const }
    : { name: whitePlayerName, rating: whiteRating, isBot: whiteIsBot, side: 'white' as const };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
        width: '100%',
        alignItems: 'center',
      }}
    >
      {/* Game Status Banner */}
      <div
        className="glass game-status-banner"
        style={{
          padding: '0.6rem 1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderLeft: '4px solid var(--accent)',
        }}
      >
        <span className="text-xs font-bold text-secondary">
          Mode: <span className="text-accent">{gameMode.toUpperCase().replace(/-/g, ' ')}</span>
        </span>
        <span className="text-xs font-semibold text-primary">{gameStatusText}</span>
      </div>

      {/* Main Layout Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr',
          gap: '1.5rem',
          width: '100%',
          maxWidth: '890px',
        }}
        // Tailwind equivalent responsive class built with inline logic
        className="desktop-layout"
      >
        {/* Playfield: Eval Bar + Board */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            alignItems: 'center',
            width: '100%',
          }}
        >
          {/* Opponent Card for Mobile (stacked) */}
          <div className="mobile-only-player-card">
            <PlayerCard {...topPlayer} />
          </div>

          <div className="board-row-container" style={{ display: 'flex', gap: '12px', width: '100%', justifyContent: 'center', alignItems: 'stretch' }}>
            {/* Eval Bar */}
            {gameState.phase === 'playing' && (
              <div style={{ flexShrink: 0 }}>
                <EvalBar />
              </div>
            )}
            
            {/* Board */}
            <div style={{ flexGrow: 1, minWidth: 0, maxWidth: '560px', width: '100%' }}>
              <Board />
            </div>
          </div>

          {/* Self Card for Mobile (stacked) */}
          <div className="mobile-only-player-card">
            <PlayerCard {...bottomPlayer} />
          </div>
        </div>

        {/* Sidebar panels */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            width: '100%',
          }}
          className="game-sidebar"
        >
          {/* Top Player (Opponent) */}
          <div className="desktop-only-player-card">
            <PlayerCard {...topPlayer} />
          </div>

          {/* Move History Log */}
          <MoveHistory />

          {/* Controls */}
          <GameControls />

          {/* Bottom Player (Self) */}
          <div className="desktop-only-player-card">
            <PlayerCard {...bottomPlayer} />
          </div>
        </div>
      </div>

      {/* Game Over modal overlay */}
      <GameOverModal onNewGame={onNewGame} />

      {/* CSS adjustments for desktop layout grid and media queries */}
      <style jsx global>{`
        .game-status-banner {
          width: 100%;
          max-width: 890px;
          margin: 0 auto;
        }
        .board-row-container {
          width: 100%;
          max-width: 600px;
          margin: 0 auto;
        }
        .mobile-only-player-card {
          display: none;
        }
        .desktop-only-player-card {
          display: block;
        }
        @media (min-width: 820px) {
          .desktop-layout {
            grid-template-columns: 1fr 300px !important;
          }
        }
        @media (max-width: 819px) {
          .desktop-layout {
            grid-template-columns: 1fr !important;
          }
          .mobile-only-player-card {
            display: block;
            max-width: min(92vw, 560px);
            width: 100%;
            margin: 0 auto;
          }
          .desktop-only-player-card {
            display: none;
          }
          .game-status-banner {
            max-width: min(92vw, 560px);
          }
          .board-row-container {
            max-width: min(92vw, 560px);
          }
          .game-sidebar {
            max-width: min(92vw, 560px);
            margin: 0 auto;
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
};
export default GameContainer;
