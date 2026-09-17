// ─── Escampe Standards — Core types ─────────────────────────────────────────

export type Side = "w" | "b";
export type ReqClass = "s" | "d" | "t" | "*";
export type PieceSymbol = "P" | "L" | "p" | "l";
export type Col = "a" | "b" | "c" | "d" | "e" | "f";
export type Row = "1" | "2" | "3" | "4" | "5" | "6";
export type Square = `${Col}${Row}`;
export type GameResult = "1-0" | "0-1" | "1/2-1/2" | "*";

// ─── ESN ─────────────────────────────────────────────────────────────────────

export interface EsnMove {
  piece: "P" | "L";
  path: Square[];      // minimum 2 squares (from → to, plus intermediates)
  capture: boolean;    // true iff this move captures the opposing licorne (xL)
}

export interface EsnPass {
  pass: true;
}

export type EsnToken = EsnMove | EsnPass;

export function isPass(m: EsnToken): m is EsnPass {
  return "pass" in m && m.pass === true;
}

// ─── EPN ─────────────────────────────────────────────────────────────────────

export interface EpnState {
  board: (PieceSymbol | null)[][];  // [row][col], row 0 = row 6 (black back)
  side: Side;
  req: ReqClass;
  fullmove: number;
  ply: number;
}

// ─── EGN ─────────────────────────────────────────────────────────────────────

export interface EgnHeaders {
  Variant: "Escampe";
  InitialPosition: string;
  White: string;
  Black: string;
  Result: GameResult;
  Event?: string;
  Site?: string;
  Date?: string;
  Round?: string;
  Annotator?: string;
  TimeControl?: string;
  Termination?: string;
  [key: string]: string | undefined;
}

export interface EgnMovePair {
  number: number;
  white: EsnToken;
  black?: EsnToken;
}

export interface EgnGame {
  headers: EgnHeaders;
  moves: EgnMovePair[];
  result: GameResult;
}

// ─── EEI-UCI ─────────────────────────────────────────────────────────────────

export interface SearchOptions {
  depth?: number;
  movetime?: number;
  wtime?: number;
  btime?: number;
}

export interface SearchInfo {
  depth: number;
  scoreCp: number;
  nodes?: number;
  time?: number;
  pv?: EsnToken[];
}

export interface EngineOption {
  name: string;
  type: "spin" | "check" | "string" | "button" | "combo";
  default?: string | number | boolean;
  min?: number;
  max?: number;
  var?: string[];
}

// ─── EEI-CECP ────────────────────────────────────────────────────────────────

export interface CecpFeatures {
  variants: string;
  setboard: 1 | 0;
  usermove: 1 | 0;
  myname: string;
  done: 1;
}