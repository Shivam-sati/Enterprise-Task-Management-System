"""
Model management API endpoints
"""
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime

from ...models.model_manager import ModelManager
from ...models.version_manager import ModelVersionManager
from ...models.config_loader import ModelConfigLoader
from ...models.metadata import ModelType, ModelStatus

router = APIRouter()

# Global instances
model_manager = ModelManager()
version_manager = ModelVersionManager()
config_loader = ModelConfigLoader()


class ModelInfo(BaseModel):
    """Model information response"""
    name: str
    version: str
    model_type: str
    status: str
    description: Optional[str] = None
    memory_requirement_mb: int
    created_at: datetime
    updated_at: datetime
    file_size_bytes: int
    is_loaded: bool = False


class LoadedModelInfo(BaseModel):
    """Loaded model information"""
    model_type: str
    name: str
    version: str
    loaded_at: Optional[datetime] = None
    memory_usage_mb: int


class ModelRegistrationRequest(BaseModel):
    """Model registration request"""
    name: str
    version: str
    model_type: str
    file_path: str
    model_source: str = "local"
    description: Optional[str] = None
    model_id: Optional[str] = None
    memory_requirement_mb: int = 512
    config: Optional[Dict[str, Any]] = None


class ModelStatusUpdate(BaseModel):
    """Model status update request"""
    status: str


@router.get("/", response_model=List[ModelInfo])
async def list_models(
    model_type: Optional[str] = Query(None, description="Filter by model type"),
    include_loaded_status: bool = Query(True, description="Include loaded status")
):
    """List all registered models"""
    try:
        # Parse model type if provided
        filter_type = None
        if model_type:
            try:
                filter_type = ModelType(model_type)
            except ValueError:
                raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        # Get models from version manager
        models = version_manager.list_models(filter_type)
        
        # Get loaded models for status checking
        loaded_models = set()
        if include_loaded_status:
            for loaded in model_manager.get_loaded_models():
                key = f"{loaded['model_type']}:{loaded['name']}:{loaded['version']}"
                loaded_models.add(key)
        
        # Convert to response format
        result = []
        for model in models:
            model_key = f"{model.model_type.value}:{model.name}:{model.version}"
            is_loaded = model_key in loaded_models if include_loaded_status else False
            
            result.append(ModelInfo(
                name=model.name,
                version=model.version,
                model_type=model.model_type.value,
                status=model.status.value,
                description=model.description,
                memory_requirement_mb=model.memory_requirement_mb,
                created_at=model.created_at,
                updated_at=model.updated_at,
                file_size_bytes=model.file_size_bytes,
                is_loaded=is_loaded
            ))
        
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list models: {str(e)}")


@router.get("/loaded", response_model=List[LoadedModelInfo])
async def list_loaded_models():
    """List currently loaded models"""
    try:
        loaded_models = model_manager.get_loaded_models()
        
        return [
            LoadedModelInfo(
                model_type=model["model_type"],
                name=model["name"],
                version=model["version"],
                loaded_at=model["loaded_at"],
                memory_usage_mb=model["memory_usage_mb"]
            )
            for model in loaded_models
        ]
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list loaded models: {str(e)}")


@router.get("/types")
async def list_model_types():
    """List available model types"""
    return {
        "model_types": [model_type.value for model_type in ModelType],
        "descriptions": {
            ModelType.TASK_PARSER.value: "Models for parsing natural language task descriptions",
            ModelType.PRIORITIZER.value: "Models for task prioritization and ranking",
            ModelType.INSIGHTS.value: "Models for generating productivity insights"
        }
    }


@router.get("/{model_type}/{name}")
async def get_model_versions(model_type: str, name: str):
    """Get all versions of a specific model"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        versions = version_manager.list_versions(mt, name)
        if not versions:
            raise HTTPException(status_code=404, detail=f"Model {name} not found")
        
        # Get detailed information for each version
        version_details = []
        for version in versions:
            metadata = version_manager.get_model_metadata(mt, name, version)
            if metadata:
                model_key = f"{model_type}:{name}:{version}"
                is_loaded = model_manager.is_model_loaded(mt, name, version)
                
                version_details.append({
                    "version": version,
                    "status": metadata.status.value,
                    "description": metadata.description,
                    "memory_requirement_mb": metadata.memory_requirement_mb,
                    "created_at": metadata.created_at,
                    "updated_at": metadata.updated_at,
                    "file_size_bytes": metadata.file_size_bytes,
                    "is_loaded": is_loaded
                })
        
        latest_version = version_manager.get_latest_version(mt, name)
        
        return {
            "name": name,
            "model_type": model_type,
            "versions": version_details,
            "latest_version": latest_version,
            "total_versions": len(versions)
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get model versions: {str(e)}")


@router.get("/{model_type}/{name}/{version}")
async def get_model_details(model_type: str, name: str, version: str):
    """Get detailed information about a specific model version"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        metadata = version_manager.get_model_metadata(mt, name, version)
        if not metadata:
            raise HTTPException(status_code=404, detail=f"Model {name}:{version} not found")
        
        # Check if loaded
        is_loaded = model_manager.is_model_loaded(mt, name, version)
        
        # Validate integrity
        is_valid, integrity_message = version_manager.validate_model_integrity(mt, name, version)
        
        # Get configuration
        config = version_manager.get_model_config(mt, name, version)
        
        return {
            "name": metadata.name,
            "version": metadata.version,
            "model_type": metadata.model_type.value,
            "status": metadata.status.value,
            "description": metadata.description,
            "model_source": metadata.model_source,
            "model_id": metadata.model_id,
            "file_path": metadata.file_path,
            "file_size_bytes": metadata.file_size_bytes,
            "file_hash": metadata.file_hash,
            "memory_requirement_mb": metadata.memory_requirement_mb,
            "created_at": metadata.created_at,
            "updated_at": metadata.updated_at,
            "dependencies": metadata.dependencies,
            "is_loaded": is_loaded,
            "integrity_valid": is_valid,
            "integrity_message": integrity_message,
            "config": config.dict() if config else None
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get model details: {str(e)}")


@router.post("/register", response_model=Dict[str, Any])
async def register_model(request: ModelRegistrationRequest):
    """Register a new model version"""
    try:
        # Validate model type
        try:
            model_type = ModelType(request.model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {request.model_type}")
        
        # Register the model
        success = model_manager.register_model_from_file(
            model_type=model_type,
            name=request.name,
            version=request.version,
            file_path=request.file_path,
            model_source=request.model_source,
            description=request.description,
            model_id=request.model_id,
            memory_requirement_mb=request.memory_requirement_mb,
            config=request.config
        )
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to register model")
        
        return {
            "message": f"Model {request.name}:{request.version} registered successfully",
            "model_type": request.model_type,
            "name": request.name,
            "version": request.version
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to register model: {str(e)}")


@router.post("/{model_type}/{name}/{version}/load")
async def load_model(model_type: str, name: str, version: str):
    """Load a model into memory"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        success = model_manager.load_model(mt, name, version)
        if not success:
            raise HTTPException(status_code=400, detail=f"Failed to load model {name}:{version}")
        
        return {
            "message": f"Model {name}:{version} loaded successfully",
            "model_type": model_type,
            "name": name,
            "version": version
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to load model: {str(e)}")


@router.post("/{model_type}/{name}/{version}/unload")
async def unload_model(model_type: str, name: str, version: str):
    """Unload a model from memory"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        success = model_manager.unload_model(mt, name, version)
        if not success:
            raise HTTPException(status_code=400, detail=f"Failed to unload model {name}:{version}")
        
        return {
            "message": f"Model {name}:{version} unloaded successfully",
            "model_type": model_type,
            "name": name,
            "version": version
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to unload model: {str(e)}")


@router.put("/{model_type}/{name}/{version}/status")
async def update_model_status(
    model_type: str, 
    name: str, 
    version: str, 
    status_update: ModelStatusUpdate
):
    """Update model status"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        # Validate status
        try:
            status = ModelStatus(status_update.status)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid status: {status_update.status}")
        
        success = version_manager.update_model_status(mt, name, version, status)
        if not success:
            raise HTTPException(status_code=400, detail=f"Failed to update status for {name}:{version}")
        
        return {
            "message": f"Status updated for {name}:{version}",
            "model_type": model_type,
            "name": name,
            "version": version,
            "new_status": status_update.status
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to update model status: {str(e)}")


@router.delete("/{model_type}/{name}/{version}")
async def delete_model_version(model_type: str, name: str, version: str):
    """Delete a specific model version"""
    try:
        # Validate model type
        try:
            mt = ModelType(model_type)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
        
        # Unload model if it's loaded
        if model_manager.is_model_loaded(mt, name, version):
            model_manager.unload_model(mt, name, version)
        
        # Delete the model version
        success = version_manager.delete_model_version(mt, name, version)
        if not success:
            raise HTTPException(status_code=400, detail=f"Failed to delete model {name}:{version}")
        
        return {
            "message": f"Model {name}:{version} deleted successfully",
            "model_type": model_type,
            "name": name,
            "version": version
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to delete model: {str(e)}")


@router.get("/stats/performance")
async def get_performance_stats():
    """Get model performance statistics"""
    try:
        stats = model_manager.get_performance_stats()
        memory_usage = model_manager.get_memory_usage()
        storage_info = version_manager.get_storage_info()
        
        return {
            "performance_stats": stats,
            "memory_usage": memory_usage,
            "storage_info": storage_info
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get performance stats: {str(e)}")


@router.get("/config/available")
async def get_available_models_from_config():
    """Get available models from configuration"""
    try:
        available = config_loader.list_available_models()
        global_config = config_loader.get_global_config()
        
        return {
            "available_models": available,
            "global_config": global_config
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get available models: {str(e)}")


@router.post("/config/initialize")
async def initialize_models_from_config():
    """Initialize models from configuration file"""
    try:
        results = config_loader.initialize_models_from_config(version_manager)
        
        success_count = sum(1 for success in results.values() if success)
        total_count = len(results)
        
        return {
            "message": f"Initialized {success_count}/{total_count} models from configuration",
            "results": results,
            "success_count": success_count,
            "total_count": total_count
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to initialize models: {str(e)}")


@router.post("/config/reload")
async def reload_configuration():
    """Reload model configuration from file"""
    try:
        success = config_loader.reload_config()
        if not success:
            raise HTTPException(status_code=400, detail="Failed to reload configuration")
        
        # Validate the reloaded configuration
        errors = config_loader.validate_config()
        if errors:
            return {
                "message": "Configuration reloaded with validation errors",
                "validation_errors": errors
            }
        
        return {
            "message": "Configuration reloaded successfully",
            "validation_errors": []
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to reload configuration: {str(e)}")