import { Hono } from "hono";
import { ChildProcess, spawn } from "node:child_process";
import { createInterface } from "node:readline";
import { parseEngineMessage, serializeGuiCommand, parseEpn, parseEsn, serializeEsn } from "./index.ts";
import type { EngineMessage, EsnToken, SearchOptions, GameResult } from "./index.ts";

const ENGINE_CMD = process.env.ENGINE_CMD  ?? "java";
const ENGINE_ARGS = (process.env.ENGINE_ARGS ?? "-jar escampe-engine.jar").split(" ");
const PORT = Number(process.env.PORT  ?? 3000);

let engineProc: ChildProcess | null = null;
let engineReady = false;

type BestmoveResolver = (move: EsnToken) => void;
const pendingBestmove: BestmoveResolver[] = [];
const wsClients = new Set<WebSocket>();

function startEngine() {
  engineProc = spawn(ENGINE_CMD, ENGINE_ARGS, { stdio: ["pipe", "pipe", "inherit"] });

  const rl = createInterface({ input: engineProc.stdout! });
  rl.on("line", (line: string) => {
    const msg = parseEngineMessage(line);
    handleEngineMessage(msg);
    broadcastToClients({ type: "engine-raw", line });
  });

  engineProc.on("exit", (code: any) => {
    console.error(`[server] Engine exited with code ${code}`);
    engineReady = false;
  });

  sendToEngine("eei");
  sendToEngine("isready");
  sendToEngine("newgame");
}

function sendToEngine(raw: string) {
  if (!engineProc?.stdin) throw new Error("Engine not running");
  engineProc.stdin.write(raw + "\n");
}

function sendGuiCommand(cmd: Parameters<typeof serializeGuiCommand>[0]) {
  sendToEngine(serializeGuiCommand(cmd));
}

function handleEngineMessage(msg: EngineMessage) {
  switch (msg.type) {
    case "readyok":
      engineReady = true;
      console.log("[server] Engine ready");
      break;
    case "bestmove": {
      const resolver = pendingBestmove.shift();
      if (resolver) resolver(msg.move);
      break;
    }
    case "result":
      broadcastToClients({ type: "game-result", result: msg.result, reason: msg.reason });
      break;
  }
}

function broadcastToClients(payload: unknown) {
  const text = JSON.stringify(payload);
  for (const ws of wsClients) {
    try { ws.send(text); } catch (_) { wsClients.delete(ws); }
  }
}



// === Game state ===============================================================
interface GameSession {
  id: string;
  initialEpn: string;
  moves: EsnToken[];
  currentEpn: string;
  result: GameResult;
}

const sessions = new Map<string, GameSession>();
const makeId = () => Math.random().toString(36).slice(2, 10);



// === Hono HTTP API ============================================================
const app = new Hono();

app.post("/game", async (c: any) => {
  const body = await c.req.json() as { epn?: string; white?: string; black?: string };
  const epnStr = body.epn ?? "p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0";
  try { parseEpn(epnStr); } catch (e) {
    return c.json({ error: `Invalid EPN: ${(e as Error).message}` }, 400);
  }
  const id = makeId();
  sessions.set(id, { id, initialEpn: epnStr, moves: [], currentEpn: epnStr, result: "*" });
  return c.json({ id, epn: epnStr });
});

app.get("/game/:id", (c: any) => {
  const session = sessions.get(c.req.param("id"));
  if (!session) return c.json({ error: "Not found" }, 404);
  return c.json({
    id:      session.id,
    epn:     session.currentEpn,
    moves:   session.moves.map(serializeEsn),
    result:  session.result,
  });
});

app.post("/game/:id/move", async (c: any) => {
  const session = sessions.get(c.req.param("id"));
  if (!session) return c.json({ error: "Not found" }, 404);
  const body = await c.req.json() as { move: string };
  let token: EsnToken;
  try { token = parseEsn(body.move); } catch (e) {
    return c.json({ error: `Invalid ESN: ${(e as Error).message}` }, 400);
  }
  session.moves.push(token);
  if ("capture" in token && token.capture) {
    const state = parseEpn(session.currentEpn);
    session.result = state.side === "w" ? "1-0" : "0-1";
  }
  broadcastToClients({ type: "move", gameId: session.id, move: serializeEsn(token) });
  return c.json({ ok: true, move: serializeEsn(token) });
});

app.post("/game/:id/engine-move", async (c: any) => {
  const session = sessions.get(c.req.param("id"));
  if (!session) return c.json({ error: "Not found" }, 404);
  if (!engineReady) return c.json({ error: "Engine not ready" }, 503);
  const body = (await c.req.json().catch(() => ({}))) as SearchOptions;
  sendGuiCommand({ cmd: "position", epn: session.currentEpn, moves: session.moves });
  sendGuiCommand({ cmd: "go", options: body });
  const move = await new Promise<EsnToken>((resolve) => { pendingBestmove.push(resolve); });
  session.moves.push(move);
  broadcastToClients({ type: "engine-move", gameId: session.id, move: serializeEsn(move) });
  return c.json({ move: serializeEsn(move) });
});



// === Start ====================================================================
startEngine();

export default {
  port: PORT,
  fetch: app.fetch,
  websocket: {
    open(ws: WebSocket)  { wsClients.add(ws); },
    close(ws: WebSocket) { wsClients.delete(ws); },
    message(_ws: WebSocket, data: string) {
      try {
        const payload = JSON.parse(data);
        if (payload.cmd) sendGuiCommand(payload);
      } catch (_) {}
    },
  },
};