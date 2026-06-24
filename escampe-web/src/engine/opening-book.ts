import type { Side } from './types';

// Pre-computed strong opening placements from Java Opening.java
// Format: unicorn/p1/p2/p3/p4/p5 (6 pieces per placement)
export const OPENING_BOOK: Record<Side, string[]> = {
  white: [
    'E1/A1/B1/C1/D1/F1',
    'D1/A1/B1/C1/E1/F1',
    'A1/B1/C1/D1/E1/F1',
    'C1/A1/B1/D1/E1/F1',
    'F1/A1/B1/C1/D1/E1',
    'B1/A1/C1/D1/E1/F1',
    'A2/A1/B1/C1/D1/E1',
    'D2/A1/B1/C1/D1/E1',
  ],
  black: [
    'E6/A6/B6/C6/D6/F6',
    'D6/A6/B6/C6/E6/F6',
    'A6/B6/C6/D6/E6/F6',
    'C6/A6/B6/D6/E6/F6',
    'F6/A6/B6/C6/D6/E6',
    'B6/A6/C6/D6/E6/F6',
    'A5/A6/B6/C6/D6/E6',
    'D5/A6/B6/C6/D6/E6',
  ],
};

export function getOpeningPlacement(side: Side, useBook: boolean): string | null {
  if (!useBook) return null;
  const book = OPENING_BOOK[side];
  return book[Math.floor(Math.random() * book.length)];
}
