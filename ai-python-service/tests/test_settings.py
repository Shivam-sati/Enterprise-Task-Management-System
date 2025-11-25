"""
Tests for application settings and configuration
"""
import pytest
import os
from unittest.mock import patch

from app.config.settings import Settings

def test_default_settings():
    """Test default settings values"""
    settings = Settings()
    
    assert settings.service_name == "ai-service-python"
    assert settings.port == 8087
    assert settings.debug is False
    assert settings.eureka_server_url == "http://eureka-server:8761/eureka"
    assert settings.eureka_enabled is True
    assert settings.models_path == "./models"
    assert settings.cache_size == 100
    assert settings.model_timeout_seconds == 30
    assert settings.max_concurrent_requests == 10
    assert settings.memory_limit_mb == 2048
    assert settings.redis_enabled is False
    assert settings.redis_host == "localhost"
    assert settings.redis_port == 6379
    assert settings.redis_ttl_seconds == 3600
    assert settings.metrics_enabled is True
    assert settings.prometheus_port == 9090

def test_settings_from_environment():
    """Test settings loaded from environment variables"""
    env_vars = {
        "AI_SERVICE_SERVICE_NAME": "test-ai-service",
        "AI_SERVICE_PORT": "9000",
        "AI_SERVICE_DEBUG": "true",
        "AI_SERVICE_EUREKA_ENABLED": "false",
        "AI_SERVICE_CACHE_SIZE": "200",
        "AI_SERVICE_REDIS_ENABLED": "true",
        "AI_SERVICE_REDIS_HOST": "redis-server",
        "AI_SERVICE_REDIS_PORT": "6380"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.service_name == "test-ai-service"
    assert settings.port == 9000
    assert settings.debug is True
    assert settings.eureka_enabled is False
    assert settings.cache_size == 200
    assert settings.redis_enabled is True
    assert settings.redis_host == "redis-server"
    assert settings.redis_port == 6380

def test_settings_type_conversion():
    """Test that environment variables are properly converted to correct types"""
    env_vars = {
        "AI_SERVICE_PORT": "8088",
        "AI_SERVICE_DEBUG": "True",
        "AI_SERVICE_EUREKA_ENABLED": "False",
        "AI_SERVICE_CACHE_SIZE": "150",
        "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "45",
        "AI_SERVICE_REDIS_TTL_SECONDS": "7200"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert isinstance(settings.port, int)
    assert isinstance(settings.debug, bool)
    assert isinstance(settings.eureka_enabled, bool)
    assert isinstance(settings.cache_size, int)
    assert isinstance(settings.model_timeout_seconds, int)
    assert isinstance(settings.redis_ttl_seconds, int)
    
    assert settings.port == 8088
    assert settings.debug is True
    assert settings.eureka_enabled is False

def test_settings_env_prefix():
    """Test that settings use correct environment variable prefix"""
    # Test with wrong prefix - should use defaults
    env_vars = {
        "WRONG_PREFIX_SERVICE_NAME": "wrong-service",
        "AI_SERVICE_SERVICE_NAME": "correct-service"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.service_name == "correct-service"

def test_eureka_configuration():
    """Test Eureka-specific configuration"""
    env_vars = {
        "AI_SERVICE_EUREKA_SERVER_URL": "http://custom-eureka:8761/eureka",
        "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "60",
        "AI_SERVICE_EUREKA_DURATION": "180"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.eureka_server_url == "http://custom-eureka:8761/eureka"
    assert settings.eureka_renewal_interval == 60
    assert settings.eureka_duration == 180

def test_performance_settings():
    """Test performance-related settings"""
    env_vars = {
        "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "20",
        "AI_SERVICE_MEMORY_LIMIT_MB": "4096",
        "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "60"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.max_concurrent_requests == 20
    assert settings.memory_limit_mb == 4096
    assert settings.model_timeout_seconds == 60

def test_redis_configuration():
    """Test Redis configuration settings"""
    env_vars = {
        "AI_SERVICE_REDIS_ENABLED": "true",
        "AI_SERVICE_REDIS_HOST": "redis.example.com",
        "AI_SERVICE_REDIS_PORT": "6380",
        "AI_SERVICE_REDIS_TTL_SECONDS": "1800"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.redis_enabled is True
    assert settings.redis_host == "redis.example.com"
    assert settings.redis_port == 6380
    assert settings.redis_ttl_seconds == 1800

def test_observability_settings():
    """Test observability configuration"""
    env_vars = {
        "AI_SERVICE_METRICS_ENABLED": "false",
        "AI_SERVICE_PROMETHEUS_PORT": "9091"
    }
    
    with patch.dict(os.environ, env_vars):
        settings = Settings()
    
    assert settings.metrics_enabled is False
    assert settings.prometheus_port == 9091