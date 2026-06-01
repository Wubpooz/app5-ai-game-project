Title: BandDPER — Full 25-minute Video Script
Source files: BandDPER.tex, rapport_final.tex, TODO.md
File: docs/model/BandDPER_full_video_script.md

Duration target: 25:00 (minutes:seconds)

Overview
--------
This is a full video script (narration + visual cues) for a 25-minute explainer about BandDPER and the Escampe AI project. It draws from `BandDPER.tex`, `rapport_final.tex`, and `TODO.md` and is arranged into timed segments with precise visual directions: Manim scenes, slides, code excerpts, and demo suggestions.

Usage
-----
- Use with the Manim scenes in `banddper_manim.py` for animated segments. Where noted, a static slide or code overlay can replace a short Manim clip for recording convenience.
- Timestamps are approximate; adjust pacing when recording to reach exactly 25:00.

Script (timed)
---------------

0:00 — 0:30  Opening / Title (30s)
 Visual: Fade in from black. Title card: "BandDPER — Band-aware dual-perspective evaluation ResNet" (use Manim `BandDPERExplainer` title frame). Soft ambient underscore music; lower-third with speaker name appears.
 Audio cue: short, warm piano hit at 0:00, sustain under voice.
 Narration: "Hello — I'm Mathieu Waharte. This talk covers BandDPER: a compact, band-aware evaluation network built to accelerate and strengthen search for the board game Escampe. In the next 25 minutes you'll see the problem, baseline engine, the model architecture, training and export pipeline, and practical integration notes for low-latency Java inference."

0:30 — 2:30  Problem statement & motivation (2:00)
 Visual: Slide summarising key constraints (6×6 board, band map, forced-pass mechanic). Simultaneously show a small animated board highlighting a sample forced-pass sequence (`BandDPERExplainer` board zoom).
 On-screen text: "Challenge: small board — strong local structure"; lower-right: repo pointer `app5-ai-game-project/docs/`.
 Narration: "Escampe is deterministic and full-information but driven by a fixed, non-uniform band topology. The band constraint creates strong local dependencies: whether a piece can move depends on the last landing band. These constraints produce forced replies and tempo effects that are difficult to capture with simple heuristics. Our target is a scalar evaluation in [-1,+1] that is fast enough to run inside a tight Java search loop."

2:30 — 4:30  Quick rules demo (2:00)
 Visual: Short animated play example showing one move, the resulting band, and the subsequent legal moves highlighted. Use `BandDPERExplainer` pieces animation or a short recorded gameplay clip if available.
 Cue: annotate legal move highlights, add small captions "last band → legal landings".
 Narration: "Here's an example: after this landing the opponent's legal landing squares are restricted to band 2 — notice how that drastically reduces response options and creates tempo opportunities."
2:30 — 4:30  Quick rules / demo (2:00)
4:30 — 8:30  Baseline search & heuristics (4:00)
 Visual: Slide: "Search backbone" with bullets: Negamax + Alpha-Beta, Iterative Deepening, Time Manager, Move Ordering. Show the `TimeManager` Java snippet (from `TODO.md`) as an overlay while discussing it.
 On-screen callouts: complexity chart, nodes/sec improvements with bitboards.
 Narration (scripted):
  "Our engine uses Negamax with alpha-beta pruning. Iterative deepening gives a reliable best move when time runs out. Time management uses soft and hard bounds — soft stops the current iteration gracefully, hard stops immediately.
  We maximize nodes/sec with bitboard-like representations and make/unmake semantics to avoid allocations. Move ordering is critical: killers, history tables, and the transposition table move (future work) push strong moves first, creating earlier cutoffs."
- Visual: Short gameplay clip (if available) or manim animation showing a sample move and the band effect; annotate legal moves and forced-pass.
8:30 — 10:30  Heuristic evaluation summary (2:00)
 Visual: Slide with the evaluation formula (from `TODO.md`) and a short table describing each term (attack distances, escape counts, band control, mobility, forced-pass penalty).
 Narration: "The baseline is a linear combination of domain signals calibrated via SPSA and other tuning runs. It is fast and interpretable, but misses nonlinear interactions between band masks and spatial patterns — this motivates a learned evaluator."
- Narration:
10:30 — 13:30  Why a neural approach? (3:00)
 Visual: Side-by-side slide: 'Handcrafted' vs 'Neural' with pros/cons; show small heatmap demos where the NN captures subtle spatial interactions.
 Narration: "A small NN can learn complex pattern combinations, e.g. when multiple paladins coordinate under a band constraint. The challenge is keeping latency low: our goal is microseconds per evaluation when integrated into Java search. BandDPER targets this trade-off: expressive yet compact."
  "Here's a short example: notice how the last landing band restricts the opponent's available moves on the next turn. These constraints produce forced responses and tempo plays, the sort of phenomena we must capture in our evaluator."
13:30 — 16:30  Model overview: BandDPER core components (3:00)
 Visual: High-level architecture diagram (Siamese encoder → fusion → residual trunk → value head + forced-pass shortcut). Use `BandDPERHead` or `BandDPERInputEncoding` frames to animate dataflow arrows.
 Lower-third: key numbers — embed_dim=256, trunk_dim=258, ~2.9MB float32.
 Narration: Structured points:
  - "Siamese encoder: shared weights process both player perspectives into 128-d embeddings."
  - "Fusion: embeddings concat and append escape scalars to form `h`."
  - "Residual trunk: three ResBlocks refine `h` while preserving identity via skips."
  - "Output head: linear reductions to 1 scalar; forced-pass `w_pass` bypasses trunk for a direct signal to tanh."

16:30 — 19:00  Input encoding & channels (2:30)
 Visual: Table of 16 channels per perspective (from `BandDPER.tex`). Show an annotated 6×6 grid and animate each channel appearing (e.g., paladin positions -> band masks -> landing maps -> unicorn-relative attack map).
 Narration (explicit script):
  "Each perspective is a [16×6×6] tensor: dynamic piece maps, fixed band masks (channels 4–6), departure masks, landing maps per band, row/column stats, and a unicorn-relative attacker map. Channel 15 recentres attacker data around the unicorn, letting the CNN learn threat patterns that are invariant relative to the unicorn while still respecting global board identity through band masks."
4:30 — 8:30  Baseline search & heuristics (4:00)
19:00 — 21:00  Residual trunk, forced-pass shortcut, and math (2:00)
 Visual: Use `BandDPERHead` to show trunk blocks, skip arrows, forced-pass shortcut, and animate the formula:
  $$y = \tanh\left(W_2\,\mathrm{ReLU}(W_1 h) + w_{pass}\,p\right)$$
 Narration (scripted):
  "The trunk refines fused features into a compact representation. The forced-pass boolean `p` is multiplied by `w_pass` and added directly to the linear output, giving rare but decisive signals strong gradients and avoiding under-training. Tanh bounds the final value into [-1,+1]."
- Visual: Slide summarising search algorithms (Negamax + alpha-beta, iterative deepening, time manager). Display small code snippets from `TODO.md` (TimeManager code) as overlay while speaking.
21:00 — 23:00  Training pipeline & export (2:00)
 Visual: Slide: data generation (minimax bootstrapping), dataset size (50k–100k), optimizer (AdamW), LR schedule (CosineAnnealingWarmRestarts). Show code snippet for export (fold BN, dump JSON/pth).
 Narration: "Labels are produced by a depth-5..7 minimax engine. We train on 50–100k positions with AdamW and scheduled restarts. Export steps fold batchnorm for inference efficiency and write compact JSON for the Java runtime. Optionally we export quantized ONNX for further speedups."
- Narration (key points):
23:00 — 24:00  Integration and performance (1:00)
 Visual: Diagram of Java inference integration (preallocated buffers, no GC during forward pass). Show sample numbers: target inference 2–10 μs (dependent on CPU), batch size 1 low-latency path.
 Narration: "In Java we pre-allocate all activation buffers and execute the forward pass without allocations. This eliminates GC pauses and keeps inference deterministic and fast. The model can be used as a direct evaluator or as a policy+value move orderer."
  - Negamax with alpha-beta pruning is the backbone of the engine.
24:00 — 25:00  Closing, results, next steps (1:00)
 Visual: Final slide with bullet summary: contributions, evaluations to run (tournament, ablations), next work (policy head, aspiration windows, quantization, distillation). Show repo link and contact.
 Narration: "BandDPER offers a compact, practical learned evaluation tailored to Escampe's band topology. Next steps include policy head for ordering, quantization for smaller footprint, and large tournament runs to measure Elo gain. Thank you — the full code and paper are in the repository."
  - Iterative deepening ensures graceful outputs under time limits; aspiration windows are a future improvement.
Appendix: Recording & render checklist (expanded)
------------------------------------
 Recommended render command (medium quality, 720p30):

  manim -qm "docs/model/banddper_manim.py" BandDPERFullVideo

 For final production (high quality):

  manim -qh "docs/model/banddper_manim.py" BandDPERFullVideo

 Stitching & narration tips:
  - Record narration per section in a DAW at 48 kHz / 24-bit. Keep a single take per section, then trim breath noises.
  - Render Manim clips per section if your workstation struggles with a continuous 25-minute render; stitch rendered clips in a video editor.
  - Use chapter markers in the final video file at the timestamps above to aid viewers.

References
----------
... (unchanged)
  - Time management is implemented with soft/hard limits. (Read short pseudo-code from `TODO.md` TimeManager.)
  - Move ordering (killer moves, history heuristic) drastically improves node throughput. We used bitboards and make/unmake to minimise allocations.
- Visual cue: show chart mock-up of nodes/sec improvement when using bitboards and move ordering (from TODO.md Optimizations summary).

8:30 — 10:30  Heuristic evaluation summary (2:00)
- Visual: Slide with the evaluation formula from `TODO.md` (the math expression) and a brief verbal explanation.
- Narration:
  "Our handcrafted evaluation combines attack/defense distances, unicorn escapability, band control and tempo. It's a compact linear model, tuned with SPSA and other calibration methods. But handcrafted heuristics miss complex nonlinear interactions — that's where BandDPER comes in."

10:30 — 13:30  Why a neural approach? (3:00)
- Visual: Side-by-side comparison slide: Handcrafted heuristic vs NN. Include `BandDPER.tex` excerpt about NN benefits. Show rough trade-offs (latency vs expressivity).
- Narration (approx):
  "Neural networks can learn nonlinear composition of signals and capture structural patterns that are hard to handcraft. For Escampe, the board's band topology creates repeated, local motifs — a CNN-based encoder that is aware of these band masks, combined with a residual trunk and a forced-pass shortcut, gives a strong inductive bias while remaining compact for fast inference."

13:30 — 16:30  Model overview: BandDPER core components (3:00)
- Visual: Manim scene or slide illustrating the high-level architecture (Siamese encoder -> fusion -> residual trunk -> value head + forced-pass shortcut). Use frames from `BandDPER.tex` Figure placeholders.
- Narration (structured):
  - "Siamese spatial encoder: same encoder processes two perspectives (current player and opponent) with shared weights — this enforces symmetry and reduces param count."
  - "Band masks: channels 4–6 are fixed binary maps encoding the board's topology; these break translation equivariance intentionally to reflect fixed square identities."
  - "Fusion: perspective outputs concatenate and append escape scalars before entering the trunk."
  - "Residual trunk: a stack of small ResBlocks preserves a 258-d hidden state."
  - "Output head: linear projections to a scalar, with a learned `w_pass` shortcut that injects the forced-pass signal directly before tanh."

16:30 — 19:00  Input encoding & channels (2:30)
- Visual: Slide/table showing the 16 channels per perspective (pull from `BandDPER.tex` table). Show a live example building the 16×6×6 tensor for a sample board (use Manim `BandDPERInputEncoding` scene or static overlays).
- Narration:
  "Channels encode paladin positions, unicorn positions, band masks, departure masks, landing maps per band, row/column stats, and the unicorn-relative attacker map. Channels 4–6 (band masks) are fixed and provide strong spatial inductive bias. Channel 15 recentres attacker maps around the unicorn, which helps the encoder detect threats relative to the unicorn position."

19:00 — 21:00  Residual trunk, forced-pass shortcut, and math (2:00)
- Visual: Use `BandDPERHead` scene: draw trunk blocks, skip connections, and the forced-pass shortcut; display the formula y = tanh(W2 ReLU(W1 h) + w_pass p).
- Narration: explain residual blocks, why injected scalars at trunk input, and why the direct forced-pass weight is beneficial (strong gradient to rare but decisive signal).

21:00 — 23:00  Training pipeline & export (2:00)
- Visual: Slide with training steps: data gen via minimax bootstrapping, MSE loss, AdamW with cosine restarts, export steps (fold BN, export weights to JSON/pth/ONNX). Use `BandDPER.tex` Export Pipeline code snippets.
- Narration:
  "Labels are generated by a depth-5..7 engine (minimax bootstrapping). We train with MSE and AdamW, use cosine restarts, and fold batchnorm for export. The final weights are exported to a compact JSON format and a .pth for further experimentation. The goal is to run inference in Java with preallocated buffers to eliminate GC pressure."

23:00 — 24:00  Integration and performance (1:00)
- Visual: Slide with latency and memory targets; show Java inference diagram from `BandDPER.tex` (preallocated buffers, ONNX possibility). Optionally display a short benchmark table: inference time target (2–5 us) and training dataset size (50k–100k positions).
- Narration: "When integrated as a move-ordering or evaluation function, BandDPER reduces nodes searched or increases playing strength relative to the baseline. Exporting to ONNX or lightweight JSON makes integration into the low-latency Java engine feasible."

24:00 — 25:00  Closing, results, next steps (1:00)
- Visual: Final slide with bullet list: key contributions, results summary, open work from TODO.md (aspiration windows, null-move pruning, policy head, quantization, distillation). Show contact/attribution.
- Narration: (approx)
  "To summarise: BandDPER provides a compact, band-aware dual-perspective evaluator that fits our inference budget and improves move ordering and evaluation quality. Next steps include adding a policy head for better move ordering, aspiration windows in iterative deepening, quantization for faster inference, and running large-scale tournaments to quantify playing strength. Thank you — the full paper and code are available in the repository; see the BandDPER.tex and rapport_final.tex for details."

Appendix: Recording & render checklist
------------------------------------
- Recommended render command (medium quality, 720p30):

  manim -qm "docs/model/banddper_manim.py" BandDPERFullVideo

- For final production (higher quality):

  manim -qh "docs/model/banddper_manim.py" BandDPERFullVideo

- Recording tips:
  - Use the Manim-produced video segments and overlay narration in a DAW (Audacity / Reaper) to control pacing precisely.
  - Record narration in one pass per section, then align with video and trim silences to hit exact timestamps.
  - Add soft underscore music at -20 to -24 dB under voice.

References
----------
- BandDPER.tex (model design and export pipeline)
- rapport_final.tex (project report and implementation details)
- TODO.md (engine heuristics, search optimizations and planned work)
