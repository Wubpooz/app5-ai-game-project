import type { WorkerRequest, WorkerResponse } from './types';
import { search, stopSearching } from './search';

self.onmessage = (event: MessageEvent<WorkerRequest>) => {
  const req = event.data;

  if (req.type === 'stop') {
    stopSearching();
    return;
  }

  if (req.type === 'search') {
    try {
      const result = search(
        req.state,
        req.level,
        req.timeLimitMs,
        (depth, score, pv) => {
          const progressMsg: WorkerResponse = {
            type: 'progress',
            depth,
            score,
            pv: pv.map(m => m.isPass ? 'E' : `${String.fromCharCode(65 + m.from.col)}${m.from.row + 1}-${String.fromCharCode(65 + m.to.col)}${m.to.row + 1}`),
          };
          self.postMessage(progressMsg);
        },
      );

      const response: WorkerResponse = { type: 'result', result };
      self.postMessage(response);
    } catch (err) {
      const errMsg: WorkerResponse = {
        type: 'error',
        message: err instanceof Error ? err.message : String(err),
      };
      self.postMessage(errMsg);
    }
  }
};
