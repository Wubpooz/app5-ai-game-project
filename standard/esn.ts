import type { EsnToken, EsnMove, Square } from "./types.ts";

const SQ_RE = /^[a-f][1-6]$/;

export function parseEsn(raw: string): EsnToken {
  const s = raw.trim();
  if (s === "pass") return { pass: true };

  const captureSuffix = s.endsWith("xL");
  const body = captureSuffix ? s.slice(0, -2) : s;

  const spaceIdx = body.indexOf(" ");
  if (spaceIdx === -1) throw new Error(`ESN: missing piece id in "${raw}"`);

  const pieceRaw = body.slice(0, spaceIdx).toUpperCase();
  if (pieceRaw !== "P" && pieceRaw !== "L") {
    throw new Error(`ESN: unknown piece "${pieceRaw}" in "${raw}"`);
  }

  const pathStr = body.slice(spaceIdx + 1);
  const parts = pathStr.split(">").map(p => p.trim());

  if (parts.length < 2) {
    throw new Error(`ESN: path must have at least 2 squares in "${raw}"`);
  }
  for (const sq of parts) {
    if (!SQ_RE.test(sq)) throw new Error(`ESN: invalid square "${sq}" in "${raw}"`);
  }

  const seen = new Set<string>();
  for (const sq of parts) {
    if (seen.has(sq)) throw new Error(`ESN: square "${sq}" appears twice in "${raw}"`);
    seen.add(sq);
  }

  return {
    piece: pieceRaw as "P" | "L",
    path: parts as Square[],
    capture: captureSuffix,
  };
}

export function serializeEsn(token: EsnToken): string {
  if ("pass" in token) return "pass";
  const m = token as EsnMove;
  return `${m.piece} ${m.path.join(">")}${m.capture ? "xL" : ""}`;
}

export function arrivalSquare(m: EsnMove): Square {
  return m.path.at(-1)!;
}

export function departureSquare(m: EsnMove): Square {
  return m.path[0];
}