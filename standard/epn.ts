import type { EpnState, PieceSymbol, Side, ReqClass } from "./types.ts";

const PIECES = new Set<string>(["P", "L", "p", "l"]);

export function parseEpn(raw: string): EpnState {
  const parts = raw.trim().split(" ");
  if (parts.length !== 5) {
    throw new Error(`EPN: expected 5 fields, got ${parts.length} in "${raw}"`);
  }

  const [boardStr, sideStr, reqStr, fullmoveStr, plyStr] = parts;

  const rows = boardStr.split("/");
  if (rows.length !== 6) throw new Error(`EPN: expected 6 rows in board "${boardStr}"`);

  const board: (PieceSymbol | null)[][] = rows.map((rowStr, ri) => {
    const cells: (PieceSymbol | null)[] = [];
    for (const ch of rowStr) {
      if (PIECES.has(ch)) {
        cells.push(ch as PieceSymbol);
      } else if (ch >= "1" && ch <= "6") {
        const n = Number.parseInt(ch, 10);
        for (let i = 0; i < n; i++) cells.push(null);
      } else {
        throw new Error(`EPN: unexpected char "${ch}" in row ${6 - ri}`);
      }
    }
    if (cells.length !== 6) {
      throw new Error(`EPN: row ${6 - ri} has ${cells.length} cells, expected 6`);
    }
    return cells;
  });

  if (sideStr !== "w" && sideStr !== "b") {
    throw new Error(`EPN: invalid side "${sideStr}"`);
  }
  if (!["s", "d", "t", "*"].includes(reqStr)) {
    throw new Error(`EPN: invalid req class "${reqStr}"`);
  }

  const fullmove = Number.parseInt(fullmoveStr, 10);
  const ply = Number.parseInt(plyStr, 10);
  if (Number.isNaN(fullmove) || fullmove < 1) throw new Error(`EPN: invalid fullmove "${fullmoveStr}"`);
  if (Number.isNaN(ply) || ply < 0) throw new Error(`EPN: invalid ply "${plyStr}"`);

  return {
    board,
    side: sideStr as Side,
    req: reqStr as ReqClass,
    fullmove,
    ply,
  };
}

export function serializeEpn(state: EpnState): string {
  const rowStrings = state.board.map(row => {
    let s = "";
    let empty = 0;
    for (const cell of row) {
      if (cell === null) {
        empty++;
      } else {
        if (empty > 0) { s += empty; empty = 0; }
        s += cell;
      }
    }
    if (empty > 0) s += empty;
    return s;
  });

  return [
    rowStrings.join("/"),
    state.side,
    state.req,
    state.fullmove,
    state.ply,
  ].join(" ");
}

export function lisereClass(col: number, row: number): "s" | "d" | "t" {
  const classes: ["t", "d", "s"] = ["t", "d", "s"];
  return classes[(col + row) % 3];
}

export function squareToCoords(sq: string): [col: number, row: number] {
  const col = sq.codePointAt(0)! - 97;
  const row = Number.parseInt(sq[1], 10);
  return [col, row];
}