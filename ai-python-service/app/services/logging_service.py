"""
Structured logging service with correlation ID support for distributed tracing.
"""

import json
import logging
import uuid
from typing import Dict, Any, Optional
from contextvars import ContextVar
from datetime import datetime
import sys

# Context variable for correlation ID
correlation_id_var: ContextVar[Optional[str]] = ContextVar('correlation_id', default=None)


class StructuredFormatter(logging.Formatter):
    """Custom formatter for structured JSON logging."""
    
    def format(self, record: logging.LogRecord) -> str:
        """Format log record as structured JSON."""
        
        # Base log structure
        log_entry = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "service": "ai-python-service",
            "version": "1.0.0"
        }
        
        # Add correlation ID if available
        correlation_id = correlation_id_var.get()
        if correlation_id:
            log_entry["correlation_id"] = correlation_id
        
        # Add exception information if present
        if record.exc_info:
            log_entry["exception"] = {
                "type": record.exc_info[0].__name__ if record.exc_info[0] else None,
                "message": str(record.exc_info[1]) if record.exc_info[1] else None,
                "traceback": self.formatException(record.exc_info)
            }
        
        # Add extra fields from the log record
        extra_fields = {}
        for key, value in record.__dict__.items():
            if key not in ['name', 'msg', 'args', 'levelname', 'levelno', 'pathname', 
                          'filename', 'module', 'lineno', 'funcName', 'created', 
                          'msecs', 'relativeCreated', 'thread', 'threadName', 
                          'processName', 'process', 'getMessage', 'exc_info', 
                          'exc_text', 'stack_info']:
                extra_fields[key] = value
        
        if extra_fields:
            log_entry["extra"] = extra_fields
        
        # Add source information
        log_entry["source"] = {
            "file": record.filename,
            "line": record.lineno,
            "function": record.funcName
        }
        
        return json.dumps(log_entry, default=str)


class CorrelationIdFilter(logging.Filter):
    """Filter to add correlation ID to log records."""
    
    def filter(self, record: logging.LogRecord) -> bool:
        """Add correlation ID to the log record."""
        correlation_id = correlation_id_var.get()
        if correlation_id:
            record.correlation_id = correlation_id
        return True


def setup_structured_logging(log_level: str = "INFO") -> None:
    """Setup structured logging configuration."""
    
    # Create structured formatter
    formatter = StructuredFormatter()
    
    # Create correlation ID filter
    correlation_filter = CorrelationIdFilter()
    
    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, log_level.upper()))
    
    # Remove existing handlers
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)
    
    # Create console handler with structured formatting
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    console_handler.addFilter(correlation_filter)
    
    root_logger.addHandler(console_handler)
    
    # Configure specific loggers
    loggers_config = {
        "uvicorn": logging.WARNING,
        "uvicorn.error": logging.INFO,
        "uvicorn.access": logging.INFO,
        "fastapi": logging.INFO,
        "app": logging.DEBUG if log_level.upper() == "DEBUG" else logging.INFO
    }
    
    for logger_name, level in loggers_config.items():
        logger = logging.getLogger(logger_name)
        logger.setLevel(level)


def generate_correlation_id() -> str:
    """Generate a new correlation ID."""
    return str(uuid.uuid4())


def set_correlation_id(correlation_id: str) -> None:
    """Set correlation ID for the current context."""
    correlation_id_var.set(correlation_id)


def get_correlation_id() -> Optional[str]:
    """Get correlation ID from the current context."""
    return correlation_id_var.get()


def clear_correlation_id() -> None:
    """Clear correlation ID from the current context."""
    correlation_id_var.set(None)


class LoggingService:
    """Service for structured logging with correlation ID support."""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
    
    def log_request_start(self, method: str, path: str, correlation_id: str, 
                         user_id: Optional[str] = None, **kwargs) -> None:
        """Log request start."""
        self.logger.info(
            "Request started",
            extra={
                "event_type": "request_start",
                "http_method": method,
                "http_path": path,
                "correlation_id": correlation_id,
                "user_id": user_id,
                **kwargs
            }
        )
    
    def log_request_end(self, method: str, path: str, status_code: int, 
                       duration_ms: float, correlation_id: str, **kwargs) -> None:
        """Log request end."""
        self.logger.info(
            "Request completed",
            extra={
                "event_type": "request_end",
                "http_method": method,
                "http_path": path,
                "http_status": status_code,
                "duration_ms": duration_ms,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_model_inference_start(self, model_type: str, model_version: str, 
                                 correlation_id: str, **kwargs) -> None:
        """Log model inference start."""
        self.logger.info(
            "Model inference started",
            extra={
                "event_type": "model_inference_start",
                "model_type": model_type,
                "model_version": model_version,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_model_inference_end(self, model_type: str, model_version: str, 
                               duration_ms: float, success: bool, 
                               correlation_id: str, **kwargs) -> None:
        """Log model inference end."""
        self.logger.info(
            "Model inference completed",
            extra={
                "event_type": "model_inference_end",
                "model_type": model_type,
                "model_version": model_version,
                "duration_ms": duration_ms,
                "success": success,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_cache_operation(self, operation: str, cache_type: str, hit: bool, 
                           correlation_id: str, **kwargs) -> None:
        """Log cache operation."""
        self.logger.debug(
            f"Cache {operation}",
            extra={
                "event_type": "cache_operation",
                "cache_operation": operation,
                "cache_type": cache_type,
                "cache_hit": hit,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_error(self, error_type: str, error_message: str, correlation_id: str, 
                  **kwargs) -> None:
        """Log error with context."""
        self.logger.error(
            f"Error occurred: {error_message}",
            extra={
                "event_type": "error",
                "error_type": error_type,
                "error_message": error_message,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_service_call(self, service_name: str, endpoint: str, method: str,
                        correlation_id: str, **kwargs) -> None:
        """Log external service call."""
        self.logger.info(
            f"Calling external service: {service_name}",
            extra={
                "event_type": "service_call",
                "service_name": service_name,
                "service_endpoint": endpoint,
                "service_method": method,
                "correlation_id": correlation_id,
                **kwargs
            }
        )
    
    def log_performance_metric(self, metric_name: str, metric_value: float,
                              metric_unit: str, correlation_id: str, **kwargs) -> None:
        """Log performance metric."""
        self.logger.info(
            f"Performance metric: {metric_name}",
            extra={
                "event_type": "performance_metric",
                "metric_name": metric_name,
                "metric_value": metric_value,
                "metric_unit": metric_unit,
                "correlation_id": correlation_id,
                **kwargs
            }
        )


# Global logging service instance
logging_service = LoggingService()


class CorrelationIdMiddleware:
    """Middleware to handle correlation ID for requests."""
    
    def __init__(self, logging_service: LoggingService):
        self.logging_service = logging_service
    
    async def __call__(self, request, call_next):
        """Process request with correlation ID."""
        import time
        
        # Get or generate correlation ID
        correlation_id = request.headers.get("X-Correlation-ID")
        if not correlation_id:
            correlation_id = generate_correlation_id()
        
        # Set correlation ID in context
        set_correlation_id(correlation_id)
        
        # Log request start
        start_time = time.time()
        self.logging_service.log_request_start(
            method=request.method,
            path=request.url.path,
            correlation_id=correlation_id,
            query_params=str(request.query_params) if request.query_params else None
        )
        
        try:
            # Process request
            response = await call_next(request)
            
            # Add correlation ID to response headers
            response.headers["X-Correlation-ID"] = correlation_id
            
            # Log request end
            duration_ms = (time.time() - start_time) * 1000
            self.logging_service.log_request_end(
                method=request.method,
                path=request.url.path,
                status_code=response.status_code,
                duration_ms=duration_ms,
                correlation_id=correlation_id
            )
            
            return response
            
        except Exception as e:
            # Log error
            duration_ms = (time.time() - start_time) * 1000
            self.logging_service.log_error(
                error_type=type(e).__name__,
                error_message=str(e),
                correlation_id=correlation_id
            )
            
            self.logging_service.log_request_end(
                method=request.method,
                path=request.url.path,
                status_code=500,
                duration_ms=duration_ms,
                correlation_id=correlation_id
            )
            
            raise
        finally:
            # Clear correlation ID from context
            clear_correlation_id()


def get_correlation_middleware() -> CorrelationIdMiddleware:
    """Get correlation ID middleware instance."""
    return CorrelationIdMiddleware(logging_service)