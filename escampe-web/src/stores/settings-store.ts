import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type BoardTheme = 'classic' | 'wood' | 'glass' | 'cyberpunk';
export type PieceTheme = 'classic' | 'minimalist' | 'neon';

interface SettingsState {
  soundEnabled: boolean;
  coordinatesVisible: boolean;
  boardTheme: BoardTheme;
  pieceTheme: PieceTheme;
  highlightLegal: boolean;
  showThreats: boolean;
  
  toggleSound: () => void;
  toggleCoordinates: () => void;
  setBoardTheme: (theme: BoardTheme) => void;
  setPieceTheme: (theme: PieceTheme) => void;
  toggleHighlightLegal: () => void;
  toggleShowThreats: () => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      soundEnabled: true,
      coordinatesVisible: true,
      boardTheme: 'wood',
      pieceTheme: 'classic',
      highlightLegal: true,
      showThreats: true,

      toggleSound: () => set((state) => ({ soundEnabled: !state.soundEnabled })),
      toggleCoordinates: () => set((state) => ({ coordinatesVisible: !state.coordinatesVisible })),
      setBoardTheme: (theme) => set({ boardTheme: theme }),
      setPieceTheme: (theme) => set({ pieceTheme: theme }),
      toggleHighlightLegal: () => set((state) => ({ highlightLegal: !state.highlightLegal })),
      toggleShowThreats: () => set((state) => ({ showThreats: !state.showThreats })),
    }),
    {
      name: 'escampe-settings',
    }
  )
);
