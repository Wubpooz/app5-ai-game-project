## Architecture
```
┌────────────────────────────────────────┐
│  Java Engine  (EEI-UCI over stdin/out) │
└────────────────┬───────────────────────┘
                 │ ChildProcess (Bun)
┌────────────────▼───────────────────────┐
│  Bun/Hono Server  src/server.ts        │
│  POST /game          → create session  │
│  GET  /game/:id      → current state   │
│  POST /game/:id/move → play ESN move   │
│  POST /game/:id/engine-move → go+wait  │
│  WS   /ws            → live events     │
└────────────────┬───────────────────────┘
                 │ HTTP + WebSocket
┌────────────────▼───────────────────────┐
│  EscampeClient  src/client.ts          │
│  (browser or Bun/Node)                 │
└────────────────────────────────────────┘
```


## Quick start

```bash
bun install

# Point at your Java engine
ENGINE_CMD=java ENGINE_ARGS="-jar escampe-engine.jar" bun run dev
```


### Client usage (browser or Bun)
```js
import { EscampeClient } from "./src/client.ts";

const client = new EscampeClient("http://localhost:3000");

// Subscribe to live events
client.connect();
client.on(event => console.log(event));

// Play a game
const game = await client.createGame("p1l1p1/pp4/6/6/4PP/1P1L1P w * 1 0");
await client.playMove(game.id, "P c2>d2>d3");
const reply = await client.requestEngineMove(game.id, { depth: 8 });
console.log(reply.move); // e.g. "L e5>d5"
```

