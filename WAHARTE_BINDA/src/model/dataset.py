import json
import os
import numpy as np
import torch
from torch.utils.data import Dataset

# LISERET[row][col], row 0 = bottom (row 01), col 0 = A
# Flattened row-major: BAND[row * 6 + col]
BAND = [
    1, 2, 2, 3, 1, 2,   # row 0 (bottom, row 01)
    3, 1, 3, 1, 3, 2,   # row 1 (row 02)
    2, 3, 1, 2, 1, 3,   # row 2 (row 03)
    2, 1, 3, 2, 3, 1,   # row 3 (row 04)
    1, 3, 1, 3, 1, 2,   # row 4 (row 05)
    3, 2, 2, 1, 3, 2    # row 5 (top, row 06)
]

# Precomputed band masks: BAND_MASK[b][sq] = 1 if BAND[sq] == b
BAND_MASK = [[1 if BAND[sq] == b else 0 for sq in range(36)] for b in range(4)]

# Orthogonal directions for move generation
DIRECTIONS = [(1, 0), (-1, 0), (0, 1), (0, -1)]


# ==========================================================
#  Channel helpers
# ==========================================================
def long_to_channel(val: int) -> np.ndarray:
    """Convert a long bitboard (bit i = square i) to a 6x6 binary array."""
    bits = [(val >> i) & 1 for i in range(36)]
    return np.array(bits, dtype=np.float32).reshape(6, 6)

def sq_to_channel(sq: int) -> np.ndarray:
    """One-hot 6x6 array for a single square index."""
    arr = np.zeros(36, dtype=np.float32)
    if 0 <= sq < 36:
        arr[sq] = 1.0
    return arr.reshape(6, 6)

def band_channel(band: int) -> np.ndarray:
    """Fixed binary mask for squares with the given band value."""
    return np.array(BAND_MASK[band], dtype=np.float32).reshape(6, 6)

def departure_mask(required_band: int, piece_bb: int) -> np.ndarray:
    """Squares where pieces CAN depart (on required band, or all if free)."""
    if required_band == 0:
        return long_to_channel(piece_bb)
    mask = np.array(BAND_MASK[required_band], dtype=np.float32)
    piece = np.array([(piece_bb >> i) & 1 for i in range(36)], dtype=np.float32)
    return (mask * piece).reshape(6, 6)


# ==========================================================
#  Move generation (Python port of Java EscampeBoard logic)
# ==========================================================
def _reachable_mask(start_sq: int, dist: int, all_pieces: int, opp_unicorn_sq: int) -> int:
    """Return bitmask of squares reachable from start_sq in exactly `dist`
    orthogonal steps, following Escampe broken-path movement rules.

    - Intermediate cells must be empty.
    - Cannot revisit cells within a single move.
    - Final cell must not be occupied by own piece or opponent paladin
      (can only land on empty cells or the opponent's unicorn).
    """
    sr, sc = start_sq // 6, start_sq % 6
    visited_init = 1 << start_sq

    # Forbidden landing squares: all occupied squares except the opponent's unicorn
    forbidden_landing = all_pieces & ~(1 << opp_unicorn_sq)

    def dfs(r, c, steps_left, visited):
        if steps_left == 0:
            sq = r * 6 + c
            # Can land on empty or opponent's unicorn
            if (forbidden_landing >> sq) & 1:
                return 0
            return 1 << sq

        mask = 0
        for dr, dc in DIRECTIONS:
            nr, nc = r + dr, c + dc
            if not (0 <= nr < 6 and 0 <= nc < 6):
                continue
            nsq = nr * 6 + nc
            if (visited >> nsq) & 1:
                continue  # already visited in this path
            if steps_left > 1:
                # Intermediate: must be empty
                if (all_pieces >> nsq) & 1:
                    continue
            else:
                # Final step: can't land on own piece or opponent's paladin
                if (forbidden_landing >> nsq) & 1:
                    continue
            mask |= dfs(nr, nc, steps_left - 1, visited | (1 << nsq))
        return mask

    return dfs(sr, sc, dist, visited_init)


def count_unicorn_escapes(uni_sq: int, all_pieces: int, opp_uni_sq: int) -> int:
    """Count legal destination squares for a unicorn (ignoring band constraint)."""
    dist = BAND[uni_sq]
    mask = _reachable_mask(uni_sq, dist, all_pieces, opp_uni_sq)
    return bin(mask).count('1')


def compute_forced_pass(required_band: int, opp_uni_sq: int, all_pieces: int, own_pieces_bb: int) -> int:
    """Return 1 if the player owning own_pieces_bb has no legal move
    under the given required_band constraint; 0 otherwise."""
    for sq in range(36):
        if not ((own_pieces_bb >> sq) & 1):
            continue
        # Band constraint: skip pieces not on the required band
        if required_band != 0 and BAND[sq] != required_band:
            continue
        dist = BAND[sq]
        mask = _reachable_mask(sq, dist, all_pieces, opp_uni_sq)
        if mask:
            return 0  # at least one legal move exists
    return 1  # no legal moves → forced pass


# ==========================================================
#  Unicorn-relative attacker map (ch 15)
# ==========================================================
def unicorn_relative_attacker_map(my_uni_sq: int, opp_pal_bb: int) -> np.ndarray:
    """Place opponent paladins in a coordinate frame centered on our unicorn.
    Unicorn is anchored at (row=2, col=2) per the spec."""
    uni_row, uni_col = my_uni_sq // 6, my_uni_sq % 6
    ch = np.zeros((6, 6), dtype=np.float32)
    for sq in range(36):
        if (opp_pal_bb >> sq) & 1:
            rel_row = (sq // 6) - uni_row + 2
            rel_col = (sq % 6) - uni_col + 2
            if 0 <= rel_row < 6 and 0 <= rel_col < 6:
                ch[rel_row, rel_col] = 1.0
    return ch


# ==========================================================
#  Full tensor builder
# ==========================================================
def build_tensor(entry: dict, perspective: str):
    """Build [16, 6, 6] tensor + scalars for one perspective.

    Returns (tensor, escape_me, escape_opp, forced_pass).
    """
    wp = int(entry['white_paladins'])
    wu = int(entry['white_unicorn'])
    bp = int(entry['black_paladins'])
    bu = int(entry['black_unicorn'])
    rb = int(entry['required_band'])

    if perspective == 'white':
        my_pal, my_uni, opp_pal, opp_uni = wp, wu, bp, bu
    else:
        my_pal, my_uni, opp_pal, opp_uni = bp, bu, wp, wu

    my_pieces_bb  = my_pal  | (1 << my_uni)
    opp_pieces_bb = opp_pal | (1 << opp_uni)
    all_pieces  = my_pieces_bb | opp_pieces_bb

    # --- Piece arrays for occupancy channels ---
    my_arr  = np.array([(my_pieces_bb >> i) & 1 for i in range(36)], dtype=np.float32)
    opp_arr = np.array([(opp_pieces_bb >> i) & 1 for i in range(36)], dtype=np.float32)
    occupied = np.array([(all_pieces >> i) & 1 for i in range(36)], dtype=np.float32)
    empty = 1.0 - occupied

    # == Channels 0–3: piece positions ==
    ch0  = long_to_channel(my_pal)
    ch1  = sq_to_channel(my_uni)
    ch2  = long_to_channel(opp_pal)
    ch3  = sq_to_channel(opp_uni)

    # == Channels 4–6: fixed band masks ==
    ch4  = band_channel(1)
    ch5  = band_channel(2)
    ch6  = band_channel(3)

    # == Channel 7: departure constraint mask ==
    ch7  = departure_mask(rb, my_pieces_bb)

    # == Channels 8–10: band-specific legal landing squares ==
    #    Empty squares on band b → landing there forces opponent onto band b
    band1_flat = np.array(BAND_MASK[1], dtype=np.float32)
    band2_flat = np.array(BAND_MASK[2], dtype=np.float32)
    band3_flat = np.array(BAND_MASK[3], dtype=np.float32)
    ch8  = (band1_flat * empty).reshape(6, 6)
    ch9  = (band2_flat * empty).reshape(6, 6)
    ch10 = (band3_flat * empty).reshape(6, 6)

    # == Channels 11–14: row/column occupancy fractions ==
    my_2d  = my_arr.reshape(6, 6)
    opp_2d = opp_arr.reshape(6, 6)

    ch11 = np.repeat(my_2d.sum(axis=1, keepdims=True),  6, axis=1) / 6.0
    ch12 = np.repeat(opp_2d.sum(axis=1, keepdims=True), 6, axis=1) / 6.0
    ch13 = np.repeat(my_2d.sum(axis=0, keepdims=True),  6, axis=0) / 6.0
    ch14 = np.repeat(opp_2d.sum(axis=0, keepdims=True), 6, axis=0) / 6.0

    # == Channel 15: unicorn-relative opponent attacker map ==
    ch15 = unicorn_relative_attacker_map(my_uni, opp_pal)

    tensor = np.stack([ch0, ch1, ch2, ch3, ch4, ch5, ch6, ch7, ch8, ch9, ch10, ch11, ch12, ch13, ch14, ch15], axis=0)  # [16, 6, 6]

    # == Scalar signals ==
    esc_me = count_unicorn_escapes(my_uni,  all_pieces, opp_uni) / 16.0
    esc_opp = count_unicorn_escapes(opp_uni, all_pieces, my_uni) / 16.0
    fp = float(compute_forced_pass(rb, opp_uni, all_pieces, my_pieces_bb))

    return tensor, esc_me, esc_opp, fp


# ==========================================================
#  PyTorch Dataset
# ==========================================================
def load_raw_data(path: str) -> list:
    """Load JSON records from a single file or a directory containing JSON files."""
    if os.path.isdir(path):
        raw = []
        # Sort files to ensure deterministic ordering of examples
        for f in sorted(os.listdir(path)):
            if f.endswith('.json'):
                with open(os.path.join(path, f), 'r') as file:
                    raw.extend(json.load(file))
        return raw
    else:
        with open(path, 'r') as file:
            return json.load(file)


class EscampeDataset(Dataset):
    def __init__(self, path: str):
        raw = load_raw_data(path)
        self.data = []
        for entry in raw:
            white_to_move = bool(entry['white_to_move'])
            score = float(entry['score'])  # from current player's perspective

            # Build perspective tensors + scalars
            me_persp  = 'white' if white_to_move else 'black'
            opp_persp = 'black' if white_to_move else 'white'

            x_me,  esc_me,  esc_opp_me,  fp_me  = build_tensor(entry, me_persp)
            x_opp, esc_opp, esc_me_opp, fp_opp      = build_tensor(entry, opp_persp)

            self.data.append((
                torch.tensor(x_me, dtype=torch.float32),
                torch.tensor(x_opp, dtype=torch.float32),
                torch.tensor(esc_me, dtype=torch.float32),
                torch.tensor(esc_opp_me, dtype=torch.float32),
                torch.tensor(fp_me, dtype=torch.float32),
                torch.tensor(score, dtype=torch.float32),
            ))
            
            # Data augmentation
            self.data.append((
                torch.tensor(x_opp,  dtype=torch.float32),
                torch.tensor(x_me,   dtype=torch.float32),
                torch.tensor(esc_opp, dtype=torch.float32),
                torch.tensor(esc_me_opp,  dtype=torch.float32),
                torch.tensor(fp_opp,     dtype=torch.float32),
                torch.tensor(-score, dtype=torch.float32),   # flipped score
            ))

    def __len__(self):
        return len(self.data)

    def __getitem__(self, i):
        return self.data[i]


if __name__ == "__main__":
    import argparse
    import json

    parser = argparse.ArgumentParser(description="Escampe Dataset Utilities")
    parser.add_argument(
        "--json-path",
        type=str,
        default="training_data.json",
        help="Path to the training data JSON file",
    )
    parser.add_argument(
        "--push-to-hub",
        type=str,
        default=None,
        help="Hugging Face repository ID to push the dataset to (e.g. username/escampe-dataset)",
    )
    parser.add_argument(
        "--private",
        action="store_true",
        help="Whether to make the Hugging Face repository private",
    )

    args = parser.parse_args()

    # Prevent path traversal vulnerabilities (CWE-23)
    json_path = os.path.normpath(args.json_path)
    if ".." in json_path.split(os.path.sep) or json_path.startswith("/") or json_path.startswith("\\"):
        # Allow absolute paths but restrict directory traversal
        if ".." in json_path:
            raise ValueError("Security Error: Directory traversal not allowed in file path.")

    if args.push_to_hub:
        try:
            from datasets import Dataset as HFDataset
            
            files_to_push = []
            if os.path.isdir(json_path):
                for f in sorted(os.listdir(json_path)):
                    if f.endswith('.json'):
                        files_to_push.append(os.path.join(json_path, f))
            else:
                files_to_push.append(json_path)

            if not files_to_push:
                print(f"No JSON files found in {json_path}")
                exit(1)

            # If it's a directory and there are multiple files, we also prepare a combined 'all' dataset
            combined_data = []

            for file_path in files_to_push:
                filename = os.path.basename(file_path)
                if os.path.isdir(json_path):
                    config_name = os.path.splitext(filename)[0]
                else:
                    config_name = "default"

                print(f"Loading dataset from {file_path} for config '{config_name}'...")
                with open(file_path, 'r', encoding='utf-8') as file:
                    data = json.load(file)
                
                if os.path.isdir(json_path):
                    combined_data.extend(data)

                print(f"Creating Hugging Face Dataset for config '{config_name}'...")
                hf_dataset = HFDataset.from_list(data)

                print(f"Pushing config '{config_name}' to Hugging Face Hub: {args.push_to_hub}...")
                hf_dataset.push_to_hub(args.push_to_hub, config_name=config_name, private=args.private)
                print(f"Successfully pushed config '{config_name}'!")

            # Push the combined 'all' dataset if we processed a directory with multiple files
            if os.path.isdir(json_path) and len(files_to_push) > 1:
                print("Creating combined 'all' dataset...")
                hf_dataset_all = HFDataset.from_list(combined_data)
                print(f"Pushing config 'all' to Hugging Face Hub: {args.push_to_hub}...")
                hf_dataset_all.push_to_hub(args.push_to_hub, config_name="all", private=args.private)
                print("Successfully pushed config 'all'!")

            print("All uploads completed successfully!")
        except ImportError:
            print(
                "Error: 'datasets' and 'huggingface_hub' libraries are required for Hugging Face upload."
            )
            print("Please install them using: pip install datasets huggingface_hub")
        except Exception as e:
            print(f"Error during Hugging Face upload: {e}")
    else:
        try:
            print(f"Validating dataset from '{json_path}'...")
            dataset = EscampeDataset(json_path)
            print(f"Successfully loaded dataset with {len(dataset)} examples.")
            if len(dataset) > 0:
                x_me, x_opp, esc_me, esc_opp_me, fp_me, score = dataset[0]
                print("First sample verification:")
                print(f"  x_me tensor shape:        {x_me.shape}")
                print(f"  x_opp tensor shape:       {x_opp.shape}")
                print(f"  My unicorn escapes:       {esc_me.item()}")
                print(f"  Opponent unicorn escapes: {esc_opp_me.item()}")
                print(f"  Forced pass signal:       {fp_me.item()}")
                print(f"  Evaluation score:         {score.item()}")
        except Exception as e:
            print(f"Validation failed: {e}")