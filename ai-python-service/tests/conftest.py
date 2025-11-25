"""
Test configuration and fixtures
"""
import pytest
import asyncio
from fastapi.testclient import TestClient
from unittest.mock import Mock, AsyncMock

from app.main import create_app
from app.config.settings import Settings

@pytest.fixture
def test_settings():
    """Test settings with Eureka disabled"""
    return Settings(
        service_name="ai-service-python-test",
        port=8087,
        debug=True,
        eureka_enabled=False,  # Disable Eureka for tests
        redis_enabled=False,   # Disable Redis for tests
        metrics_enabled=False  # Disable metrics for tests
    )

@pytest.fixture
def app(test_settings):
    """Create test FastAPI application"""
    # Override settings for testing
    import app.config.settings
    app.config.settings.settings = test_settings
    
    return create_app()

@pytest.fixture
def client(app):
    """Create test client"""
    return TestClient(app)

@pytest.fixture
def mock_eureka_client():
    """Mock Eureka client for testing"""
    mock_client = Mock()
    mock_client.register = AsyncMock()
    mock_client.deregister = AsyncMock()
    mock_client.is_registered = Mock(return_value=True)
    return mock_client

@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()