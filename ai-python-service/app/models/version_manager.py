"""
Model version management system
"""
import os
import json
import shutil
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from datetime import datetime
import logging

from .metadata import ModelMetadata, ModelConfig, ModelType, ModelStatus, create_model_metadata

logger = logging.getLogger(__name__)


class ModelVersionManager:
    """Manages model versions and directory structure"""
    
    def __init__(self, models_base_path: str = "./models"):
        self.models_base_path = Path(models_base_path)
        self.models_base_path.mkdir(parents=True, exist_ok=True)
        
        # Initialize directory structure
        self._initialize_directory_structure()
        
        # Cache for loaded metadata
        self._metadata_cache: Dict[str, ModelMetadata] = {}
    
    def _initialize_directory_structure(self):
        """Initialize the model directory structure"""
        for model_type in ModelType:
            model_dir = self.models_base_path / model_type.value
            model_dir.mkdir(parents=True, exist_ok=True)
            
            # Create registry file if it doesn't exist
            registry_file = model_dir / "registry.json"
            if not registry_file.exists():
                with open(registry_file, 'w') as f:
                    json.dump({"models": {}}, f, indent=2)
    
    def get_model_directory(self, model_type: ModelType, name: str, version: str) -> Path:
        """Get the directory path for a specific model version"""
        return self.models_base_path / model_type.value / name / version
    
    def get_registry_file(self, model_type: ModelType) -> Path:
        """Get the registry file path for a model type"""
        return self.models_base_path / model_type.value / "registry.json"
    
    def register_model(self, metadata: ModelMetadata) -> bool:
        """Register a new model version"""
        try:
            # Create model directory
            model_dir = self.get_model_directory(
                metadata.model_type, 
                metadata.name, 
                metadata.version
            )
            model_dir.mkdir(parents=True, exist_ok=True)
            
            # Save metadata
            metadata_file = model_dir / "metadata.json"
            with open(metadata_file, 'w') as f:
                json.dump(metadata.dict(), f, indent=2, default=str)
            
            # Update registry
            registry_file = self.get_registry_file(metadata.model_type)
            registry = self._load_registry(metadata.model_type)
            
            model_key = f"{metadata.name}:{metadata.version}"
            registry["models"][model_key] = {
                "name": metadata.name,
                "version": metadata.version,
                "status": metadata.status.value,
                "created_at": metadata.created_at.isoformat(),
                "updated_at": metadata.updated_at.isoformat(),
                "file_path": str(metadata.file_path),
                "memory_requirement_mb": metadata.memory_requirement_mb
            }
            
            with open(registry_file, 'w') as f:
                json.dump(registry, f, indent=2)
            
            # Cache metadata
            self._metadata_cache[model_key] = metadata
            
            logger.info(f"Registered model {model_key} of type {metadata.model_type.value}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to register model {metadata.name}:{metadata.version}: {str(e)}")
            return False
    
    def get_model_metadata(self, model_type: ModelType, name: str, version: str) -> Optional[ModelMetadata]:
        """Get metadata for a specific model version"""
        model_key = f"{name}:{version}"
        
        # Check cache first
        if model_key in self._metadata_cache:
            return self._metadata_cache[model_key]
        
        # Load from file
        model_dir = self.get_model_directory(model_type, name, version)
        metadata_file = model_dir / "metadata.json"
        
        if not metadata_file.exists():
            return None
        
        try:
            with open(metadata_file, 'r') as f:
                data = json.load(f)
            
            # Convert datetime strings back to datetime objects
            if 'created_at' in data:
                data['created_at'] = datetime.fromisoformat(data['created_at'])
            if 'updated_at' in data:
                data['updated_at'] = datetime.fromisoformat(data['updated_at'])
            
            metadata = ModelMetadata.from_dict(data)
            self._metadata_cache[model_key] = metadata
            return metadata
            
        except Exception as e:
            logger.error(f"Failed to load metadata for {model_key}: {str(e)}")
            return None
    
    def list_models(self, model_type: Optional[ModelType] = None) -> List[ModelMetadata]:
        """List all available models, optionally filtered by type"""
        models = []
        
        model_types = [model_type] if model_type else list(ModelType)
        
        for mt in model_types:
            registry = self._load_registry(mt)
            for model_key, model_info in registry["models"].items():
                name, version = model_key.split(":", 1)
                metadata = self.get_model_metadata(mt, name, version)
                if metadata:
                    models.append(metadata)
        
        return models
    
    def list_versions(self, model_type: ModelType, name: str) -> List[str]:
        """List all versions of a specific model"""
        versions = []
        registry = self._load_registry(model_type)
        
        for model_key in registry["models"].keys():
            model_name, version = model_key.split(":", 1)
            if model_name == name:
                versions.append(version)
        
        return sorted(versions, reverse=True)  # Latest first
    
    def get_latest_version(self, model_type: ModelType, name: str) -> Optional[str]:
        """Get the latest version of a model"""
        versions = self.list_versions(model_type, name)
        return versions[0] if versions else None
    
    def delete_model_version(self, model_type: ModelType, name: str, version: str) -> bool:
        """Delete a specific model version"""
        try:
            # Remove from registry
            registry = self._load_registry(model_type)
            model_key = f"{name}:{version}"
            
            if model_key not in registry["models"]:
                logger.warning(f"Model {model_key} not found in registry")
                return False
            
            del registry["models"][model_key]
            
            registry_file = self.get_registry_file(model_type)
            with open(registry_file, 'w') as f:
                json.dump(registry, f, indent=2)
            
            # Remove directory
            model_dir = self.get_model_directory(model_type, name, version)
            if model_dir.exists():
                shutil.rmtree(model_dir)
            
            # Remove from cache
            if model_key in self._metadata_cache:
                del self._metadata_cache[model_key]
            
            logger.info(f"Deleted model {model_key}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete model {name}:{version}: {str(e)}")
            return False
    
    def update_model_status(self, model_type: ModelType, name: str, version: str, status: ModelStatus) -> bool:
        """Update the status of a model"""
        try:
            metadata = self.get_model_metadata(model_type, name, version)
            if not metadata:
                return False
            
            metadata.status = status
            metadata.updated_at = datetime.now()
            
            # Save updated metadata
            model_dir = self.get_model_directory(model_type, name, version)
            metadata_file = model_dir / "metadata.json"
            
            with open(metadata_file, 'w') as f:
                json.dump(metadata.dict(), f, indent=2, default=str)
            
            # Update registry
            registry = self._load_registry(model_type)
            model_key = f"{name}:{version}"
            if model_key in registry["models"]:
                registry["models"][model_key]["status"] = status.value
                registry["models"][model_key]["updated_at"] = metadata.updated_at.isoformat()
                
                registry_file = self.get_registry_file(model_type)
                with open(registry_file, 'w') as f:
                    json.dump(registry, f, indent=2)
            
            # Update cache
            self._metadata_cache[model_key] = metadata
            
            logger.info(f"Updated status of {model_key} to {status.value}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to update status for {name}:{version}: {str(e)}")
            return False
    
    def validate_model_integrity(self, model_type: ModelType, name: str, version: str) -> Tuple[bool, str]:
        """Validate model file integrity"""
        metadata = self.get_model_metadata(model_type, name, version)
        if not metadata:
            return False, "Model metadata not found"
        
        # Check if file exists
        if not os.path.exists(metadata.file_path):
            return False, f"Model file not found: {metadata.file_path}"
        
        # Check file size
        current_size = os.path.getsize(metadata.file_path)
        if current_size != metadata.file_size_bytes:
            return False, f"File size mismatch: expected {metadata.file_size_bytes}, got {current_size}"
        
        # Check file hash
        try:
            from .metadata import calculate_file_hash
            current_hash = calculate_file_hash(metadata.file_path)
            if current_hash != metadata.file_hash:
                return False, f"File hash mismatch: expected {metadata.file_hash}, got {current_hash}"
        except Exception as e:
            return False, f"Error calculating hash: {str(e)}"
        
        return True, "Model integrity validated"
    
    def get_model_config(self, model_type: ModelType, name: str, version: str) -> Optional[ModelConfig]:
        """Get model configuration"""
        metadata = self.get_model_metadata(model_type, name, version)
        if not metadata:
            return None
        
        # Create default config and merge with stored config
        config_data = {
            "model_type": model_type,
            "name": name,
            "version": version
        }
        
        # Merge with stored configuration
        if metadata.config:
            config_data.update(metadata.config)
        
        try:
            return ModelConfig(**config_data)
        except Exception as e:
            logger.error(f"Failed to create config for {name}:{version}: {str(e)}")
            return None
    
    def _load_registry(self, model_type: ModelType) -> Dict:
        """Load registry for a model type"""
        registry_file = self.get_registry_file(model_type)
        
        try:
            with open(registry_file, 'r') as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            # Return empty registry if file doesn't exist or is corrupted
            return {"models": {}}
    
    def get_storage_info(self) -> Dict[str, any]:
        """Get storage information for all models"""
        total_size = 0
        model_count = 0
        
        for model_type in ModelType:
            registry = self._load_registry(model_type)
            for model_key, model_info in registry["models"].items():
                model_count += 1
                if "file_path" in model_info:
                    try:
                        size = os.path.getsize(model_info["file_path"])
                        total_size += size
                    except OSError:
                        pass  # File might not exist
        
        return {
            "total_models": model_count,
            "total_size_bytes": total_size,
            "total_size_mb": round(total_size / (1024 * 1024), 2),
            "models_path": str(self.models_base_path)
        }