import React from 'react';
import { useGameStore } from '../../stores/game-store';
import { Modal } from '../ui/Modal';

interface GameOverModalProps {
  onNewGame: () => void;
}

export const GameOverModal: React.FC<GameOverModalProps> = ({ onNewGame }) => {
  const isGameOver = useGameStore((state) => state.isGameOver);
  const gameStatusText = useGameStore((state) => state.gameStatusText);
  const setReviewMode = useGameStore((state) => state.setReviewMode);

  // Close helper
  const handleClose = () => {
    // Just dismiss the modal
    useGameStore.setState({ isGameOver: false });
  };

  const handleReview = () => {
    handleClose();
    setReviewMode(true);
  };

  return (
    <Modal isOpen={isGameOver} onClose={handleClose} title="Game Over">
      <div className="flex flex-col items-center gap-4 text-center py-2 animate-fade-in">
        {/* Trophy icon */}
        <div style={{ fontSize: '3rem', margin: '0.5rem 0' }}>🏆</div>

        {/* Win status text */}
        <h4 className="text-lg font-bold text-primary">{gameStatusText}</h4>
        
        <p className="text-sm text-secondary">
          Excellent match! You can now analyze the game move-by-move or start a new round.
        </p>

        {/* Action Buttons */}
        <div className="flex gap-3 w-full" style={{ marginTop: '1rem' }}>
          <button
            onClick={handleReview}
            className="btn btn-secondary flex-1 text-xs"
            style={{ padding: '0.6rem 1rem' }}
          >
            📊 Review Move Log
          </button>
          <button
            onClick={() => {
              handleClose();
              onNewGame();
            }}
            className="btn btn-primary flex-1 text-xs"
            style={{ padding: '0.6rem 1rem' }}
          >
            ⚔️ Play Rematch
          </button>
        </div>
      </div>
    </Modal>
  );
};
export default GameOverModal;
