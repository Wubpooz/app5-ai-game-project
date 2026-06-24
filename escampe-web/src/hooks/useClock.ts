import { useEffect, useRef } from 'react';
import { useGameStore } from '../stores/game-store';

export function useClock() {
  const clockActive = useGameStore((state) => state.clockActive);
  const gameState = useGameStore((state) => state.gameState);
  const clocks = useGameStore((state) => state.clocks);
  const isGameOver = useGameStore((state) => state.isGameOver);
  const timeControl = useGameStore((state) => state.timeControl);

  const lastTickRef = useRef<number | null>(null);

  useEffect(() => {
    if (!clockActive || isGameOver || !timeControl) {
      lastTickRef.current = null;
      return;
    }

    lastTickRef.current = Date.now();

    const interval = setInterval(() => {
      const now = Date.now();
      const delta = (now - (lastTickRef.current ?? now)) / 1000;
      lastTickRef.current = now;

      const currentSide = gameState.currentSide;
      const currentClocks = { ...useGameStore.getState().clocks };

      if (currentSide === 'white') {
        currentClocks.white = Math.max(0, currentClocks.white - delta);
      } else {
        currentClocks.black = Math.max(0, currentClocks.black - delta);
      }

      // Check timeout
      if (currentClocks.white <= 0) {
        clearInterval(interval);
        useGameStore.setState({
          clocks: { white: 0, black: currentClocks.black },
          isGameOver: true,
          clockActive: false,
          gameStatusText: 'Black wins by timeout!',
          soundEvent: 'gameover',
        });
        return;
      }

      if (currentClocks.black <= 0) {
        clearInterval(interval);
        useGameStore.setState({
          clocks: { white: currentClocks.white, black: 0 },
          isGameOver: true,
          clockActive: false,
          gameStatusText: 'White wins by timeout!',
          soundEvent: 'gameover',
        });
        return;
      }

      // Tick sound if time is low (< 20 seconds remaining)
      const currentRemaining = currentSide === 'white' ? currentClocks.white : currentClocks.black;
      if (currentRemaining <= 15 && Math.floor(currentClocks.white) !== Math.floor(useGameStore.getState().clocks.white) ||
          currentRemaining <= 15 && Math.floor(currentClocks.black) !== Math.floor(useGameStore.getState().clocks.black)) {
        useGameStore.setState({ soundEvent: 'tick' });
      }

      useGameStore.setState({ clocks: currentClocks });
    }, 100);

    return () => clearInterval(interval);
  }, [clockActive, gameState.currentSide, isGameOver, timeControl]);
}
export default useClock;
