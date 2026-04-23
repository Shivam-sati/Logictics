"""
train.py — Predict DELAY RATIO (delay / base_time)
Final delay = ratio × real Google Maps base time
This generalizes correctly to ANY route distance.
"""

import pandas as pd
import numpy as np
import pickle
import os
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

# ── Paths ─────────────────────────────────────────────────────

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
DATA_PATH  = os.path.join(BASE_DIR, "../data/dataset.csv")
MODEL_PATH = os.path.join(BASE_DIR, "model.pkl")

# ── Load Dataset ──────────────────────────────────────────────

print("📂 Loading dataset...")
df = pd.read_csv(DATA_PATH)

print(f"   Rows: {len(df)}")
print(f"   Columns: {list(df.columns)}\n")

# ── Compute delay_ratio (if not already in CSV) ───────────────

if "delay_ratio" not in df.columns:
    print("⚙️  Computing delay_ratio from delay_min and distance_km...")
    df["base_time_min"] = (df["distance_km"] / 50) * 60
    df["delay_ratio"]   = df["delay_min"] / df["base_time_min"]

# Clip ratios to a sane range (0.01 – 1.0) to remove any outliers
df["delay_ratio"] = df["delay_ratio"].clip(0.01, 1.0)

print(f"   delay_ratio stats:\n{df['delay_ratio'].describe().round(3)}\n")

# ── Features / Target ─────────────────────────────────────────
# NOTE: distance_km intentionally excluded — the model should learn
#       traffic behaviour, not distance. Ratio already encodes scale.

FEATURES = [
    "traffic_level",
    "time_of_day",
    "route_type"
]

TARGET = "delay_ratio"

X = df[FEATURES]
y = df[TARGET]

# ── Train/Test Split ──────────────────────────────────────────

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

print(f"🔀 Train: {len(X_train)} | Test: {len(X_test)}")

# ── Model Training ────────────────────────────────────────────

print("\n🌲 Training Random Forest (Delay Ratio Model)...")

model = RandomForestRegressor(
    n_estimators=120,
    max_depth=8,
    min_samples_split=3,
    random_state=42,
    n_jobs=-1
)

model.fit(X_train, y_train)

# ── Evaluation ────────────────────────────────────────────────

y_pred = model.predict(X_test)

mae = mean_absolute_error(y_test, y_pred)
r2  = r2_score(y_test, y_pred)

print("\n📊 Model Evaluation (Delay Ratio):")
print(f"   MAE : {mae:.4f}  (ratio units — multiply by base_time for minutes)")
print(f"   R²  : {r2:.4f}")

# ── Sample Predictions ────────────────────────────────────────

print("\n🧪 Sample Predictions (Delay Ratio):")
for i in range(min(5, len(X_test))):
    actual    = y_test.iloc[i]
    predicted = y_pred[i]
    print(f"   Actual: {actual:.3f} | Predicted: {predicted:.3f}")

# ── Feature Importance ────────────────────────────────────────

print("\n🔍 Feature Importance:")
importances = dict(zip(FEATURES, model.feature_importances_))

for feat, imp in sorted(importances.items(), key=lambda x: -x[1]):
    bar = "█" * int(imp * 40)
    print(f"   {feat:<20} {bar} {imp:.3f}")

# ── Save Model ────────────────────────────────────────────────

with open(MODEL_PATH, "wb") as f:
    pickle.dump(model, f)

print(f"\n✅ Model saved to: {MODEL_PATH}")
print("🚀 Ready: Model predicts DELAY RATIO")
print("   Usage: predicted_delay_min = model.predict(...) × base_time_min\n")