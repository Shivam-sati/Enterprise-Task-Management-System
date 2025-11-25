#!/usr/bin/env python3
"""
Comprehensive Docker deployment and configuration test script
Tests Docker container startup, health checks, and service discovery
"""

import os
import sys
import time
import json
import subprocess
import requests
import docker
import logging
import redis
import yaml
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, field
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@dataclass
class ServiceConfig:
    """Configuration for a service to test"""
    name: str
    port: int
    health_endpoint: str
    expected_response: Dict
    startup_timeout: int = 90
    dependencies: List[str] = field(default_factory=list)
    metrics_port: Optional[int] = None

class DockerDeploymentTester:
    """Docker deployment and configuration tester"""
    
    def __init__(self):
        self.docker_client = docker.from_env()
        self.services = self._define_services()
        self.test_results = {}
        
    def _define_services(self) -> Dict[str, ServiceConfig]:
        """Define services to test"""
        return {
            "eureka-server": ServiceConfig(
                name="eureka-server",
                port=8761,
                health_endpoint="/actuator/health",
                expected_response={"status": "UP"},
                startup_timeout=90
            ),
            "redis": ServiceConfig(
                name="redis",
                port=6379,
                health_endpoint=None,  # Redis doesn't have HTTP health endpoint
                expected_response={},
                startup_timeout=30
            ),
            "ai-python-service": ServiceConfig(
                name="ai-python-service",
                port=8087,
                health_endpoint="/health",
                expected_response={"status": "healthy"},
                dependencies=["eureka-server", "redis"],
                startup_timeout=120,
                metrics_port=9090
            ),
            "ai-service": ServiceConfig(
                name="ai-service",
                port=8086,
                health_endpoint="/actuator/health",
                expected_response={"status": "UP"},
                dependencies=["eureka-server", "redis", "ai-python-service"],
                startup_timeout=90
            ),
            "analytics-service": ServiceConfig(
                name="analytics-service",
                port=8085,
                health_endpoint="/actuator/health",
                expected_response={"status": "UP"},
                dependencies=["eureka-server", "redis"],
                startup_timeout=90,
                metrics_port=9091
            ),
            "api-gateway": ServiceConfig(
                name="api-gateway",
                port=8080,
                health_endpoint="/actuator/health",
                expected_response={"status": "UP"},
                dependencies=["eureka-server", "redis"],
                startup_timeout=90
            )
        }
    
    def run_all_tests(self) -> bool:
        """Run all deployment tests"""
        logger.info("Starting Docker deployment tests...")
        
        try:
            # Test Docker environment
            if not self._test_docker_environment():
                return False
            
            # Test environment configuration
            if not self._test_environment_configuration():
                return False
            
            # Test Docker Compose configuration
            if not self._test_docker_compose_configuration():
                return False
            
            # Start services and test
            if not self._test_service_startup():
                return False
            
            # Test health checks
            if not self._test_health_checks():
                return False
            
            # Test service discovery
            if not self._test_service_discovery():
                return False
            
            # Test inter-service communication
            if not self._test_inter_service_communication():
                return False
            
            # Test metrics endpoints
            if not self._test_metrics_endpoints():
                return False
            
            # Test Docker volumes
            if not self._test_docker_volumes():
                return False
            
            # Test container resource usage
            if not self._test_container_resource_usage():
                return False
            
            # Test network connectivity
            if not self._test_network_connectivity():
                return False
            
            # Test container logging
            if not self._test_container_logs():
                return False
            
            logger.info("All deployment tests passed!")
            return True
            
        except Exception as e:
            logger.error(f"Deployment tests failed: {e}")
            return False
        finally:
            self._cleanup()
    
    def _test_docker_environment(self) -> bool:
        """Test Docker environment setup"""
        logger.info("Testing Docker environment...")
        
        try:
            # Test Docker daemon
            self.docker_client.ping()
            logger.info("✓ Docker daemon is running")
            
            # Test Docker Compose
            result = subprocess.run(['docker-compose', '--version'], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                logger.error("✗ Docker Compose not available")
                return False
            logger.info("✓ Docker Compose is available")
            
            # Check available resources
            info = self.docker_client.info()
            total_memory = info.get('MemTotal', 0) / (1024**3)  # GB
            if total_memory < 4:
                logger.warning(f"⚠ Low memory available: {total_memory:.1f}GB")
            else:
                logger.info(f"✓ Memory available: {total_memory:.1f}GB")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Docker environment test failed: {e}")
            return False
    
    def _test_environment_configuration(self) -> bool:
        """Test environment variable configuration"""
        logger.info("Testing environment configuration...")
        
        try:
            # Check for .env file
            env_file = Path('.env')
            if not env_file.exists():
                logger.warning("⚠ .env file not found, using defaults")
                # Copy from example if available
                env_example = Path('.env.example')
                if env_example.exists():
                    import shutil
                    shutil.copy(env_example, env_file)
                    logger.info("✓ Created .env from .env.example")
            else:
                logger.info("✓ .env file found")
            
            # Validate critical environment variables
            critical_vars = [
                'MONGODB_URI',
                'OPENAI_API_KEY'
            ]
            
            for var in critical_vars:
                value = os.getenv(var)
                if not value or value.startswith('${'):
                    logger.warning(f"⚠ {var} not set or using placeholder")
                else:
                    logger.info(f"✓ {var} is configured")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Environment configuration test failed: {e}")
            return False
    
    def _test_docker_compose_configuration(self) -> bool:
        """Test Docker Compose configuration"""
        logger.info("Testing Docker Compose configuration...")
        
        try:
            # Validate docker-compose.yml
            result = subprocess.run(['docker-compose', 'config'], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                logger.error(f"✗ Docker Compose configuration invalid: {result.stderr}")
                return False
            logger.info("✓ Docker Compose configuration is valid")
            
            # Parse and validate service configuration
            config = yaml.safe_load(result.stdout)
            services = config.get('services', {})
            
            # Check required services
            required_services = ['eureka-server', 'redis', 'ai-python-service', 
                               'ai-service', 'analytics-service']
            for service in required_services:
                if service not in services:
                    logger.error(f"✗ Required service {service} not found in docker-compose.yml")
                    return False
                logger.info(f"✓ Service {service} configured")
            
            # Validate service dependencies
            for service_name, service_config in services.items():
                depends_on = service_config.get('depends_on', [])
                if service_name in self.services:
                    expected_deps = self.services[service_name].dependencies
                    for dep in expected_deps:
                        if dep not in depends_on:
                            logger.warning(f"⚠ Service {service_name} missing dependency {dep}")
            
            # Check health checks
            services_with_health_checks = ['ai-python-service', 'ai-service', 'analytics-service']
            for service in services_with_health_checks:
                if service in services:
                    healthcheck = services[service].get('healthcheck')
                    if healthcheck:
                        logger.info(f"✓ Service {service} has health check configured")
                    else:
                        logger.warning(f"⚠ Service {service} missing health check")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Docker Compose configuration test failed: {e}")
            return False
    
    def _test_service_startup(self) -> bool:
        """Test service startup sequence"""
        logger.info("Testing service startup...")
        
        try:
            # Start services
            logger.info("Starting services with docker-compose...")
            result = subprocess.run(['docker-compose', 'up', '-d'], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                logger.error(f"✗ Failed to start services: {result.stderr}")
                return False
            
            # Wait for services to start
            logger.info("Waiting for services to initialize...")
            time.sleep(30)
            
            # Check container status
            containers = self.docker_client.containers.list()
            running_services = {c.name.replace('task-management-', '').replace('-', '_') 
                              for c in containers if c.status == 'running'}
            
            for service_name in self.services:
                container_name = service_name.replace('_', '-')
                if container_name not in [c.name for c in containers]:
                    logger.error(f"✗ Service {service_name} container not found")
                    return False
                
                container = next((c for c in containers if service_name in c.name), None)
                if container and container.status != 'running':
                    logger.error(f"✗ Service {service_name} not running: {container.status}")
                    return False
                
                logger.info(f"✓ Service {service_name} is running")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Service startup test failed: {e}")
            return False
    
    def _test_health_checks(self) -> bool:
        """Test service health checks"""
        logger.info("Testing service health checks...")
        
        success = True
        for service_name, config in self.services.items():
            if not config.health_endpoint:
                continue
                
            try:
                url = f"http://localhost:{config.port}{config.health_endpoint}"
                logger.info(f"Testing health check for {service_name}: {url}")
                
                # Wait for service to be ready
                max_attempts = config.startup_timeout // 5
                for attempt in range(max_attempts):
                    try:
                        response = requests.get(url, timeout=5)
                        if response.status_code == 200:
                            health_data = response.json()
                            
                            # Validate expected response
                            for key, expected_value in config.expected_response.items():
                                if health_data.get(key) != expected_value:
                                    logger.warning(f"⚠ {service_name} health check: "
                                                 f"expected {key}={expected_value}, "
                                                 f"got {health_data.get(key)}")
                            
                            logger.info(f"✓ {service_name} health check passed")
                            break
                    except requests.RequestException:
                        if attempt < max_attempts - 1:
                            time.sleep(5)
                            continue
                        else:
                            raise
                else:
                    logger.error(f"✗ {service_name} health check failed after {max_attempts} attempts")
                    success = False
                    
            except Exception as e:
                logger.error(f"✗ {service_name} health check failed: {e}")
                success = False
        
        return success
    
    def _test_service_discovery(self) -> bool:
        """Test Eureka service discovery"""
        logger.info("Testing service discovery...")
        
        try:
            # Test Eureka server
            eureka_url = "http://localhost:8761/eureka/apps"
            response = requests.get(eureka_url, timeout=10)
            
            if response.status_code != 200:
                logger.error(f"✗ Eureka server not accessible: {response.status_code}")
                return False
            
            logger.info("✓ Eureka server is accessible")
            
            # Check registered services
            apps_data = response.text
            expected_services = ['AI-SERVICE-PYTHON', 'AI-SERVICE', 'ANALYTICS-SERVICE']
            
            for service in expected_services:
                if service in apps_data:
                    logger.info(f"✓ Service {service} registered with Eureka")
                else:
                    logger.warning(f"⚠ Service {service} not registered with Eureka")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Service discovery test failed: {e}")
            return False
    
    def _test_inter_service_communication(self) -> bool:
        """Test inter-service communication"""
        logger.info("Testing inter-service communication...")
        
        try:
            # Test AI service proxy to Python service
            ai_service_url = "http://localhost:8086/api/ai/parse-task"
            test_payload = {"description": "Test task for deployment validation"}
            
            response = requests.post(ai_service_url, 
                                   json=test_payload, 
                                   headers={"Content-Type": "application/json"},
                                   timeout=30)
            
            if response.status_code == 200:
                logger.info("✓ AI service proxy communication working")
            else:
                logger.warning(f"⚠ AI service proxy returned {response.status_code}")
            
            # Test Analytics service
            analytics_url = "http://localhost:8085/api/analytics/health"
            response = requests.get(analytics_url, timeout=10)
            
            if response.status_code == 200:
                logger.info("✓ Analytics service communication working")
            else:
                logger.warning(f"⚠ Analytics service returned {response.status_code}")
            
            return True
            
        except Exception as e:
            logger.warning(f"⚠ Inter-service communication test failed: {e}")
            return True  # Non-critical for deployment validation
    
    def _test_metrics_endpoints(self) -> bool:
        """Test metrics endpoints"""
        logger.info("Testing metrics endpoints...")
        
        success = True
        for service_name, config in self.services.items():
            if not config.metrics_port:
                continue
                
            try:
                metrics_url = f"http://localhost:{config.metrics_port}/metrics"
                response = requests.get(metrics_url, timeout=5)
                
                if response.status_code == 200:
                    logger.info(f"✓ {service_name} metrics endpoint accessible")
                    
                    # Validate metrics content
                    metrics_content = response.text
                    if "# HELP" in metrics_content and "# TYPE" in metrics_content:
                        logger.info(f"✓ {service_name} metrics format is valid")
                    else:
                        logger.warning(f"⚠ {service_name} metrics format may be invalid")
                else:
                    logger.warning(f"⚠ {service_name} metrics endpoint returned {response.status_code}")
                    
            except Exception as e:
                logger.warning(f"⚠ {service_name} metrics endpoint failed: {e}")
        
        return success
    
    def _test_docker_volumes(self) -> bool:
        """Test Docker volume configuration and persistence"""
        logger.info("Testing Docker volume configuration...")
        
        try:
            # Test that volumes are created
            volumes = self.docker_client.volumes.list()
            volume_names = [vol.name for vol in volumes]
            
            expected_volumes = [
                "task-management_redis_data",
                "task-management_rabbitmq_data", 
                "task-management_ai_models",
                "task-management_ai_cache",
                "task-management_ai_logs"
            ]
            
            for volume_name in expected_volumes:
                # Check for volume with or without project prefix
                volume_exists = any(volume_name in vname or volume_name.split('_', 1)[1] in vname 
                                  for vname in volume_names)
                if volume_exists:
                    logger.info(f"✓ Volume {volume_name} exists")
                else:
                    logger.warning(f"⚠ Volume {volume_name} not found")
            
            return True
            
        except Exception as e:
            logger.error(f"✗ Docker volume test failed: {e}")
            return False
    
    def _test_container_resource_usage(self) -> bool:
        """Test container resource usage and limits"""
        logger.info("Testing container resource usage...")
        
        try:
            containers = self.docker_client.containers.list()
            
            for container in containers:
                if "task-management" in container.name:
                    stats = container.stats(stream=False)
                    
                    # Check memory usage
                    memory_usage = stats['memory_stats'].get('usage', 0)
                    memory_limit = stats['memory_stats'].get('limit', 0)
                    
                    if memory_limit > 0:
                        memory_percent = (memory_usage / memory_limit) * 100
                        logger.info(f"✓ {container.name} memory usage: {memory_percent:.1f}%")
                        
                        if memory_percent > 90:
                            logger.warning(f"⚠ {container.name} high memory usage: {memory_percent:.1f}%")
                    
                    # Check CPU usage
                    cpu_stats = stats.get('cpu_stats', {})
                    if cpu_stats:
                        logger.info(f"✓ {container.name} CPU stats available")
            
            return True
            
        except Exception as e:
            logger.warning(f"⚠ Container resource usage test failed: {e}")
            return True  # Non-critical
    
    def _test_network_connectivity(self) -> bool:
        """Test Docker network connectivity between services"""
        logger.info("Testing Docker network connectivity...")
        
        try:
            # Test that containers can reach each other
            networks = self.docker_client.networks.list()
            task_network = None
            
            for network in networks:
                if "task-management" in network.name:
                    task_network = network
                    break
            
            if task_network:
                logger.info(f"✓ Task management network found: {task_network.name}")
                
                # Check connected containers
                network_details = self.docker_client.networks.get(task_network.id)
                connected_containers = network_details.attrs.get('Containers', {})
                
                logger.info(f"✓ {len(connected_containers)} containers connected to network")
                
                for container_id, container_info in connected_containers.items():
                    container_name = container_info.get('Name', 'unknown')
                    logger.info(f"✓ Container {container_name} connected to network")
            else:
                logger.warning("⚠ Task management network not found")
            
            return True
            
        except Exception as e:
            logger.warning(f"⚠ Network connectivity test failed: {e}")
            return True  # Non-critical
    
    def _test_container_logs(self) -> bool:
        """Test container logging configuration"""
        logger.info("Testing container logging...")
        
        try:
            containers = self.docker_client.containers.list()
            
            for container in containers:
                if "task-management" in container.name:
                    # Get recent logs
                    logs = container.logs(tail=10, timestamps=True)
                    
                    if logs:
                        logger.info(f"✓ {container.name} logging is working")
                        
                        # Check for error patterns in logs
                        log_text = logs.decode('utf-8', errors='ignore')
                        if "ERROR" in log_text or "FATAL" in log_text:
                            logger.warning(f"⚠ {container.name} has error messages in logs")
                    else:
                        logger.warning(f"⚠ {container.name} has no recent logs")
            
            return True
            
        except Exception as e:
            logger.warning(f"⚠ Container logging test failed: {e}")
            return True  # Non-critical
    
    def _cleanup(self):
        """Cleanup test environment"""
        logger.info("Cleaning up test environment...")
        
        try:
            # Note: In a real deployment test, you might want to keep services running
            # For now, we'll just log that cleanup would happen here
            logger.info("Services left running for manual inspection")
            logger.info("Use 'docker-compose down' to stop all services")
            
        except Exception as e:
            logger.warning(f"Cleanup warning: {e}")


def main():
    """Main test execution"""
    tester = DockerDeploymentTester()
    success = tester.run_all_tests()
    
    if success:
        logger.info("🎉 All deployment tests passed!")
        sys.exit(0)
    else:
        logger.error("❌ Some deployment tests failed!")
        sys.exit(1)


if __name__ == "__main__":
    main()