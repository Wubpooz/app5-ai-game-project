"""
Bayesian Optimization for Escampe AI heuristic parameters.
Uses scikit-learn GaussianProcessRegressor with Matern kernel and Expected Improvement.

Runs in two modes:
  1. 9-parameter (all weights including band control)
  2. 7-parameter (forcing band control weights to 0)

Calls the Java Evaluator via subprocess for each evaluation.
"""

import subprocess
import sys
import os
import numpy as np
from scipy.stats import norm
from scipy.optimize import minimize
from sklearn.gaussian_process import GaussianProcessRegressor
from sklearn.gaussian_process.kernels import Matern, ConstantKernel

# Path configuration
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CLASSPATH = os.path.join(SCRIPT_DIR, "build", "classes", "java", "main") + ";" + \
os.path.join(PROJECT_ROOT, "libraries", "escampeobf.jar")

# Parameter definitions
PARAM_NAMES_9 = [
  "weightMinDist", "weightAvgDist", "weightUnicornDangerMinDist",
  "weightUnicornDangerAvgDist", "weightEscapability", "weightTrappedUnicorn",
  "weightLegalMoves", "weightBandControl", "weightOppBandControl"
]

PARAM_NAMES_7 = PARAM_NAMES_9[:7]

# Parameter bounds: (min, max) for each parameter
BOUNDS_9 = [
  (0, 50),     # weightMinDist
  (0, 50),     # weightAvgDist
  (0, 50),     # weightUnicornDangerMinDist
  (0, 50),     # weightUnicornDangerAvgDist
  (0, 200),    # weightEscapability
  (0, 150),    # weightTrappedUnicorn
  (0, 100),    # weightLegalMoves
  (-100, 100), # weightBandControl
  (-100, 100), # weightOppBandControl
]

BOUNDS_7 = BOUNDS_9[:7]


def evaluate_weights(weights_9):
  """Call the Java Evaluator and return the win rate."""
  int_weights = [str(int(round(w))) for w in weights_9]
  cmd = ["java", "-cp", CLASSPATH, "ranking.Evaluator"] + int_weights

  try:
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=300, cwd=SCRIPT_DIR)
    for line in result.stdout.strip().split("\n"):
      if line.startswith("SCORE:"):
        return float(line.split(":")[1].strip())
    print(f"  [WARN] No SCORE in output: {result.stdout[:200]}", file=sys.stderr)
    return 0.5
  except subprocess.TimeoutExpired:
    print("  [WARN] Evaluation timed out", file=sys.stderr)
    return 0.5
  except Exception as e:
    print(f"  [ERROR] {e}", file=sys.stderr)
    return 0.5


def expected_improvement(X, gp, y_best, xi=0.01):
  """Compute Expected Improvement at points X."""
  mu, sigma = gp.predict(X, return_std=True)
  sigma = np.maximum(sigma, 1e-8)
  Z = (mu - y_best - xi) / sigma
  ei = (mu - y_best - xi) * norm.cdf(Z) + sigma * norm.pdf(Z)
  return ei


def propose_next(gp, y_best, bounds, n_restarts=25):
  """Find the point that maximizes Expected Improvement."""
  dim = len(bounds)
  best_x = None
  best_ei = -1.0

  for _ in range(n_restarts):
    x0 = np.array([np.random.uniform(lo, hi) for lo, hi in bounds])

    def neg_ei(x):
      return -expected_improvement(x.reshape(1, -1), gp, y_best)[0]

    result = minimize(neg_ei, x0, bounds=bounds, method='L-BFGS-B')
    if -result.fun > best_ei:
      best_ei = -result.fun
      best_x = result.x

  return best_x


def latin_hypercube_sample(bounds, n_samples):
  """Generate initial points using Latin Hypercube Sampling."""
  dim = len(bounds)
  samples = np.zeros((n_samples, dim))
  for i in range(dim):
    lo, hi = bounds[i]
    # Create evenly spaced intervals, then randomly sample within each
    intervals = np.linspace(lo, hi, n_samples + 1)
    for j in range(n_samples):
      samples[j, i] = np.random.uniform(intervals[j], intervals[j + 1])
  # Shuffle rows independently per column
  for i in range(dim):
    np.random.shuffle(samples[:, i])
  return samples


def run_bayesian_optimization(dim, bounds, param_names, force_zero_band=False, n_initial=8, n_iterations=25):
  """Run Bayesian Optimization."""
  print(f"\n{'='*60}")
  if force_zero_band:
    print(f"BAYESIAN OPTIMIZATION: {dim}-PARAMETER (Band Control = 0)")
  else:
    print(f"BAYESIAN OPTIMIZATION: {dim}-PARAMETER")
  print(f"{'='*60}")
  print(f"Initial samples: {n_initial}, Iterations: {n_iterations}")
  print(f"Total evaluations: {n_initial + n_iterations}")

  # Generate initial samples
  X = latin_hypercube_sample(bounds, n_initial)
  y = np.zeros(n_initial)

  print(f"\n--- Phase 1: Initial Sampling ({n_initial} points) ---")
  for i in range(n_initial):
    weights_9 = build_weights_9(X[i], force_zero_band)
    score = evaluate_weights(weights_9)
    y[i] = score
    print(f"  [Init {i+1:02d}/{n_initial}] Score: {score:.4f} | " +
        ", ".join(f"{param_names[j]}={int(round(X[i,j]))}" for j in range(dim)))

  # Bayesian Optimization loop
  kernel = ConstantKernel(1.0) * Matern(length_scale=np.ones(dim), nu=2.5)
  gp = GaussianProcessRegressor(kernel=kernel, alpha=0.01, n_restarts_optimizer=5, normalize_y=True)

  print(f"\n--- Phase 2: Bayesian Optimization ({n_iterations} iterations) ---")
  for iteration in range(n_iterations):
    gp.fit(X, y)
    y_best = np.max(y)

    x_next = propose_next(gp, y_best, bounds)
    weights_9 = build_weights_9(x_next, force_zero_band)
    score = evaluate_weights(weights_9)

    X = np.vstack([X, x_next.reshape(1, -1)])
    y = np.append(y, score)

    best_idx = np.argmax(y)
    print(f"  [Iter {iteration+1:02d}/{n_iterations}] Score: {score:.4f} | "
        f"Best so far: {y[best_idx]:.4f} | " +
        ", ".join(f"{param_names[j]}={int(round(x_next[j]))}" for j in range(dim)))

  # Find best
  best_idx = np.argmax(y)
  best_x = X[best_idx]
  best_score = y[best_idx]

  print(f"\n{'='*60}")
  print("OPTIMIZATION COMPLETE")
  print(f"{'='*60}")
  print(f"Best score: {best_score:.4f}")
  print("\nOptimized weights:")
  for j in range(dim):
    print(f"  {param_names[j]:30s} : {int(round(best_x[j]))}")
  if force_zero_band:
    print(f"  {'weightBandControl':30s} : 0 (FORCED)")
    print(f"  {'weightOppBandControl':30s} : 0 (FORCED)")

  # Print copy-paste Java code
  weights_9_final = build_weights_9(best_x, force_zero_band)
  int_w = [int(round(w)) for w in weights_9_final]
  print("\nJava code for HeuristicConfig.java:")
  if force_zero_band:
    print(f'return new HeuristicConfig("Bayes-Tuned-NoBand", {int_w[0]}, {int_w[1]}, {int_w[2]}, '
        f'{int_w[3]}, {int_w[4]}, {int_w[5]}, {int_w[6]}, 0, 0);')
  else:
    print(f'return new HeuristicConfig("Bayes-Tuned-Full", {int_w[0]}, {int_w[1]}, {int_w[2]}, '
        f'{int_w[3]}, {int_w[4]}, {int_w[5]}, {int_w[6]}, {int_w[7]}, {int_w[8]});')

  return best_x, best_score


def build_weights_9(x_partial, force_zero_band):
  """Convert a partial parameter vector to full 9-parameter weights."""
  if force_zero_band:
    return list(x_partial) + [0.0, 0.0]
  else:
    return list(x_partial)


if __name__ == "__main__":
  print("=" * 60)
  print("ESCAMPE BAYESIAN OPTIMIZATION PARAMETER TUNING")
  print("=" * 60)

  # Verify Java Evaluator works
  print("\nVerifying Java Evaluator...")
  test_score = evaluate_weights([10, 2, 5, 5, 50, 30, 10, 80, 90])
  print(f"Default weights score: {test_score:.4f}")

  # RUN 1: 9-parameter optimization
  score_9 = 0.0
  print("\n" + "=" * 60)
  print(">>> RUN 1: Full 9-Parameter Optimization")
  best_9, score_9 = run_bayesian_optimization(
    dim=9, bounds=BOUNDS_9, param_names=PARAM_NAMES_9,
    force_zero_band=False, n_initial=8, n_iterations=20
  )

  # RUN 2: 7-parameter optimization (forcing band control to 0)
  print("\n" + "=" * 60)
  print(">>> RUN 2: 7-Parameter Optimization (Band Control = 0)")
  best_7, score_7 = run_bayesian_optimization(
    dim=7, bounds=BOUNDS_7, param_names=PARAM_NAMES_7,
    force_zero_band=True, n_initial=8, n_iterations=20
  )

  # Summary
  print("\n" + "=" * 60)
  print("FINAL COMPARISON")
  print("=" * 60)
  print(f"9-Parameter best score: {score_9:.4f}")
  print(f"7-Parameter best score: {score_7:.4f} (band control forced to 0)")

  if score_7 >= score_9:
    print("\n>>> RECOMMENDATION: Use 7-parameter weights (band control = 0)")
    print("    Band control does not contribute to strength even when optimized.")
  else:
    print("\n>>> RECOMMENDATION: Use 9-parameter weights")
    print("    Band control contributes positively when properly weighted.")
