"""
app.py — Python FastAPI ML Microservice
Port: 5000

Endpoints:
    GET  /health    → Health check (useful for demo)
    POST /predict   → Accepts route features, returns predicted time

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
    description="Random Forest model predicting delivery time based on route features",
    version="1.0.0"
)

# Allow Spring Boot backend to call this service
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
    Must match the encoding used in SimulationService.java.
    """
    distance_km:   float = Field(..., gt=0, description="Route distance in kilometers")
    traffic_level: int   = Field(..., ge=0, le=2, description="0=LOW, 1=MEDIUM, 2=HIGH")
    time_of_day:   int   = Field(..., ge=0, le=3, description="0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT")
    route_type:    int   = Field(..., ge=0, le=1, description="0=CITY, 1=HIGHWAY")

class PredictResponse(BaseModel):
    predicted_time: float   # minutes
    model_used:     str
    features_used:  dict

# ── Endpoints ─────────────────────────────────────────────────

@app.get("/health")
def health_check():
    """Quick health check — confirms ML service is running."""
    return {
        "status": "healthy",
        "model": "RandomForestRegressor",
        "message": "ML service is running"
    }

@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    """
    Predicts delivery time in minutes using the trained Random Forest model.

    Input features:
        distance_km   : route distance in kilometers
        traffic_level : 0=LOW, 1=MEDIUM, 2=HIGH
        time_of_day   : 0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT
        route_type    : 0=CITY, 1=HIGHWAY

    Returns:
        predicted_time : estimated delivery time in minutes
    """
    try:
        # Prepare feature array in the same order as training
        features = np.array([[
            request.distance_km,
            request.traffic_level,
            request.time_of_day,
            request.route_type
        ]])

        logger.info(
            "Predicting for: dist=%.1f km, traffic=%d, time=%d, route=%d",
            request.distance_km, request.traffic_level,
            request.time_of_day, request.route_type
        )

        # Run inference
        predicted = model.predict(features)[0]
        predicted = round(float(predicted), 2)

        logger.info("Predicted time: %.2f minutes", predicted)

        return PredictResponse(
            predicted_time=predicted,
            model_used="RandomForestRegressor (100 trees)",
            features_used={
                "distance_km":   request.distance_km,
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