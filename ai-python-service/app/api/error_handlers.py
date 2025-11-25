"""
Comprehensive error handling for AI service endpoints
"""
import logging
from typing import Dict, Any, Optional
from fastapi import HTTPException, Request, status
from fastapi.responses import JSONResponse
from pydantic import ValidationError
import asyncio
import traceback
from datetime import datetime

logger = logging.getLogger(__name__)


class AIServiceError(Exception):
    """Base exception for AI service errors"""
    def __init__(self, message: str, error_code: str = "AI_ERROR", details: Optional[Dict[str, Any]] = None):
        self.message = message
        self.error_code = error_code
        self.details = details or {}
        super().__init__(self.message)


class ModelLoadError(AIServiceError):
    """Error loading AI models"""
    def __init__(self, message: str, model_name: str = "unknown"):
        super().__init__(message, "MODEL_LOAD_ERROR", {"model_name": model_name})


class ProcessingTimeoutError(AIServiceError):
    """Error when AI processing times out"""
    def __init__(self, message: str, timeout_seconds: float):
        super().__init__(message, "PROCESSING_TIMEOUT", {"timeout_seconds": timeout_seconds})


class InvalidInputError(AIServiceError):
    """Error for invalid input data"""
    def __init__(self, message: str, field: str = "unknown"):
        super().__init__(message, "INVALID_INPUT", {"field": field})


class InsufficientDataError(AIServiceError):
    """Error when insufficient data is provided for analysis"""
    def __init__(self, message: str, required_items: int = 0, provided_items: int = 0):
        super().__init__(
            message, 
            "INSUFFICIENT_DATA", 
            {"required_items": required_items, "provided_items": provided_items}
        )


async def timeout_handler(coro, timeout_seconds: float = 30.0):
    """
    Handle async operations with timeout
    
    Args:
        coro: Coroutine to execute
        timeout_seconds: Timeout in seconds
        
    Returns:
        Result of the coroutine
        
    Raises:
        ProcessingTimeoutError: If operation times out
    """
    try:
        return await asyncio.wait_for(coro, timeout=timeout_seconds)
    except asyncio.TimeoutError:
        logger.error(f"Operation timed out after {timeout_seconds} seconds")
        raise ProcessingTimeoutError(
            f"AI processing timed out after {timeout_seconds} seconds. Please try with simpler input or contact support.",
            timeout_seconds
        )


def create_error_response(
    error: Exception,
    status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR,
    include_traceback: bool = False
) -> Dict[str, Any]:
    """
    Create standardized error response
    
    Args:
        error: The exception that occurred
        status_code: HTTP status code
        include_traceback: Whether to include traceback in response
        
    Returns:
        Standardized error response dictionary
    """
    error_response = {
        "error": True,
        "timestamp": datetime.utcnow().isoformat(),
        "status_code": status_code,
        "message": str(error),
        "type": type(error).__name__
    }
    
    # Add specific error details for AIServiceError
    if isinstance(error, AIServiceError):
        error_response.update({
            "error_code": error.error_code,
            "details": error.details
        })
    
    # Add validation details for Pydantic errors
    elif isinstance(error, ValidationError):
        error_response.update({
            "error_code": "VALIDATION_ERROR",
            "details": {
                "validation_errors": [
                    {
                        "field": ".".join(str(loc) for loc in err["loc"]),
                        "message": err["msg"],
                        "type": err["type"]
                    }
                    for err in error.errors()
                ]
            }
        })
    
    # Add traceback for debugging (only in development)
    if include_traceback:
        error_response["traceback"] = traceback.format_exc()
    
    return error_response


async def ai_service_exception_handler(request: Request, exc: AIServiceError) -> JSONResponse:
    """Handle AIServiceError exceptions"""
    logger.error(f"AI Service Error: {exc.message} - Code: {exc.error_code}")
    
    status_code_map = {
        "MODEL_LOAD_ERROR": status.HTTP_503_SERVICE_UNAVAILABLE,
        "PROCESSING_TIMEOUT": status.HTTP_408_REQUEST_TIMEOUT,
        "INVALID_INPUT": status.HTTP_400_BAD_REQUEST,
        "INSUFFICIENT_DATA": status.HTTP_400_BAD_REQUEST,
        "AI_ERROR": status.HTTP_500_INTERNAL_SERVER_ERROR
    }
    
    status_code = status_code_map.get(exc.error_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
    
    return JSONResponse(
        status_code=status_code,
        content=create_error_response(exc, status_code)
    )


async def validation_exception_handler(request: Request, exc: ValidationError) -> JSONResponse:
    """Handle Pydantic validation errors"""
    logger.warning(f"Validation error: {exc}")
    
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=create_error_response(exc, status.HTTP_422_UNPROCESSABLE_ENTITY)
    )


async def general_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Handle general exceptions"""
    logger.error(f"Unexpected error: {exc}", exc_info=True)
    
    # Don't expose internal errors in production
    safe_message = "An internal error occurred. Please try again later."
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": True,
            "timestamp": datetime.utcnow().isoformat(),
            "status_code": status.HTTP_500_INTERNAL_SERVER_ERROR,
            "message": safe_message,
            "type": "InternalServerError",
            "error_code": "INTERNAL_ERROR"
        }
    )


def validate_task_data(task_data: Dict[str, Any]) -> None:
    """
    Validate task data for insights generation
    
    Args:
        task_data: Task data dictionary
        
    Raises:
        InvalidInputError: If data is invalid
        InsufficientDataError: If insufficient data provided
    """
    if not isinstance(task_data, dict):
        raise InvalidInputError("Task data must be a dictionary", "task_data")
    
    tasks = task_data.get('tasks', [])
    
    if not isinstance(tasks, list):
        raise InvalidInputError("Tasks must be a list", "tasks")
    
    if len(tasks) == 0:
        raise InsufficientDataError(
            "At least one task is required for analysis",
            required_items=1,
            provided_items=0
        )
    
    # Validate individual tasks
    for i, task in enumerate(tasks):
        if not isinstance(task, dict):
            raise InvalidInputError(f"Task at index {i} must be a dictionary", f"tasks[{i}]")
        
        # Check for required fields
        if not task.get('title') and not task.get('description'):
            raise InvalidInputError(
                f"Task at index {i} must have either title or description",
                f"tasks[{i}]"
            )


def validate_prioritization_tasks(tasks: list) -> None:
    """
    Validate tasks for prioritization
    
    Args:
        tasks: List of task dictionaries
        
    Raises:
        InvalidInputError: If data is invalid
        InsufficientDataError: If insufficient data provided
    """
    if not tasks:
        raise InsufficientDataError(
            "At least one task is required for prioritization",
            required_items=1,
            provided_items=0
        )
    
    if len(tasks) > 50:
        raise InvalidInputError(
            f"Too many tasks for prioritization. Maximum 50 allowed, got {len(tasks)}",
            "tasks"
        )
    
    for i, task in enumerate(tasks):
        if not task.id or not task.id.strip():
            raise InvalidInputError(f"Task at index {i} must have a valid ID", f"tasks[{i}].id")
        
        if not task.title or not task.title.strip():
            raise InvalidInputError(f"Task at index {i} must have a valid title", f"tasks[{i}].title")


def validate_parse_request(text: str) -> None:
    """
    Validate task parsing request
    
    Args:
        text: Task description text
        
    Raises:
        InvalidInputError: If text is invalid
    """
    if not text or not text.strip():
        raise InvalidInputError("Task text cannot be empty", "text")
    
    if len(text) > 2000:
        raise InvalidInputError(
            f"Task text too long. Maximum 2000 characters allowed, got {len(text)}",
            "text"
        )
    
    # Check for potentially problematic content
    if text.count('\n') > 50:
        raise InvalidInputError(
            "Task text has too many line breaks. Please provide a more concise description.",
            "text"
        )


class RateLimitError(AIServiceError):
    """Error when rate limit is exceeded"""
    def __init__(self, message: str, retry_after: int = 60):
        super().__init__(message, "RATE_LIMIT_EXCEEDED", {"retry_after": retry_after})


class ServiceUnavailableError(AIServiceError):
    """Error when service is temporarily unavailable"""
    def __init__(self, message: str, service_name: str = "AI Service"):
        super().__init__(message, "SERVICE_UNAVAILABLE", {"service_name": service_name})


# Error response templates for common scenarios
ERROR_TEMPLATES = {
    "model_not_loaded": {
        "message": "AI models are not currently loaded. Please try again in a few moments.",
        "error_code": "MODEL_NOT_LOADED",
        "details": {"suggestion": "The service may be starting up or updating models"}
    },
    "processing_overload": {
        "message": "AI service is currently overloaded. Please try again later.",
        "error_code": "SERVICE_OVERLOADED",
        "details": {"suggestion": "Reduce request complexity or try again in a few minutes"}
    },
    "invalid_task_format": {
        "message": "Task data format is invalid. Please check the request structure.",
        "error_code": "INVALID_FORMAT",
        "details": {"suggestion": "Ensure all required fields are provided and properly formatted"}
    }
}


def get_error_template(template_name: str) -> Dict[str, Any]:
    """Get a predefined error template"""
    return ERROR_TEMPLATES.get(template_name, {
        "message": "An unknown error occurred",
        "error_code": "UNKNOWN_ERROR",
        "details": {}
    })