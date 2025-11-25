"""
Model configuration loader and management
"""
import json
import os
from pathlib import Path
from typing import Dict, Any, Optional, List
import logging

from .metadata import ModelType, ModelConfig, create_model_metadata
from .version_manager import ModelVersionManager

logger = logging.getLogger(__name__)


class ModelConfigLoader:
    """Loads and manages model configurations from JSON files"""
    
    def __init__(self, config_path: str = "./config/models.json"):
        self.config_path = Path(config_path)
        self.config_data: Dict[str, Any] = {}
        self.load_config()
    
    def load_config(self) -> bool:
        """Load model configuration from JSON file"""
        try:
            if not self.config_path.exists():
                logger.warning(f"Model config file not found: {self.config_path}")
                self._create_default_config()
                return False
            
            with open(self.config_path, 'r') as f:
                self.config_data = json.load(f)
            
            logger.info(f"Loaded model configuration from {self.config_path}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to load model config: {str(e)}")
            self._create_default_config()
            return False
    
    def _create_default_config(self):
        """Create default configuration"""
        self.config_data = {
            "model_definitions": {
                "task_parser": {
                    "default": {
                        "versions": {
                            "v1": {
                                "model_source": "mock",
                                "description": "Mock task parsing model",
                                "memory_requirement_mb": 128,
                                "config": {
                                    "max_length": 512,
                                    "temperature": 0.7
                                }
                            }
                        },
                        "default_version": "v1"
                    }
                }
            },
            "global_config": {
                "cache_enabled": True,
                "cache_size": 100,
                "timeout_seconds": 30
            }
        }
    
    def get_model_definition(self, model_type: ModelType, name: str) -> Optional[Dict[str, Any]]:
        """Get model definition for a specific model type and name"""
        try:
            return self.config_data["model_definitions"][model_type.value][name]
        except KeyError:
            logger.warning(f"Model definition not found: {model_type.value}/{name}")
            return None
    
    def get_version_definition(self, model_type: ModelType, name: str, version: str) -> Optional[Dict[str, Any]]:
        """Get version definition for a specific model"""
        model_def = self.get_model_definition(model_type, name)
        if not model_def:
            return None
        
        try:
            return model_def["versions"][version]
        except KeyError:
            logger.warning(f"Version definition not found: {model_type.value}/{name}:{version}")
            return None
    
    def get_default_version(self, model_type: ModelType, name: str) -> Optional[str]:
        """Get default version for a model"""
        model_def = self.get_model_definition(model_type, name)
        if not model_def:
            return None
        
        return model_def.get("default_version")
    
    def list_available_models(self, model_type: Optional[ModelType] = None) -> Dict[str, List[str]]:
        """List all available models and their versions"""
        available = {}
        
        model_types = [model_type] if model_type else list(ModelType)
        
        for mt in model_types:
            available[mt.value] = []
            
            if mt.value in self.config_data.get("model_definitions", {}):
                for model_name, model_def in self.config_data["model_definitions"][mt.value].items():
                    versions = list(model_def.get("versions", {}).keys())
                    available[mt.value].append({
                        "name": model_name,
                        "versions": versions,
                        "default_version": model_def.get("default_version")
                    })
        
        return available
    
    def create_model_config(self, model_type: ModelType, name: str, version: str) -> Optional[ModelConfig]:
        """Create ModelConfig from configuration definition"""
        version_def = self.get_version_definition(model_type, name, version)
        if not version_def:
            return None
        
        try:
            # Start with global config
            config_data = self.config_data.get("global_config", {}).copy()
            
            # Override with model-specific config
            if "config" in version_def:
                config_data.update(version_def["config"])
            
            # Add required fields
            config_data.update({
                "model_type": model_type,
                "name": name,
                "version": version
            })
            
            return ModelConfig(**config_data)
            
        except Exception as e:
            logger.error(f"Failed to create config for {model_type.value}/{name}:{version}: {str(e)}")
            return None
    
    def initialize_models_from_config(self, version_manager: ModelVersionManager) -> Dict[str, bool]:
        """Initialize models from configuration definitions"""
        results = {}
        
        for model_type_str, models in self.config_data.get("model_definitions", {}).items():
            try:
                model_type = ModelType(model_type_str)
            except ValueError:
                logger.warning(f"Unknown model type: {model_type_str}")
                continue
            
            for model_name, model_def in models.items():
                for version, version_def in model_def.get("versions", {}).items():
                    model_key = f"{model_type_str}:{model_name}:{version}"
                    
                    try:
                        # Check if model is already registered
                        existing = version_manager.get_model_metadata(model_type, model_name, version)
                        if existing:
                            logger.info(f"Model {model_key} already registered")
                            results[model_key] = True
                            continue
                        
                        # Create mock model file for configuration-based models
                        if version_def.get("model_source") == "mock":
                            success = self._create_mock_model(
                                version_manager, model_type, model_name, version, version_def
                            )
                            results[model_key] = success
                        else:
                            # For real models, just register the metadata without file
                            logger.info(f"Skipping file-based registration for {model_key} (requires actual model file)")
                            results[model_key] = False
                        
                    except Exception as e:
                        logger.error(f"Failed to initialize model {model_key}: {str(e)}")
                        results[model_key] = False
        
        return results
    
    def _create_mock_model(
        self, 
        version_manager: ModelVersionManager, 
        model_type: ModelType, 
        name: str, 
        version: str, 
        version_def: Dict[str, Any]
    ) -> bool:
        """Create a mock model for testing purposes"""
        try:
            # Create mock model directory
            model_dir = version_manager.get_model_directory(model_type, name, version)
            model_dir.mkdir(parents=True, exist_ok=True)
            
            # Create mock model file
            mock_file = model_dir / "model.json"
            mock_data = {
                "type": "mock_model",
                "name": name,
                "version": version,
                "model_type": model_type.value,
                "created_at": "2024-01-01T00:00:00"
            }
            
            with open(mock_file, 'w') as f:
                json.dump(mock_data, f, indent=2)
            
            # Create metadata
            metadata = create_model_metadata(
                name=name,
                version=version,
                model_type=model_type,
                file_path=str(mock_file),
                model_source=version_def.get("model_source", "mock"),
                description=version_def.get("description", f"Mock {model_type.value} model"),
                memory_requirement_mb=version_def.get("memory_requirement_mb", 128),
                config=version_def.get("config", {})
            )
            
            return version_manager.register_model(metadata)
            
        except Exception as e:
            logger.error(f"Failed to create mock model {name}:{version}: {str(e)}")
            return False
    
    def validate_config(self) -> List[str]:
        """Validate the loaded configuration"""
        errors = []
        
        if not self.config_data:
            errors.append("No configuration data loaded")
            return errors
        
        # Check required sections
        if "model_definitions" not in self.config_data:
            errors.append("Missing 'model_definitions' section")
            return errors
        
        # Validate model definitions
        for model_type_str, models in self.config_data["model_definitions"].items():
            # Check if model type is valid
            try:
                ModelType(model_type_str)
            except ValueError:
                errors.append(f"Invalid model type: {model_type_str}")
                continue
            
            for model_name, model_def in models.items():
                if "versions" not in model_def:
                    errors.append(f"Missing versions for {model_type_str}/{model_name}")
                    continue
                
                if "default_version" not in model_def:
                    errors.append(f"Missing default_version for {model_type_str}/{model_name}")
                
                default_version = model_def.get("default_version")
                if default_version and default_version not in model_def["versions"]:
                    errors.append(f"Default version '{default_version}' not found in versions for {model_type_str}/{model_name}")
                
                # Validate version definitions
                for version, version_def in model_def["versions"].items():
                    if "model_source" not in version_def:
                        errors.append(f"Missing model_source for {model_type_str}/{model_name}:{version}")
                    
                    if "memory_requirement_mb" not in version_def:
                        errors.append(f"Missing memory_requirement_mb for {model_type_str}/{model_name}:{version}")
        
        return errors
    
    def get_global_config(self) -> Dict[str, Any]:
        """Get global configuration settings"""
        return self.config_data.get("global_config", {})
    
    def reload_config(self) -> bool:
        """Reload configuration from file"""
        return self.load_config()