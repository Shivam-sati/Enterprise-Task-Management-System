"""
Enhanced Model Manager with versioning support
"""
import os
import logging
from typing import Dict, Optional, Any, List
from functools import lru_cache
from datetime import datetime, timedelta
import asyncio
import threading
from concurrent.futures import ThreadPoolExecutor

from .version_manager import ModelVersionManager
from .metadata import ModelMetadata, ModelConfig, ModelType, ModelStatus
from ..config.settings import settings
from ..services.metrics_service import metrics_service

logger = logging.getLogger(__name__)


class ModelManager:
    """Enhanced model manager with versioning and caching support"""
    
    def __init__(self, models_path: Optional[str] = None):
        self.models_path = models_path or settings.models_path
        self.version_manager = ModelVersionManager(self.models_path)
        
        # Loaded models cache
        self._loaded_models: Dict[str, Any] = {}
        self._model_configs: Dict[str, ModelConfig] = {}
        self._load_times: Dict[str, datetime] = {}
        
        # Thread pool for model operations
        self._executor = ThreadPoolExecutor(max_workers=2)
        self._loading_locks: Dict[str, threading.Lock] = {}
        
        # Performance tracking
        self._inference_stats: Dict[str, List[float]] = {}
        
        logger.info(f"ModelManager initialized with path: {self.models_path}")
    
    def get_model_key(self, model_type: ModelType, name: str, version: str) -> str:
        """Generate a unique key for a model"""
        return f"{model_type.value}:{name}:{version}"
    
    def register_model_from_file(
        self,
        model_type: ModelType,
        name: str,
        version: str,
        file_path: str,
        model_source: str = "local",
        description: Optional[str] = None,
        model_id: Optional[str] = None,
        memory_requirement_mb: int = 512,
        config: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Register a model from a file"""
        try:
            from .metadata import create_model_metadata
            
            metadata = create_model_metadata(
                name=name,
                version=version,
                model_type=model_type,
                file_path=file_path,
                model_source=model_source,
                description=description,
                model_id=model_id,
                memory_requirement_mb=memory_requirement_mb,
                config=config
            )
            
            return self.version_manager.register_model(metadata)
            
        except Exception as e:
            logger.error(f"Failed to register model {name}:{version}: {str(e)}")
            return False
    
    def load_model(self, model_type: ModelType, name: str, version: Optional[str] = None) -> bool:
        """Load a model into memory"""
        # Use latest version if not specified
        if version is None:
            version = self.version_manager.get_latest_version(model_type, name)
            if version is None:
                logger.error(f"No versions found for model {name} of type {model_type.value}")
                return False
        
        model_key = self.get_model_key(model_type, name, version)
        
        # Check if already loaded
        if model_key in self._loaded_models:
            logger.info(f"Model {model_key} already loaded")
            return True
        
        # Get or create loading lock
        if model_key not in self._loading_locks:
            self._loading_locks[model_key] = threading.Lock()
        
        with self._loading_locks[model_key]:
            # Double-check after acquiring lock
            if model_key in self._loaded_models:
                return True
            
            try:
                # Get model metadata and config
                metadata = self.version_manager.get_model_metadata(model_type, name, version)
                if not metadata:
                    logger.error(f"Metadata not found for model {model_key}")
                    return False
                
                # Validate model integrity
                is_valid, message = self.version_manager.validate_model_integrity(model_type, name, version)
                if not is_valid:
                    logger.error(f"Model integrity check failed for {model_key}: {message}")
                    self.version_manager.update_model_status(model_type, name, version, ModelStatus.ERROR)
                    return False
                
                # Update status to loading
                self.version_manager.update_model_status(model_type, name, version, ModelStatus.LOADING)
                
                # Load model configuration
                config = self.version_manager.get_model_config(model_type, name, version)
                if not config:
                    logger.error(f"Configuration not found for model {model_key}")
                    return False
                
                # Simulate model loading (placeholder for actual model loading)
                # In a real implementation, this would load the actual model using transformers, etc.
                mock_model = {
                    "name": name,
                    "version": version,
                    "type": model_type.value,
                    "loaded_at": datetime.now(),
                    "metadata": metadata,
                    "config": config,
                    "status": "loaded"
                }
                
                # Store loaded model and config
                self._loaded_models[model_key] = mock_model
                self._model_configs[model_key] = config
                self._load_times[model_key] = datetime.now()
                
                # Update status to available
                self.version_manager.update_model_status(model_type, name, version, ModelStatus.AVAILABLE)
                
                # Update metrics
                metrics_service.set_active_models(len(self._loaded_models))
                metrics_service.set_model_memory_usage(
                    model_type.value, 
                    version, 
                    metadata.memory_requirement_mb * 1024 * 1024  # Convert MB to bytes
                )
                
                logger.info(f"Successfully loaded model {model_key}")
                return True
                
            except Exception as e:
                logger.error(f"Failed to load model {model_key}: {str(e)}")
                self.version_manager.update_model_status(model_type, name, version, ModelStatus.ERROR)
                return False
    
    def unload_model(self, model_type: ModelType, name: str, version: str) -> bool:
        """Unload a model from memory"""
        model_key = self.get_model_key(model_type, name, version)
        
        if model_key not in self._loaded_models:
            logger.warning(f"Model {model_key} is not loaded")
            return False
        
        try:
            # Remove from caches
            del self._loaded_models[model_key]
            if model_key in self._model_configs:
                del self._model_configs[model_key]
            if model_key in self._load_times:
                del self._load_times[model_key]
            if model_key in self._inference_stats:
                del self._inference_stats[model_key]
            
            # Update metrics
            metrics_service.set_active_models(len(self._loaded_models))
            
            logger.info(f"Successfully unloaded model {model_key}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to unload model {model_key}: {str(e)}")
            return False
    
    def is_model_loaded(self, model_type: ModelType, name: str, version: str) -> bool:
        """Check if a model is loaded"""
        model_key = self.get_model_key(model_type, name, version)
        return model_key in self._loaded_models
    
    def get_loaded_models(self) -> List[Dict[str, Any]]:
        """Get list of all loaded models"""
        loaded = []
        for model_key, model in self._loaded_models.items():
            model_type, name_version = model_key.split(":", 1)
            name, version = name_version.rsplit(":", 1)
            
            loaded.append({
                "model_type": model_type,
                "name": name,
                "version": version,
                "loaded_at": self._load_times.get(model_key),
                "memory_usage_mb": model["metadata"].memory_requirement_mb if "metadata" in model else 0
            })
        
        return loaded
    
    @lru_cache(maxsize=settings.cache_size)
    def process_task_parsing(self, text: str, model_name: str = "default", version: Optional[str] = None) -> Dict[str, Any]:
        """Process task parsing with caching"""
        start_time = datetime.now()
        
        try:
            # Import TaskParser here to avoid circular imports
            from ..ai.task_parser import TaskParser
            
            # Initialize task parser (lightweight, no model loading needed)
            parser = TaskParser()
            
            # Parse the task
            result = parser.parse_task(text)
            
            # Convert to dictionary format
            result_dict = parser.to_dict(result)
            
            # Track performance
            processing_time = (datetime.now() - start_time).total_seconds()
            model_key = self.get_model_key(ModelType.TASK_PARSER, model_name, version or "latest")
            self._track_inference_time(model_key, processing_time * 1000)  # Store in ms
            
            # Record metrics
            metrics_service.record_model_inference(
                model_type="task_parser",
                model_version=version or "latest",
                duration=processing_time,
                status="success"
            )
            
            logger.info(f"Task parsing completed in {processing_time * 1000:.2f}ms")
            return result_dict
            
        except Exception as e:
            logger.error(f"Task parsing failed: {str(e)}")
            
            # Record error metrics
            processing_time = (datetime.now() - start_time).total_seconds()
            metrics_service.record_model_inference(
                model_type="task_parser",
                model_version=version or "latest",
                duration=processing_time,
                status="error"
            )
            
            # Return fallback result
            return {
                "title": text[:50] + "..." if len(text) > 50 else text,
                "description": text,
                "priority": "medium",
                "estimated_hours": 1.0,
                "tags": ["error"],
                "confidence": 0.1,
                "category": None
            }
    
    @lru_cache(maxsize=settings.cache_size)
    def process_task_prioritization(self, tasks_json: str, model_name: str = "default", version: Optional[str] = None) -> Dict[str, Any]:
        """Process task prioritization with caching"""
        start_time = datetime.now()
        
        try:
            # Import TaskPrioritizer here to avoid circular imports
            from ..ai.task_prioritizer import TaskPrioritizer
            import json
            
            # Parse tasks from JSON
            tasks = json.loads(tasks_json)
            
            # Initialize task prioritizer
            prioritizer = TaskPrioritizer()
            
            # Prioritize the tasks
            result = prioritizer.prioritize_tasks(tasks)
            
            # Convert to dictionary format
            result_dict = prioritizer.to_dict(result)
            
            # Track performance
            processing_time = (datetime.now() - start_time).total_seconds()
            model_key = self.get_model_key(ModelType.PRIORITIZER, model_name, version or "latest")
            self._track_inference_time(model_key, processing_time * 1000)  # Store in ms
            
            # Record metrics
            metrics_service.record_model_inference(
                model_type="prioritizer",
                model_version=version or "latest",
                duration=processing_time,
                status="success"
            )
            
            logger.info(f"Task prioritization completed in {processing_time * 1000:.2f}ms")
            return result_dict
            
        except Exception as e:
            logger.error(f"Task prioritization failed: {str(e)}")
            
            # Record error metrics
            processing_time = (datetime.now() - start_time).total_seconds()
            metrics_service.record_model_inference(
                model_type="prioritizer",
                model_version=version or "latest",
                duration=processing_time,
                status="error"
            )
            
            # Return fallback result
            try:
                import json
                tasks = json.loads(tasks_json)
                return {
                    "prioritized_tasks": [
                        {
                            "task_id": str(i),
                            "priority_score": 0.5,
                            "urgency": "medium",
                            "importance": "medium",
                            "reasoning": "Fallback prioritization due to error",
                            "factors": ["error"],
                            "confidence": 0.1
                        }
                        for i, task in enumerate(tasks)
                    ],
                    "reasoning": "Prioritization failed, using fallback ordering",
                    "confidence": 0.1,
                    "factors_considered": ["error"],
                    "total_tasks": len(tasks),
                    "processing_time_ms": 0.0
                }
            except:
                return {
                    "prioritized_tasks": [],
                    "reasoning": "Failed to parse tasks",
                    "confidence": 0.0,
                    "factors_considered": ["error"],
                    "total_tasks": 0,
                    "processing_time_ms": 0.0
                }
    
    @lru_cache(maxsize=settings.cache_size)
    def process_insights_generation(self, data_json: str, model_name: str = "default", version: Optional[str] = None) -> Dict[str, Any]:
        """Process insights generation with caching"""
        start_time = datetime.now()
        
        try:
            # Import InsightGenerator here to avoid circular imports
            from ..ai.insight_generator import InsightGenerator
            import json
            
            # Parse task data from JSON
            task_data = json.loads(data_json)
            
            # Initialize insight generator
            generator = InsightGenerator()
            
            # Generate insights
            result = generator.generate_insights(task_data)
            
            # Convert to dictionary format
            result_dict = generator.to_dict(result)
            
            # Track performance
            processing_time = (datetime.now() - start_time).total_seconds()
            model_key = self.get_model_key(ModelType.INSIGHTS, model_name, version or "latest")
            self._track_inference_time(model_key, processing_time * 1000)  # Store in ms
            
            # Record metrics
            metrics_service.record_model_inference(
                model_type="insights",
                model_version=version or "latest",
                duration=processing_time,
                status="success"
            )
            
            logger.info(f"Insights generation completed in {processing_time * 1000:.2f}ms")
            return result_dict
            
        except Exception as e:
            logger.error(f"Insights generation failed: {str(e)}")
            
            # Record error metrics
            processing_time = (datetime.now() - start_time).total_seconds()
            metrics_service.record_model_inference(
                model_type="insights",
                model_version=version or "latest",
                duration=processing_time,
                status="error"
            )
            
            # Return fallback result
            return {
                "insights": [
                    "Focus on completing one task at a time to build momentum",
                    "Break large tasks into smaller, manageable pieces",
                    "Set specific time blocks for different types of work"
                ],
                "recommendations": [
                    "Track your tasks consistently to enable better insights",
                    "Note completion times to improve future planning",
                    "Categorize tasks to identify patterns over time"
                ],
                "patterns": {
                    "total_patterns": 0,
                    "pattern_types": [],
                    "strongest_pattern": None,
                    "average_confidence": 0.0
                },
                "confidence": 0.3,
                "analysis_period": "Unknown",
                "data_quality": f"Analysis failed: {str(e)}",
                "detailed_insights": [],
                "detected_patterns": []
            }
    
    def _track_inference_time(self, model_key: str, time_ms: float):
        """Track inference time for performance monitoring"""
        if model_key not in self._inference_stats:
            self._inference_stats[model_key] = []
        
        self._inference_stats[model_key].append(time_ms)
        
        # Keep only last 100 measurements
        if len(self._inference_stats[model_key]) > 100:
            self._inference_stats[model_key] = self._inference_stats[model_key][-100:]
    
    def get_performance_stats(self) -> Dict[str, Any]:
        """Get performance statistics for all models"""
        stats = {}
        
        for model_key, times in self._inference_stats.items():
            if times:
                stats[model_key] = {
                    "avg_inference_time_ms": sum(times) / len(times),
                    "min_inference_time_ms": min(times),
                    "max_inference_time_ms": max(times),
                    "total_inferences": len(times)
                }
        
        return stats
    
    def cleanup_old_models(self, max_age_days: int = 30):
        """Clean up old unused models"""
        cutoff_date = datetime.now() - timedelta(days=max_age_days)
        
        # Create a list of keys to avoid modifying dict during iteration
        old_models = []
        for model_key, load_time in self._load_times.items():
            if load_time < cutoff_date:
                old_models.append(model_key)
        
        for model_key in old_models:
            model_type_str, name_version = model_key.split(":", 1)
            name, version = name_version.rsplit(":", 1)
            model_type = ModelType(model_type_str)
            
            logger.info(f"Unloading old model {model_key}")
            self.unload_model(model_type, name, version)
    
    def get_memory_usage(self) -> Dict[str, Any]:
        """Get memory usage information"""
        total_memory_mb = 0
        model_count = len(self._loaded_models)
        
        for model in self._loaded_models.values():
            if "metadata" in model:
                total_memory_mb += model["metadata"].memory_requirement_mb
        
        return {
            "loaded_models_count": model_count,
            "total_memory_mb": total_memory_mb,
            "average_memory_per_model_mb": total_memory_mb / model_count if model_count > 0 else 0
        }
    
    def shutdown(self):
        """Shutdown the model manager"""
        logger.info("Shutting down ModelManager")
        
        # Unload all models
        for model_key in list(self._loaded_models.keys()):
            model_type_str, name_version = model_key.split(":", 1)
            name, version = name_version.rsplit(":", 1)
            model_type = ModelType(model_type_str)
            self.unload_model(model_type, name, version)
        
        # Shutdown thread pool
        self._executor.shutdown(wait=True)
        
        logger.info("ModelManager shutdown complete")