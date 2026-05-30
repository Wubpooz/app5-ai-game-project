# BANDPER Model

## Requirements
Python 3.13 was used for development, but Python 3.10+ should work fine. Install the required dependencies using pip:
```bash
pip install -r escampe/src/model/requirements.txt
```

## Workflow: Data Generation, Training, and Export
The pipeline is split into four distinct steps:

### 1. Data Generation (Java)
Generate a JSON training dataset of self-play games labeled with minimax evaluations by running the Gradle task.
```bash
# Compile and run the exporter (default generates 50,000 states)
./gradlew runDataExport --args="--size 50000 --output training_data.json --depth 5 --seed 42"
```

### 2. Model Training (Python / Google Colab)
Train the PyTorch ResNet model to learn the minimax evaluations.
- **Locally**:
  ```bash
  python escampe/src/model/train.py
  ```
- **Google Colab**:
  Upload the `src/model` folder along with `training_data.json` to a Google Colab GPU instance and run `train.py`.
- **Training Process**:
  - `dataset.py` loads `training_data.json` and dynamically constructs the 16-channel `[16, 6, 6]` perspective tensors and scalar signals.
  - `train.py` runs a PyTorch training loop using AdamW, Cosine Annealing, and MSE loss, saving the best-performing model to `banddper.pth`.

### 3. Weight Exporting (Python)
Fold the batch normalization layers into the convolutional/linear weights (so that Java does not need to compute BN at runtime) and serialize them to JSON.
```bash
python escampe/src/model/export.py
```
This loads `banddper.pth` and outputs `banddper_weights.json` containing:
- Folded convolutional/linear weights and biases.
- The `w_forced_pass` shortcut parameter.
- Precomputed band masks (for Java index convenience).

### 4. Java Inference
During the game, the Java agent loads `banddper_weights.json` once at startup (e.g. using Jackson's `ObjectMapper`) and runs the forward pass:
- Pre-allocate activation buffers to avoid Garbage Collector (GC) pressure in the search hot path.
- Encode the current player's perspective and opponent's perspective using the shared spatial encoder weights.
- Concatenate the encoded embeddings with the unicorn escape counts (normalized to `[0, 1]`).
- Pass the fused representation through the 3 residual blocks.
- Compute the output head value, add the direct shortcut (`w_forced_pass` * `forced_pass`), and apply `Math.tanh()` to yield the evaluation in `[-1.0, 1.0]`.


#### Running the Java Dataset Exporter
You can compile the Java codebase and generate a JSON training dataset of simulated games using the Gradle task:
```bash
# In the escampe directory:
./gradlew runDataExport --args="--size 50000 --output training_data.json --depth 5 --seed 42"
```

**Exporter Arguments**:
- `--size`: Number of positions to generate (default: `50000`).
- `--output`: File path to write the JSON data to (default: `training_data.json`).
- `--depth`: Minimax evaluation search depth (default: `5`).
- `--seed`: Random seed for generating placements and moves (default: `42`).

*Note on Simulation*: The exporter starts each game from a random opening placement in `Opening.java`. It simulates self-play games where a move is selected from `possibleMoves()` with a mix of 70% random moves (to explore diverse board configurations) and 30% alpha-beta depth 2 moves (to keep games structurally sound).

#### Validating & Uploading to Hugging Face
To validate that the generated JSON dataset loads correctly in Python (which parses the JSON objects into 16-channel tensors and scalar signals):
```bash
# Validate a single JSON file
python escampe/src/model/dataset.py --json-path escampe/src/model/data/dataset_50k_d6.json

# Validate a whole folder containing JSON files
python escampe/src/model/dataset.py --json-path escampe/src/model/data/
```

To upload the dataset to the Hugging Face Hub. If a directory is provided, it will automatically upload each JSON file as a separate configuration (subset), push a combined `all` configuration, and upload `DATASET_CARD.md` as `README.md`:
```bash
python escampe/src/model/dataset.py --json-path escampe/src/model/data/ --push-to-hub Bluefir/escampe-dataset
```

## Loading the Dataset & Model in Python
### Loading the Dataset Locally
```python
from escampe.src.model.dataset import EscampeDataset

# Initialize PyTorch dataset from a JSON file or directory of JSONs
dataset = EscampeDataset("escampe/src/model/data/")
```

### Loading the Dataset from Hugging Face
```python
from datasets import load_dataset

# Load a specific configuration (e.g. 1m_d7)
dataset_d7 = load_dataset("Bluefir/escampe-dataset", "dataset_1m_d7", split="train")

# Load all configurations combined
dataset_all = load_dataset("Bluefir/escampe-dataset", "all", split="train")
```

### Loading the Model
```python
import torch
from escampe.src.model.model import BandDPER

# Initialize the model structure
model = BandDPER(num_res_blocks=3)

# Load the weights
model.load_state_dict(torch.load("banddper.pth", map_location="cpu"))
model.eval()
```
