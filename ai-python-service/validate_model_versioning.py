#!/usr/bin/env python3
"""
Validation script for model versioning system
"""
import sys
import tempfile
import shutil
import json
import os
from pathlib import Path

# Add the app directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'app'))

from models.metadata import (
    ModelMetadata, ModelConfig, ModelType, ModelStatus, 
    create_model_metadata, calculate_file_hash
)
from models.version_manager import ModelVersionManager
from models.config_loader import ModelConfigLoader


def create_sample_model_file(temp_dir):
    """Create a sample model file for testing"""
    model_file = Path(temp_dir) / "sample_model.json"
    sample_data = {"type": "test_model", "version": "v1", "data": "sample"}
    
    with open(model_file, 'w') as f:
        json.dump(sample_data, f)
    
    return str(model_file)


def test_model_metadata():
    """Test model metadata functionality"""
    print("Testing ModelMetadata...")
    
    try:
        metadata = ModelMetadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path="/path/to/model.bin",
            file_size_bytes=1024,
            file_hash="a" * 64,  # Valid SHA-256 hash
            model_source="test",
            memory_requirement_mb=512,
            created_at="2024-01-01T00:00:00",
            updated_at="2024-01-01T00:00:00"
        )
        
        assert metadata.name == "test_model"
        assert metadata.version == "v1"
        assert metadata.model_type == ModelType.TASK_PARSER
        print("✓ ModelMetadata creation successful")
        
    except Exception as e:
        print(f"✗ ModelMetadata test failed: {e}")
        return False
    
    return True


def test_model_config():
    """Test model configuration functionality"""
    print("Testing ModelConfig...")
    
    try:
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
        print("✓ ModelConfig creation successful")
        
    except Exception as e:
        print(f"✗ ModelConfig test failed: {e}")
        return False
    
    return True


def test_version_manager():
    """Test model version manager functionality"""
    print("Testing ModelVersionManager...")
    
    temp_dir = tempfile.mkdtemp()
    
    try:
        # Create version manager
        version_manager = ModelVersionManager(temp_dir)
        
        # Create sample model file
        sample_file = create_sample_model_file(temp_dir)
        
        # Create and register model metadata
        metadata = create_model_metadata(
            name="test_model",
            version="v1",
            model_type=ModelType.TASK_PARSER,
            file_path=sample_file,
            model_source="test",
            memory_requirement_mb=256
        )
        
        # Register model
        success = version_manager.register_model(metadata)
        assert success, "Model registration failed"
        print("✓ Model registration successful")
        
        # Retrieve model
        retrieved = version_manager.get_model_metadata(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert retrieved is not None, "Model retrieval failed"
        assert retrieved.name == "test_model"
        print("✓ Model retrieval successful")
        
        # List models
        models = version_manager.list_models(ModelType.TASK_PARSER)
        assert len(models) == 1, "Model listing failed"
        print("✓ Model listing successful")
        
        # Update status
        success = version_manager.update_model_status(
            ModelType.TASK_PARSER, "test_model", "v1", ModelStatus.LOADING
        )
        assert success, "Status update failed"
        print("✓ Status update successful")
        
        # Validate integrity
        is_valid, message = version_manager.validate_model_integrity(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert is_valid, f"Integrity validation failed: {message}"
        print("✓ Integrity validation successful")
        
        # Get storage info
        storage_info = version_manager.get_storage_info()
        assert storage_info["total_models"] >= 1, "Storage info failed"
        print("✓ Storage info retrieval successful")
        
    except Exception as e:
        print(f"✗ ModelVersionManager test failed: {e}")
        return False
    
    finally:
        shutil.rmtree(temp_dir)
    
    return True


def test_config_loader():
    """Test model configuration loader"""
    print("Testing ModelConfigLoader...")
    
    # Create temporary config file
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
    
    try:
        # Create config loader
        loader = ModelConfigLoader(temp_file.name)
        
        # Test configuration loading
        assert loader.config_data is not None, "Config loading failed"
        print("✓ Configuration loading successful")
        
        # Test model definition retrieval
        definition = loader.get_model_definition(ModelType.TASK_PARSER, "test_model")
        assert definition is not None, "Model definition retrieval failed"
        print("✓ Model definition retrieval successful")
        
        # Test version definition retrieval
        version_def = loader.get_version_definition(
            ModelType.TASK_PARSER, "test_model", "v1"
        )
        assert version_def is not None, "Version definition retrieval failed"
        print("✓ Version definition retrieval successful")
        
        # Test model config creation
        config = loader.create_model_config(ModelType.TASK_PARSER, "test_model", "v1")
        assert config is not None, "Model config creation failed"
        assert config.temperature == 0.7, "Config values incorrect"
        print("✓ Model config creation successful")
        
        # Test configuration validation
        errors = loader.validate_config()
        assert len(errors) == 0, f"Config validation failed: {errors}"
        print("✓ Configuration validation successful")
        
    except Exception as e:
        print(f"✗ ModelConfigLoader test failed: {e}")
        return False
    
    finally:
        os.unlink(temp_file.name)
    
    return True


def test_file_hash():
    """Test file hash calculation"""
    print("Testing file hash calculation...")
    
    # Create temporary file
    temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False)
    temp_file.write("test content for hashing")
    temp_file.close()
    
    try:
        file_hash = calculate_file_hash(temp_file.name)
        assert len(file_hash) == 64, "Hash length incorrect"
        assert isinstance(file_hash, str), "Hash type incorrect"
        
        # Calculate again to ensure consistency
        file_hash2 = calculate_file_hash(temp_file.name)
        assert file_hash == file_hash2, "Hash inconsistent"
        print("✓ File hash calculation successful")
        
    except Exception as e:
        print(f"✗ File hash test failed: {e}")
        return False
    
    finally:
        os.unlink(temp_file.name)
    
    return True


def main():
    """Run all validation tests"""
    print("=== Model Versioning System Validation ===\n")
    
    tests = [
        test_model_metadata,
        test_model_config,
        test_file_hash,
        test_version_manager,
        test_config_loader
    ]
    
    passed = 0
    total = len(tests)
    
    for test in tests:
        if test():
            passed += 1
        print()  # Add spacing between tests
    
    print(f"=== Results: {passed}/{total} tests passed ===")
    
    if passed == total:
        print("🎉 All model versioning tests passed!")
        return 0
    else:
        print("❌ Some tests failed. Please check the implementation.")
        return 1


if __name__ == "__main__":
    sys.exit(main())