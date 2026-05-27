# BANDPER Model

## Requirements
Python 3.13 was used for development, but Python 3.10+ should work fine. Install the required dependencies using pip:
```bash
pip install -r escampe/src/model/requirements.txt
```

## How to use
1. Export the data for training
2. Train the model using colab


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
python escampe/src/model/dataset.py --json-path escampe/training_data.json
```

To upload the dataset directly to the Hugging Face Hub (as a Parquet/Arrow dataset):
```bash
python escampe/src/model/dataset.py --json-path escampe/training_data.json --push-to-hub username/escampe-dataset
```
