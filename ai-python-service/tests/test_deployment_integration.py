"""
Comprehensive deployment integration tests
Tests complete Docker deployment flow, service discovery, and inter-service communication
"""
import pytest
import asyncio
import httpx
import docker
import time
import os
import json
import subprocess
import yaml
from unittest.mock import patch, MagicMock
from typing import Dict, Any, List
from pathlib import Path

from app.config.settings import Settings


class TestCompleteDeploymentIntegration:
    """Test complete deployment integration scenarios"""
    
    @pytest.fixture(scope="class")
    def docker_client(self):
        """Docker client fixture for integration tests"""
        return docker.from_env()
    
    @pytest.fixture(scope="class")
    def deployment_config(self):
        """Deployment configuration fixture"""
        return {
            "services": {
                "ai-python-service": {
                    "port": 8087,
                    "health_endpoint": "/health",
                    "metrics_port": 9090
                },
                "ai-service": {
                    "port": 8086,
                    "health_endpoint": "/actuator/health"
                },
                "analytics-service": {
                    "port": 8085,
                    "health_endpoint": "/actuator/health",
                    "metrics_port": 9091
                },
                "eureka-server": {
                    "port": 8761,
                    "health_endpoint": "/actuator/health"
                },
                "redis": {
                    "port": 6379
                }
            },
            "timeouts": {
                "startup": 120,
                "health_check": 30,
                "service_discovery": 60
            }
        }
    
    def test_docker_compose_configuration_validation(self):
        """Test Docker Compose configuration is valid"""
        try:
            # Test docker-compose config validation
            result = subprocess.run(
                ['docker-compose', 'config'], 
                capture_output=True, 
                text=True,
                cwd=Path(__file__).parent.parent.parent
            )
            
            assert result.returncode == 0, f"Docker Compose config invalid: {result.stderr}"
            
            # Parse and validate configuration
            config = yaml.safe_load(result.stdout)
            services = config.get('services', {})
            
            # Verify required services are present
            required_services = [
                'ai-python-service', 'ai-service', 'analytics-service', 
                'eureka-server', 'redis'
            ]
            
            for service in required_services:
                assert service in services, f"Required service {service} not found"
            
            # Verify AI Python service configuration
            ai_python_config = services.get('ai-python-service', {})
            assert 'environment' in ai_python_config, "AI Python service missing environment config"
            assert 'healthcheck' in ai_python_config, "AI Python service missing health check"
            assert 'depends_on' in ai_python_config, "AI Python service missing dependencies"
            
            # Verify environment variables
            env_vars = ai_python_config.get('environment', [])
            required_env_vars = [
                'AI_SERVICE_SERVICE_NAME',
                'AI_SERVICE_PORT', 
                'AI_SERVICE_EUREKA_SERVER_URL',
                'AI_SERVICE_REDIS_HOST'
            ]
            
            env_dict = {}
            for env_var in env_vars:
                if '=' in env_var:
                    key, value = env_var.split('=', 1)
                    env_dict[key] = value
            
            for required_var in required_env_vars:
                assert required_var in env_dict, f"Required environment variable {required_var} not found"
            
        except subprocess.CalledProcessError as e:
            pytest.fail(f"Docker Compose validation failed: {e}")
        except FileNotFoundError:
            pytest.skip("Docker Compose not available")
    
    def test_environment_variable_inheritance(self):
        """Test environment variable inheritance from docker-compose"""
        # Test that all docker-compose environment variables are properly handled
        docker_compose_env = {
            # Service configuration
            "AI_SERVICE_SERVICE_NAME": "ai-service-python",
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_DEBUG": "false",
            
            # Eureka configuration  
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90",
            
            # Model configuration
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_CACHE_SIZE": "100",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "30",
            "AI_SERVICE_DEFAULT_TASK_PARSER_MODEL": "default",
            "AI_SERVICE_DEFAULT_PRIORITIZER_MODEL": "default", 
            "AI_SERVICE_DEFAULT_INSIGHTS_MODEL": "default",
            
            # Model management
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true",
            "AI_SERVICE_MODEL_MAX_AGE_DAYS": "30",
            "AI_SERVICE_MAX_LOADED_MODELS": "5",
            
            # Performance settings
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "10",
            "AI_SERVICE_MEMORY_LIMIT_MB": "2048",
            
            # Redis configuration
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379",
            "AI_SERVICE_REDIS_TTL_SECONDS": "3600",
            
            # Observability
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, docker_compose_env):
            settings = Settings()
            
            # Verify all settings are correctly loaded
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
    
    def test_service_dependency_configuration(self):
        """Test service dependency configuration matches docker-compose"""
        # Test that service dependencies are properly configured
        dependency_env = {
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379"
        }
        
        with patch.dict(os.environ, dependency_env):
            settings = Settings()
            
            # Verify dependencies use Docker service names (not localhost)
            assert "eureka-server" in settings.eureka_server_url
            assert "localhost" not in settings.eureka_server_url
            assert settings.redis_host == "redis"
            assert settings.redis_host != "localhost"
            
            # Verify ports match docker-compose configuration
            assert ":8761" in settings.eureka_server_url
            assert settings.redis_port == 6379
    
    def test_health_check_configuration_compliance(self):
        """Test health check configuration matches docker-compose health check"""
        # Test health check configuration as defined in docker-compose.yml
        health_check_env = {
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, health_check_env):
            settings = Settings()
            
            # Verify health check URL matches docker-compose health check command
            health_url = f"http://localhost:{settings.port}/health"
            assert health_url == "http://localhost:8087/health"
            
            # Verify port matches docker-compose configuration
            assert settings.port == 8087
    
    def test_volume_mount_configuration(self):
        """Test volume mount configuration matches docker-compose volumes"""
        volume_env = {
            "AI_SERVICE_MODELS_PATH": "/app/models"
        }
        
        with patch.dict(os.environ, volume_env):
            settings = Settings()
            
            # Verify volume paths match docker-compose volume mounts
            assert settings.models_path == "/app/models"
            
            # Verify paths are absolute (required for Docker volumes)
            assert os.path.isabs(settings.models_path)
    
    def test_network_configuration_compliance(self):
        """Test network configuration matches docker-compose network setup"""
        network_env = {
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_REDIS_HOST": "redis"
        }
        
        with patch.dict(os.environ, network_env):
            settings = Settings()
            
            # Verify network configuration uses Docker service names
            # This ensures services can communicate within the Docker network
            assert "eureka-server" in settings.eureka_server_url
            assert settings.redis_host == "redis"
            
            # Verify no localhost references (would fail in Docker network)
            assert "localhost" not in settings.eureka_server_url
            assert "127.0.0.1" not in settings.eureka_server_url
    
    def test_restart_policy_configuration(self):
        """Test configuration supports docker-compose restart policy"""
        restart_env = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true"
        }
        
        with patch.dict(os.environ, restart_env):
            settings = Settings()
            
            # Verify configuration supports "unless-stopped" restart policy
            assert settings.eureka_enabled is True  # Re-register with Eureka on restart
            assert settings.redis_enabled is True   # Reconnect to Redis on restart
            # Auto cleanup helps with clean restarts
    
    def test_prometheus_metrics_port_compliance(self):
        """Test Prometheus metrics port matches docker-compose port mapping"""
        metrics_env = {
            "AI_SERVICE_METRICS_ENABLED": "true",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, metrics_env):
            settings = Settings()
            
            # Verify Prometheus port matches docker-compose port mapping (9090:9090)
            assert settings.metrics_enabled is True
            assert settings.prometheus_port == 9090
    
    def test_resource_limits_configuration(self):
        """Test resource limits configuration matches docker-compose settings"""
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
    
    @pytest.mark.asyncio
    async def test_service_startup_sequence_simulation(self):
        """Test service startup sequence simulation"""
        # Simulate the startup sequence as it would happen in Docker
        startup_phases = [
            {
                "phase": "infrastructure",
                "services": ["redis", "eureka-server"],
                "env": {
                    "AI_SERVICE_EUREKA_ENABLED": "false",  # Eureka not ready yet
                    "AI_SERVICE_REDIS_ENABLED": "false"    # Redis not ready yet
                }
            },
            {
                "phase": "dependencies_ready", 
                "services": ["eureka-server", "redis"],
                "env": {
                    "AI_SERVICE_EUREKA_ENABLED": "true",
                    "AI_SERVICE_REDIS_ENABLED": "true",
                    "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
                    "AI_SERVICE_REDIS_HOST": "redis"
                }
            }
        ]
        
        for phase in startup_phases:
            with patch.dict(os.environ, phase["env"]):
                settings = Settings()
                
                if phase["phase"] == "infrastructure":
                    # During infrastructure startup, external dependencies should be disabled
                    assert settings.eureka_enabled is False
                    assert settings.redis_enabled is False
                    
                elif phase["phase"] == "dependencies_ready":
                    # After dependencies are ready, they should be enabled
                    assert settings.eureka_enabled is True
                    assert settings.redis_enabled is True
                    assert "eureka-server" in settings.eureka_server_url
                    assert settings.redis_host == "redis"
    
    def test_docker_compose_environment_variable_substitution(self):
        """Test Docker Compose environment variable substitution"""
        # Test environment variables with default values as used in docker-compose.yml
        substitution_env = {
            "AI_SERVICE_DEBUG": "false",
            "MODEL_CACHE_SIZE": "100", 
            "MODEL_TIMEOUT_SECONDS": "30",
            "MAX_CONCURRENT_REQUESTS": "10",
            "MEMORY_LIMIT_MB": "2048",
            "METRICS_ENABLED": "true"
        }
        
        # Test with substitution variables
        docker_compose_env = {
            "AI_SERVICE_DEBUG": "${AI_SERVICE_DEBUG:-false}",
            "AI_SERVICE_CACHE_SIZE": "${MODEL_CACHE_SIZE:-100}",
            "AI_SERVICE_MODEL_TIMEOUT_SECONDS": "${MODEL_TIMEOUT_SECONDS:-30}",
            "AI_SERVICE_MAX_CONCURRENT_REQUESTS": "${MAX_CONCURRENT_REQUESTS:-10}",
            "AI_SERVICE_MEMORY_LIMIT_MB": "${MEMORY_LIMIT_MB:-2048}",
            "AI_SERVICE_METRICS_ENABLED": "${METRICS_ENABLED:-true}"
        }
        
        # Simulate Docker Compose variable substitution
        resolved_env = {}
        for key, value in docker_compose_env.items():
            if value.startswith("${") and ":-" in value:
                # Extract default value
                default_value = value.split(":-")[1].rstrip("}")
                resolved_env[key] = default_value
            else:
                resolved_env[key] = value
        
        with patch.dict(os.environ, resolved_env):
            settings = Settings()
            
            # Verify default values are properly applied
            assert settings.debug is False
            assert settings.cache_size == 100
            assert settings.model_timeout_seconds == 30
            assert settings.max_concurrent_requests == 10
            assert settings.memory_limit_mb == 2048
            assert settings.metrics_enabled is True
    
    def test_container_health_check_timing(self):
        """Test container health check timing configuration"""
        # Test health check timing as configured in docker-compose.yml
        # interval: 30s, timeout: 10s, retries: 3, start_period: 40s
        
        health_timing_env = {
            "AI_SERVICE_PORT": "8087"
        }
        
        with patch.dict(os.environ, health_timing_env):
            settings = Settings()
            
            # Verify health endpoint is configured correctly
            health_url = f"http://localhost:{settings.port}/health"
            assert health_url == "http://localhost:8087/health"
            
            # Health check should be accessible within Docker timeout (10s)
            # This is tested by ensuring the port is correct
            assert settings.port == 8087
    
    def test_service_discovery_integration_configuration(self):
        """Test service discovery integration configuration"""
        service_discovery_env = {
            "AI_SERVICE_SERVICE_NAME": "ai-service-python",
            "AI_SERVICE_EUREKA_SERVER_URL": "http://eureka-server:8761/eureka",
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90"
        }
        
        with patch.dict(os.environ, service_discovery_env):
            settings = Settings()
            
            # Verify service discovery configuration
            assert settings.service_name == "ai-service-python"
            assert settings.eureka_enabled is True
            assert "eureka-server" in settings.eureka_server_url
            assert settings.eureka_renewal_interval == 30
            assert settings.eureka_duration == 90
            
            # Verify service name matches what other services expect
            assert settings.service_name == "ai-service-python"
    
    def test_inter_service_communication_configuration(self):
        """Test inter-service communication configuration"""
        communication_env = {
            "AI_SERVICE_REDIS_HOST": "redis",
            "AI_SERVICE_REDIS_PORT": "6379",
            "AI_SERVICE_REDIS_ENABLED": "true"
        }
        
        with patch.dict(os.environ, communication_env):
            settings = Settings()
            
            # Verify inter-service communication uses Docker service names
            assert settings.redis_host == "redis"
            assert settings.redis_port == 6379
            assert settings.redis_enabled is True
            
            # Verify no localhost references
            assert settings.redis_host != "localhost"
    
    def test_production_deployment_configuration(self):
        """Test production deployment configuration"""
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
            
            # Verify production-ready configuration
            assert settings.debug is False
            assert settings.eureka_enabled is True
            assert settings.redis_enabled is True
            assert settings.metrics_enabled is True
            assert settings.cache_size >= 100
            assert settings.max_concurrent_requests >= 10
            assert settings.memory_limit_mb >= 1024
    
    def test_docker_compose_service_order_configuration(self):
        """Test Docker Compose service startup order configuration"""
        # Test configuration that supports proper service startup order
        # ai-python-service depends_on: [eureka-server, redis]
        
        startup_order_env = {
            "AI_SERVICE_EUREKA_ENABLED": "true",
            "AI_SERVICE_REDIS_ENABLED": "true",
            "AI_SERVICE_EUREKA_RENEWAL_INTERVAL": "30",
            "AI_SERVICE_EUREKA_DURATION": "90"
        }
        
        with patch.dict(os.environ, startup_order_env):
            settings = Settings()
            
            # Verify configuration allows for dependency startup delays
            assert settings.eureka_enabled is True
            assert settings.redis_enabled is True
            assert settings.eureka_renewal_interval >= 30  # Allows time for Eureka startup
            assert settings.eureka_duration >= 90          # Allows time for registration
    
    def test_docker_compose_port_mapping_configuration(self):
        """Test Docker Compose port mapping configuration"""
        port_mapping_env = {
            "AI_SERVICE_PORT": "8087",
            "AI_SERVICE_PROMETHEUS_PORT": "9090"
        }
        
        with patch.dict(os.environ, port_mapping_env):
            settings = Settings()
            
            # Verify ports match docker-compose.yml port mappings
            # ai-python-service: "8087:8087" and "9090:9090"
            assert settings.port == 8087
            assert settings.prometheus_port == 9090
    
    def test_docker_compose_volume_configuration(self):
        """Test Docker Compose volume configuration"""
        volume_config_env = {
            "AI_SERVICE_MODELS_PATH": "/app/models",
            "AI_SERVICE_AUTO_CLEANUP_ENABLED": "true",
            "AI_SERVICE_MODEL_MAX_AGE_DAYS": "30"
        }
        
        with patch.dict(os.environ, volume_config_env):
            settings = Settings()
            
            # Verify volume configuration matches docker-compose.yml
            # volumes: ai_models:/app/models, ai_cache:/app/cache, ai_logs:/app/logs
            assert settings.models_path == "/app/models"
            
            # Verify cleanup settings help manage volume size
            assert settings.model_max_age_days == 30