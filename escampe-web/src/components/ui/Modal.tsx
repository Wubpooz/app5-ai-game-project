import React, { useEffect } from 'react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ isOpen, onClose, title, children }) => {
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexShrink: 0 }}>
          <h3 className="text-xl font-bold" style={{ color: 'var(--accent)' }}>{title}</h3>
          <button 
            onClick={onClose} 
            style={{ 
              padding: '0.2rem 0.5rem', 
              borderRadius: '4px', 
              fontSize: '1.5rem', 
              background: 'transparent', 
              border: 'none',
              cursor: 'pointer', 
              color: 'var(--text-secondary)' 
            }}
          >
            &times;
          </button>
        </div>
        <div className="modal-scroll-content" style={{ maxHeight: '60vh', overflowY: 'auto', flex: 1, paddingRight: '4px', WebkitOverflowScrolling: 'touch' }}>{children}</div>
      </div>
    </div>
  );
};
export default Modal;
