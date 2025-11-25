"""
Tests for model management API endpoints
"""
import pytest
import tempfile
import shutil
import json
import os
from pathlib import Path
from fastapi.testclient import TestClient

from app.main import create_app
from app.models.metadata import ModelType, ModelStatus


@pytest.fixture
def temp_models_dir():
    """Create temporary models directory"""
    temp_dir = tempfile.mkdtemp()
    yield temp_dir
    shutil.rmtree(temp_dir)


@pytest.fixture
def temp_config_file():
    """Create temporary config file"""
    config_data = {
        "model_definitions": {
            "task_parser": {
                "test_model": {
                    "versions": {
                        "v1": {
                            "model_source": "mock",
                            "description": "Test task parser model",
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


@pytest.fixture
def sample_model_file(temp_models_dir):
    """Create a sample model file"""
    model_file = Path(temp_models_dir) / "sample_model.json"
    sample_data = {"type": "test_model", "version": "v1"}
    
    with open(model_file, 'w') as f:
        json.dump(sample_data, f)
    
    return str(model_file)


@pytest.fixture
def client():
    """Create test client"""
    app = create_app()
    return TestClient(app)


class TestModelsAPI:
    """Test model management API endpoints"""
    
    def test_list_model_types(self, client):
        """Test listing available model types"""
        response = client.get("/models/types")
        assert response.status_code == 200
        
        data = response.json()
        assert "model_types" in data
        assert "descriptions" in data
        
        # Check that all model types are included
        expected_types = [mt.value for mt in ModelType]
        assert all(mt in data["model_types"] for mt in expected_types)
    
    def test_list_models_empty(self, client):
        """Test listing models when none are registered"""
        response = client.get("/models/")
        assert response.status_code == 200
        
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 0
    
    def test_list_models_with_filter(self, client):
        """Test listing models with type filter"""
        response = client.get("/models/?model_type=task_parser")
        assert response.status_code == 200
        
        data = response.json()
        assert isinstance(data, list)
    
    def test_list_models_invalid_type(self, client):
        """Test listing models with invalid type filter"""
        response = client.get("/models/?model_type=invalid_type")
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_list_loaded_models(self, client):
        """Test listing loaded models"""
        response = client.get("/models/loaded")
        assert response.status_code == 200
        
        data = response.json()
        assert isinstance(data, list)
    
    def test_get_model_versions_not_found(self, client):
        """Test getting versions for non-existent model"""
        response = client.get("/models/task_parser/nonexistent")
        assert response.status_code == 404
        
        data = response.json()
        assert "not found" in data["detail"]
    
    def test_get_model_versions_invalid_type(self, client):
        """Test getting versions with invalid model type"""
        response = client.get("/models/invalid_type/test_model")
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_get_model_details_not_found(self, client):
        """Test getting details for non-existent model"""
        response = client.get("/models/task_parser/nonexistent/v1")
        assert response.status_code == 404
        
        data = response.json()
        assert "not found" in data["detail"]
    
    def test_register_model_invalid_type(self, client, sample_model_file):
        """Test registering model with invalid type"""
        registration_data = {
            "name": "test_model",
            "version": "v1",
            "model_type": "invalid_type",
            "file_path": sample_model_file,
            "memory_requirement_mb": 256
        }
        
        response = client.post("/models/register", json=registration_data)
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_register_model_success(self, client, sample_model_file):
        """Test successful model registration"""
        registration_data = {
            "name": "test_model",
            "version": "v1",
            "model_type": "task_parser",
            "file_path": sample_model_file,
            "model_source": "test",
            "description": "Test model",
            "memory_requirement_mb": 256
        }
        
        response = client.post("/models/register", json=registration_data)
        assert response.status_code == 200
        
        data = response.json()
        assert "registered successfully" in data["message"]
        assert data["name"] == "test_model"
        assert data["version"] == "v1"
    
    def test_load_model_not_found(self, client):
        """Test loading non-existent model"""
        response = client.post("/models/task_parser/nonexistent/v1/load")
        assert response.status_code == 400
        
        data = response.json()
        assert "Failed to load model" in data["detail"]
    
    def test_load_model_invalid_type(self, client):
        """Test loading model with invalid type"""
        response = client.post("/models/invalid_type/test_model/v1/load")
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_unload_model_not_loaded(self, client):
        """Test unloading model that's not loaded"""
        response = client.post("/models/task_parser/nonexistent/v1/unload")
        assert response.status_code == 400
        
        data = response.json()
        assert "Failed to unload model" in data["detail"]
    
    def test_update_model_status_invalid_type(self, client):
        """Test updating status with invalid model type"""
        status_data = {"status": "available"}
        
        response = client.put("/models/invalid_type/test/v1/status", json=status_data)
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_update_model_status_invalid_status(self, client):
        """Test updating status with invalid status value"""
        status_data = {"status": "invalid_status"}
        
        response = client.put("/models/task_parser/test/v1/status", json=status_data)
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid status" in data["detail"]
    
    def test_delete_model_not_found(self, client):
        """Test deleting non-existent model"""
        response = client.delete("/models/task_parser/nonexistent/v1")
        assert response.status_code == 400
        
        data = response.json()
        assert "Failed to delete model" in data["detail"]
    
    def test_delete_model_invalid_type(self, client):
        """Test deleting model with invalid type"""
        response = client.delete("/models/invalid_type/test/v1")
        assert response.status_code == 400
        
        data = response.json()
        assert "Invalid model type" in data["detail"]
    
    def test_get_performance_stats(self, client):
        """Test getting performance statistics"""
        response = client.get("/models/stats/performance")
        assert response.status_code == 200
        
        data = response.json()
        assert "performance_stats" in data
        assert "memory_usage" in data
        assert "storage_info" in data
    
    def test_get_available_models_from_config(self, client):
        """Test getting available models from configuration"""
        response = client.get("/models/config/available")
        assert response.status_code == 200
        
        data = response.json()
        assert "available_models" in data
        assert "global_config" in data
    
    def test_initialize_models_from_config(self, client):
        """Test initializing models from configuration"""
        response = client.post("/models/config/initialize")
        assert response.status_code == 200
        
        data = response.json()
        assert "results" in data
        assert "success_count" in data
        assert "total_count" in data
    
    def test_reload_configuration(self, client):
        """Test reloading configuration"""
        response = client.post("/models/config/reload")
        assert response.status_code == 200
        
        data = response.json()
        assert "message" in data
        assert "validation_errors" in data


class TestModelsAPIIntegration:
    """Integration tests for model management API"""
    
    def test_full_model_lifecycle(self, client, sample_model_file):
        """Test complete model lifecycle: register -> load -> unload -> delete"""
        
        # 1. Register model
        registration_data = {
            "name": "lifecycle_test",
            "version": "v1",
            "model_type": "task_parser",
            "file_path": sample_model_file,
            "model_source": "test",
            "description": "Lifecycle test model",
            "memory_requirement_mb": 256
        }
        
        response = client.post("/models/register", json=registration_data)
        assert response.status_code == 200
        
        # 2. Verify model appears in list
        response = client.get("/models/")
        assert response.status_code == 200
        models = response.json()
        model_names = [m["name"] for m in models]
        assert "lifecycle_test" in model_names
        
        # 3. Get model details
        response = client.get("/models/task_parser/lifecycle_test/v1")
        assert response.status_code == 200
        details = response.json()
        assert details["name"] == "lifecycle_test"
        assert details["version"] == "v1"
        assert details["is_loaded"] is False
        
        # 4. Load model
        response = client.post("/models/task_parser/lifecycle_test/v1/load")
        assert response.status_code == 200
        
        # 5. Verify model is loaded
        response = client.get("/models/loaded")
        assert response.status_code == 200
        loaded_models = response.json()
        loaded_names = [m["name"] for m in loaded_models]
        assert "lifecycle_test" in loaded_names
        
        # 6. Update model status
        status_data = {"status": "deprecated"}
        response = client.put("/models/task_parser/lifecycle_test/v1/status", json=status_data)
        assert response.status_code == 200
        
        # 7. Verify status update
        response = client.get("/models/task_parser/lifecycle_test/v1")
        assert response.status_code == 200
        details = response.json()
        assert details["status"] == "deprecated"
        
        # 8. Unload model
        response = client.post("/models/task_parser/lifecycle_test/v1/unload")
        assert response.status_code == 200
        
        # 9. Verify model is unloaded
        response = client.get("/models/loaded")
        assert response.status_code == 200
        loaded_models = response.json()
        loaded_names = [m["name"] for m in loaded_models]
        assert "lifecycle_test" not in loaded_names
        
        # 10. Delete model
        response = client.delete("/models/task_parser/lifecycle_test/v1")
        assert response.status_code == 200
        
        # 11. Verify model is deleted
        response = client.get("/models/task_parser/lifecycle_test/v1")
        assert response.status_code == 404
    
    def test_multiple_versions_management(self, client, sample_model_file):
        """Test managing multiple versions of the same model"""
        
        # Register multiple versions
        for version in ["v1", "v2", "v3"]:
            registration_data = {
                "name": "multi_version_test",
                "version": version,
                "model_type": "task_parser",
                "file_path": sample_model_file,
                "model_source": "test",
                "description": f"Multi-version test model {version}",
                "memory_requirement_mb": 256
            }
            
            response = client.post("/models/register", json=registration_data)
            assert response.status_code == 200
        
        # Get all versions
        response = client.get("/models/task_parser/multi_version_test")
        assert response.status_code == 200
        
        data = response.json()
        assert data["name"] == "multi_version_test"
        assert data["total_versions"] == 3
        assert len(data["versions"]) == 3
        
        version_list = [v["version"] for v in data["versions"]]
        assert "v1" in version_list
        assert "v2" in version_list
        assert "v3" in version_list
        
        # Latest version should be v3 (sorted in reverse order)
        assert data["latest_version"] == "v3"
        
        # Clean up - delete all versions
        for version in ["v1", "v2", "v3"]:
            response = client.delete(f"/models/task_parser/multi_version_test/{version}")
            assert response.status_code == 200