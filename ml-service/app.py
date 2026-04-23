"""
app.py — Python FastAPI ML Microservice
Port: 5000

Endpoints:
    GET  /health    → Health check (useful for demo)
    POST /predict   → Accepts route features, returns predicted DELAY RATIO

Start: uvicorn app:app --port 5000 --reload
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import pickle
import numpy as np
import os
import logging

# ── Logging ───────────────────────────────────────────────────

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ── Load Model ────────────────────────────────────────────────

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "model/model.pkl")

if not os.path.exists(MODEL_PATH):
    raise RuntimeError(
        f"❌ model.pkl not found at {MODEL_PATH}\n"
        "   Please run: python model/train.py"
    )

with open(MODEL_PATH, "rb") as f:
    model = pickle.load(f)

logger.info("✅ ML model loaded from %s", MODEL_PATH)

# ── FastAPI App ───────────────────────────────────────────────

app = FastAPI(
    title="Logistics Delay Prediction API",
    description="Random Forest model predicting delay ratio based on traffic features",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Request / Response Schemas ────────────────────────────────

class PredictRequest(BaseModel):
    """
    Feature input for the ML model.
    NOTE: distance_km is accepted but NOT passed to model —
    the model predicts a ratio, so distance is irrelevant here.
    Actual delay = ratio × base_time (applied in MLClientService.java)
    """
    distance_km:   float = Field(0, description="Accepted but not used by model")
    traffic_level: int   = Field(..., ge=0, le=2, description="0=LOW, 1=MEDIUM, 2=HIGH")
    time_of_day:   int   = Field(..., ge=0, le=3, description="0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT")
    route_type:    int   = Field(..., ge=0, le=1, description="0=CITY, 1=HIGHWAY")

class PredictResponse(BaseModel):
    predicted_time: float   # delay RATIO (e.g. 0.2 = 20% of base time)
    model_used:     str
    features_used:  dict

# ── Endpoints ─────────────────────────────────────────────────

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "model": "RandomForestRegressor (delay ratio)",
        "message": "ML service is running"
    }

@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    """
    Predicts DELAY RATIO using trained Random Forest model.

    Model features (3 only — no distance):
        traffic_level : 0=LOW, 1=MEDIUM, 2=HIGH
        time_of_day   : 0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT
        route_type    : 0=CITY, 1=HIGHWAY

    Returns:
        predicted_time : delay ratio (e.g. 0.2)
                         Java converts this → delay_minutes = ratio × base_time
    """
    try:
        # ── IMPORTANT: 3 features only, matching train.py FEATURES list ──
        features = np.array([[
            request.traffic_level,
            request.time_of_day,
            request.route_type
        ]])

        logger.info(
            "Predicting for: traffic=%d, time=%d, route=%d",
            request.traffic_level, request.time_of_day, request.route_type
        )

        # Run inference — returns delay ratio
        delay_ratio = model.predict(features)[0]
        delay_ratio = round(float(delay_ratio), 4)

        # Clamp to sane range
        delay_ratio = max(0.01, min(delay_ratio, 1.0))

        logger.info("Predicted delay ratio: %.4f", delay_ratio)

        return PredictResponse(
            predicted_time=delay_ratio,
            model_used="RandomForestRegressor — delay ratio model",
            features_used={
                "traffic_level": request.traffic_level,
                "time_of_day":   request.time_of_day,
                "route_type":    request.route_type
            }
        )

    except Exception as e:
        logger.error("Prediction failed: %s", str(e))
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")


# ── Run directly (for development) ───────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=5000, reload=True)