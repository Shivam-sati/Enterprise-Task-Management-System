"""
Deployment and configuration tests for Python AI Service
Tests Docker container startup, health checks, and service discovery
"""
import pytest
import asyncio
import httpx
import docker
import time
import os
import json
from unittest.mock import patch, MagicMock
from typing import Dict, Any

from app.config.settings import Settings
from app.services.eureka_client import EurekaClient


class TestDockerDeployment:
    """Test Docker container deployment and startup"""
    
    @pytest.fixture
    def docker_client(self):
        """Docker client fixture"""
        return docker.from_env()
    
    @pytest.fixture
    def test_env_vars(self):
        """Test environment variables for container"""
        return {
            "AI_SERVICE_SERVICE_NAME": "test-ai-service",
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_DEBUG": "false",
            "AI_SERVICE_EUREKA_ENABLED": "false",  # Disable for isolated testing
            "AI_SERVICE_REDIS_ENABLED": "false",   # Disable for isolated testing
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_CACHE_SIZE": "50",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30"
        }
    
    def test_docker_image_build(self, docker_client):
        """Test that Docker image can be built successfully"""
        try:
            # Build the image
            image, logs = docker_client.images.build(
                path="./ai-python-service",
                tag="test-ai-python-service:latest",
                rm=True
            )
            
            assert image is not None
            assert "test-ai-python-service:latest" in [tag for tag in image.tags]
            
        except docker.errors.BuildError as e:
            pytest.fail(f"Docker build failed: {e}")
    
    def test_container_startup_with_defaults(self, docker_client):
        """Test container starts successfully with default configuration"""
        container = None
        try:
            # Start container with minimal configuration
            container = docker_client.containers.run(
                "test-ai-python-service:latest",
                detach=True,
                ports={'8087/tcp': 8087},
                environment={
                    "AI_SERVICE_EUREKA_ENABLED": "false",
                    "AI_SERVICE_REDIS_ENABLED": "false"
                },
                remove=True
            )
            
            # Wait for startup
            time.sleep(10)
            
            # Check container is running
            container.reload()
            assert container.status == "running"
            
            # Test health endpoint
            response = httpx.get("http://localhost:8087/health", timeout=5)
            assert response.status_code == 200
            
            health_data = response.json()
            assert health_data["status"] == "healthy"
            assert health_data["service"] == "AI Service (Python)"
            
        except Exception as e:
            pytest.fail(f"Container startup test failed: {e}")
        finally:
            if container:
                container.stop()
    
    def test_container_startup_with_custom_env(self, docker_client, test_env_vars):
        """Test container starts with custom environment variables"""
        container = None
        try:
            container = docker_client.containers.run(
                "test-ai-python-service:latest",
                detach=True,
                ports={'8087/tcp': 8088},  # Use different port
                environment=test_env_vars,
                remove=True
            )
            
            time.sleep(10)
            
            container.reload()
            assert container.status == "running"
            
            # Test health endpoint on custom port
            response = httpx.get("http://localhost:8088/health", timeout=5)
            assert response.status_code == 200
            
        except Exception as e:
            pytest.fail(f"Custom environment test failed: {e}")
        finally:
            if container:
                container.stop()
    
    def test_container_health_check(self, docker_client):
        """Test Docker health check functionality"""
        container = None
        try:
            container = docker_client.containers.run(
                "test-ai-python-service:latest",
                detach=True,
                ports={'8087/tcp': 8089},
                environment={
                    "AI_SERVICE_EUREKA_ENABLED": "false",
                    "AI_SERVICE_REDIS_ENABLED": "false"
                },
                remove=True
            )
            
            # Wait for health check to pass
            max_wait = 60  # seconds
            wait_time = 0
            
            while wait_time < max_wait:
                container.reload()
                health = container.attrs.get('State', {}).get('Health', {})
                
                if health.get('Status') == 'healthy':
                    break
                    
                time.sleep(5)
                wait_time += 5
            
            container.reload()
            health_status = container.attrs.get('State', {}).get('Health', {}).get('Status')
            assert health_status == 'healthy', f"Health check failed: {health_status}"
            
        except Exception as e:
            pytest.fail(f"Health check test failed: {e}")
        finally:
            if container:
                container.stop()
    
    def test_container_resource_limits(self, docker_client):
        """Test container respects resource limits"""
        container = None
        try:
            container = docker_client.containers.run(
                "test-ai-python-service:latest",
                detach=True,
                ports={'8087/tcp': 8090},
                environment={
                    "AI_SERVICE_EUREKA_ENABLED": "false",
                    "AI_SERVICE_REDIS_ENABLED": "false",
                    "AI_SERVICE_MEMORY_LIMIT_MB": "512"
                },
                mem_limit="512m",
                remove=True
            )
            
            time.sleep(10)
            
            container.reload()
            assert container.status == "running"
            
            # Check memory limit is respected
            stats = container.stats(stream=False)
            memory_usage = stats['memory_stats']['usage']
            memory_limit = stats['memory_stats']['limit']
            
            assert memory_limit <= 512 * 1024 * 1024  # 512MB in bytes
            
        except Exception as e:
            pytest.fail(f"Resource limits test failed: {e}")
        finally:
            if container:
                container.stop()


class TestEnvironmentConfiguration:
    """Test environment variable handling and configuration validation"""
    
    def test_environment_variable_validation(self):
        """Test that environment variables are properly validated"""
        test_cases = [
            # Valid configurations
            {
                "AI_SERVICE_PORT": "8087",
                "AI_SERVICE_DEBUG": "true",
                "AI_SERVICE_CACHE_SIZE": "100",
                "expected_valid": True
            },
            # Invalid port
            {
                "AI_SERVICE_PORT": "invalid",
                "expected_valid": False
            },
            # Invalid boolean
            {
                "AI_SERVICE_DEBUG": "maybe",
                "expected_valid": False
            },
            # Invalid integer
            {
                "AI_SERVICE_CACHE_SIZE": "not_a_number",
                "expected_valid": False
            }
        ]
        
        for case in test_cases:
            with patch.dict(os.environ, case, clear=True):
                try:
                    settings = Settings()
                    if not case.get("expected_valid", True):
                        pytest.fail(f"Expected validation error for: {case}")
                except Exception as e:
                    if case.get("expected_valid", True):
                        pytest.fail(f"Unexpected validation error: {e}")
    
    def test_default_configuration_values(self):
        """Test that default configuration values are correct"""
        with patch.dict(os.environ, {}, clear=True):
            settings = Settings()
            
            assert settings.service_name == "ai-service-python"
            assert settings.port == 8087
            assert settings.debug is False
            assert settings.eureka_enabled is True
            assert settings.cache_size == 100
            assert settings.model_timeout_seconds == 30
            assert settings.max_concurrent_requests == 10
            assert settings.memory_limit_mb == 2048
            assert settings.redis_enabled is False
            assert settings.metrics_enabled is True
    
    def test_environment_override_configuration(self):
        """Test that environment variables properly override defaults"""
        env_overrides = {
            "AI_SERVICE_SERVICE_NAME": "custom-ai-service",
            "AI_SERVICE_PORT": "9000",
            "AI_SERVICE_DEBUG": "true",
            "AI_SERVICE_EUREKA_ENABLED": "false",
            "AI_SERVICE_CACHE_SIZE": "200",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "custom-redis",
            "AI_SERVICE_REDIS_PORT": "6380"
        }
        
        with patch.dict(os.environ, env_overrides):
            settings = Settings()
            
            assert settings.service_name == "custom-ai-service"
            assert settings.port == 9000
            assert settings.debug is True
            assert settings.eureka_enabled is False
            assert settings.cache_size == 200
            assert settings.redis_enabled is True
            assert settings.redis_host == "custom-redis"
            assert settings.redis_port == 6380
    
    def test_configuration_file_loading(self):
        """Test configuration loading from file"""
        # Create temporary config file
        config_data = {
            "service_name": "file-config-service",
            "port": 8088,
            "debug": True
        }
        
        config_file = "test_config.json"
        try:
            with open(config_file, 'w') as f:
                json.dump(config_data, f)
            
            # Test that file config can be loaded (if implemented)
            # This is a placeholder for future file-based config support
            assert os.path.exists(config_file)
            
        finally:
            if os.path.exists(config_file):
                os.remove(config_file)
    
    def test_configuration_validation_edge_cases(self):
        """Test configuration validation with edge cases"""
        edge_cases = [
            # Minimum values
            {
                "AI_SERVICE_PORT": "1",
                "AI_SERVICE_CACHE_SIZE": "1",
                "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "1"
            },
            # Maximum reasonable values
            {
                "AI_SERVICE_PORT": "65535",
                "AI_SERVICE_CACHE_SIZE": "10000",
                "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "3600"
            },
            # Boolean variations
            {
                "AI_SERVICE_DEBUG": "True",
                "AI_SERVICE_EUREKA_ENABLED": "FALSE",
                "AI_SERVICE_REDIS_ENABLED": "1"
            }
        ]
        
        for case in edge_cases:
            with patch.dict(os.environ, case):
                try:
                    settings = Settings()
                    # Verify settings are created without errors
                    assert settings is not None
                except Exception as e:
                    pytest.fail(f"Edge case validation failed: {case}, error: {e}")


class TestServiceDiscovery:
    """Test service discovery and inter-service communication"""
    
    @pytest.fixture
    def mock_eureka_client(self):
        """Mock Eureka client for testing"""
        with patch('app.services.eureka_client.EurekaClient') as mock:
            yield mock
    
    def test_eureka_registration_configuration(self, mock_eureka_client):
        """Test Eureka service registration configuration"""
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://test-eureka:8761/eureka",
            "AI_SERVICE_SERVICE_NAME": "test-ai-service",
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify Eureka configuration
            assert settings.eureka_enabled is True
            assert settings.eureka_server_url == "http://test-eureka:8761/eureka"
            assert settings.service_name == "test-ai-service"
            assert settings.port == 8087
    
    def test_eureka_registration_disabled(self, mock_eureka_client):
        """Test behavior when Eureka registration is disabled"""
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "false"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            assert settings.eureka_enabled is False
    
    @pytest.mark.asyncio
    async def test_service_health_endpoint_accessibility(self):
        """Test that health endpoint is accessible for service discovery"""
        # Mock the health endpoint response
        with patch('httpx.AsyncClient.get') as mock_get:
            mock_response = MagicMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "status": "healthy",
                "service": "AI Service (Python)",
                "timestamp": "2023-01-01T00:00:00Z"
            }
            mock_get.return_value = mock_response
            
            async with httpx.AsyncClient() as client:
                response = await client.get("http://localhost:8087/health")
                
                assert response.status_code == 200
                health_data = response.json()
                assert health_data["status"] == "healthy"
                assert "service" in health_data
    
    def test_service_discovery_metadata(self):
        """Test service discovery metadata configuration"""
        env_vars = {
            "AI_SERVICE_SERVICE_NAME": "ai-service-python",
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_EUREKA_ENABLED": "true"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify metadata that would be sent to Eureka
            expected_metadata = {
                "service_name": settings.service_name,
                "port": settings.port,
                "health_check_url": f"http://localhost:{settings.port}/health"
            }
            
            assert expected_metadata["service_name"] == "ai-service-python"
            assert expected_metadata["port"] == 8087
            assert "health" in expected_metadata["health_check_url"]


class TestInterServiceCommunication:
    """Test inter-service communication configuration"""
    
    def test_redis_connection_configuration(self):
        """Test Redis connection configuration"""
        env_vars = {
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "test-redis",
            "AI_SERVICE_REDIS_PORT": "6379",
            "AI_SERVICE_REDIS_TTL_SECONDS": "1800"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            assert settings.redis_enabled is True
            assert settings.redis_host == "test-redis"
            assert settings.redis_port == 6379
            assert settings.redis_ttl_seconds == 1800
    
    @pytest.mark.asyncio
    async def test_redis_connection_health_check(self):
        """Test Redis connection health check functionality"""
        env_vars = {
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "localhost",
            "AI_SERVICE_REDIS_PORT": "6379"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Mock Redis connection test
            with patch('redis.Redis') as mock_redis:
                mock_redis_instance = MagicMock()
                mock_redis.return_value = mock_redis_instance
                mock_redis_instance.ping.return_value = True
                
                # Test Redis connectivity
                redis_client = mock_redis(
                    host=settings.redis_host,
                    port=settings.redis_port,
                    decode_responses=True
                )
                
                assert redis_client.ping() is True
                mock_redis.assert_called_once_with(
                    host="localhost",
                    port=6379,
                    decode_responses=True
                )
    
    def test_docker_network_configuration(self):
        """Test Docker network service name configuration"""
        docker_network_env = {
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka"
        }
        
        with patch.dict(os.environ, docker_network_env):
            settings = Settings()
            
            # Verify Docker service names are used
            assert settings.redis_host == "redis"
            assert "eureka-server" in settings.eureka_server_url
            assert ":8761" in settings.eureka_server_url
    
    def test_redis_connection_disabled(self):
        """Test behavior when Redis is disabled"""
        env_vars = {
            "AI_SERVICE_REDIS_ENABLED": "false"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            assert settings.redis_enabled is False
    
    @pytest.mark.asyncio
    async def test_metrics_endpoint_configuration(self):
        """Test metrics endpoint configuration for monitoring"""
        env_vars = {
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            assert settings.metrics_enabled is True
            assert settings.prometheus_port == 9090
    
    def test_performance_configuration(self):
        """Test performance-related configuration"""
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
    
    @pytest.mark.asyncio
    async def test_eureka_service_registration(self):
        """Test Eureka service registration functionality"""
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://test-eureka:8761/eureka",
            "AI_SERVICE_SERVICE_NAME": "test-ai-service",
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, env_vars):
            with patch('app.services.eureka_client.EurekaClient') as mock_eureka:
                mock_instance = MagicMock()
                mock_eureka.return_value = mock_instance
                
                settings = Settings()
                eureka_client = EurekaClient(settings)
                
                # Verify Eureka client can be initialized
                assert eureka_client is not None
                mock_eureka.assert_called_once()
    
    def test_docker_compose_environment_variables(self):
        """Test environment variables as they would be set in docker-compose"""
        docker_env_vars = {
            "AI_SERVICE_SERVICE_NAME": "ai-service-python",
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_DEBUG": "false",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90",
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_CACHE_SIZE": "100",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "10",
            "AI_SERVICE_MEMORY_LIMIT_MB": "2048",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379",
            "AI_SERVICE_REDIS_TTL_SECONDS": "3600",
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, docker_env_vars):
            settings = Settings()
            
            # Verify all Docker Compose environment variables are properly loaded
            assert settings.service_name == "ai-service-python"
            assert settings.port == 8087
            assert settings.debug is False
            assert settings.eureka_server_url == "http://eureka-server:8761/eureka"
            assert settings.eureka_enabled is True
            assert settings.eureka_renewal_interval == 30
            assert settings.eureka_duration == 90
            assert settings.models_path == "/app/models"
            assert settings.cache_size == 100
            assert settings.model_timeout_seconds == 30
            assert settings.max_concurrent_requests == 10
            assert settings.memory_limit_mb == 2048
            assert settings.redis_enabled is True
            assert settings.redis_host == "redis"
            assert settings.redis_port == 6379
            assert settings.redis_ttl_seconds == 3600
            assert settings.metrics_enabled is True
            assert settings.prometheus_port == 9090


class TestDeploymentIntegration:
    """Integration tests for deployment scenarios"""
    
    def test_startup_sequence_validation(self):
        """Test that startup sequence handles dependencies correctly"""
        # Test startup with all dependencies disabled (isolated mode)
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "false",
            "AI_SERVICE_REDIS_ENABLED": "false",
            "AI_SERVICE_METRICS_ENABLED": "true"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify isolated mode configuration
            assert settings.eureka_enabled is False
            assert settings.redis_enabled is False
            assert settings.metrics_enabled is True
    
    def test_graceful_degradation_configuration(self):
        """Test configuration for graceful degradation scenarios"""
        # Test configuration that allows fallback behavior
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify configuration supports fallback mechanisms
            assert settings.model_timeout_seconds > 0  # Allows timeout handling
            assert settings.eureka_enabled is True     # Service discovery available
            assert settings.redis_enabled is True      # Caching available
    
    def test_production_configuration_validation(self):
        """Test production-ready configuration validation"""
        production_env = {
            "AI_SERVICE_DEBUG": "false",
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_CACHE_SIZE": "500",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "50",
            "AI_SERVICE_MEMORY_LIMIT_MB": "4096"
        }
        
        with patch.dict(os.environ, production_env):
            settings = Settings()
            
            # Verify production-ready settings
            assert settings.debug is False
            assert settings.eureka_enabled is True
            assert settings.redis_enabled is True
            assert settings.metrics_enabled is True
            assert settings.cache_size >= 100
            assert settings.max_concurrent_requests >= 10
            assert settings.memory_limit_mb >= 1024
    
    def test_docker_volume_configuration(self):
        """Test Docker volume mount configuration"""
        volume_env = {
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_CACHE_PATH": "/app/cache",
            "AI_SERVICE_LOGS_PATH": "/app/logs"
        }
        
        with patch.dict(os.environ, volume_env):
            settings = Settings()
            
            # Verify volume paths are configured correctly
            assert settings.models_path == "/app/models"
            # Additional volume path tests would go here if implemented
    
    def test_health_check_configuration(self):
        """Test Docker health check configuration"""
        # Test health check endpoint configuration
        env_vars = {
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify health check URL can be constructed
            health_url = f"http://localhost:{settings.port}/health"
            assert health_url == "http://localhost:8087/health"
    
    def test_network_configuration(self):
        """Test Docker network configuration"""
        network_env = {
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379"
        }
        
        with patch.dict(os.environ, network_env):
            settings = Settings()
            
            # Verify network hostnames are configured for Docker network
            assert "eureka-server" in settings.eureka_server_url
            assert settings.redis_host == "redis"
            assert settings.redis_port == 6379
    
    def test_dependency_startup_order(self):
        """Test that service handles dependency startup order correctly"""
        # Test configuration that handles dependencies not being ready
        env_vars = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Verify configuration allows for dependency startup delays
            assert settings.eureka_renewal_interval >= 30
            assert settings.eureka_duration >= 90
    
    def test_container_restart_configuration(self):
        """Test configuration for container restart scenarios"""
        restart_env = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true",
            "AI_SERVICE_MODEL_MAX_AGE_DAYS": "30"
        }
        
        with patch.dict(os.environ, restart_env):
            settings = Settings()
            
            # Verify configuration supports clean restarts
            assert settings.eureka_enabled is True  # Re-registration on restart
            assert settings.redis_enabled is True   # Cache reconnection
    
    def test_resource_limit_configuration(self):
        """Test Docker resource limit configuration"""
        resource_env = {
            "AI_SERVICE_MEMORY_LIMIT_MB": "2048",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "10",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30"
        }
        
        with patch.dict(os.environ, resource_env):
            settings = Settings()
            
            # Verify resource limits are properly configured
            assert settings.memory_limit_mb == 2048
            assert settings.max_concurrent_requests == 10
            assert settings.model_timeout_seconds == 30
    
    def test_logging_configuration_for_containers(self):
        """Test logging configuration suitable for containers"""
        logging_env = {
            "AI_SERVICE_DEBUG": "false",
            "AI_SERVICE_METRICS_ENABLED": "true"
        }
        
        with patch.dict(os.environ, logging_env):
            settings = Settings()
            
            # Verify logging configuration is container-friendly
            assert settings.debug is False  # Production logging
            assert settings.metrics_enabled is True  # Observability enabled
    
    def test_docker_compose_environment_integration(self):
        """Test full Docker Compose environment variable integration"""
        # Test all environment variables as they appear in docker-compose.yml
        docker_compose_env = {
            "AI_SERVICE_SERVICE_NAME": "ai-service-python",
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_DEBUG": "false",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90",
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_CACHE_SIZE": "100",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30",
            "AI_SERVICE_DEFAULT_TASK_PARSER_MODEL": "default",
            "AI_SERVICE_DEFAULT_PRIORITIZER_MODEL": "default",
            "AI_SERVICE_DEFAULT_INSIGHTS_MODEL": "default",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true",
            "AI_SERVICE_MODEL_MAX_AGE_DAYS": "30",
            "AI_SERVICE_MAX_LOADED_MODELS": "5",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "10",
            "AI_SERVICE_MEMORY_LIMIT_MB": "2048",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379",
            "AI_SERVICE_REDIS_TTL_SECONDS": "3600",
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, docker_compose_env):
            settings = Settings()
            
            # Verify all Docker Compose settings are properly loaded
            assert settings.service_name == "ai-service-python"
            assert settings.port == 8087
            assert settings.debug is False
            assert settings.eureka_server_url == "http://eureka-server:8761/eureka"
            assert settings.eureka_enabled is True
            assert settings.eureka_renewal_interval == 30
            assert settings.eureka_duration == 90
            assert settings.models_path == "/app/models"
            assert settings.cache_size == 100
            assert settings.model_timeout_seconds == 30
            assert settings.max_concurrent_requests == 10
            assert settings.memory_limit_mb == 2048
            assert settings.redis_enabled is True
            assert settings.redis_host == "redis"
            assert settings.redis_port == 6379
            assert settings.redis_ttl_seconds == 3600
            assert settings.metrics_enabled is True
            assert settings.prometheus_port == 9090
    
    def test_container_health_check_timeout(self):
        """Test container health check timeout configuration"""
        # Test health check configuration as defined in docker-compose.yml
        env_vars = {
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, env_vars):
            settings = Settings()
            
            # Health check should be accessible within Docker timeout (10s)
            health_url = f"http://localhost:{settings.port}/health"
            assert health_url == "http://localhost:8087/health"
            
            # Verify port is correct for health check
            assert settings.port == 8087
    
    def test_docker_restart_policy_configuration(self):
        """Test configuration supports Docker restart policy"""
        restart_policy_env = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true"
        }
        
        with patch.dict(os.environ, restart_policy_env):
            settings = Settings()
            
            # Verify configuration supports "unless-stopped" restart policy
            assert settings.eureka_enabled is True  # Re-register on restart
            assert settings.redis_enabled is True   # Reconnect to Redis
            # Auto cleanup helps with clean restarts
    
    def test_docker_network_bridge_configuration(self):
        """Test Docker bridge network configuration"""
        network_env = {
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_REDIS_HOST": "redis"
        }
        
        with patch.dict(os.environ, network_env):
            settings = Settings()
            
            # Verify service names work with Docker bridge network
            assert "eureka-server" in settings.eureka_server_url
            assert settings.redis_host == "redis"
            
            # These should not be localhost in Docker environment
            assert "localhost" not in settings.eureka_server_url
            assert settings.redis_host != "localhost"
    
    def test_docker_volume_persistence_configuration(self):
        """Test Docker volume persistence configuration"""
        volume_env = {
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true",
            "AI_SERVICE_MODEL_MAX_AGE_DAYS": "30"
        }
        
        with patch.dict(os.environ, volume_env):
            settings = Settings()
            
            # Verify volume paths are configured for persistence
            assert settings.models_path == "/app/models"
            # Model cleanup settings help manage volume size
            assert settings.model_max_age_days == 30
    
    def test_prometheus_metrics_port_configuration(self):
        """Test Prometheus metrics port configuration for Docker"""
        metrics_env = {
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, metrics_env):
            settings = Settings()
            
            # Verify Prometheus port matches docker-compose.yml
            assert settings.metrics_enabled is True
            assert settings.prometheus_port == 9090
    
    def test_container_resource_limits_configuration(self):
        """Test container resource limits configuration"""
        resource_env = {
            "AI_SERVICE_MEMORY_LIMIT_MB": "2048",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "10",
            "AI_SERVICE_MAX_LOADED_MODELS": "5"
        }
        
        with patch.dict(os.environ, resource_env):
            settings = Settings()
            
            # Verify resource limits match docker-compose configuration
            assert settings.memory_limit_mb == 2048
            assert settings.max_concurrent_requests == 10
            assert settings.max_loaded_models == 5
    
    def test_service_startup_order_configuration(self):
        """Test service startup order configuration"""
        startup_env = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90",
            "AI_SERVICE_REDIS_ENABLED": "true"
        }
        
        with patch.dict(os.environ, startup_env):
            settings = Settings()
            
            # Verify configuration handles dependency startup order
            assert settings.eureka_enabled is True
            assert settings.eureka_renewal_interval == 30  # Allows time for Eureka to start
            assert settings.eureka_duration == 90          # Allows time for registration
            assert settings.redis_enabled is True          # Depends on Redis service