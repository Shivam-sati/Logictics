"""
train.py — Trains a Random Forest Regressor to predict delivery time.

Features used:
    distance_km   → actual road distance
    traffic_level → 0=LOW, 1=MEDIUM, 2=HIGH
    time_of_day   → 0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT
    route_type    → 0=CITY, 1=HIGHWAY

Target:
    actual_time_min → real delivery time in minutes

Run: python model/train.py
Output: model/model.pkl
"""

import pandas as pd
import numpy as np
import pickle
import os
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

# ── Load Dataset ──────────────────────────────────────────────

# Resolve paths relative to this file's location
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_PATH = os.path.join(BASE_DIR, "../data/dataset.csv")
MODEL_PATH = os.path.join(BASE_DIR, "model.pkl")

print("📂 Loading dataset...")
df = pd.read_csv(DATA_PATH)
print(f"   Loaded {len(df)} rows, {df.shape[1]} columns")
print(f"   Columns: {list(df.columns)}\n")

# ── Feature / Target Split ────────────────────────────────────

FEATURES = ["distance_km", "traffic_level", "time_of_day", "route_type"]
TARGET   = "actual_time_min"

X = df[FEATURES]
y = df[TARGET]

# ── Train / Test Split ────────────────────────────────────────

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)
print(f"🔀 Train set: {len(X_train)} rows | Test set: {len(X_test)} rows")

# ── Model Training ────────────────────────────────────────────

print("\n🌲 Training Random Forest Regressor...")
model = RandomForestRegressor(
    n_estimators=100,    # 100 decision trees in the ensemble
    max_depth=8,         # prevents overfitting on small dataset
    min_samples_split=3,
    random_state=42,
    n_jobs=-1            # use all CPU cores
)
model.fit(X_train, y_train)

# ── Evaluation ────────────────────────────────────────────────

y_pred = model.predict(X_test)
mae  = mean_absolute_error(y_test, y_pred)
r2   = r2_score(y_test, y_pred)

print(f"\n📊 Model Evaluation:")
print(f"   Mean Absolute Error (MAE): {mae:.2f} minutes")
print(f"   R² Score:                  {r2:.4f}")
print(f"   (R² of 1.0 = perfect, 0.0 = random guessing)")

# ── Feature Importance ────────────────────────────────────────

importances = dict(zip(FEATURES, model.feature_importances_))
print(f"\n🔍 Feature Importance:")
for feat, imp in sorted(importances.items(), key=lambda x: -x[1]):
    bar = "█" * int(imp * 40)
    print(f"   {feat:<20} {bar} {imp:.3f}")

# ── Save Model ────────────────────────────────────────────────

with open(MODEL_PATH, "wb") as f:
    pickle.dump(model, f)

print(f"\n✅ Model saved to: {MODEL_PATH}")
print("   Ready to serve predictions via app.py\n")