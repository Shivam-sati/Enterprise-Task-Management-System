"""
Tests for Eureka service discovery client
"""
import pytest
from unittest.mock import Mock, AsyncMock, patch
import asyncio

from app.services.eureka_client import EurekaClient
from app.config.settings import Settings

@pytest.fixture
def eureka_settings():
    """Settings with Eureka enabled for testing"""
    return Settings(
        service_name="test-service",
        port=8087,
        eureka_enabled=True,
        eureka_server_url="http://test-eureka:8761/eureka",
        eureka_renewal_interval=10,
        eureka_duration=30
    )

@pytest.fixture
def eureka_settings_disabled():
    """Settings with Eureka disabled for testing"""
    return Settings(
        service_name="test-service",
        port=8087,
        eureka_enabled=False
    )

@pytest.mark.asyncio
async def test_eureka_client_initialization(eureka_settings):
    """Test Eureka client initialization"""
    client = EurekaClient(eureka_settings)
    
    assert client.settings == eureka_settings
    assert client.registered is False
    assert client.eureka_client is None
    assert client.registration_task is None

@pytest.mark.asyncio
async def test_eureka_registration_disabled(eureka_settings_disabled):
    """Test Eureka registration when disabled"""
    client = EurekaClient(eureka_settings_disabled)
    
    await client.register()
    
    assert client.registered is False
    assert client.eureka_client is None

@pytest.mark.asyncio
@patch('app.services.eureka_client.eureka_client')
async def test_eureka_registration_success(mock_eureka_module, eureka_settings):
    """Test successful Eureka registration"""
    # Mock the eureka client module
    mock_eureka_instance = Mock()
    mock_eureka_module.init.return_value = mock_eureka_instance
    
    client = EurekaClient(eureka_settings)
    
    with patch.object(client, '_get_local_ip', return_value='192.168.1.100'):
        await client.register()
    
    assert client.registered is True
    assert mock_eureka_module.init.called
    
    # Verify init was called with correct parameters
    call_args = mock_eureka_module.init.call_args
    assert call_args[1]['app_name'] == 'test-service'
    assert call_args[1]['instance_port'] == 8087
    assert call_args[1]['instance_ip'] == '192.168.1.100'

@pytest.mark.asyncio
@patch('app.services.eureka_client.eureka_client')
async def test_eureka_registration_failure(mock_eureka_module, eureka_settings):
    """Test Eureka registration failure"""
    # Mock the eureka client to raise an exception
    mock_eureka_module.init.side_effect = Exception("Connection failed")
    
    client = EurekaClient(eureka_settings)
    
    await client.register()
    
    assert client.registered is False

@pytest.mark.asyncio
@patch('app.services.eureka_client.eureka_client')
async def test_eureka_deregistration(mock_eureka_module, eureka_settings):
    """Test Eureka deregistration"""
    mock_eureka_instance = Mock()
    mock_eureka_module.init.return_value = mock_eureka_instance
    
    client = EurekaClient(eureka_settings)
    
    # First register
    with patch.object(client, '_get_local_ip', return_value='192.168.1.100'):
        await client.register()
    
    assert client.registered is True
    
    # Then deregister
    await client.deregister()
    
    assert client.registered is False
    assert mock_eureka_module.stop.called

@pytest.mark.asyncio
async def test_eureka_deregistration_when_not_registered(eureka_settings):
    """Test deregistration when not registered"""
    client = EurekaClient(eureka_settings)
    
    # Should not raise any errors
    await client.deregister()
    
    assert client.registered is False

def test_is_registered(eureka_settings):
    """Test is_registered method"""
    client = EurekaClient(eureka_settings)
    
    assert client.is_registered() is False
    
    client.registered = True
    assert client.is_registered() is True

def test_get_local_ip(eureka_settings):
    """Test local IP detection"""
    client = EurekaClient(eureka_settings)
    
    ip = client._get_local_ip()
    
    # Should return a valid IP address format
    assert isinstance(ip, str)
    assert len(ip.split('.')) == 4  # IPv4 format

@pytest.mark.asyncio
@patch('app.services.eureka_client.eureka_client')
async def test_maintain_registration_task(mock_eureka_module, eureka_settings):
    """Test that registration maintenance task is created"""
    mock_eureka_instance = Mock()
    mock_eureka_module.init.return_value = mock_eureka_instance
    
    client = EurekaClient(eureka_settings)
    
    with patch.object(client, '_get_local_ip', return_value='192.168.1.100'):
        await client.register()
    
    assert client.registration_task is not None
    assert not client.registration_task.done()
    
    # Clean up
    await client.deregister()