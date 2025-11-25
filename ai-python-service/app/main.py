"""
FastAPI main application for AI Python Service
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import logging
import signal
import sys
from contextlib import asynccontextmanager

from app.config.settings import settings
from app.api.routes import health, ai, models, metrics
from app.services.eureka_client import EurekaClient
from app.services.metrics_service import get_metrics_middleware
from app.services.logging_service import setup_structured_logging, get_correlation_middleware
from app.api.error_handlers import (
    AIServiceError, ai_service_exception_handler,
    validation_exception_handler, general_exception_handler
)
from pydantic import ValidationError

# Configure structured logging
setup_structured_logging(log_level="DEBUG" if settings.debug else "INFO")
logger = logging.getLogger(__name__)

# Global eureka client instance
eureka_client = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Handle application startup and shutdown"""
    global eureka_client
    
    # Startup
    logger.info("Starting AI Python Service...")
    
    # Initialize Eureka client
    eureka_client = EurekaClient(settings)
    await eureka_client.register()
    
    yield
    
    # Shutdown
    logger.info("Shutting down AI Python Service...")
    if eureka_client:
        await eureka_client.deregister()

def create_app() -> FastAPI:
    """Create and configure FastAPI application"""
    
    app = FastAPI(
        title="AI Python Service",
        description="Python-based AI microservice using open source models",
        version="1.0.0",
        lifespan=lifespan
    )
    
    # Add CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    # Add metrics middleware
    app.middleware("http")(get_metrics_middleware())
    
    # Add correlation ID middleware for distributed tracing
    app.middleware("http")(get_correlation_middleware())
    
    # Add exception handlers
    app.add_exception_handler(AIServiceError, ai_service_exception_handler)
    app.add_exception_handler(ValidationError, validation_exception_handler)
    app.add_exception_handler(Exception, general_exception_handler)
    
    # Include routers
    app.include_router(health.router, prefix="/health", tags=["health"])
    app.include_router(ai.router, prefix="/ai", tags=["ai"])
    app.include_router(models.router, prefix="/models", tags=["models"])
    app.include_router(metrics.router, tags=["metrics"])
    
    return app

app = create_app()

def signal_handler(signum, frame):
    """Handle shutdown signals gracefully"""
    logger.info(f"Received signal {signum}, shutting down gracefully...")
    sys.exit(0)

if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.port,
        reload=settings.debug
    )