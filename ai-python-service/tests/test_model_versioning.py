"""
Tests for model versioning system
"""
import pytest
import tempfile
import shutil
import json
import os
from datetime import datetime
from pathlib import Path

from app.models.metadata import (
    ModelMetadata, ModelConfig, ModelType, ModelStatus, 
    create_model_metadata, calculate_file_hash
)
from app.models.version_manager import ModelVersionManager
from app.models.config_loader import ModelConfigLoader


class TestModelMetadata:
    """Test model metadata functionality"""
    
    def test_model_metadata_creation(self):
        """Test creating model metadata"""
        metadata = ModelMetadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path="/path/to/model.bin",
            file_size_bytes=1024,
            file_hash="a" * 64,  # Valid SHA-256 hash
            model_source="huggingface",
            memory_requirement_mb=512,
            created_at=datetime.now(),
            updated_at=datetime.now()
        )
        
        assert metadata.name == "test_model"
        assert metadata.version == "v1"
        assert metadata.model_type == ModelType.TASK_PARSER
        assert metadata.status == ModelStatus.AVAILABLE
    
    def test_model_metadata_validation(self):
        """Test model metadata validation"""
        # Test invalid accuracy score
        with pytest.raises(ValueError, match="Accuracy score must be between 0.0 and 1.0"):
            ModelMetadata(
                name="test",
                version="v1",
                model_type=ModelType.TASK_PARSER,
                file_path="/path/to/model.bin",
                file_size_bytes=1024,
                file_hash="a" * 64,
                model_source="test",
                memory_requirement_mb=512,
                created_at=datetime.now(),
                updated_at=datetime.now(),
                accuracy_score=1.5
            )
        
        # Test invalid file hash
        with pytest.raises(ValueError, match="File hash must be a valid SHA-256 hash"):
            ModelMetadata(
                name="test",
                version="v1",
                model_type=ModelType.TASK_PARSER,
                file_path="/path/to/model.bin",
                file_size_bytes=1024,
                file_hash="invalid_hash",
                model_source="test",
                memory_requirement_mb=512,
                created_at=datetime.now(),
                updated_at=datetime.now()
            )
    
    def test_model_config_creation(self):
        """Test creating model configuration"""
        config = ModelConfig(
            model_type=ModelType.TASK_PARSER,
            name="test_model",
            version="v1",
            temperature=0.7,
            top_p=0.9
        )
        
        assert config.model_type == ModelType.TASK_PARSER
        assert config.temperature == 0.7
        assert config.top_p == 0.9
        assert config.device == "cpu"  # default value
    
    def test_model_config_validation(self):
        """Test model configuration validation"""
        # Test invalid temperature
        with pytest.raises(ValueError, match="Temperature must be between 0.0 and 2.0"):
            ModelConfig(
                model_type=ModelType.TASK_PARSER,
                name="test",
                version="v1",
                temperature=3.0
            )
        
        # Test invalid top_p
        with pytest.raises(ValueError, match="Top-p must be between 0.0 and 1.0"):
            ModelConfig(
                model_type=ModelType.TASK_PARSER,
                name="test",
                version="v1",
                top_p=1.5
            )


class TestModelVersionManager:
    """Test model version manager functionality"""
    
    @pytest.fixture
    def temp_models_dir(self):
        """Create temporary models directory"""
        temp_dir = tempfile.mkdtemp()
        yield temp_dir
        shutil.rmtree(temp_dir)
    
    @pytest.fixture
    def version_manager(self, temp_models_dir):
        """Create version manager with temporary directory"""
        return ModelVersionManager(temp_models_dir)
    
    @pytest.fixture
    def sample_model_file(self, temp_models_dir):
        """Create a sample model file"""
        model_file = Path(temp_models_dir) / "sample_model.json"
        sample_data = {"type": "test_model", "version": "v1"}
        
        with open(model_file, 'w') as f:
            json.dump(sample_data, f)
        
        return str(model_file)
    
    def test_directory_initialization(self, version_manager):
        """Test that directory structure is created correctly"""
        for model_type in ModelType:
            model_dir = version_manager.models_base_path / model_type.value
            assert model_dir.exists()
            
            registry_file = model_dir / "registry.json"
            assert registry_file.exists()
    
    def test_model_registration(self, version_manager, sample_model_file):
        """Test model registration"""
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        
        success = version_manager.register_model(metadata)
        assert success
        
        # Check that model directory was created
        model_dir = version_manager.get_model_directory(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert model_dir.exists()
        
        # Check that metadata file was created
        metadata_file = model_dir / "metadata.json"
        assert metadata_file.exists()
        
        # Check registry was updated
        registry = version_manager._load_registry(ModelType.TASK_PARSER)
        assert "test_model:v1" in registry["models"]
    
    def test_model_retrieval(self, version_manager, sample_model_file):
        """Test model metadata retrieval"""
        # Register a model first
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        version_manager.register_model(metadata)
        
        # Retrieve the model
        retrieved = version_manager.get_model_metadata(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        
        assert retrieved is not None
        assert retrieved.name == "test_model"
        assert retrieved.version == "v1"
        assert retrieved.model_type == ModelType.TASK_PARSER
    
    def test_list_models(self, version_manager, sample_model_file):
        """Test listing models"""
        # Register multiple models
        for i in range(3):
            metadata = create_model_metadata(
                name=f"model_{i}",
                version="v1",
                model_type=ModelType.TASK_PARSER,
                file_path=sample_model_file,
                model_source="test",
                memory_requirement_mb=256
            )
            version_manager.register_model(metadata)
        
        models = version_manager.list_models(ModelType.TASK_PARSER)
        assert len(models) == 3
        
        model_names = [m.name for m in models]
        assert "model_0" in model_names
        assert "model_1" in model_names
        assert "model_2" in model_names
    
    def test_version_management(self, version_manager, sample_model_file):
        """Test version listing and latest version"""
        # Register multiple versions of the same model
        for version in ["v1", "v2", "v3"]:
            metadata = create_model_metadata(
                name="test_model",
                version=version,
                model_type=ModelType.TASK_PARSER,
                file_path=sample_model_file,
                model_source="test",
                memory_requirement_mb=256
            )
            version_manager.register_model(metadata)
        
        versions = version_manager.list_versions(ModelType.TASK_PARSER, "test_model")
        assert len(versions) == 3
        assert "v1" in versions
        assert "v2" in versions
        assert "v3" in versions
        
        latest = version_manager.get_latest_version(ModelType.TASK_PARSER, "test_model")
        assert latest == "v3"  # Should be sorted in reverse order
    
    def test_model_deletion(self, version_manager, sample_model_file):
        """Test model deletion"""
        # Register a model
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        version_manager.register_model(metadata)
        
        # Verify it exists
        retrieved = version_manager.get_model_metadata(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert retrieved is not None
        
        # Delete it
        success = version_manager.delete_model_version(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert success
        
        # Verify it's gone
        retrieved = version_manager.get_model_metadata(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert retrieved is None
    
    def test_status_update(self, version_manager, sample_model_file):
        """Test model status updates"""
        # Register a model
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        version_manager.register_model(metadata)
        
        # Update status
        success = version_manager.update_model_status(
            ModelType.TASK_PARSER, "test_model", "v1", ModelStatus.LOADING
        )
        assert success
        
        # Verify status was updated
        retrieved = version_manager.get_model_metadata(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert retrieved.status == ModelStatus.LOADING
    
    def test_integrity_validation(self, version_manager, sample_model_file):
        """Test model integrity validation"""
        # Register a model
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        version_manager.register_model(metadata)
        
        # Validate integrity
        is_valid, message = version_manager.validate_model_integrity(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert is_valid
        assert "validated" in message.lower()
    
    def test_storage_info(self, version_manager, sample_model_file):
        """Test storage information retrieval"""
        # Register a model
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_model_file,
            model_source="test",
            memory_requirement_mb=256
        )
        version_manager.register_model(metadata)
        
        storage_info = version_manager.get_storage_info()
        
        assert "total_models" in storage_info
        assert "total_size_bytes" in storage_info
        assert "total_size_mb" in storage_info
        assert "models_path" in storage_info
        
        assert storage_info["total_models"] >= 1


class TestModelConfigLoader:
    """Test model configuration loader"""
    
    @pytest.fixture
    def temp_config_file(self):
        """Create temporary config file"""
        config_data = {
            "model_definitions": {
                "task_parser": {
                    "test_model": {
                        "versions": {
                            "v1": {
                                "model_source": "mock",
                                "description": "Test model",
                                "memory_requirement_mb": 256,
                                "config": {
                                    "temperature": 0.7,
                                    "max_length": 512
                                }
                            }
                        },
                        "default_version": "v1"
                    }
                }
            },
            "global_config": {
                "cache_enabled": True,
                "cache_size": 100
            }
        }
        
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False)
        json.dump(config_data, temp_file, indent=2)
        temp_file.close()
        
        yield temp_file.name
        os.unlink(temp_file.name)
    
    def test_config_loading(self, temp_config_file):
        """Test configuration loading"""
        loader = ModelConfigLoader(temp_config_file)
        
        assert loader.config_data is not None
        assert "model_definitions" in loader.config_data
        assert "global_config" in loader.config_data
    
    def test_model_definition_retrieval(self, temp_config_file):
        """Test model definition retrieval"""
        loader = ModelConfigLoader(temp_config_file)
        
        definition = loader.get_model_definition(ModelType.TASK_PARSER, "test_model")
        assert definition is not None
        assert "versions" in definition
        assert "default_version" in definition
    
    def test_version_definition_retrieval(self, temp_config_file):
        """Test version definition retrieval"""
        loader = ModelConfigLoader(temp_config_file)
        
        version_def = loader.get_version_definition(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert version_def is not None
        assert version_def["model_source"] == "mock"
        assert version_def["memory_requirement_mb"] == 256
    
    def test_default_version_retrieval(self, temp_config_file):
        """Test default version retrieval"""
        loader = ModelConfigLoader(temp_config_file)
        
        default_version = loader.get_default_version(ModelType.TASK_PARSER, "test_model")
        assert default_version == "v1"
    
    def test_model_config_creation(self, temp_config_file):
        """Test model config creation from definition"""
        loader = ModelConfigLoader(temp_config_file)
        
        config = loader.create_model_config(ModelType.TASK_PARSER, "test_model", "v1")
        assert config is not None
        assert config.model_type == ModelType.TASK_PARSER
        assert config.name == "test_model"
        assert config.version == "v1"
        assert config.temperature == 0.7
        assert config.max_length == 512
    
    def test_available_models_listing(self, temp_config_file):
        """Test listing available models"""
        loader = ModelConfigLoader(temp_config_file)
        
        available = loader.list_available_models()
        assert "task_parser" in available
        assert len(available["task_parser"]) == 1
        
        model_info = available["task_parser"][0]
        assert model_info["name"] == "test_model"
        assert "v1" in model_info["versions"]
        assert model_info["default_version"] == "v1"
    
    def test_config_validation(self, temp_config_file):
        """Test configuration validation"""
        loader = ModelConfigLoader(temp_config_file)
        
        errors = loader.validate_config()
        assert len(errors) == 0  # Should be valid
    
    def test_invalid_config_validation(self):
        """Test validation with invalid configuration"""
        # Create loader with non-existent file (will use default config)
        loader = ModelConfigLoader("/non/existent/file.json")
        
        # Manually set invalid config
        loader.config_data = {
            "model_definitions": {
                "invalid_type": {
                    "test_model": {
                        "versions": {}  # Missing versions
                    }
                }
            }
        }
        
        errors = loader.validate_config()
        assert len(errors) > 0


class TestFileHashCalculation:
    """Test file hash calculation utility"""
    
    def test_calculate_file_hash(self):
        """Test file hash calculation"""
        # Create temporary file
        temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False)
        temp_file.write("test content")
        temp_file.close()
        
        try:
            file_hash = calculate_file_hash(temp_file.name)
            assert len(file_hash) == 64  # SHA-256 hash length
            assert isinstance(file_hash, str)
            
            # Calculate again to ensure consistency
            file_hash2 = calculate_file_hash(temp_file.name)
            assert file_hash == file_hash2
            
        finally:
            os.unlink(temp_file.name)
    
    def test_calculate_hash_nonexistent_file(self):
        """Test hash calculation with non-existent file"""
        with pytest.raises(ValueError, match="File not found"):
            calculate_file_hash("/non/existent/file.txt")