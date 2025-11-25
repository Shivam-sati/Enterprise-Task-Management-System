"""
Integration tests for health check endpoints and monitoring functionality.
"""

import pytest
import json
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.main import create_app


@pytest.fixture
def client():
    """Create test client."""
    app = create_app()
    return TestClient(app)


class TestHealthEndpoints:
    """Test cases for health check endpoints."""
    
    def test_health_check_endpoint(self, client):
        """Test basic health check endpoint."""
        response = client.get("/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "timestamp" in data
        assert "service" in data
        assert data["service"] == "ai-python-service"
    
    def test_health_detailed_endpoint(self, client):
        """Test detailed health check endpoint."""
        response = client.get("/health/detailed")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "checks" in data
        assert "system" in data["checks"]
        assert "models" in data["checks"]
        assert "cache" in data["checks"]
    
    @patch('app.services.eureka_client.EurekaClient')
    def test_health_readiness_endpoint(self, mock_eureka, client):
        """Test readiness check endpoint."""
        # Mock Eureka client as healthy
        mock_eureka_instance = MagicMock()
        mock_eureka_instance.is_registered.return_value = True
        mock_eureka.return_value = mock_eureka_instance
        
        response = client.get("/health/ready")
        
        assert response.status_code == 200
        data = response.json()
        assert data["ready"] is True
        assert "checks" in data
    
    def test_health_liveness_endpoint(self, client):
        """Test liveness check endpoint."""
        response = client.get("/health/live")
        
        assert response.status_code == 200
        data = response.json()
        assert data["alive"] is True
        assert "uptime" in data


class TestMetricsEndpoints:
    """Test cases for metrics endpoints."""
    
    def test_metrics_endpoint(self, client):
        """Test Prometheus metrics endpoint."""
        response = client.get("/metrics")
        
        assert response.status_code == 200
        assert response.headers["content-type"] == "text/plain; version=0.0.4; charset=utf-8"
        
        # Verify Prometheus format
        content = response.text
        assert "# HELP" in content
        assert "# TYPE" in content
        assert "ai_service_" in content
    
    def test_metrics_health_endpoint(self, client):
        """Test metrics service health endpoint."""
        response = client.get("/metrics/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "metrics_available" in data
        assert "active_models" in data
    
    def test_metrics_contain_expected_metrics(self, client):
        """Test that metrics endpoint contains expected metrics."""
        # Make some requests to generate metrics
        client.get("/health")
        client.post("/ai/parse", json={"text": "test task"})
        
        response = client.get("/metrics")
        content = response.text
        
        # Verify expected metrics are present
        expected_metrics = [
            "ai_service_requests_total",
            "ai_service_request_duration_seconds",
            "ai_service_memory_usage_bytes",
            "ai_service_cpu_usage_percent",
            "ai_service_active_models",
            "ai_service_info"
        ]
        
        for metric in expected_metrics:
            assert metric in content


class TestCorrelationIdPropagation:
    """Test cases for correlation ID propagation."""
    
    def test_correlation_id_generation(self, client):
        """Test that correlation ID is generated when not provided."""
        response = client.get("/health")
        
        assert response.status_code == 200
        assert "X-Correlation-ID" in response.headers
        
        correlation_id = response.headers["X-Correlation-ID"]
        assert len(correlation_id) > 0
        assert "-" in correlation_id  # UUID format
    
    def test_correlation_id_preservation(self, client):
        """Test that provided correlation ID is preserved."""
        test_correlation_id = "test-correlation-id-123"
        
        response = client.get("/health", headers={"X-Correlation-ID": test_correlation_id})
        
        assert response.status_code == 200
        assert response.headers["X-Correlation-ID"] == test_correlation_id
    
    def test_correlation_id_in_logs(self, client, caplog):
        """Test that correlation ID appears in logs."""
        test_correlation_id = "test-log-correlation-id"
        
        with caplog.at_level("INFO"):
            response = client.get("/health", headers={"X-Correlation-ID": test_correlation_id})
        
        assert response.status_code == 200
        
        # Check that correlation ID appears in log records
        log_records = [record for record in caplog.records if hasattr(record, 'correlation_id')]
        assert len(log_records) > 0
        
        # Verify correlation ID is in at least one log record
        correlation_ids = [getattr(record, 'correlation_id', None) for record in log_records]
        assert test_correlation_id in correlation_ids


class TestErrorHandlingAndMetrics:
    """Test cases for error handling and error metrics."""
    
    def test_404_error_metrics(self, client):
        """Test that 404 errors are recorded in metrics."""
        # Make request to non-existent endpoint
        response = client.get("/nonexistent")
        
        assert response.status_code == 404
        
        # Check metrics
        metrics_response = client.get("/metrics")
        content = metrics_response.text
        
        # Should have request metrics with 404 status
        assert 'status="404"' in content
    
    def test_validation_error_metrics(self, client):
        """Test that validation errors are recorded in metrics."""
        # Make request with invalid data
        response = client.post("/ai/parse", json={"invalid": "data"})
        
        # Should get validation error
        assert response.status_code == 422
        
        # Check metrics
        metrics_response = client.get("/metrics")
        content = metrics_response.text
        
        # Should have error metrics
        assert "ai_service_errors_total" in content
    
    def test_error_correlation_id_preservation(self, client):
        """Test that correlation ID is preserved in error responses."""
        test_correlation_id = "test-error-correlation-id"
        
        response = client.get("/nonexistent", headers={"X-Correlation-ID": test_correlation_id})
        
        assert response.status_code == 404
        assert response.headers["X-Correlation-ID"] == test_correlation_id


@pytest.mark.integration
class TestMonitoringIntegration:
    """Integration tests for monitoring functionality."""
    
    def test_health_and_metrics_integration(self, client):
        """Test integration between health checks and metrics."""
        # Make health check request
        health_response = client.get("/health/detailed")
        assert health_response.status_code == 200
        
        # Check that health check was recorded in metrics
        metrics_response = client.get("/metrics")
        content = metrics_response.text
        
        # Should have request metrics for health endpoint
        assert 'endpoint="/health/detailed"' in content
        assert 'method="GET"' in content
        assert 'status="200"' in content
    
    def test_multiple_requests_metrics_aggregation(self, client):
        """Test that multiple requests are properly aggregated in metrics."""
        # Make multiple requests
        for i in range(5):
            client.get("/health")
        
        for i in range(3):
            client.get("/health/detailed")
        
        # Check metrics
        metrics_response = client.get("/metrics")
        content = metrics_response.text
        
        # Should show aggregated counts
        # Note: Exact count verification depends on Prometheus client implementation
        assert "ai_service_requests_total" in content
        assert 'endpoint="/health"' in content
        assert 'endpoint="/health/detailed"' in content
    
    @patch('app.services.metrics_service.psutil')
    def test_system_metrics_collection(self, mock_psutil, client):
        """Test system metrics collection."""
        # Mock system information
        mock_memory_info = MagicMock()
        mock_memory_info.rss = 1024 * 1024 * 150  # 150MB
        
        mock_process = MagicMock()
        mock_process.memory_info.return_value = mock_memory_info
        mock_process.cpu_percent.return_value = 35.7
        
        mock_psutil.Process.return_value = mock_process
        
        # Wait a bit for metrics collection
        import time
        time.sleep(0.1)
        
        # Check metrics
        response = client.get("/metrics")
        content = response.text
        
        # Should have system metrics
        assert "ai_service_memory_usage_bytes" in content
        assert "ai_service_cpu_usage_percent" in content
    
    def test_concurrent_requests_correlation_id_isolation(self, client):
        """Test that correlation IDs are properly isolated in concurrent requests."""
        import concurrent.futures
        import threading
        
        def make_request_with_correlation_id(correlation_id):
            response = client.get("/health", headers={"X-Correlation-ID": correlation_id})
            return response.headers.get("X-Correlation-ID")
        
        # Make concurrent requests with different correlation IDs
        correlation_ids = [f"test-concurrent-{i}" for i in range(10)]
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [
                executor.submit(make_request_with_correlation_id, cid)
                for cid in correlation_ids
            ]
            
            results = [future.result() for future in concurrent.futures.as_completed(futures)]
        
        # Verify all correlation IDs were preserved
        assert len(results) == len(correlation_ids)
        for original_id in correlation_ids:
            assert original_id in results
    
    def test_metrics_endpoint_performance(self, client):
        """Test that metrics endpoint responds quickly."""
        import time
        
        # Make some requests to generate metrics
        for i in range(10):
            client.get("/health")
        
        # Time the metrics endpoint
        start_time = time.time()
        response = client.get("/metrics")
        end_time = time.time()
        
        assert response.status_code == 200
        
        # Should respond within reasonable time (adjust threshold as needed)
        response_time = end_time - start_time
        assert response_time < 1.0  # Should respond within 1 second