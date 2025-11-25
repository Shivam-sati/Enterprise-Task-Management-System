"""
Application settings and configuration
"""
import os
from typing import Optional
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    """Application settings"""
    
    # Service configuration
    service_name: str = "ai-service-python"
    port: int = 8087
    debug: bool = False
    
    # Eureka configuration
    eureka_server_url: str = "http://eureka-server:8761/eureka"
    eureka_enabled: bool = True
    eureka_renewal_interval: int = 30
    eureka_duration: int = 90
    
    # Model configuration
    models_path: str = "./models"
    cache_size: int = 100
    model_timeout_seconds: int = 30
    
    # Model versioning configuration
    default_task_parser_model: str = "default"
    default_task_parser_version: Optional[str] = None
    default_prioritizer_model: str = "default"
    default_prioritizer_version: Optional[str] = None
    default_insights_model: str = "default"
    default_insights_version: Optional[str] = None
    
    # Model management settings
    auto_cleanup_enabled: bool = True
    model_max_age_days: int = 30
    max_loaded_models: int = 5
    
    # Performance settings
    max_concurrent_requests: int = 10
    memory_limit_mb: int = 2048
    
    # Redis configuration
    redis_enabled: bool = False
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_ttl_seconds: int = 3600
    
    # Observability
    metrics_enabled: bool = True
    prometheus_port: int = 9090
    
    class Config:
        env_file = ".env"
        env_prefix = "AI_SERVICE_"

# Global settings instance
settings = Settings()