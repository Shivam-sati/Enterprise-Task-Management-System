"""
Metrics endpoints for Prometheus monitoring.
"""

from fastapi import APIRouter, Response
from app.services.metrics_service import metrics_service

router = APIRouter()


@router.get("/metrics", 
           summary="Prometheus metrics endpoint",
           description="Returns metrics in Prometheus format for monitoring and alerting")
async def get_metrics():
    """
    Get Prometheus metrics.
    
    Returns:
        Response: Metrics in Prometheus text format
    """
    metrics_data = metrics_service.get_metrics()
    return Response(
        content=metrics_data,
        media_type="text/plain; version=0.0.4; charset=utf-8"
    )


@router.get("/metrics/health",
           summary="Metrics service health check",
           description="Check if metrics collection is working properly")
async def metrics_health():
    """
    Check metrics service health.
    
    Returns:
        dict: Health status of metrics service
    """
    try:
        # Test metrics collection
        metrics_data = metrics_service.get_metrics()
        
        return {
            "status": "healthy",
            "metrics_available": len(metrics_data) > 0,
            "active_models": metrics_service.active_models._value._value if hasattr(metrics_service.active_models, '_value') else 0
        }
    except Exception as e:
        return {
            "status": "unhealthy",
            "error": str(e)
        }