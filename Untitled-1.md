Since you have **not implemented anything yet**, the right goal is not “collect every plausible trick,” but define a **coherent roadmap** with no internal conflicts and with the highest expected return under your inference budget. Your current paper describes a small board-game evaluator with a Siamese CNN encoder, 3 ResBlocks, ClippedReLU, minimax bootstrapping, optional policy head, and planned quantization, so the best final stack should preserve fast inference and avoid combinations that cancel each other out. [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf)

## Final stack

I would split the plan into **core**, **optional later**, and **rejected**. For your 300–900 ms move budget, the best coherent direction is a **speed-first path** centered on stronger inputs, better training, and inference-friendly architecture, rather than attention-heavy or transformer-style additions. [chessprogramming](https://www.chessprogramming.org/NNUE)

| Step | Technique | Why it stays | Expected gain | Inference impact | Effort | Priority |
|---|---|---|---|---|---|---|
| 1 | Extra spatial input channels | You already identified mobility, band constraints, unicorn routes, and attackers as important signals, so encoding more of them directly is aligned with the paper and should improve sample efficiency.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | Medium-high eval gain | Near-zero runtime overhead beyond slightly larger first conv | Low | Very high |
| 2 | Coordinate channels | Your board is small and not translation-symmetric in a pure chess sense because band topology matters, so absolute row/column information should help.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | Small-medium eval gain | Negligible | Very low | Very high |
| 3 | Loss weighting for decisive positions | Your labels are normalized minimax scores in \([-1,1]\), and decisive positions likely matter more than near-zero ones for search quality.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | Medium practical gain | None | Very low | Very high |
| 4 | Iterative bootstrapping | Re-running the train → stronger search → better labels loop is one of the most likely ways to improve actual playing strength without changing inference cost.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | High Elo/search-quality gain | None | Medium | Very high |
| 5 | SE-style channel recalibration | This is one of the few architectural additions that can help a compact residual network while staying cheap enough for your setting. | Small-medium eval gain | Low overhead | Low-medium | High |
| 6 | Depthwise separable replacement for only the 3×3/dilated convs | This can reduce encoder cost while preserving most of the useful spatial structure if done selectively.  [theaisummer](https://theaisummer.com/receptive-field/) | Small-medium speed gain | Positive, faster | Medium | High |
| 7 | Quantization | You already planned this, and for this type of compact evaluator it is one of the cleanest deployment wins.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | Medium speed gain | Positive, faster and smaller | Low-medium | High |
| 8 | Distillation into a smaller student | This is a strong second-phase optimization once you have a good teacher. Gomoku work suggests efficient distilled nets can retain much of the strength while being cheaper.  [arxiv](https://arxiv.org/html/2503.13178) | High deployment gain | Positive, often much faster | Medium | Medium-high |
| 9 | Optional policy head for move ordering | Your paper already proposes this; if accurate enough, it can improve alpha-beta pruning more than a small eval gain would.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) | Potentially high search gain | Some added cost per node if always called | Medium | Conditional |

## What each accepted step likely contributes

These are not precise guarantees, but realistic **relative contributions** if implemented well in order.

| Technique | Main benefit type | Contribution to final outcome |
|---|---|---|
| Extra channels | Better inductive bias | Often one of the best “cheap” gains because you inject domain structure directly |
| Coordinate channels | Better positional awareness | Helpful but secondary; mostly improves data efficiency |
| Weighted loss | Better decision quality on sharp positions | Small in raw MSE terms, but can matter disproportionately for move choice |
| Iterative bootstrapping | Better labels over time | Likely one of the biggest contributors to actual engine strength |
| SE blocks | Better feature selection | Useful but not transformative alone |
| Selective depthwise separable convs | Lower latency | More about speed budget than raw strength |
| Quantization | Faster deployment | Little to no strength gain, but strong practical speedup |
| Distillation | Faster net with retained strength | Big practical win if teacher is already strong |
| Policy head | Better move ordering | Can be huge if calibrated and cheap enough, but depends on implementation details |

A good rough expectation is: **most raw playing-strength gain comes from better targets and better inputs**, while **most latency gain comes from selective convolution changes, quantization, and later distillation**. [arxiv](https://arxiv.org/html/2503.13178)

## Best implementation order

If I were you, I would do it in this order:

| Phase | Changes | Why this order |
|---|---|---|
| A | Extra channels + coordinate channels | Fastest way to improve representation before touching architecture |
| B | Weighted loss + better dataset curation | Cheap training gains before costly refactors |
| C | Iterative bootstrapping | Multiplies value of A and B by improving labels |
| D | SE blocks | Low-risk architecture upgrade after data/training baseline is solid |
| E | Selective depthwise separable convs | Optimize speed only after you know the stronger baseline |
| F | Quantization | Deployment optimization after architecture stabilizes |
| G | Distillation | Best done at the end, from your best teacher |
| H | Optional policy head | Add only if profiling says move ordering gain beats its cost |

## Recommended concrete final roadmap

Here is the **full list I would actually recommend**, in final form.

| Category | Keep? | Technique | Reason |
|---|---|---|---|
| Inputs | Keep | Band masks, departure mask, landing mask, plus extra attack/escape/tension-style maps | Strongest low-cost inductive bias improvement for your setting  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Inputs | Keep | Coordinate channels | Helps with absolute-position asymmetry on 6×6 banded board  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Core net | Keep | Siamese/shared dual perspective | It is central to your design and appropriate for zero-sum symmetry  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Core net | Keep | Residual trunk | Still the right default compact backbone here  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Core net | Keep | ClippedReLU / bounded activations | Fits your bounded-value design and helps stability  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Core net | Keep | Optional SE blocks | Cheap and compatible |
| Core net | Keep with care | Depthwise separable convs on standard convs only | Speed-friendly if not over-applied |
| Training | Keep | Minimax bootstrapping | Already aligned with your project goals  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Training | Keep | Iterative re-bootstrap rounds | Very likely high value for engine strength |
| Training | Keep | Weighted value loss | Cheap and useful |
| Training | Keep later | Distillation | Strong deployment-stage optimization  [arxiv](https://arxiv.org/html/2503.13178) |
| Search | Keep conditionally | Policy head for move ordering | Could improve pruning if profile says yes  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Deployment | Keep | Quantization | Already justified in the paper  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |

## Rejections

These are the ones I would **not** put in your final mainline plan.

| Rejected technique | Why reject it |
|---|---|
| Attention pooling / self-attention in encoder | It conflicts with the strongest future speed path, namely incremental-style updates, and it introduces more global interaction cost than your board size likely justifies. |
| NNUE-style incremental updates as immediate plan | Conceptually powerful, but your current architecture is convolutional and not naturally NNUE-like; making it truly incrementally updatable is a major redesign, not a drop-in optimization.  [chessprogramming](https://www.chessprogramming.org/NNUE) |
| LayerNorm replacing folded BatchNorm | Your export pipeline already folds BatchNorm away, which is inference-friendly; switching norms adds churn without clear payoff.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| Pre-activation ResNet | Probably too little upside for only 3 residual blocks.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |
| DenseNet-style dense connections | Adds memory traffic and projection complexity without obvious benefit on a flattened compact trunk. |
| mHC / DeepSeek-style hyper-connections | Solves a depth/scale regime that is completely different from your small CNN evaluator.  [arxiv](https://arxiv.org/pdf/2512.24880.pdf) |
| Full transformerization | Too expensive and poorly matched to your latency target and board size. |
| Auxiliary losses as immediate baseline | Potentially useful, but I would postpone until the single-task baseline is strong and stable; otherwise you complicate attribution.  [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf) |

## Important compatibility notes

Some combinations are especially good, and some are bad.

| Combination | Verdict | Why |
|---|---|---|
| Extra channels + coordinate channels + residual CNN | Excellent | Stronger inputs, same efficient backbone |
| Weighted loss + iterative bootstrapping | Excellent | Better labels and better emphasis reinforce each other |
| SE + richer channels | Good | More meaningful channel gating |
| Depthwise separable convs + quantization | Good | Both help latency |
| Distillation after all improvements | Excellent | Best teacher gives best student |
| Attention-heavy pooling + future incremental updates | Bad | Global dependencies destroy locality advantages |
| Over-factorized asymmetric convs + depthwise everywhere | Bad | Risk of reducing expressivity too much |
| Policy head too early | Risky | Can distract from getting the value evaluator correct first |

## How much overall improvement to expect

A realistic expectation, if the baseline is decent:

| Phase | Likely net effect |
|---|---|
| Better inputs + weighted loss | Noticeable but not miraculous, often the first clear jump |
| Iterative bootstrapping | Often the largest strength increase |
| SE + selective speed-oriented conv changes | Incremental strength plus some latency relief |
| Quantization | Mostly speed/model-size win |
| Distillation | Big deployment win, possibly small strength loss for much better speed |
| Policy head | Potentially large search gain, but only if the ranking quality is strong |

In plain terms: I would expect the **largest strength gains** from **input engineering + iterative bootstrapping**, and the **largest practical speed gains** from **selective conv optimization + quantization + later distillation**. [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf)

## Versioning plan

Since nothing is implemented yet, start with a clean experiment/versioning scheme from day one.

| Artifact | Recommendation |
|---|---|
| Code repo | One Git repo for engine + training, with tags per stable experiment |
| Paper source | Keep the paper source in the same repo or in a sibling repo with matching tags |
| Model versions | Use semantic experiment IDs like `v0.1-inputs`, `v0.2-loss`, `v0.3-bootstrap1`, `v0.4-se`, `v0.5-dwconv`, `v0.6-q8`, `v0.7-distilled` |
| Checkpoints | Save `model.pt`, `config.json`, `metrics.json`, `train_manifest.json` together |
| Paper versions | Match paper revision to architecture revision, e.g. `paper-r3` corresponding to `model-v0.4` |
| Changelog | Maintain a short changelog with architecture changes, training data changes, and benchmark changes |

A simple naming convention could be:

- `banddper-v0.1-base`
- `banddper-v0.2-extra-channels`
- `banddper-v0.3-weighted-loss`
- `banddper-v0.4-bootstrap-round1`
- `banddper-v0.5-se`
- `banddper-v0.6-dwconv`
- `banddper-v0.7-int8`
- `banddper-v0.8-distilled`

## Final recommendation

If I had to give you the **single final list** to pursue, it would be:

1. Keep the current BandDPER backbone idea. [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf)
2. Add extra spatial feature channels.
3. Add coordinate channels.
4. Train with weighted value loss.
5. Run iterative minimax bootstrapping rounds.
6. Add SE blocks.
7. Replace only the standard 3×3 and dilated convs with depthwise separable versions if profiling says encoder cost matters.
8. Quantize for deployment. [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf)
9. Distill into a smaller student.
10. Add the policy head only after profiling confirms move-ordering benefit. [ppl-ai-file-upload.s3.amazonaws](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/103996491/8b9f3be1-2b5d-420c-b972-d00a1ba3f5dc/BandDPER.pdf)

And the **main rejections** are: attention pooling, mHC, DenseNet-style dense connectivity, pre-activation refactor, LayerNorm swap, and immediate NNUE-style redesign. [arxiv](https://arxiv.org/pdf/2512.24880.pdf)

If you want, I can turn this into a **decision matrix with numeric scores** (strength gain, speed gain, implementation risk, paper complexity, and research value) so you can choose a final thesis/project roadmap.
























Great question — no search needed here, this is a current ML landscape question I can answer directly.

## What Muon Is

Muon (Momentum + Orthogonalization via Nesterov) is a recent optimizer developed by Keller Jordan (2024) that applies **Nesterov momentum followed by orthogonalization via Newton-Schulz iterations** to the gradient matrix before the update step. The key insight: it enforces that weight update matrices are approximately orthogonal, which empirically leads to faster loss reduction per step in transformer LLM training.

## Why AdamW is Still Correct for You

| Dimension | Muon | AdamW |
|---|---|---|
| **Primary target** | Large transformer weight matrices (2D Linear layers with large hidden dims) | General neural networks — CNNs, MLPs, small models |
| **Works on Conv2d?** | No — Muon requires the weight to be a matrix \(W \in \mathbb{R}^{m \times n}\) with \(\min(m,n) \geq 2\). Conv2d weights are 4D tensors `[C_out, C_in, kH, kW]` and require reshaping heuristics that aren't well-validated | Yes, natively |
| **Embedding/bias layers** | Falls back to Adam anyway | Handles all layer types natively |
| **Small models (<1M params)** | Unclear benefit — theoretical gains are argued at scale (100M+ param runs) | Well-characterized at all scales |
| **Training data size (50–100K)** | Risk of over-optimizing into a sharp minimum | Better generalization regularization via weight decay |
| **Batch size sensitivity** | More sensitive — orthogonalization is estimated from the gradient batch | Robust |

Muon's real wins have been demonstrated on **GPT-scale transformer training** where the dominant operation is large 2D matrix multiplies. Your model is a **compact CNN** — the dominant layers are `Conv2d` with `3×3` kernels, depthwise convolutions, and small linear layers. The 4D tensor issue alone makes Muon inapplicable to your encoder without non-trivial monkey-patching.

## What You Should Actually Use

**AdamW with a cosine schedule** remains the right choice for your setup. If you want to squeeze more out of training:

- **AdamW** (`lr=1e-3`, `weight_decay=1e-4`) — your baseline, solid and well-understood
- **Cosine annealing with warm restarts** (`CosineAnnealingWarmRestarts`) — helps escape plateaus during iterative bootstrapping rounds since each round is essentially a new curriculum
- **Gradient clipping** (`clip_grad_norm_=1.0`) — already discussed, especially useful with ClippedReLU
- **OneCycleLR** — if you want aggressive fast training (Karpathy recommends this for small models): ramps up LR then decays sharply, often converges in half the epochs

If you genuinely want to experiment with Muon, the only sensible way to use it on your architecture is a **hybrid**: Muon on the output head linear layers (which are proper 2D matrices), and AdamW on all Conv2d layers. But the practical gain for a 200K–800K param model trained on 100K positions will almost certainly be **within noise** compared to AdamW with a well-tuned LR schedule. The engineering effort is not worth it.

**Bottom line:** AdamW is not a weak default you should upgrade from. For your model size, data regime, and architecture, it is the correct choice.