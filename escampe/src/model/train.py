# train.py  — run this in Colab
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, random_split

try:
    from model.model import BandDPER
    from model.dataset import EscampeDataset
except ImportError:
    from model import BandDPER
    from dataset import EscampeDataset

# == Config ==============================================
DATA_PATH      = "training_data.json"
EPOCHS         = 40
BATCH_SIZE     = 256
LR             = 1e-3
WEIGHT_DECAY   = 1e-4
VAL_SPLIT      = 0.1
CHECKPOINT     = "banddper.pth"
DEVICE         = "cuda" if torch.cuda.is_available() else "cpu"
# ========================================================

print(f"Training on {DEVICE}")
dataset = EscampeDataset(DATA_PATH)
val_size  = int(len(dataset) * VAL_SPLIT)
train_size = len(dataset) - val_size
train_ds, val_ds = random_split(dataset, [train_size, val_size],
                                generator=torch.Generator().manual_seed(42))

train_loader = DataLoader(train_ds, batch_size=BATCH_SIZE, shuffle=True,
                          num_workers=2, pin_memory=(DEVICE == "cuda"), drop_last=True)
val_loader   = DataLoader(val_ds,   batch_size=BATCH_SIZE, shuffle=False,
                          num_workers=2, pin_memory=(DEVICE == "cuda"), drop_last=False)

model = BandDPER(num_res_blocks=3).to(DEVICE)
optimizer = torch.optim.AdamW(model.parameters(), lr=LR, weight_decay=WEIGHT_DECAY)
scheduler = torch.optim.lr_scheduler.CosineAnnealingWarmRestarts(optimizer, T_0=EPOCHS)
criterion = nn.MSELoss()

best_val = float('inf')
for epoch in range(1, EPOCHS + 1):
    # == Train ==
    model.train()
    train_loss = 0.0
    for x_me, x_opp, esc_me, esc_opp, fp, targets in train_loader:
        x_me    = x_me.to(DEVICE, non_blocking=True)
        x_opp   = x_opp.to(DEVICE, non_blocking=True)
        esc_me  = esc_me.to(DEVICE, non_blocking=True)
        esc_opp = esc_opp.to(DEVICE, non_blocking=True)
        fp      = fp.to(DEVICE, non_blocking=True)
        targets = targets.to(DEVICE, non_blocking=True)

        optimizer.zero_grad(set_to_none=True)
        preds = model(x_me, x_opp, esc_me, esc_opp, fp)
        loss  = criterion(preds, targets)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()
        train_loss += loss.item() * len(targets)
    train_loss /= train_size

    # == Validate ==
    model.eval()
    val_loss = 0.0
    with torch.no_grad():
        for x_me, x_opp, esc_me, esc_opp, fp, targets in val_loader:
            x_me    = x_me.to(DEVICE, non_blocking=True)
            x_opp   = x_opp.to(DEVICE, non_blocking=True)
            esc_me  = esc_me.to(DEVICE, non_blocking=True)
            esc_opp = esc_opp.to(DEVICE, non_blocking=True)
            fp      = fp.to(DEVICE, non_blocking=True)
            targets = targets.to(DEVICE, non_blocking=True)

            preds = model(x_me, x_opp, esc_me, esc_opp, fp)
            val_loss += criterion(preds, targets).item() * len(targets)
    val_loss /= val_size
    scheduler.step()

    print(f"Epoch {epoch:3d} | train_loss={train_loss:.4f} | val_loss={val_loss:.4f}")

    if val_loss < best_val:
        best_val = val_loss
        torch.save(model.state_dict(), CHECKPOINT)
        print(f"  ✓ Saved checkpoint (val={val_loss:.4f})")

print(f"\nBest val loss: {best_val:.4f} → {CHECKPOINT}")