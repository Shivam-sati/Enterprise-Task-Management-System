"""
Health check endpoints
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from datetime import datetime
import psutil
import os

from app.config.settings import settings

router = APIRouter()

class HealthResponse(BaseModel):
    """Health check response model"""
    status: str
    timestamp: datetime
    service: str
    version: str
    uptime_seconds: float

class DetailedHealthResponse(BaseModel):
    """Detailed health check response model"""
    status: str
    timestamp: datetime
    service: str
    version: str
    uptime_seconds: float
    memory_usage_mb: float
    cpu_percent: float
    models_loaded: bool
    eureka_registered: bool

# Track service start time
service_start_time = datetime.now()

@router.get("/", response_model=HealthResponse)
async def health_check():
    """Basic health check endpoint"""
    uptime = (datetime.now() - service_start_time).total_seconds()
    
    return HealthResponse(
        status="UP",
        timestamp=datetime.now(),
        service=settings.service_name,
        version="1.0.0",
        uptime_seconds=uptime
    )

@router.get("/detailed", response_model=DetailedHealthResponse)
async def detailed_health_check():
    """Detailed health check with system metrics"""
    uptime = (datetime.now() - service_start_time).total_seconds()
    
    # Get system metrics
    process = psutil.Process(os.getpid())
    memory_usage = process.memory_info().rss / 1024 / 1024  # Convert to MB
    cpu_percent = process.cpu_percent()
    
    # Check if models are loaded using ModelManager
    try:
        from ...models.model_manager import ModelManager
        model_manager = ModelManager()
        loaded_models = model_manager.get_loaded_models()
        models_loaded = len(loaded_models) > 0
    except Exception:
        models_loaded = False
    
    # Check Eureka registration status
    from app.main import eureka_client
    eureka_registered = eureka_client.is_registered() if eureka_client else False
    
    return DetailedHealthResponse(
        status="UP",
        timestamp=datetime.now(),
        service=settings.service_name,
        version="1.0.0",
        uptime_seconds=uptime,
        memory_usage_mb=memory_usage,
        cpu_percent=cpu_percent,
        models_loaded=models_loaded,
        eureka_registered=eureka_registered
    )