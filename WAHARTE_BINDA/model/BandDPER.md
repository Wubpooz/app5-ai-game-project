# BandDPER
> BandDPER — Band-Aware Dual-Perspective Residual Network

<!--! TODO add Dropout -->


&nbsp;

## Analysis

### Problem Statement

Since the game is a full-information, deterministic, zero-sum game with a small board (6×6) and a fixed but non-uniform topology (the band map), strong domain knowledge can be leveraged to design an efficient evaluation function. The key challenge is to capture the complex interactions between pieces, the band constraints (which create strong local dependencies between adjacent squares), and the strategic patterns that emerge from the unique movement rules.

### Requirements

The evaluation function returns a scalar in $$[-1, +1]$$, where −1 represents a losing position for the current player, +1 represents a winning position, and intermediate values represent varying degrees of advantage.

The function must capture several key signals:

- Which pieces can legally move right now (band constraint)
- How many moves each side has (mobility asymmetry)
- Proximity of attackers to the opponent's unicorn, weighted by band alignment and measured in a unicorn-relative coordinate frame
- Unicorn escape routes (counted explicitly as a scalar signal)
- Structural patterns (corridor control, band-type distribution)
- Tempo forcing: which band the current move lands on determines the opponent's forced departure
- Forced-pass detection: whether the current move leaves the opponent with no legal response

### Why a Neural Network

Compared to a handcrafted evaluation function which already captures the most obvious signals, a neural network can learn nonlinear interactions, generalise from training data, and capture structural patterns not encoded in any explicit formula. The main drawback is slower inference, but according to [AlphaZero](https://arxiv.org/abs/1712.01815), a better evaluation at shallower depth can outperform a weaker evaluation at greater depth. Furthermore, the game is very small (6×6, 12 pieces), and modern inference optimisations can make network evaluation nearly as fast as a handcrafted eval.

### Existing Models

| Model  | Notes | Recommendation |
|--------|-------|----------------|
| MLP    | Fast | No spatial awareness or relational reasoning — avoid |
| CNN    | Exploits local spatial patterns; fast enough | OK but translation equivariance may be wrong for the non-uniform band map |
| NNUE   | Fast incremental updates, dual-perspective design, ClippedReLU $$[0,1]$$ activation | Doesn't fit this problem size, but dual-perspective design, ClippedReLU, king-relative features, and direct output shortcuts are worth borrowing |
| ResNet | Good when a baseline evaluation exists | Good, but lacks inherent spatial awareness |
| GNN    | Movement graph; invariant to symmetries | Avoid — dynamic graph changes, implementation complexity, and slower performance |

***

&nbsp;

## Model Architecture

This architecture blends known components into a novel evaluation network:

- **Siamese CNN spatial encoder** with shared weights and dual-perspective input
- **Band topology and game signals encoded as fixed and dynamic input channels** (16 channels total)
- **Unicorn-relative attacker map** inspired by NNUE's HalfKP king-relative encoding
- **Residual trunk** with ClippedReLU at the perspective boundary
- **Scalar injection** of escape counts at the trunk boundary
- **Direct output shortcut** for the forced-pass signal, bypassing the trunk

> **Note on the name**: "Dual-Perspective" refers to player-perspective symmetry enforced via shared Siamese weights. The architecture deliberately breaks spatial translation equivariance in two places: fixed band-map channels (4–6) encode absolute square identity, and channel 15 recentres around the unicorn. Both choices are intentional and correct for Escampe's non-uniform topology.

&nbsp;

Core design elements:

- **Siamese Network** (Shared-weight dual-perspective encoding, [Bromley et al. — 1993](https://papers.nips.cc/paper/1993/hash/0d4262ad58b4d3866d5aa0f4f9e1c06b-Abstract.html))
- **ResNet** (CNN + residual blocks, [He et al. — 2015](https://arxiv.org/abs/1512.03385))
- **Atrous convolution** (Dilated convolutions for range, [Chen et al. — 2015](https://arxiv.org/abs/1511.07122))
- **ClippedReLU** bounded activations `[0, 1]` ([NNUE — 2018](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html))
- **Dual accumulator** for game positions ([NNUE — 2018](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html))
- **Unicorn-relative encoding** inspired by HalfKP king-relative features ([NNUE — 2018](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html))
- **Direct output shortcut** for near-linear signals ([Stockfish HalfKAv2 PSQT bypass — 2021](https://github.com/official-stockfish/Stockfish/blob/master/src/nnue/nnue_architecture.h))
- **Value head** (Value network for board games, [AlphaZero — 2017](https://arxiv.org/abs/1712.01815))

&nbsp;

**Parameter Count**

| Component | Parameters |
|-----------|------------|
| Encoder (shared, used ×2) | ~312,000 |
| ResBlock ×3 (dim=258) | ~402,648 |
| Output head (258→64→1) | ~16,577 |
| **Total** | **~730,625** |

This model is approximately 2.9 MB in float32. The reduced variant with `embed_dim=64`, `num_res_blocks=2` has around **185K parameters**.

&nbsp;

### Layers

#### Input

For each perspective the input is a `[16, 6, 6]` tensor with the following channels:

| Channel | Content | Type |
|---------|---------|------|
| 0 | My paladin positions | Dynamic 0/1 |
| 1 | My unicorn position | Dynamic 0/1 |
| 2 | Opponent paladin positions | Dynamic 0/1 |
| 3 | Opponent unicorn position | Dynamic 0/1 |
| 4 | Band-1 square mask | **Fixed** 0/1 |
| 5 | Band-2 square mask | **Fixed** 0/1 |
| 6 | Band-3 square mask | **Fixed** 0/1 |
| 7 | Departure constraint mask | Dynamic 0/1 |
| 8 | Band-1 legal landing squares | Dynamic 0/1 |
| 9 | Band-2 legal landing squares | Dynamic 0/1 |
| 10 | Band-3 legal landing squares | Dynamic 0/1 |
| 11 | My row occupancy fraction | Dynamic  |
| 12 | Opponent row occupancy fraction | Dynamic  |
| 13 | My column occupancy fraction | Dynamic  |
| 14 | Opponent column occupancy fraction | Dynamic  |
| 15 | Unicorn-relative opponent attacker map | Dynamic 0/1 |

*Band masks (ch. 4–6) are fixed binary inputs representing the board's absolute spatial structure.*  
*Channels 8–10 make the tempo-forcing mechanic explicit.*  
*Channel 15 places opponent paladins in a coordinate frame centred on our unicorn.*

**Precise construction rules for channels 11–14 and 15:**

- **Channels 11–14 (occupancy fractions):** Each cell in channel 11 holds the fraction of that cell's row occupied by the current player's pieces, i.e. `row_sum / 6.0`. The value is broadcast uniformly across all 6 cells of that row, giving a constant-valued row stripe. Channel 12 is the same for the opponent. Channels 13–14 apply the same logic column-wise (`col_sum / 6.0`, broadcast across the column).

- **Channel 15 (unicorn-relative attacker map):** Opponent paladins are placed in a 6×6 frame centred on the current player's unicorn. The unicorn is anchored at position `(row=2, col=2)` in the relative frame. For each opponent paladin at absolute square `sq`, the relative position is `(sq // 6 − uni_row + 2, sq % 6 − uni_col + 2)`. Positions outside `[0,5]×[0,5]` are clipped (i.e. attackers beyond the frame boundary are silently dropped). This lets the CNN learn threat patterns that are invariant relative to the unicorn's location.

This encodes the full current board state. The CNN learns spatial correlations between pieces and band types. Having two perspectives follows the **Siamese network principle**: the same encoder processes both perspectives with shared weights, enabling the network to generalise patterns relevant for both sides without needing separate encoders.

In addition, **3 scalars are injected** outside the CNN:

| Scalar | Content | Injection point |
|--------|---------|-----------------|
| `escape_me` | Legal move count for my unicorn (0–16), normalised by 16 | Concatenated into `h` |
| `escape_opp` | Legal move count for opponent unicorn, normalised by 16 | Concatenated into `h` |
| `forced_pass` | 1 if current move leaves opponent with no legal reply | Direct output bypass |

The escape counts are appended to `h` after Siamese fusion. The forced-pass bit bypasses the trunk entirely via a single learned scalar weight `w_pass` added directly to the output before `tanh`. This is intentional: `tanh` bounds the sum correctly, and since `w_pass` is initialised to zero and trained from data, it learns an appropriate magnitude without requiring additional scaling.

&nbsp;

#### Shared Spatial Encoder



With $$B$$ the batch size.

Architecture: `Conv2d(16→32, 3×3, pad=1) → BN → ReLU → Conv2d(32→32, 3×3, pad=2, dil=2) → BN → ReLU → Flatten → Linear(1152→128) → BN → ClippedReLU[0,1]`

**Notes:**

- The first convolution captures **local spatial patterns** with a 3×3 receptive field, including adjacent pieces and band types. Broken-path movement of band=2 pieces reaches exactly the diagonal neighbours captured by this kernel, making it the primary short-range movement detector.
- The second convolution with `dilation=2` expands the receptive field to an effective 5×5, capturing medium-range interactions such as L-shaped movement patterns and corridor control.
- **BatchNorm** is applied after each convolution to stabilise training and accelerate convergence. BatchNorm parameters are folded into convolution weights at export time, so Java inference requires no BatchNorm at runtime.
- **ClippedReLU `[0,1]`** is used at the encoder output to ensure both perspective embeddings share the same activation scale before concatenation, preventing one perspective from dominating the other due to magnitude differences. This is borrowed from NNUE's design.
- **Dual-Perspective Fusion:** outputs from both perspectives are concatenated to form `h` of shape `[B, 256]`, then escape scalars are appended giving `h` shape `[B, 258]`. Always pass inputs as `[current_player, opponent]`.

&nbsp;

#### Residual Trunk

There is a trunk of 3 ResBlocks with skip connections.



A residual block computes $$y = F(x) + x$$, where $$F$$ is a small network (two linear layers with BatchNorm and activation). This structure allows the block to learn a *correction* to its input rather than a full transformation from scratch. The skip connection allows gradients to flow directly through the block, mitigating the vanishing gradient problem. If a block learns $$F(x) \approx 0$$ it becomes an identity function, providing graceful degradation.

**Input dimension:** 258 (256 Siamese CNN output + 2 escape scalars).

**Notes:**

- **Why residual blocks:** The network learns *corrections* on top of a near-correct base representation from minimax bootstrapping. Skip connections prevent vanishing gradients with depth.
- **Why 3 ResBlocks:** Maps to Escampe's three natural abstraction levels (proximity/unicorn-relative → band interaction/tempo → forced-pass/escape dynamics). Matches the empirical optimum from board-game literature at this parameter scale ([AlphaZero](https://en.wikipedia.org/wiki/AlphaZero), [Train on Small, Play the Large](https://arxiv.org/pdf/2107.08387), [Mastering Chess with a Transformer Model](https://arxiv.org/html/2409.12272v1#S6)). Going beyond 3 blocks with ClippedReLU risks training instability, as the gradient path through skip connections becomes dominant and the $$F(x)$$ branch receives progressively weaker gradients.
- **Why ClippedReLU `[0,1]` in ResBlocks:** Keeps activations bounded at every depth, preventing value explosion through skip connections and maintaining compatibility with INT8 quantisation (known dynamic range).
- **Scalar injection at trunk input:** Escape counts are global signals with no natural spatial representation. Injecting them at `h` (after CNN, before trunk) lets the ResBlocks combine spatial features with game-state scalars; the trunk's `Linear(258→258)` learns the projection automatically.

&nbsp;

#### Output Head

The output head uses a direct bypass for the forced-pass signal, inspired by [Stockfish's PSQT direct-to-output shortcut](https://github.com/official-stockfish/Stockfish/blob/master/src/nnue/nnue_architecture.h).



`w_pass` is a single learned scalar parameter initialised to zero. Wiring the forced-pass signal directly to the output bypasses the trunk entirely, giving this near-linear signal maximum gradient and preventing the trunk from having to learn to route it. This mirrors Stockfish HalfKAv2's `FullThreats` feature set, which is passed directly to the output layer because some signals have a nearly linear effect on evaluation and the hidden layers add noise rather than value. No ClippedReLU is applied at the output since `tanh` already bounds the final value to $$[-1, +1]$$.

***

&nbsp;

## Training

### Data Generation (Minimax Bootstrapping)

The existing alpha-beta engine is used at **fixed depth 5** to label positions with ground-truth scores. The network learns to approximate these deep search evaluations. Terminal positions (unicorn captured) are labelled exactly ±1.0. All other positions are normalised as `score_label = minimax(board, depth=5) / MATE_SCORE`, where `MATE_SCORE` is the score returned by the engine for an immediate win (a large constant, e.g. `100_000`). This normalises scores to `[-1, 1]`.

The data generation procedure:
1. Reset the board to a random legal opening configuration.
2. Play a mixed game: each ply either selects a random legal move (probability 0.7) or the engine's best move at depth 2 (probability 0.3), to generate diverse positions.
3. At each ply after the 4th, label the current position with probability 0.3 using the depth-5 engine.
4. Continue until the game ends or 100 plies are reached.

Approximately **50K–100K labeled positions** are sufficient for the network to learn meaningful patterns without overfitting. Training examples store `(board_state, last_landing_band, label)` triples — not just `(board_state, label)` — because both the departure constraint mask and the forced-pass signal require the last landing band. Forced-pass positions are rare (~2–5% of positions); the direct shortcut ensures they receive strong gradient despite low frequency. Iterative re-bootstrap rounds at depth 6–8 using the trained network as evaluation function can further improve label quality.

### Loss Function

Mean Squared Error (MSE) between the network's predicted score and the minimax label:

```python
loss = F.mse_loss(prediction, target)
```

### Optimizer and Training Hyperparameters

| Hyperparameter | Value |
|----------------|-------|
| Optimiser | AdamW |
| Learning rate | 1e-3 |
| Weight decay | 1e-4 |
| LR schedule | CosineAnnealingWarmRestarts (T_0=40) |
| Gradient clipping | clip_grad_norm_ = 1.0 |
| Batch size | 256 |
| Epochs | 40 |
| Val split | 10% |
| `drop_last` | True (avoids BatchNorm1d crash on batch size 1) |

Weight initialisation uses `xavier_uniform_(gain=0.5)` to keep activations in the `[0,1]` range from the start, reducing ClippedReLU saturation during early training.

### Training Hardware

Training was performed on a GTX 1080 GPU (CUDA 11.8), which is sufficient for this model size and dataset.

```bash
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
```

### Export Pipeline

BatchNorm parameters are folded into preceding conv/linear weights at export time, so Java inference needs no BatchNorm operations at runtime:

```python
# 1. Fold BatchNorm into conv/linear weights
def fold_batchnorm_conv(conv, bn):
    std = (bn.running_var + bn.eps).sqrt()
    w_new = conv.weight.data * (bn.weight.data / std).view(-1, 1, 1, 1)
    b_new = bn.weight.data * (conv.bias.data - bn.running_mean) / std + bn.bias.data
    return w_new, b_new

# 2. Export all weights to JSON
weights = {k: v.tolist() for k, v in model.state_dict().items()}
weights["band1_mask"] = BAND_MASK[1]
weights["band2_mask"] = BAND_MASK[2]
weights["band3_mask"] = BAND_MASK[3]
weights["w_forced_pass"] = model.w_forced_pass.item()
json.dump(weights, open("escampe_net_weights.json", "w"))

# 3. Save .pth for resuming training
torch.save(model.state_dict(), "escampe_net.pth")

# 4. Push to HuggingFace (optional)
api.upload_file("escampe_net.pth",
                path_in_repo="escampe_net.pth",
                repo_id="mathieu-waharte/escampe-eval")
```

***

&nbsp;

## Inference

Weights are loaded once at startup; the forward pass is then executed manually in Java with pre-allocated buffers to eliminate GC pressure in the hot path:

```java
// Pre-allocate all activation buffers (no GC pressure in hot path)
float[][][] convBuf1, convBuf2;
float[]     encBuf, wEmbed, bEmbed, trunk, h1, outBuf;
float       wForcedPass;   // loaded once at startup

public float evaluate(EscampeBoard board, int lastLandingBand) {
    float[][][] xMe  = boardToTensor(board, board.currentPlayer(), lastLandingBand);
    float[][][] xOpp = boardToTensor(board, board.opponent(),      lastLandingBand);

    float[] w = encode(xMe);   // [128], ClippedReLU [0,1] output
    float[] b = encode(xOpp);  // [128]

    // Scalar signals: escape counts normalised to [0,1]
    float escapeMe  = computeUnicornEscapeCount(board, board.currentPlayer()) / 16.0f;
    float escapeOpp = computeUnicornEscapeCount(board, board.opponent())      / 16.0f;

    // Concatenate perspectives + escape scalars → [258]
    float[] h = concat(w, b, new float[]{escapeMe, escapeOpp});

    // Residual trunk (3 blocks, ClippedReLU [0,1])
    for (ResBlockWeights rb : resBlocks) {
        h = resBlock(h, rb);
    }

    // Output head: Linear(258→64) → ReLU → Linear(64→1)
    float[] out = linear(h, wOut1, bOut1);  // [64]
    out = relu(out);
    float raw = linear(out, wOut2, bOut2)[0];  // [1]

    // Forced-pass direct output shortcut (bypasses trunk)
    int forcedPass = computeForcedPass(board, lastLandingBand);
    raw += wForcedPass * forcedPass;

    return (float) Math.tanh(raw);  // bounded to [-1, +1]
}
```

***

&nbsp;

## Optimizations

### Masks, Maps and Channels

Band masks, departure masks, band-landing maps, row/column occupancy maps, unicorn-relative attacker maps, escape scalars, and the forced-pass direct shortcut provide the strongest low-cost inductive bias improvements for this setting. They give the network explicit information about the board's spatial structure, piece interactions, and game-specific mechanics that are critical for effective learning.

Currently active inputs:

- Row/column occupancy maps (channels 11–14)
- Band-landing maps (channels 8–10)
- Unicorn escape counts (scalars at `h`)
- Forced-pass direct shortcut (scalar at output)
- Unicorn-relative attacker map (channel 15)

&nbsp;

### Iterative Re-bootstrap Rounds

After training the initial network, it can be used to generate new labels by running a deeper minimax search (depth 6–8) with the network as evaluation function. This iterative bootstrapping is the key technique that gives AlphaZero its progressive improvement.

&nbsp;

### Weighted Value Loss

The MSE loss can be modified to give higher weight to positions closer to winning or losing, improving gradient signal near terminal positions.

&nbsp;

### SE Blocks

Squeeze-and-Excitation blocks can be added after convolutional layers to allow the network to learn channel-wise attention. Lightweight and easily integrated.

&nbsp;

### Depthwise Separable Convolutions

Replacing standard convolutions in the encoder with depthwise separable convolutions reduces parameters and computational cost while maintaining performance.

&nbsp;

### Policy Head for Move Ordering

A policy head that outputs a probability distribution over the ~96 possible moves (due to broken-path movement reaching 4/8/16 unique destinations for band 1/2/3 respectively from interior squares) can be used purely for move ordering in alpha-beta search.

> **Move indexing scheme:** Index = `from_sq * MAX_DESTS + dest_idx`, where `MAX_DESTS = 16` (maximum destinations for a band-3 piece from an interior square), giving at most `36 * 16 = 576` possible indices. In practice only ~96 are reachable from any given position; illegal indices are masked to −∞ before softmax.

```python
policy_head = nn.Sequential(
    nn.Linear(258, 64),
    nn.ReLU(),
    nn.Linear(64, 96),
    nn.Softmax(dim=-1)
)
```

For each node in the search tree:
1. Generate legal moves using the existing move generator (very fast).
2. Call the policy head to obtain move probabilities; sort moves by descending probability.
3. Run alpha-beta with well-ordered moves for stronger pruning.
4. Use the handcrafted eval at leaf nodes unless network inference is fast enough.

&nbsp;

### Quantization

The primary deployment path exports weights to JSON for manual Java inference, which does not require quantization. The ONNX path below is an **optional alternative** that enables INT8 quantization but requires static calibration.

| Layer | Params | Quantize? | Why |
|-------|--------|-----------|-----|
| Conv layers (encoder) | ~33K | INT8 | Weights well-distributed; ClippedReLU bounds activations |
| Linear proj (1152→128) | ~148K | INT8 | Largest layer; most to gain |
| ResBlock linears | ~402K | INT8 | Clean bounded range after ClippedReLU |
| Output Linear(64→1) | 65 | keep float32 | Tiny; output precision matters |
| w_forced_pass | 1 | keep float32 | Single scalar; precision matters |
| Band mask channels | fixed | N/A | Already binary (0/1) |

`torch.quantization.quantize_dynamic` does **not** support `nn.Conv2d`. Static quantization with calibration is required:

```python
# Static quantization (supports Conv2d)
model.eval()
model.qconfig = torch.quantization.get_default_qconfig('fbgemm')
torch.quantization.prepare(model, inplace=True)
# Run calibration: feed representative batches through the model
for x_me, x_opp, esc_me, esc_opp, fp, _ in calibration_loader:
    model(x_me, x_opp, esc_me, esc_opp, fp)
torch.quantization.convert(model, inplace=True)

# Export to ONNX for Java ONNX Runtime
torch.onnx.export(model, (x_me, x_opp, esc_me, esc_opp, fp),
                  "escampe_net_q8.onnx",
                  input_names=["x_me","x_opp","esc_me","esc_opp","forced_pass"],
                  output_names=["eval"])
```

Java inference via ONNX Runtime:

```java
// pom.xml: com.microsoft.onnxruntime:onnxruntime:1.17.0
OrtEnvironment env = OrtEnvironment.getEnvironment();
OrtSession session = env.createSession("escampe_net_q8.onnx");
// Target: ~2–5µs inference per position on modern hardware (to be benchmarked)
```

To evaluate quantization impact: compare MAE and MSE between float32 and INT8 predictions on a held-out set of random positions, then run a tournament of 1000 self-play games comparing both models as evaluation functions.

&nbsp;

### Distillation

A smaller "student" network can be trained to mimic the outputs of the larger "teacher" network using KL divergence + MSE distillation loss.  
[Rapfi: Distilling Efficient Neural Network for the Game of Gomoku](https://arxiv.org/html/2503.13178)

&nbsp;

### Rejected Optimizations

- **1×6/6×1 asymmetric kernels:** Originally considered to capture row/column patterns, but broken-path movement means pieces reach both orthogonal and diagonal neighbours, making a standard 3×3 kernel more appropriate. Misunderstanding of the movement rules.
- **Attention pooling / self-attention in encoder:** Conflicts with incremental-style updates; global interaction cost is excessive for this board size.
- **NNUE-style incremental updates:** Architecture is convolutional and not naturally amenable to NNUE-style incremental computation.
- **LayerNorm replacing folded BatchNorm:** Export pipeline folds BatchNorm away at inference, making LayerNorm unnecessary.
- **Pre-activation ResNet:** Too little upside for only 3 residual blocks.
- **DenseNet-style dense connections:** Memory overhead without obvious benefit at this scale.
- **Full transformerisation:** Too expensive and poorly matched to the latency target and board size.

***

&nbsp;

## References

### Papers and Architecture References

- [Bromley et al. — 1993](https://papers.nips.cc/paper/1993/hash/0d4262ad58b4d3866d5aa0f4f9e1c06b-Abstract.html): Siamese network / shared-weight dual-perspective encoding
- [He et al. — 2015](https://arxiv.org/abs/1512.03385): ResNet residual blocks
- [Chen et al. — 2015](https://arxiv.org/abs/1511.07122): Atrous (dilated) convolutions
- [AlphaZero — 2017](https://arxiv.org/abs/1712.01815): Value head and improved evaluation via search
- [Train on Small, Play the Large: scaling up board games with AlphaZero and GNN](https://arxiv.org/pdf/2107.08387): Small-board AlphaZero scaling
- [Mastering Chess with a Transformer Model](https://arxiv.org/html/2409.12272v1#S6): Transformer-based chess scaling insights
- [NNUE — 2018](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html): ClippedReLU `[0,1]`, dual accumulator, HalfKP king-relative features, direct output shortcuts
- [Stockfish HalfKAv2 architecture](https://github.com/official-stockfish/Stockfish/blob/master/src/nnue/nnue_architecture.h): FullThreats direct-to-output connection
- [Stockfish NNUE — Chessprogramming wiki](https://www.chessprogramming.org/Stockfish_NNUE): HalfKP/HalfKAv2 feature set design rationale
- [Rapfi: Distilling Efficient Neural Network for the Game of Gomoku](https://arxiv.org/html/2503.13178): Distillation techniques for board-game evaluation networks

### Tools and Implementation References

- [PyTorch CUDA 11.8 wheels](https://download.pytorch.org/whl/cu118): GPU build used for training on GTX 1080
- [ONNX Runtime](https://onnxruntime.ai): Inference engine for the optional quantized ONNX deployment path