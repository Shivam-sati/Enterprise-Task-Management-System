"""
Tests for metrics service functionality.
"""

import pytest
import time
from unittest.mock import patch, MagicMock
from prometheus_client import REGISTRY, CollectorRegistry
from app.services.metrics_service import MetricsService, MetricsMiddleware


class TestMetricsService:
    """Test cases for MetricsService."""
    
    @pytest.fixture
    def metrics_service(self):
        """Create a metrics service instance for testing."""
        # Use a separate registry for testing to avoid conflicts
        test_registry = CollectorRegistry()
        with patch('app.services.metrics_service.REGISTRY', test_registry):
            return MetricsService()
    
    def test_record_request(self, metrics_service):
        """Test recording request metrics."""
        # Record a request
        metrics_service.record_request("/test", "GET", "200", 0.5)
        
        # Verify metrics were recorded
        metrics_data = metrics_service.get_metrics()
        assert "ai_service_requests_total" in metrics_data
        assert "ai_service_request_duration_seconds" in metrics_data
    
    def test_record_model_inference(self, metrics_service):
        """Test recording model inference metrics."""
        # Record model inference
        metrics_service.record_model_inference("task_parser", "v1", 0.1, "success")
        
        # Verify metrics were recorded
        metrics_data = metrics_service.get_metrics()
        assert "ai_model_inference_duration_seconds" in metrics_data
        assert "ai_model_inference_total" in metrics_data
    
    def test_record_cache_operations(self, metrics_service):
        """Test recording cache operations."""
        # Record cache hit and miss
        metrics_service.record_cache_hit("task_parser", "memory")
        metrics_service.record_cache_miss("task_parser", "memory")
        
        # Verify metrics were recorded
        metrics_data = metrics_service.get_metrics()
        assert "ai_model_cache_hits_total" in metrics_data
        assert "ai_model_cache_misses_total" in metrics_data
    
    def test_record_error(self, metrics_service):
        """Test recording error metrics."""
        # Record an error
        metrics_service.record_error("ValidationError", "/test")
        
        # Verify metrics were recorded
        metrics_data = metrics_service.get_metrics()
        assert "ai_service_errors_total" in metrics_data
    
    def test_set_active_models(self, metrics_service):
        """Test setting active models gauge."""
        # Set active models count
        metrics_service.set_active_models(3)
        
        # Verify gauge was set
        metrics_data = metrics_service.get_metrics()
        assert "ai_service_active_models" in metrics_data
    
    def test_set_model_memory_usage(self, metrics_service):
        """Test setting model memory usage."""
        # Set model memory usage
        metrics_service.set_model_memory_usage("task_parser", "v1", 1024 * 1024 * 100)  # 100MB
        
        # Verify gauge was set
        metrics_data = metrics_service.get_metrics()
        assert "ai_model_memory_usage_bytes" in metrics_data
    
    def test_queue_metrics(self, metrics_service):
        """Test queue-related metrics."""
        # Set queue size and record wait time
        metrics_service.set_queue_size(5)
        metrics_service.record_queue_wait_time(0.2)
        
        # Verify metrics were recorded
        metrics_data = metrics_service.get_metrics()
        assert "ai_service_queue_size" in metrics_data
        assert "ai_service_queue_wait_seconds" in metrics_data
    
    @patch('psutil.virtual_memory')
    @patch('psutil.Process')
    def test_system_metrics_collection(self, mock_process, mock_virtual_memory, metrics_service):
        """Test system metrics collection."""
        # Mock system metrics
        mock_memory = MagicMock()
        mock_memory.rss = 1024 * 1024 * 100  # 100MB
        mock_process.return_value.memory_info.return_value = mock_memory
        mock_process.return_value.cpu_percent.return_value = 25.5
        
        # Wait a bit for background thread to collect metrics
        time.sleep(0.1)
        
        # Verify system metrics are available
        metrics_data = metrics_service.get_metrics()
        assert "ai_service_memory_usage_bytes" in metrics_data
        assert "ai_service_cpu_usage_percent" in metrics_data


class TestMetricsMiddleware:
    """Test cases for MetricsMiddleware."""
    
    @pytest.fixture
    def metrics_middleware(self):
        """Create a metrics middleware instance for testing."""
        test_registry = CollectorRegistry()
        with patch('app.services.metrics_service.REGISTRY', test_registry):
            metrics_service = MetricsService()
            return MetricsMiddleware(metrics_service)
    
    @pytest.mark.asyncio
    async def test_middleware_success_request(self, metrics_middleware):
        """Test middleware handling successful request."""
        # Mock request and response
        mock_request = MagicMock()
        mock_request.url.path = "/test"
        mock_request.method = "GET"
        
        mock_response = MagicMock()
        mock_response.status_code = 200
        
        async def mock_call_next(request):
            return mock_response
        
        # Process request through middleware
        response = await metrics_middleware(mock_request, mock_call_next)
        
        # Verify response is returned
        assert response == mock_response
        
        # Verify metrics were recorded
        metrics_data = metrics_middleware.metrics.get_metrics()
        assert "ai_service_requests_total" in metrics_data
        assert "ai_service_request_duration_seconds" in metrics_data
    
    @pytest.mark.asyncio
    async def test_middleware_error_request(self, metrics_middleware):
        """Test middleware handling request with error."""
        # Mock request
        mock_request = MagicMock()
        mock_request.url.path = "/test"
        mock_request.method = "POST"
        
        async def mock_call_next(request):
            raise ValueError("Test error")
        
        # Process request through middleware and expect exception
        with pytest.raises(ValueError):
            await metrics_middleware(mock_request, mock_call_next)
        
        # Verify error metrics were recorded
        metrics_data = metrics_middleware.metrics.get_metrics()
        assert "ai_service_requests_total" in metrics_data
        assert "ai_service_errors_total" in metrics_data


@pytest.mark.integration
class TestMetricsIntegration:
    """Integration tests for metrics functionality."""
    
    def test_metrics_endpoint_format(self):
        """Test that metrics endpoint returns proper Prometheus format."""
        test_registry = CollectorRegistry()
        with patch('app.services.metrics_service.REGISTRY', test_registry):
            metrics_service = MetricsService()
            
            # Record some test metrics
            metrics_service.record_request("/test", "GET", "200", 0.1)
            metrics_service.record_model_inference("task_parser", "v1", 0.05, "success")
            
            # Get metrics data
            metrics_data = metrics_service.get_metrics()
            
            # Verify Prometheus format
            assert isinstance(metrics_data, str)
            assert "# HELP" in metrics_data
            assert "# TYPE" in metrics_data
            assert "ai_service_requests_total" in metrics_data
            assert "ai_model_inference_duration_seconds" in metrics_data
    
    def test_metrics_labels_and_values(self):
        """Test that metrics contain proper labels and values."""
        test_registry = CollectorRegistry()
        with patch('app.services.metrics_service.REGISTRY', test_registry):
            metrics_service = MetricsService()
            
            # Record metrics with specific labels
            metrics_service.record_request("/parse", "POST", "200", 0.2)
            metrics_service.record_model_inference("prioritizer", "v2", 0.15, "success")
            metrics_service.record_cache_hit("insights", "redis")
            
            # Get metrics data
            metrics_data = metrics_service.get_metrics()
            
            # Verify labels are present
            assert 'endpoint="/parse"' in metrics_data
            assert 'method="POST"' in metrics_data
            assert 'status="200"' in metrics_data
            assert 'model_type="prioritizer"' in metrics_data
            assert 'model_version="v2"' in metrics_data
            assert 'cache_type="redis"' in metrics_data
    
    @patch('app.services.metrics_service.psutil')
    def test_system_metrics_accuracy(self, mock_psutil):
        """Test system metrics accuracy."""
        # Mock system information
        mock_memory_info = MagicMock()
        mock_memory_info.rss = 1024 * 1024 * 200  # 200MB
        
        mock_process = MagicMock()
        mock_process.memory_info.return_value = mock_memory_info
        mock_process.cpu_percent.return_value = 45.2
        
        mock_psutil.Process.return_value = mock_process
        
        test_registry = CollectorRegistry()
        with patch('app.services.metrics_service.REGISTRY', test_registry):
            metrics_service = MetricsService()
            
            # Wait for system metrics collection
            time.sleep(0.1)
            
            # Get metrics data
            metrics_data = metrics_service.get_metrics()
            
            # Verify system metrics are collected
            assert "ai_service_memory_usage_bytes" in metrics_data
            assert "ai_service_cpu_usage_percent" in metrics_data