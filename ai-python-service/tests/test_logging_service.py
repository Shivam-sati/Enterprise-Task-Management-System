"""
Tests for logging service functionality.
"""

import pytest
import json
import logging
from unittest.mock import patch, MagicMock
from app.services.logging_service import (
    StructuredFormatter, CorrelationIdFilter, LoggingService,
    CorrelationIdMiddleware, setup_structured_logging,
    generate_correlation_id, set_correlation_id, get_correlation_id,
    clear_correlation_id, correlation_id_var
)


class TestStructuredFormatter:
    """Test cases for StructuredFormatter."""
    
    def test_format_basic_log(self):
        """Test formatting basic log record."""
        formatter = StructuredFormatter()
        
        # Create a log record
        record = logging.LogRecord(
            name="test.logger",
            level=logging.INFO,
            pathname="/test/path.py",
            lineno=42,
            msg="Test message",
            args=(),
            exc_info=None
        )
        
        # Format the record
        formatted = formatter.format(record)
        
        # Parse JSON and verify structure
        log_data = json.loads(formatted)
        assert log_data["level"] == "INFO"
        assert log_data["logger"] == "test.logger"
        assert log_data["message"] == "Test message"
        assert log_data["service"] == "ai-python-service"
        assert log_data["version"] == "1.0.0"
        assert "timestamp" in log_data
        assert "source" in log_data
        assert log_data["source"]["line"] == 42
        assert log_data["source"]["function"] == "<module>"
    
    def test_format_log_with_correlation_id(self):
        """Test formatting log with correlation ID."""
        formatter = StructuredFormatter()
        correlation_id = "test-correlation-id"
        
        # Set correlation ID in context
        set_correlation_id(correlation_id)
        
        try:
            # Create a log record
            record = logging.LogRecord(
                name="test.logger",
                level=logging.ERROR,
                pathname="/test/path.py",
                lineno=100,
                msg="Error message",
                args=(),
                exc_info=None
            )
            
            # Format the record
            formatted = formatter.format(record)
            
            # Parse JSON and verify correlation ID
            log_data = json.loads(formatted)
            assert log_data["correlation_id"] == correlation_id
            
        finally:
            clear_correlation_id()
    
    def test_format_log_with_exception(self):
        """Test formatting log with exception information."""
        formatter = StructuredFormatter()
        
        try:
            raise ValueError("Test exception")
        except ValueError:
            # Create a log record with exception info
            record = logging.LogRecord(
                name="test.logger",
                level=logging.ERROR,
                pathname="/test/path.py",
                lineno=50,
                msg="Exception occurred",
                args=(),
                exc_info=True
            )
            
            # Format the record
            formatted = formatter.format(record)
            
            # Parse JSON and verify exception info
            log_data = json.loads(formatted)
            assert "exception" in log_data
            assert log_data["exception"]["type"] == "ValueError"
            assert log_data["exception"]["message"] == "Test exception"
            assert "traceback" in log_data["exception"]
    
    def test_format_log_with_extra_fields(self):
        """Test formatting log with extra fields."""
        formatter = StructuredFormatter()
        
        # Create a log record with extra fields
        record = logging.LogRecord(
            name="test.logger",
            level=logging.INFO,
            pathname="/test/path.py",
            lineno=25,
            msg="Message with extras",
            args=(),
            exc_info=None
        )
        
        # Add extra fields
        record.user_id = "user123"
        record.request_id = "req456"
        record.duration_ms = 150.5
        
        # Format the record
        formatted = formatter.format(record)
        
        # Parse JSON and verify extra fields
        log_data = json.loads(formatted)
        assert "extra" in log_data
        assert log_data["extra"]["user_id"] == "user123"
        assert log_data["extra"]["request_id"] == "req456"
        assert log_data["extra"]["duration_ms"] == 150.5


class TestCorrelationIdFunctions:
    """Test cases for correlation ID utility functions."""
    
    def test_generate_correlation_id(self):
        """Test correlation ID generation."""
        correlation_id = generate_correlation_id()
        
        assert isinstance(correlation_id, str)
        assert len(correlation_id) > 0
        assert "-" in correlation_id  # UUID format
    
    def test_set_and_get_correlation_id(self):
        """Test setting and getting correlation ID."""
        test_id = "test-correlation-id"
        
        # Initially should be None
        assert get_correlation_id() is None
        
        # Set correlation ID
        set_correlation_id(test_id)
        assert get_correlation_id() == test_id
        
        # Clear correlation ID
        clear_correlation_id()
        assert get_correlation_id() is None
    
    def test_correlation_id_context_isolation(self):
        """Test that correlation IDs are isolated per context."""
        import asyncio
        
        async def task_with_correlation_id(correlation_id, results):
            set_correlation_id(correlation_id)
            await asyncio.sleep(0.01)  # Simulate async work
            results.append(get_correlation_id())
            clear_correlation_id()
        
        async def test_isolation():
            results = []
            tasks = [
                task_with_correlation_id("id1", results),
                task_with_correlation_id("id2", results),
                task_with_correlation_id("id3", results)
            ]
            
            await asyncio.gather(*tasks)
            
            # Each task should have maintained its own correlation ID
            assert len(results) == 3
            assert "id1" in results
            assert "id2" in results
            assert "id3" in results
        
        # Run the test
        asyncio.run(test_isolation())


class TestLoggingService:
    """Test cases for LoggingService."""
    
    @pytest.fixture
    def logging_service(self):
        """Create a logging service instance for testing."""
        return LoggingService()
    
    @patch('app.services.logging_service.logging.getLogger')
    def test_log_request_start(self, mock_get_logger, logging_service):
        """Test logging request start."""
        mock_logger = MagicMock()
        mock_get_logger.return_value = mock_logger
        
        correlation_id = "test-correlation-id"
        logging_service.log_request_start("GET", "/test", correlation_id, user_id="user123")
        
        # Verify logger was called with correct parameters
        mock_logger.info.assert_called_once()
        call_args = mock_logger.info.call_args
        assert call_args[0][0] == "Request started"
        assert call_args[1]["extra"]["event_type"] == "request_start"
        assert call_args[1]["extra"]["http_method"] == "GET"
        assert call_args[1]["extra"]["http_path"] == "/test"
        assert call_args[1]["extra"]["correlation_id"] == correlation_id
        assert call_args[1]["extra"]["user_id"] == "user123"
    
    @patch('app.services.logging_service.logging.getLogger')
    def test_log_request_end(self, mock_get_logger, logging_service):
        """Test logging request end."""
        mock_logger = MagicMock()
        mock_get_logger.return_value = mock_logger
        
        correlation_id = "test-correlation-id"
        logging_service.log_request_end("POST", "/api/test", 200, 150.5, correlation_id)
        
        # Verify logger was called with correct parameters
        mock_logger.info.assert_called_once()
        call_args = mock_logger.info.call_args
        assert call_args[0][0] == "Request completed"
        assert call_args[1]["extra"]["event_type"] == "request_end"
        assert call_args[1]["extra"]["http_method"] == "POST"
        assert call_args[1]["extra"]["http_path"] == "/api/test"
        assert call_args[1]["extra"]["http_status"] == 200
        assert call_args[1]["extra"]["duration_ms"] == 150.5
        assert call_args[1]["extra"]["correlation_id"] == correlation_id
    
    @patch('app.services.logging_service.logging.getLogger')
    def test_log_model_inference(self, mock_get_logger, logging_service):
        """Test logging model inference events."""
        mock_logger = MagicMock()
        mock_get_logger.return_value = mock_logger
        
        correlation_id = "test-correlation-id"
        
        # Test inference start
        logging_service.log_model_inference_start("task_parser", "v1", correlation_id)
        
        # Test inference end
        logging_service.log_model_inference_end("task_parser", "v1", 50.2, True, correlation_id)
        
        # Verify both calls were made
        assert mock_logger.info.call_count == 2
        
        # Check start call
        start_call = mock_logger.info.call_args_list[0]
        assert start_call[0][0] == "Model inference started"
        assert start_call[1]["extra"]["event_type"] == "model_inference_start"
        assert start_call[1]["extra"]["model_type"] == "task_parser"
        assert start_call[1]["extra"]["model_version"] == "v1"
        
        # Check end call
        end_call = mock_logger.info.call_args_list[1]
        assert end_call[0][0] == "Model inference completed"
        assert end_call[1]["extra"]["event_type"] == "model_inference_end"
        assert end_call[1]["extra"]["duration_ms"] == 50.2
        assert end_call[1]["extra"]["success"] is True
    
    @patch('app.services.logging_service.logging.getLogger')
    def test_log_error(self, mock_get_logger, logging_service):
        """Test logging errors."""
        mock_logger = MagicMock()
        mock_get_logger.return_value = mock_logger
        
        correlation_id = "test-correlation-id"
        logging_service.log_error("ValidationError", "Invalid input data", correlation_id)
        
        # Verify error was logged
        mock_logger.error.assert_called_once()
        call_args = mock_logger.error.call_args
        assert call_args[0][0] == "Error occurred: Invalid input data"
        assert call_args[1]["extra"]["event_type"] == "error"
        assert call_args[1]["extra"]["error_type"] == "ValidationError"
        assert call_args[1]["extra"]["error_message"] == "Invalid input data"
        assert call_args[1]["extra"]["correlation_id"] == correlation_id


class TestCorrelationIdMiddleware:
    """Test cases for CorrelationIdMiddleware."""
    
    @pytest.fixture
    def middleware(self):
        """Create middleware instance for testing."""
        logging_service = LoggingService()
        return CorrelationIdMiddleware(logging_service)
    
    @pytest.mark.asyncio
    async def test_middleware_with_existing_correlation_id(self, middleware):
        """Test middleware with existing correlation ID in request."""
        # Mock request with correlation ID header
        mock_request = MagicMock()
        mock_request.method = "GET"
        mock_request.url.path = "/test"
        mock_request.query_params = {}
        mock_request.headers = {"X-Correlation-ID": "existing-id"}
        
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.headers = {}
        
        async def mock_call_next(request):
            # Verify correlation ID is set during request processing
            assert get_correlation_id() == "existing-id"
            return mock_response
        
        # Process request
        response = await middleware(mock_request, mock_call_next)
        
        # Verify response has correlation ID header
        assert response.headers["X-Correlation-ID"] == "existing-id"
        
        # Verify correlation ID is cleared after request
        assert get_correlation_id() is None
    
    @pytest.mark.asyncio
    async def test_middleware_generates_correlation_id(self, middleware):
        """Test middleware generates correlation ID when not present."""
        # Mock request without correlation ID header
        mock_request = MagicMock()
        mock_request.method = "POST"
        mock_request.url.path = "/api/test"
        mock_request.query_params = {"param": "value"}
        mock_request.headers = {}
        
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 201
        mock_response.headers = {}
        
        generated_id = None
        
        async def mock_call_next(request):
            nonlocal generated_id
            generated_id = get_correlation_id()
            # Verify correlation ID was generated
            assert generated_id is not None
            assert len(generated_id) > 0
            return mock_response
        
        # Process request
        response = await middleware(mock_request, mock_call_next)
        
        # Verify response has generated correlation ID header
        assert response.headers["X-Correlation-ID"] == generated_id
        
        # Verify correlation ID is cleared after request
        assert get_correlation_id() is None
    
    @pytest.mark.asyncio
    async def test_middleware_handles_exceptions(self, middleware):
        """Test middleware handles exceptions properly."""
        # Mock request
        mock_request = MagicMock()
        mock_request.method = "GET"
        mock_request.url.path = "/error"
        mock_request.query_params = {}
        mock_request.headers = {"X-Correlation-ID": "error-test-id"}
        
        async def mock_call_next(request):
            raise ValueError("Test exception")
        
        # Process request and expect exception
        with pytest.raises(ValueError):
            await middleware(mock_request, mock_call_next)
        
        # Verify correlation ID is cleared even after exception
        assert get_correlation_id() is None


@pytest.mark.integration
class TestLoggingIntegration:
    """Integration tests for logging functionality."""
    
    def test_setup_structured_logging(self):
        """Test structured logging setup."""
        # Setup logging
        setup_structured_logging("DEBUG")
        
        # Get root logger
        root_logger = logging.getLogger()
        
        # Verify logger configuration
        assert root_logger.level == logging.DEBUG
        assert len(root_logger.handlers) > 0
        
        # Verify handler has structured formatter
        handler = root_logger.handlers[0]
        assert isinstance(handler.formatter, StructuredFormatter)
    
    def test_end_to_end_structured_logging(self, caplog):
        """Test end-to-end structured logging."""
        # Setup structured logging
        setup_structured_logging("INFO")
        
        # Set correlation ID
        correlation_id = generate_correlation_id()
        set_correlation_id(correlation_id)
        
        try:
            # Create logger and log message
            logger = logging.getLogger("test.integration")
            logger.info("Integration test message", extra={"test_field": "test_value"})
            
            # Verify log was captured (note: caplog might not capture structured format)
            # This test mainly ensures no exceptions are raised during logging
            
        finally:
            clear_correlation_id()