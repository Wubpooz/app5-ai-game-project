import torch
import json

try:
    from model.model import BandDPER
    from model.dataset import BAND, BAND_MASK
except ImportError:
    from model import BandDPER
    from dataset import BAND, BAND_MASK

def fold_batchnorm_conv(conv, bn):
    """Fold BN into conv weights so Java needs no BN at inference."""
    w = conv.weight.data.clone()     # [out, in, kH, kW]
    b = conv.bias.data.clone() if conv.bias is not None else torch.zeros(w.shape[0])
    mean  = bn.running_mean
    var   = bn.running_var
    gamma = bn.weight.data
    beta  = bn.bias.data
    eps   = bn.eps
    std   = (var + eps).sqrt()
    w_new = w * (gamma / std).view(-1, 1, 1, 1)
    b_new = gamma * (b - mean) / std + beta
    return w_new, b_new

def fold_batchnorm_linear(linear, bn):
    w = linear.weight.data.clone()
    b = linear.bias.data.clone() if linear.bias is not None else torch.zeros(w.shape[0])
    mean  = bn.running_mean
    var   = bn.running_var
    gamma = bn.weight.data
    beta  = bn.bias.data
    eps   = bn.eps
    std   = (var + eps).sqrt()
    w_new = w * (gamma / std).unsqueeze(1)
    b_new = gamma * (b - mean) / std + beta
    return w_new, b_new

def t(tensor):
    """Convert tensor to nested Python list for JSON."""
    return tensor.detach().cpu().numpy().tolist()

model = BandDPER(num_res_blocks=3)
model.load_state_dict(torch.load("banddper.pth", map_location='cpu', weights_only=True))
model.eval()

enc = model.encoder
weights = {}

# == Encoder (shared, used for both perspectives) ==
w1, b1 = fold_batchnorm_conv(enc.conv1, enc.bn1)
weights['enc_conv1_w'] = t(w1)
weights['enc_conv1_b'] = t(b1)

w2, b2 = fold_batchnorm_conv(enc.conv2, enc.bn2)
weights['enc_conv2_w'] = t(w2)
weights['enc_conv2_b'] = t(b2)

w_proj, b_proj = fold_batchnorm_linear(enc.proj, enc.bn3)
weights['enc_proj_w'] = t(w_proj)
weights['enc_proj_b'] = t(b_proj)

# == ResBlocks ==
for i, block in enumerate(model.trunk):
    # Fold BN into linear weights
    wl1, bl1 = fold_batchnorm_linear(block.l1, block.bn1)
    wl2, bl2 = fold_batchnorm_linear(block.l2, block.bn2)
    weights[f'res{i}_l1_w'] = t(wl1)
    weights[f'res{i}_l1_b'] = t(bl1)
    weights[f'res{i}_l2_w'] = t(wl2)
    weights[f'res{i}_l2_b'] = t(bl2)

# == Output head ==
weights['out1_w'] = t(model.out1.weight.data)
weights['out1_b'] = t(model.out1.bias.data)
weights['out2_w'] = t(model.out2.weight.data)
weights['out2_b'] = t(model.out2.bias.data)

# == Forced-pass direct shortcut ==
weights['w_forced_pass'] = model.w_forced_pass.item()

# == Fixed band masks (for Java convenience) ==
weights['band1_mask'] = BAND_MASK[1]
weights['band2_mask'] = BAND_MASK[2]
weights['band3_mask'] = BAND_MASK[3]

with open("banddper_weights.json", "w") as f:
    json.dump(weights, f)

print(f"Exported {len(weights)} weight tensors → banddper_weights.json")