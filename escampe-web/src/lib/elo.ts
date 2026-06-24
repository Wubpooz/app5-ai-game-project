// ELO Rating calculation utilities
// Classic ELO formula: R_new = R_old + K*(S - E)
// K = 32, E = expected score based on rating difference

export const ELO_K_FACTOR = 32;
export const INITIAL_RATING = 1200;
export const LOCAL_STORAGE_KEY = 'escampe_local_elo';

export interface LocalEloData {
  rating: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  history: Array<{ date: string; delta: number; opponent: string; result: 'win' | 'loss' | 'draw' }>;
}

export function expectedScore(ratingA: number, ratingB: number): number {
  return 1 / (1 + Math.pow(10, (ratingB - ratingA) / 400));
}

export function calculateNewRatings(
  ratingA: number,
  ratingB: number,
  result: 'a_wins' | 'b_wins' | 'draw',
): { newA: number; newB: number; deltaA: number; deltaB: number } {
  const eA = expectedScore(ratingA, ratingB);
  const eB = expectedScore(ratingB, ratingA);

  const sA = result === 'a_wins' ? 1 : result === 'draw' ? 0.5 : 0;
  const sB = result === 'b_wins' ? 1 : result === 'draw' ? 0.5 : 0;

  const deltaA = Math.round(ELO_K_FACTOR * (sA - eA));
  const deltaB = Math.round(ELO_K_FACTOR * (sB - eB));

  return {
    newA: ratingA + deltaA,
    newB: ratingB + deltaB,
    deltaA,
    deltaB,
  };
}

// Local ELO (for guests, stored in localStorage)
export function getLocalElo(): LocalEloData {
  if (typeof window === 'undefined') {
    return {
      rating: INITIAL_RATING,
      gamesPlayed: 0,
      wins: 0,
      losses: 0,
      draws: 0,
      history: [],
    };
  }
  const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (!stored) {
    const initial: LocalEloData = {
      rating: INITIAL_RATING,
      gamesPlayed: 0,
      wins: 0,
      losses: 0,
      draws: 0,
      history: [],
    };
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(initial));
    return initial;
  }
  return JSON.parse(stored) as LocalEloData;
}

export function updateLocalElo(
  result: 'win' | 'loss' | 'draw',
  opponentRating: number,
  opponentName: string,
): LocalEloData {
  const data = getLocalElo();
  const outcomeMap = { win: 'a_wins', loss: 'b_wins', draw: 'draw' } as const;
  const { newA, deltaA } = calculateNewRatings(data.rating, opponentRating, outcomeMap[result]);

  const updated: LocalEloData = {
    rating: Math.max(100, newA), // floor at 100
    gamesPlayed: data.gamesPlayed + 1,
    wins: data.wins + (result === 'win' ? 1 : 0),
    losses: data.losses + (result === 'loss' ? 1 : 0),
    draws: data.draws + (result === 'draw' ? 1 : 0),
    history: [
      { date: new Date().toISOString(), delta: deltaA, opponent: opponentName, result },
      ...data.history.slice(0, 99), // keep last 100 games
    ],
  };

  if (typeof window !== 'undefined') {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated));
  }

  return updated;
}

export function getRatingCategory(rating: number): string {
  if (rating < 600) return 'Novice';
  if (rating < 900) return 'Beginner';
  if (rating < 1200) return 'Intermediate';
  if (rating < 1500) return 'Advanced';
  if (rating < 1800) return 'Expert';
  if (rating < 2100) return 'Master';
  return 'Grandmaster';
}

export function getRatingColor(rating: number): string {
  if (rating < 600) return 'var(--rating-novice)';
  if (rating < 900) return 'var(--rating-beginner)';
  if (rating < 1200) return 'var(--rating-intermediate)';
  if (rating < 1500) return 'var(--rating-advanced)';
  if (rating < 1800) return 'var(--rating-expert)';
  if (rating < 2100) return 'var(--rating-master)';
  return 'var(--rating-grandmaster)';
}
