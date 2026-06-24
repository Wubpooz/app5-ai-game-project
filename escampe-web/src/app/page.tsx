'use client';

import React, { useState } from 'react';
import { useGameStore, GameMode } from '../stores/game-store';
import { GameContainer } from '../components/game/GameContainer';
import { ReviewContainer } from '../components/review/ReviewContainer';
import { BotSelector } from '../components/lobby/BotSelector';
import { TimeControlPicker } from '../components/lobby/TimeControlPicker';
import { Piece } from '../components/board/Piece';
import type { Side } from '../engine/types';
import { SettingsModal } from '../components/ui/SettingsModal';

export default function Home() {
  // Game states
  const gameState = useGameStore((state) => state.gameState);
  const gameActive = useGameStore((state) => state.gameActive);
  const reviewMode = useGameStore((state) => state.reviewMode);
  const initGame = useGameStore((state) => state.initGame);

  // Hydration safety mount check
  const [mounted, setMounted] = useState(false);
  React.useEffect(() => {
    setMounted(true);
  }, []);

  // Settings modal visibility state
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  // Local lobby settings
  const [activeTab, setActiveTab] = useState<GameMode>('vs-bot');
  const [botLevelWhite, setBotLevelWhite] = useState<number>(5); // default Player level
  const [botLevelBlack, setBotLevelBlack] = useState<number>(5);
  const [playerColor, setPlayerColor] = useState<Side | 'random'>('white');
  const [timeControl, setTimeControl] = useState<{ limitSecs: number; incrementSecs: number } | null>(null);

  // Check if a game is currently active
  const isGameActive = gameActive;

  const handleStartGame = () => {
    console.log('[LOBBY] Start Game clicked. Configuration:', {
      mode: activeTab,
      botLevelWhite: activeTab === 'bot-vs-bot' ? botLevelWhite : botLevelBlack,
      botLevelBlack,
      playerColor,
      timeControl,
    });
    initGame({
      mode: activeTab,
      botLevelWhite: activeTab === 'bot-vs-bot' ? botLevelWhite : botLevelBlack,
      botLevelBlack,
      playerColor,
      timeControl,
    });
  };

  const handleNewGameClick = () => {
    console.log('[GAME] Rematch button clicked. Restarting game settings.');
    const store = useGameStore.getState();
    store.initGame({
      mode: store.gameMode,
      botLevelWhite: store.botLevelWhite,
      botLevelBlack: store.botLevelBlack,
      playerColor: store.playerColor,
      timeControl: store.timeControl,
    });
  };

  const exitToLobby = () => {
    console.log('[HEADER] Exit to Lobby clicked. Resetting active game state.');
    useGameStore.setState({ gameActive: false, reviewMode: false });
  };

  const handleOpenSettings = () => {
    console.log('[HEADER] Settings button clicked. Opening settings panel modal.');
    setIsSettingsOpen(true);
  };

  // Prevent hydration event listener mismatches on load
  if (!mounted) {
    return (
      <div style={{ display: 'flex', minHeight: '100vh', alignItems: 'center', justifyContent: 'center', background: '#0f1117' }}>
        <div style={{ color: 'var(--accent)', fontWeight: 800, fontSize: '1.5rem', opacity: 0.8, letterSpacing: '0.1em' }}>
          ESCAMPE<span style={{ color: 'var(--text-primary)' }}>.AI</span>
        </div>
      </div>
    );
  }

  // Render review container
  if (reviewMode) {
    return (
      <div className="page-container animate-fade-in">
        <header className="flex items-center justify-between" style={{ marginBottom: '2rem' }}>
          <div className="site-logo" style={{ cursor: 'pointer' }} onClick={exitToLobby}>
            ESCAMPE<span>.AI</span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={exitToLobby}
              className="btn btn-secondary text-xs flex items-center gap-2"
              style={{ padding: '0.4rem 0.8rem', border: '1px solid var(--border-default)' }}
            >
              🚪 Lobby
            </button>
            <button
              onClick={handleOpenSettings}
              className="btn btn-secondary text-xs flex items-center gap-2"
              style={{ padding: '0.4rem 0.8rem', border: '1px solid var(--border-default)' }}
            >
              ⚙️ Settings
            </button>
          </div>
        </header>
        <main>
          <ReviewContainer />
        </main>
        <SettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
      </div>
    );
  }

  // Render active game
  if (isGameActive) {
    return (
      <div className="page-container animate-fade-in">
        <header className="flex items-center justify-between" style={{ marginBottom: '2rem' }}>
          <div className="site-logo" style={{ cursor: 'pointer' }} onClick={exitToLobby}>
            ESCAMPE<span>.AI</span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={exitToLobby}
              className="btn btn-secondary text-xs flex items-center gap-2"
              style={{ padding: '0.4rem 0.8rem', border: '1px solid var(--border-default)' }}
            >
              🚪 Exit to Lobby
            </button>
            <button
              onClick={handleOpenSettings}
              className="btn btn-secondary text-xs flex items-center gap-2"
              style={{ padding: '0.4rem 0.8rem', border: '1px solid var(--border-default)' }}
            >
              ⚙️ Settings
            </button>
          </div>
        </header>
        <main>
          <GameContainer onNewGame={handleNewGameClick} />
        </main>
        <SettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
      </div>
    );
  }

  // Render lobby screen
  return (
    <div className="page-container animate-fade-in" style={{ minHeight: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      {/* Header */}
      <header className="flex items-center justify-between" style={{ marginBottom: '4rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1.2rem' }}>
        <div className="site-logo">
          ESCAMPE<span>.AI</span>
        </div>
        
        <div className="flex items-center gap-4">
          <div className="nav-links" style={{ margin: 0 }}>
            <a href="#" className="nav-link active">Lobby Play</a>
            <a
              href="https://github.com/m-c-c/escampe"
              target="_blank"
              rel="noopener noreferrer"
              className="nav-link"
            >
              GitHub rules
            </a>
          </div>
          <button
            onClick={handleOpenSettings}
            className="btn btn-secondary text-xs flex items-center gap-2"
            style={{ padding: '0.4rem 0.8rem', border: '1px solid var(--border-default)' }}
          >
            ⚙️ Settings
          </button>
        </div>
      </header>

      {/* Main Lobby Split Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr',
          gap: '4rem',
          alignItems: 'center',
          width: '100%',
          maxWidth: '1100px',
          margin: '0 auto',
          paddingBottom: '3rem',
          flex: 1,
        }}
        className="lobby-split"
      >
        {/* Left Side: Game configuration panel */}
        <div className="glass p-8 flex flex-col gap-6" style={{ width: '100%', maxWidth: '460px', margin: '0 auto', boxShadow: 'var(--shadow-md)' }}>
          <div className="flex flex-col gap-1">
            <h2 className="text-2xl font-black text-primary">Battle Room</h2>
            <p className="text-xs text-secondary">
              Configure your game rules and start playing Escampe.
            </p>
          </div>

          {/* Mode Selector Tabs */}
          <div
            style={{
              display: 'flex',
              background: 'rgba(0,0,0,0.2)',
              borderRadius: 'var(--radius-md)',
              padding: '0.2rem',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <button
              onClick={() => setActiveTab('vs-bot')}
              className="flex-1 text-xs font-bold"
              style={{
                background: activeTab === 'vs-bot' ? 'var(--accent)' : 'transparent',
                color: activeTab === 'vs-bot' ? 'var(--text-inverse)' : 'var(--text-secondary)',
                border: 'none',
                padding: '0.5rem 0',
                borderRadius: '6px',
                transition: 'all var(--transition-fast)',
              }}
            >
              VS Bot
            </button>
            <button
              onClick={() => setActiveTab('pass-and-play')}
              className="flex-1 text-xs font-bold"
              style={{
                background: activeTab === 'pass-and-play' ? 'var(--accent)' : 'transparent',
                color: activeTab === 'pass-and-play' ? 'var(--text-inverse)' : 'var(--text-secondary)',
                border: 'none',
                padding: '0.5rem 0',
                borderRadius: '6px',
                transition: 'all var(--transition-fast)',
              }}
            >
              Local Play
            </button>
            <button
              onClick={() => setActiveTab('bot-vs-bot')}
              className="flex-1 text-xs font-bold"
              style={{
                background: activeTab === 'bot-vs-bot' ? 'var(--accent)' : 'transparent',
                color: activeTab === 'bot-vs-bot' ? 'var(--text-inverse)' : 'var(--text-secondary)',
                border: 'none',
                padding: '0.5rem 0',
                borderRadius: '6px',
                transition: 'all var(--transition-fast)',
              }}
            >
              Bot vs Bot
            </button>
          </div>

          {/* Time control selector */}
          <TimeControlPicker onSelectTime={setTimeControl} />

          {/* Bot Level settings */}
          {activeTab === 'vs-bot' && (
            <BotSelector selectedLevel={botLevelBlack} onSelectLevel={setBotLevelBlack} />
          )}

          {activeTab === 'bot-vs-bot' && (
            <div className="flex flex-col gap-4">
              <BotSelector selectedLevel={botLevelWhite} onSelectLevel={setBotLevelWhite} />
              <div style={{ borderBottom: '1px dashed var(--border-subtle)' }} />
              <BotSelector selectedLevel={botLevelBlack} onSelectLevel={setBotLevelBlack} />
            </div>
          )}

          {/* Side selection (Only for vs-bot) */}
          {activeTab === 'vs-bot' && (
            <div className="flex flex-col gap-2">
              <span className="text-sm font-bold text-accent">Choose Side (White moves first)</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPlayerColor('white')}
                  className={`btn ${playerColor === 'white' ? 'btn-primary' : 'btn-secondary'} text-xs flex-1`}
                  style={{ padding: '0.6rem 0' }}
                >
                  🏰 White
                </button>
                <button
                  onClick={() => setPlayerColor('random')}
                  className={`btn ${playerColor === 'random' ? 'btn-primary' : 'btn-secondary'} text-xs flex-1`}
                  style={{ padding: '0.6rem 0' }}
                >
                  ❓ Random
                </button>
                <button
                  onClick={() => setPlayerColor('black')}
                  className={`btn ${playerColor === 'black' ? 'btn-primary' : 'btn-secondary'} text-xs flex-1`}
                  style={{ padding: '0.6rem 0' }}
                >
                  ⚔️ Black
                </button>
              </div>
            </div>
          )}

          {/* Play CTA */}
          <button
            onClick={handleStartGame}
            className="btn btn-primary w-full text-sm font-black"
            style={{ padding: '0.8rem 0', marginTop: '0.5rem' }}
          >
            {activeTab === 'vs-bot' && '⚔️ CHALLENGE ENGINE'}
            {activeTab === 'pass-and-play' && '👥 START PASS & PLAY'}
            {activeTab === 'bot-vs-bot' && '🤖 LAUNCH BOT SIMULATION'}
          </button>
        </div>

        {/* Right Side: Showcase board graphic */}
        <div className="lobby-showcase flex flex-col items-center text-center gap-6" style={{ margin: '0 auto' }}>
          <div
            style={{
              width: '280px',
              height: '280px',
              borderRadius: 'var(--radius-lg)',
              background: '#5c3d10',
              border: '8px solid #3d2508',
              boxShadow: 'var(--shadow-lg), 0 0 30px rgba(0,0,0,0.5)',
              display: 'grid',
              gridTemplateRows: 'repeat(6, 1fr)',
              gridTemplateColumns: 'repeat(6, 1fr)',
              transform: 'perspective(600px) rotateX(15deg) rotateY(-5deg) rotateZ(5deg)',
              animation: 'spin-board 15s ease-in-out infinite alternate',
            }}
          >
            {Array.from({ length: 36 }).map((_, idx) => {
              const row = Math.floor(idx / 6);
              const col = idx % 6;
              const isDark = (row + col) % 2 === 0;
              
              // Add some demo pieces for design aesthetics
              const isWhiteUnicorn = row === 0 && col === 4;
              const isBlackUnicorn = row === 5 && col === 1;
              const isWhitePaladin = row === 0 && col === 0 || row === 1 && col === 3;
              const isBlackPaladin = row === 5 && col === 5 || row === 4 && col === 2;

              return (
                <div
                  key={idx}
                  style={{
                    background: isDark ? '#a87c32' : '#dfc593',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    position: 'relative',
                  }}
                >
                  {isWhiteUnicorn && <Piece type="B" size={32} />}
                  {isBlackUnicorn && <Piece type="N" size={32} />}
                  {isWhitePaladin && <Piece type="b" size={28} />}
                  {isBlackPaladin && <Piece type="n" size={28} />}
                  <div
                    style={{
                      position: 'absolute',
                      inset: '2px',
                      borderRadius: '50%',
                      border: '1px solid rgba(255,255,255,0.08)',
                    }}
                  />
                </div>
              );
            })}
          </div>

          <div className="flex flex-col gap-2 max-w-sm">
            <h1 className="text-3xl font-black text-primary">ESCAMPE</h1>
            <p className="text-sm text-secondary">
              A game of steps and constraints. Land a piece to restrict where your opponent can move next. Capture their Unicorn to win.
            </p>
          </div>
        </div>
      </div>

      {/* Showcase animation style */}
      <style jsx global>{`
        @keyframes spin-board {
          0% {
            transform: perspective(800px) rotateX(20deg) rotateY(-10deg) rotateZ(5deg);
          }
          100% {
            transform: perspective(800px) rotateX(25deg) rotateY(10deg) rotateZ(-5deg);
          }
        }
        @media (min-width: 820px) {
          .lobby-split {
            grid-template-columns: 1fr 1fr !important;
          }
        }
        @media (max-width: 819px) {
          .lobby-split {
            grid-template-columns: 1fr !important;
            gap: 4rem !important;
          }
          .lobby-showcase {
            order: -1;
          }
        }
      `}</style>
      <SettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
    </div>
  );
}
