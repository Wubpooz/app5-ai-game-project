import torch
import torch.nn as nn
import torch.nn.functional as F

class ResBlock(nn.Module):
    def __init__(self, dim):
        super().__init__()
        self.l1 = nn.Linear(dim, dim)
        self.l2 = nn.Linear(dim, dim)
        self.bn1 = nn.BatchNorm1d(dim)
        self.bn2 = nn.BatchNorm1d(dim)

    def forward(self, x):
        h = F.relu(self.bn1(self.l1(x)))
        h = self.bn2(self.l2(h))
        # ClippedReLU after skip: clamp to [0,1] as specified in the PDF to prevent value explosion
        return torch.clamp(x + h, 0.0, 1.0) #TODO may saturate at 1

class SharedSpatialEncoder(nn.Module):
    """Processes one [16, 6, 6] perspective tensor -> [128] embedding.

    Architecture (from BandDPER spec PDF):
      Conv2d(16->32, 3x3, pad=1) -> BN -> ReLU
      Conv2d(32->32, 3x3, pad=2, dilation=2) -> BN -> ReLU
      Flatten -> Linear(1152->128) -> BN -> ClippedReLU
    """
    def __init__(self):
        super().__init__()
        # Local patterns (3x3)
        self.conv1 = nn.Conv2d(16, 32, kernel_size=3, padding=1)
        self.bn1   = nn.BatchNorm2d(32)
        # Dilated for medium range (effective 5x5 receptive field)
        self.conv2 = nn.Conv2d(32, 64, kernel_size=3, padding=2, dilation=2)
        self.bn2   = nn.BatchNorm2d(64)
        # Projection: 64*6*6 = 2304 -> 128
        self.proj = nn.Linear(64 * 6 * 6, 128)
        self.bn3  = nn.BatchNorm1d(128)

    def forward(self, x):   # x: [B, 16, 6, 6]
        h = F.relu(self.bn1(self.conv1(x)))           # [B, 32, 6, 6]
        h = F.relu(self.bn2(self.conv2(h)))           # [B, 64, 6, 6]
        flat = h.flatten(1)                            # [B, 2304]
        # ClippedReLU at perspective boundary to prevent scale dominance
        return torch.clamp(self.bn3(self.proj(flat)), 0.0, 1.0)  # [B, 128]
        #TODO may saturate at 1

class BandDPER(nn.Module):
    """BandDPER evaluation network.

    Forward signature:
        forward(x_me, x_opp, escape_me, escape_opp, forced_pass)
        - x_me, x_opp:   [B, 16, 6, 6]  perspective tensors
        - escape_me:      [B]            my unicorn escape count (normalized 0-1)
        - escape_opp:     [B]            opp unicorn escape count (normalized 0-1)
        - forced_pass:    [B]            1 if current player must pass, else 0

    Returns: [B] scalar evaluation in [-1, +1] (tanh output).
    """
    def __init__(self, num_res_blocks=3):
        super().__init__()
        self.encoder = SharedSpatialEncoder()  # shared weights, used twice
        # Trunk: 258 -> 258 x num_res_blocks (256 Siamese + 2 escape scalars)
        self.trunk = nn.ModuleList([ResBlock(258) for _ in range(num_res_blocks)])
        # Output head
        self.out1 = nn.Linear(258, 64)
        self.out2 = nn.Linear(64, 1)
        # Direct output shortcut for forced-pass signal (bypasses trunk)
        self.w_forced_pass = nn.Parameter(torch.zeros(1))
        
        # Initialize weights with xavier_uniform (gain=0.5) to keep activations in [0, 1]
        self._init_weights()

    def _init_weights(self):
        for m in self.modules():
            if isinstance(m, (nn.Conv2d, nn.Linear)):
                nn.init.xavier_uniform_(m.weight, gain=0.5)
                if m.bias is not None:
                    nn.init.constant_(m.bias, 0.0)

    def forward(self, x_me, x_opp, escape_me, escape_opp, forced_pass):
        # x_me, x_opp: [B, 16, 6, 6]
        w = self.encoder(x_me)         # [B, 128]
        b = self.encoder(x_opp)        # [B, 128]

        # Ensure scalars are [B, 1] for concatenation
        if escape_me.dim() == 1:
            escape_me = escape_me.unsqueeze(1)
        if escape_opp.dim() == 1:
            escape_opp = escape_opp.unsqueeze(1)

        # Dual-perspective fusion + escape scalar injection -> [B, 258]
        h = torch.cat([w, b, escape_me, escape_opp], dim=1)

        # Residual trunk
        for block in self.trunk:
            h = block(h)

        # Output head: Linear(258->64) -> ReLU -> Linear(64->1)
        h = F.relu(self.out1(h))
        raw = self.out2(h).squeeze(-1)  # [B]

        # Forced-pass direct output shortcut (bypasses trunk)
        if forced_pass.dim() > 1:
            forced_pass = forced_pass.squeeze(-1)
        raw = raw + self.w_forced_pass * forced_pass

        return torch.tanh(raw)  # [B], bounded to [-1, +1]