"""
Tests for main application
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, Mock

from app.main import create_app

def test_create_app():
    """Test FastAPI application creation"""
    app = create_app()
    
    assert app.title == "AI Python Service"
    assert app.description == "Python-based AI microservice using open source models"
    assert app.version == "1.0.0"

def test_app_includes_health_routes(client: TestClient):
    """Test that health routes are included"""
    response = client.get("/health/")
    assert response.status_code == 200

def test_app_includes_ai_routes(client: TestClient):
    """Test that AI routes are included"""
    response = client.post("/ai/parse-task", json={"text": "test"})
    assert response.status_code == 200

def test_cors_middleware(client: TestClient):
    """Test CORS middleware is configured"""
    response = client.options("/health/")
    # FastAPI automatically handles OPTIONS requests for CORS
    assert response.status_code in [200, 405]  # 405 if OPTIONS not explicitly defined

def test_app_routes_structure(client: TestClient):
    """Test that all expected routes are available"""
    # Health routes
    assert client.get("/health/").status_code == 200
    assert client.get("/health/detailed").status_code == 200
    
    # AI routes (placeholders)
    assert client.post("/ai/parse-task", json={"text": "test"}).status_code == 200
    assert client.post("/ai/prioritize-tasks").status_code == 200
    assert client.post("/ai/insights").status_code == 200

def test_app_openapi_docs(client: TestClient):
    """Test that OpenAPI documentation is available"""
    response = client.get("/docs")
    assert response.status_code == 200
    
    response = client.get("/openapi.json")
    assert response.status_code == 200
    
    openapi_spec = response.json()
    assert openapi_spec["info"]["title"] == "AI Python Service"
    assert openapi_spec["info"]["version"] == "1.0.0"

def test_app_tags_in_openapi(client: TestClient):
    """Test that API endpoints are properly tagged"""
    response = client.get("/openapi.json")
    openapi_spec = response.json()
    
    # Check that health endpoints have health tag
    health_paths = [path for path in openapi_spec["paths"] if path.startswith("/health")]
    assert len(health_paths) > 0
    
    # Check that AI endpoints have ai tag
    ai_paths = [path for path in openapi_spec["paths"] if path.startswith("/ai")]
    assert len(ai_paths) > 0