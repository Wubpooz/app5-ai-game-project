import { parseEsn, serializeEsn } from "./esn.ts";
import type { EsnToken, EngineOption, SearchInfo, SearchOptions, GameResult } from "./types.ts";

export type GuiCommand =
  | { cmd: "eei" }
  | { cmd: "isready" }
  | { cmd: "setoption"; name: string; value?: string }
  | { cmd: "newgame" }
  | { cmd: "position"; epn: string; moves?: EsnToken[] }
  | { cmd: "go"; options?: SearchOptions }
  | { cmd: "stop" }
  | { cmd: "quit" };

export function serializeGuiCommand(c: GuiCommand): string {
  switch (c.cmd) {
    case "eei":      return "eei";
    case "isready":  return "isready";
    case "newgame":  return "newgame";
    case "stop":     return "stop";
    case "quit":     return "quit";
    case "setoption":
      return c.value !== undefined
        ? `setoption name ${c.name} value ${c.value}`
        : `setoption name ${c.name}`;
    case "position": {
      let line = `position epn ${c.epn}`;
      if (c.moves?.length) line += " moves " + c.moves.map(serializeEsn).join(" ");
      return line;
    }
    case "go": {
      const o = c.options ?? {};
      const parts: string[] = ["go"];
      if (o.depth    !== undefined) parts.push("depth",    String(o.depth));
      if (o.movetime !== undefined) parts.push("movetime", String(o.movetime));
      if (o.wtime    !== undefined) parts.push("wtime",    String(o.wtime));
      if (o.btime    !== undefined) parts.push("btime",    String(o.btime));
      return parts.join(" ");
    }
  }
}

// === Engine → GUI =============================================================
export type EngineMessage =
  | { type: "id";       key: "name" | "author"; value: string }
  | { type: "variant";  name: string }
  | { type: "option";   option: EngineOption }
  | { type: "eeiok" }
  | { type: "readyok" }
  | { type: "info";     info: SearchInfo }
  | { type: "bestmove"; move: EsnToken }
  | { type: "result";   result: GameResult; reason: string }
  | { type: "unknown";  raw: string };

export function parseEngineMessage(raw: string): EngineMessage {
  const line = raw.trim();

  if (line === "eeiok")   return { type: "eeiok" };
  if (line === "readyok") return { type: "readyok" };

  const tokens = line.split(/\s+/);

  if (tokens[0] === "id") {
    const key = tokens[1] as "name" | "author";
    return { type: "id", key, value: tokens.slice(2).join(" ") };
  }

  if (tokens[0] === "variant") {
    return { type: "variant", name: tokens[1] ?? "" };
  }

  if (tokens[0] === "option") {
    const name  = extractValue(tokens, "name",    "type")    ?? "";
    const type  = extractValue(tokens, "type",    "default") ?? "string";
    const def   = extractValue(tokens, "default", "min");
    const min   = extractValue(tokens, "min",     "max");
    const max   = extractValue(tokens, "max",     undefined);
    const opt: EngineOption = { name, type: type as EngineOption["type"] };
    if (def !== undefined) opt.default = Number.isNaN(Number(def)) ? def : Number(def);
    if (min !== undefined) opt.min = Number(min);
    if (max !== undefined) opt.max = Number(max);
    return { type: "option", option: opt };
  }

  if (tokens[0] === "info") {
    const info: SearchInfo = {
      depth:   Number(extractValue(tokens, "depth", "score") ?? 0),
      scoreCp: Number(extractValue(tokens, "cp",    "nodes") ?? 0),
      nodes:   extractValue(tokens, "nodes", "time") ? Number(extractValue(tokens, "nodes", "time")) : undefined,
      time:    extractValue(tokens, "time",  "pv")   ? Number(extractValue(tokens, "time",  "pv"))   : undefined,
    };
    const pvIdx = tokens.indexOf("pv");
    if (pvIdx !== -1) {
      info.pv = tokens.slice(pvIdx + 1).map(m => parseEsn(m));
    }
    return { type: "info", info };
  }

  if (tokens[0] === "bestmove") {
    return { type: "bestmove", move: parseEsn(tokens[1]) };
  }

  if (tokens[0] === "result") {
    const result = tokens[1] as GameResult;
    const reason = tokens.slice(3).join(" ");
    return { type: "result", result, reason };
  }

  return { type: "unknown", raw: line };
}

function extractValue(
  tokens: string[], key: string, nextKey: string | undefined
): string | undefined {
  const idx = tokens.indexOf(key);
  if (idx === -1) return undefined;
  const endIdx = nextKey ? tokens.indexOf(nextKey, idx + 1) : tokens.length;
  const end = endIdx === -1 ? tokens.length : endIdx;
  const value = tokens.slice(idx + 1, end).join(" ");
  return value || undefined;
}