import { parseEsn, serializeEsn } from "./esn.ts";
import type { EgnGame, EgnHeaders, GameResult } from "./types.ts";

const TAG_RE = /^\[(\w+)\s+"([^"]*)"\]$/;
const MOVE_NUM_RE = /^\d+\.$/;
const RESULT_VALUES = new Set(["1-0", "0-1", "1/2-1/2", "*"]);

export function parseEgn(raw: string): EgnGame {
  const lines = raw.split(/\r?\n/);

  const headers: Partial<EgnHeaders> = {};
  let i = 0;
  for (; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line === "") break;
    const m = TAG_RE.exec(line);
    if (m) headers[m[1]] = m[2];
  }

  const required: (keyof EgnHeaders)[] = ["Variant", "InitialPosition", "White", "Black", "Result"];
  for (const key of required) {
    if (!headers[key]) throw new Error(`EGN: missing required header "${key}"`);
  }
  if (headers.Variant !== "Escampe") throw new Error(`EGN: Variant must be "Escampe"`);

  const moveParts: string[] = [];
  for (i++; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line) moveParts.push(...line.split(/\s+/));
  }

  const cleanText = moveParts.join(" ")
    .replace(/\{[^}]*\}/g, " ")
    .replace(/\([^)]*\)/g, " ");

  const tokens = cleanText.split(/\s+/).filter(Boolean);

  const moves: EgnGame["moves"] = [];
  let moveNum = 0;

  for (let t = 0; t < tokens.length; ) {
    const tok = tokens[t];
    if (RESULT_VALUES.has(tok)) break;

    if (MOVE_NUM_RE.test(tok)) {
      moveNum = Number.parseInt(tok, 10);
      t++;
      continue;
    }

    const whiteTok = tokens[t++];
    if (!whiteTok || RESULT_VALUES.has(whiteTok)) break;
    const white = parseEsn(whiteTok);

    let black;
    if (t < tokens.length && !MOVE_NUM_RE.test(tokens[t]) && !RESULT_VALUES.has(tokens[t])) {
      black = parseEsn(tokens[t++]);
    }

    moves.push({ number: moveNum, white, black });
  }

  return {
    headers: headers as EgnHeaders,
    moves,
    result: headers.Result as GameResult,
  };
}

export function serializeEgn(game: EgnGame): string {
  const headerOrder: Set<keyof EgnHeaders> = new Set([
    "Variant", "InitialPosition", "White", "Black", "Result",
    "Event", "Site", "Date", "Round", "Annotator", "TimeControl", "Termination",
  ]);

  const lines: string[] = [];

  for (const key of headerOrder) {
    if (game.headers[key] !== undefined) {
      lines.push(`[${key} "${game.headers[key]}"]`);
    }
  }
  for (const [k, v] of Object.entries(game.headers)) {
    if (!headerOrder.has(k as keyof EgnHeaders) && v !== undefined) {
      lines.push(`[${k} "${v}"]`);
    }
  }

  lines.push("");

  const moveText = game.moves.map(mp => {
    const parts = [`${mp.number}.`, serializeEsn(mp.white)];
    if (mp.black) parts.push(serializeEsn(mp.black));
    return parts.join(" ");
  });

  moveText.push(game.result);
  lines.push(moveText.join("\n"));

  return lines.join("\n");
}