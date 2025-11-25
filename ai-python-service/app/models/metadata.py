"""
Model metadata and configuration management
"""
from datetime import datetime
from typing import Dict, Any, Optional, List
from pydantic import BaseModel, validator
from enum import Enum
import hashlib
import json


class ModelType(str, Enum):
    """Supported model types"""
    TASK_PARSER = "task_parser"
    PRIORITIZER = "prioritizer"
    INSIGHTS = "insights"


class ModelStatus(str, Enum):
    """Model status enumeration"""
    AVAILABLE = "available"
    LOADING = "loading"
    ERROR = "error"
    DEPRECATED = "deprecated"


class ModelMetadata(BaseModel):
    """Model metadata information"""
    
    name: str
    version: str
    model_type: ModelType
    description: Optional[str] = None
    
    # File information
    file_path: str
    file_size_bytes: int
    file_hash: str
    
    # Model information
    model_source: str  # e.g., "huggingface", "local", "custom"
    model_id: Optional[str] = None  # e.g., "microsoft/DialoGPT-medium"
    
    # Performance characteristics
    memory_requirement_mb: int
    inference_time_ms: Optional[int] = None
    accuracy_score: Optional[float] = None
    
    # Versioning
    created_at: datetime
    updated_at: datetime
    status: ModelStatus = ModelStatus.AVAILABLE
    
    # Dependencies and requirements
    dependencies: List[str] = []
    python_version: Optional[str] = None
    framework_version: Optional[str] = None
    
    # Configuration
    config: Dict[str, Any] = {}
    
    @validator('accuracy_score')
    def validate_accuracy_score(cls, v):
        if v is not None and (v < 0.0 or v > 1.0):
            raise ValueError('Accuracy score must be between 0.0 and 1.0')
        return v
    
    @validator('file_hash')
    def validate_file_hash(cls, v):
        if len(v) != 64:  # SHA-256 hash length
            raise ValueError('File hash must be a valid SHA-256 hash')
        return v
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert metadata to dictionary"""
        return self.dict()
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'ModelMetadata':
        """Create metadata from dictionary"""
        return cls(**data)


class ModelConfig(BaseModel):
    """Model configuration for loading and inference"""
    
    model_type: ModelType
    name: str
    version: str
    
    # Loading configuration
    device: str = "cpu"  # "cpu", "cuda", "auto"
    precision: str = "float32"  # "float32", "float16", "int8"
    max_memory_mb: Optional[int] = None
    
    # Inference configuration
    max_length: int = 512
    temperature: float = 0.7
    top_p: float = 0.9
    top_k: int = 50
    
    # Caching configuration
    cache_enabled: bool = True
    cache_size: int = 100
    cache_ttl_seconds: int = 3600
    
    # Performance settings
    batch_size: int = 1
    timeout_seconds: int = 30
    
    # Custom parameters
    custom_params: Dict[str, Any] = {}
    
    @validator('temperature')
    def validate_temperature(cls, v):
        if v <= 0.0 or v > 2.0:
            raise ValueError('Temperature must be between 0.0 and 2.0')
        return v
    
    @validator('top_p')
    def validate_top_p(cls, v):
        if v <= 0.0 or v > 1.0:
            raise ValueError('Top-p must be between 0.0 and 1.0')
        return v


def calculate_file_hash(file_path: str) -> str:
    """Calculate SHA-256 hash of a file"""
    hash_sha256 = hashlib.sha256()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_sha256.update(chunk)
        return hash_sha256.hexdigest()
    except FileNotFoundError:
        raise ValueError(f"File not found: {file_path}")
    except Exception as e:
        raise ValueError(f"Error calculating hash for {file_path}: {str(e)}")


def create_model_metadata(
    name: str,
    version: str,
    model_type: ModelType,
    file_path: str,
    model_source: str,
    memory_requirement_mb: int,
    description: Optional[str] = None,
    model_id: Optional[str] = None,
    config: Optional[Dict[str, Any]] = None
) -> ModelMetadata:
    """Create model metadata with file validation"""
    
    import os
    
    if not os.path.exists(file_path):
        raise ValueError(f"Model file not found: {file_path}")
    
    file_size = os.path.getsize(file_path)
    file_hash = calculate_file_hash(file_path)
    
    now = datetime.now()
    
    return ModelMetadata(
        name=name,
        version=version,
        model_type=model_type,
        description=description,
        file_path=file_path,
        file_size_bytes=file_size,
        file_hash=file_hash,
        model_source=model_source,
        model_id=model_id,
        memory_requirement_mb=memory_requirement_mb,
        created_at=now,
        updated_at=now,
        config=config or {}
    )