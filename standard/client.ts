import { parseEsn, serializeEsn, parseEpn, serializeEpn } from "./index.ts";
import type { EsnToken, EpnState, GameResult, SearchOptions } from "./index.ts";

export interface GameInfo {
  id: string;
  epn: string;
  moves: string[];
  result: GameResult;
}

export type ServerEvent =
  | { type: "move";        gameId: string; move: string; epn: string }
  | { type: "engine-move"; gameId: string; move: string }
  | { type: "game-result"; result: GameResult; reason: string }
  | { type: "engine-raw";  line: string }
  | { type: "unknown";     raw: unknown };

export type EventHandler = (event: ServerEvent) => void;

export class EscampeClient {
  private readonly baseUrl: string;
  private readonly wsUrl:   string;
  private ws:      WebSocket | null = null;
  private handlers: EventHandler[] = [];

  constructor(baseUrl = "http://localhost:3000") {
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.wsUrl   = this.baseUrl.replace(/^http/, "ws") + "/ws";
  }

  async createGame(epn?: string, white = "White", black = "Black"): Promise<GameInfo> {
    const res = await fetch(`${this.baseUrl}/game`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ epn, white, black }),
    });
    if (!res.ok) throw new Error(`createGame failed: ${await res.text()}`);
    return res.json() as Promise<GameInfo>;
  }

  async getGame(id: string): Promise<GameInfo> {
    const res = await fetch(`${this.baseUrl}/game/${id}`);
    if (!res.ok) throw new Error(`getGame failed: ${await res.text()}`);
    return res.json() as Promise<GameInfo>;
  }

  async playMove(id: string, esn: string): Promise<{ ok: boolean; move: string }> {
    parseEsn(esn); // local validation
    const res = await fetch(`${this.baseUrl}/game/${id}/move`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ move: esn }),
    });
    if (!res.ok) throw new Error(`playMove failed: ${await res.text()}`);
    return res.json() as Promise<{ ok: boolean; move: string }>;
  }

  async requestEngineMove(id: string, options?: SearchOptions): Promise<{ move: string }> {
    const res = await fetch(`${this.baseUrl}/game/${id}/engine-move`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(options ?? {}),
    });
    if (!res.ok) throw new Error(`requestEngineMove failed: ${await res.text()}`);
    return res.json() as Promise<{ move: string }>;
  }

  connect(): void {
    if (this.ws) return;
    this.ws = new WebSocket(this.wsUrl);
    this.ws.addEventListener("message", (ev) => {
      try {
        const data = JSON.parse(ev.data as string) as ServerEvent;
        for (const h of this.handlers) h(data);
      } catch {
        // Ignore JSON parse errors
        for (const h of this.handlers) h({ type: "unknown", raw: ev.data });
      }
    });
    this.ws.addEventListener("close", () => { this.ws = null; });
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
  }

  on(handler: EventHandler): () => void {
    this.handlers.push(handler);
    return () => { this.handlers = this.handlers.filter(h => h !== handler); };
  }

  parsePosition(epn: string): EpnState    { return parseEpn(epn); }
  serializePosition(s: EpnState): string  { return serializeEpn(s); }
  parseMove(esn: string): EsnToken        { return parseEsn(esn); }
  serializeMove(t: EsnToken): string      { return serializeEsn(t); }
}