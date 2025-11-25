"""
Tests for health check endpoints
"""
import pytest
from fastapi.testclient import TestClient
from datetime import datetime

def test_basic_health_check(client: TestClient):
    """Test basic health check endpoint"""
    response = client.get("/health/")
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["status"] == "UP"
    assert data["service"] == "ai-service-python-test"
    assert data["version"] == "1.0.0"
    assert "timestamp" in data
    assert "uptime_seconds" in data
    assert isinstance(data["uptime_seconds"], (int, float))

def test_detailed_health_check(client: TestClient):
    """Test detailed health check endpoint"""
    response = client.get("/health/detailed")
    
    assert response.status_code == 200
    data = response.json()
    
    # Basic health check fields
    assert data["status"] == "UP"
    assert data["service"] == "ai-service-python-test"
    assert data["version"] == "1.0.0"
    assert "timestamp" in data
    assert "uptime_seconds" in data
    
    # Detailed fields
    assert "memory_usage_mb" in data
    assert "cpu_percent" in data
    assert "models_loaded" in data
    assert "eureka_registered" in data
    
    # Validate data types
    assert isinstance(data["memory_usage_mb"], (int, float))
    assert isinstance(data["cpu_percent"], (int, float))
    assert isinstance(data["models_loaded"], bool)
    assert isinstance(data["eureka_registered"], bool)
    
    # For test environment, these should be False
    assert data["models_loaded"] is False
    assert data["eureka_registered"] is False

def test_health_check_response_format(client: TestClient):
    """Test that health check response follows expected format"""
    response = client.get("/health/")
    data = response.json()
    
    # Validate timestamp format
    timestamp_str = data["timestamp"]
    timestamp = datetime.fromisoformat(timestamp_str.replace("Z", "+00:00"))
    assert isinstance(timestamp, datetime)
    
    # Validate uptime is positive
    assert data["uptime_seconds"] >= 0

def test_health_endpoints_are_accessible(client: TestClient):
    """Test that health endpoints are accessible without authentication"""
    # Basic health check
    response = client.get("/health/")
    assert response.status_code == 200
    
    # Detailed health check
    response = client.get("/health/detailed")
    assert response.status_code == 200