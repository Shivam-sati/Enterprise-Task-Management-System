"""
Prometheus metrics service for AI Python service.
Provides custom metrics for inference latency, throughput, and model performance.
"""

import time
from typing import Dict, Any
from prometheus_client import Counter, Histogram, Gauge, Info, CollectorRegistry, generate_latest
from prometheus_client.core import REGISTRY
import psutil
import threading
import logging

logger = logging.getLogger(__name__)


class MetricsService:
    """Service for collecting and exposing Prometheus metrics."""
    
    def __init__(self):
        """Initialize metrics collectors."""
        # Request metrics
        self.request_count = Counter(
            'ai_service_requests_total',
            'Total number of requests to AI service',
            ['endpoint', 'method', 'status']
        )
        
        self.request_duration = Histogram(
            'ai_service_request_duration_seconds',
            'Request duration in seconds',
            ['endpoint', 'method'],
            buckets=[0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0]
        )
        
        # AI model metrics
        self.model_inference_duration = Histogram(
            'ai_model_inference_duration_seconds',
            'Model inference duration in seconds',
            ['model_type', 'model_version'],
            buckets=[0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0]
        )
        
        self.model_inference_count = Counter(
            'ai_model_inference_total',
            'Total number of model inferences',
            ['model_type', 'model_version', 'status']
        )
        
        self.model_cache_hits = Counter(
            'ai_model_cache_hits_total',
            'Total number of cache hits',
            ['model_type', 'cache_type']
        )
        
        self.model_cache_misses = Counter(
            'ai_model_cache_misses_total',
            'Total number of cache misses',
            ['model_type', 'cache_type']
        )
        
        # System metrics
        self.memory_usage = Gauge(
            'ai_service_memory_usage_bytes',
            'Memory usage in bytes'
        )
        
        self.cpu_usage = Gauge(
            'ai_service_cpu_usage_percent',
            'CPU usage percentage'
        )
        
        self.active_models = Gauge(
            'ai_service_active_models',
            'Number of currently loaded models'
        )
        
        self.model_memory_usage = Gauge(
            'ai_model_memory_usage_bytes',
            'Memory usage by loaded models',
            ['model_type', 'model_version']
        )
        
        # Service info
        self.service_info = Info(
            'ai_service_info',
            'AI service information'
        )
        
        # Error metrics
        self.error_count = Counter(
            'ai_service_errors_total',
            'Total number of errors',
            ['error_type', 'endpoint']
        )
        
        # Queue metrics
        self.queue_size = Gauge(
            'ai_service_queue_size',
            'Current queue size for pending requests'
        )
        
        self.queue_wait_time = Histogram(
            'ai_service_queue_wait_seconds',
            'Time spent waiting in queue',
            buckets=[0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0]
        )
        
        # Start system metrics collection
        self._start_system_metrics_collection()
        
        # Set service info
        self._set_service_info()
    
    def _start_system_metrics_collection(self):
        """Start background thread for system metrics collection."""
        def collect_system_metrics():
            while True:
                try:
                    # Memory usage
                    memory_info = psutil.virtual_memory()
                    process = psutil.Process()
                    process_memory = process.memory_info()
                    
                    self.memory_usage.set(process_memory.rss)
                    
                    # CPU usage
                    cpu_percent = process.cpu_percent()
                    self.cpu_usage.set(cpu_percent)
                    
                    time.sleep(10)  # Collect every 10 seconds
                except Exception as e:
                    logger.error(f"Error collecting system metrics: {e}")
                    time.sleep(10)
        
        thread = threading.Thread(target=collect_system_metrics, daemon=True)
        thread.start()
    
    def _set_service_info(self):
        """Set service information metrics."""
        import sys
        import platform
        
        self.service_info.info({
            'version': '1.0.0',
            'python_version': sys.version,
            'platform': platform.platform(),
            'service_name': 'ai-python-service'
        })
    
    def record_request(self, endpoint: str, method: str, status: str, duration: float):
        """Record request metrics."""
        self.request_count.labels(
            endpoint=endpoint,
            method=method,
            status=status
        ).inc()
        
        self.request_duration.labels(
            endpoint=endpoint,
            method=method
        ).observe(duration)
    
    def record_model_inference(self, model_type: str, model_version: str, 
                             duration: float, status: str = 'success'):
        """Record model inference metrics."""
        self.model_inference_duration.labels(
            model_type=model_type,
            model_version=model_version
        ).observe(duration)
        
        self.model_inference_count.labels(
            model_type=model_type,
            model_version=model_version,
            status=status
        ).inc()
    
    def record_cache_hit(self, model_type: str, cache_type: str = 'memory'):
        """Record cache hit."""
        self.model_cache_hits.labels(
            model_type=model_type,
            cache_type=cache_type
        ).inc()
    
    def record_cache_miss(self, model_type: str, cache_type: str = 'memory'):
        """Record cache miss."""
        self.model_cache_misses.labels(
            model_type=model_type,
            cache_type=cache_type
        ).inc()
    
    def record_error(self, error_type: str, endpoint: str):
        """Record error occurrence."""
        self.error_count.labels(
            error_type=error_type,
            endpoint=endpoint
        ).inc()
    
    def set_active_models(self, count: int):
        """Set number of active models."""
        self.active_models.set(count)
    
    def set_model_memory_usage(self, model_type: str, model_version: str, memory_bytes: int):
        """Set memory usage for a specific model."""
        self.model_memory_usage.labels(
            model_type=model_type,
            model_version=model_version
        ).set(memory_bytes)
    
    def set_queue_size(self, size: int):
        """Set current queue size."""
        self.queue_size.set(size)
    
    def record_queue_wait_time(self, wait_time: float):
        """Record queue wait time."""
        self.queue_wait_time.observe(wait_time)
    
    def get_metrics(self) -> str:
        """Get metrics in Prometheus format."""
        return generate_latest(REGISTRY).decode('utf-8')


# Global metrics instance
metrics_service = MetricsService()


class MetricsMiddleware:
    """Middleware for automatic request metrics collection."""
    
    def __init__(self, metrics: MetricsService):
        self.metrics = metrics
    
    async def __call__(self, request, call_next):
        """Process request and collect metrics."""
        start_time = time.time()
        
        try:
            response = await call_next(request)
            duration = time.time() - start_time
            
            # Record successful request
            self.metrics.record_request(
                endpoint=request.url.path,
                method=request.method,
                status=str(response.status_code),
                duration=duration
            )
            
            return response
            
        except Exception as e:
            duration = time.time() - start_time
            
            # Record failed request
            self.metrics.record_request(
                endpoint=request.url.path,
                method=request.method,
                status='error',
                duration=duration
            )
            
            # Record error
            self.metrics.record_error(
                error_type=type(e).__name__,
                endpoint=request.url.path
            )
            
            raise


def get_metrics_middleware() -> MetricsMiddleware:
    """Get metrics middleware instance."""
    return MetricsMiddleware(metrics_service)