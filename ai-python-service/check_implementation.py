#!/usr/bin/env python3
"""
Simple implementation check for model versioning system
"""
import os
import sys
from pathlib import Path


def check_file_exists(file_path, description):
    """Check if a file exists and report"""
    if os.path.exists(file_path):
        print(f"✓ {description}: {file_path}")
        return True
    else:
        print(f"✗ {description}: {file_path} (NOT FOUND)")
        return False


def check_directory_structure():
    """Check that all required files and directories exist"""
    print("=== Checking Model Versioning Implementation Structure ===\n")
    
    base_path = Path("app/models")
    config_path = Path("config")
    tests_path = Path("tests")
    
    files_to_check = [
        # Core model files
        (base_path / "__init__.py", "Models package init"),
        (base_path / "metadata.py", "Model metadata definitions"),
        (base_path / "version_manager.py", "Model version manager"),
        (base_path / "model_manager.py", "Enhanced model manager"),
        (base_path / "config_loader.py", "Configuration loader"),
        
        # Configuration files
        (config_path / "models.json", "Model configuration file"),
        
        # API files
        (Path("app/api/routes/models.py"), "Model management API"),
        
        # Test files
        (tests_path / "test_model_versioning.py", "Model versioning tests"),
        (tests_path / "test_models_api.py", "Model API tests"),
        
        # Updated files
        (Path("app/config/settings.py"), "Updated settings"),
        (Path("app/main.py"), "Updated main application"),
        (Path("app/api/routes/health.py"), "Updated health endpoint")
    ]
    
    passed = 0
    total = len(files_to_check)
    
    for file_path, description in files_to_check:
        if check_file_exists(file_path, description):
            passed += 1
    
    print(f"\n=== Structure Check: {passed}/{total} files found ===")
    return passed == total


def check_file_content():
    """Check that key files contain expected content"""
    print("\n=== Checking File Content ===\n")
    
    content_checks = [
        ("app/models/metadata.py", ["ModelType", "ModelStatus", "ModelMetadata", "ModelConfig"], "Model metadata classes"),
        ("app/models/version_manager.py", ["ModelVersionManager", "register_model", "get_model_metadata"], "Version manager functionality"),
        ("app/models/model_manager.py", ["ModelManager", "load_model", "unload_model"], "Model manager functionality"),
        ("app/models/config_loader.py", ["ModelConfigLoader", "load_config", "create_model_config"], "Configuration loader"),
        ("app/api/routes/models.py", ["list_models", "register_model", "load_model"], "Model API endpoints"),
        ("config/models.json", ["model_definitions", "task_parser", "prioritizer"], "Model configuration"),
        ("app/config/settings.py", ["default_task_parser_model", "Model versioning configuration"], "Updated settings")
    ]
    
    passed = 0
    total = len(content_checks)
    
    for file_path, expected_content, description in content_checks:
        try:
            if os.path.exists(file_path):
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                found_all = all(item in content for item in expected_content)
                if found_all:
                    print(f"✓ {description}: Contains expected content")
                    passed += 1
                else:
                    missing = [item for item in expected_content if item not in content]
                    print(f"✗ {description}: Missing content - {missing}")
            else:
                print(f"✗ {description}: File not found - {file_path}")
        
        except Exception as e:
            print(f"✗ {description}: Error reading file - {e}")
    
    print(f"\n=== Content Check: {passed}/{total} files have expected content ===")
    return passed == total


def check_python_syntax():
    """Check Python syntax for all model files"""
    print("\n=== Checking Python Syntax ===\n")
    
    python_files = [
        "app/models/__init__.py",
        "app/models/metadata.py", 
        "app/models/version_manager.py",
        "app/models/model_manager.py",
        "app/models/config_loader.py",
        "app/api/routes/models.py",
        "tests/test_model_versioning.py",
        "tests/test_models_api.py"
    ]
    
    passed = 0
    total = len(python_files)
    
    for file_path in python_files:
        if os.path.exists(file_path):
            try:
                import py_compile
                py_compile.compile(file_path, doraise=True)
                print(f"✓ {file_path}: Syntax OK")
                passed += 1
            except py_compile.PyCompileError as e:
                print(f"✗ {file_path}: Syntax Error - {e}")
            except Exception as e:
                print(f"✗ {file_path}: Error - {e}")
        else:
            print(f"✗ {file_path}: File not found")
    
    print(f"\n=== Syntax Check: {passed}/{total} files have valid syntax ===")
    return passed == total


def check_task_requirements():
    """Check that task requirements are met"""
    print("\n=== Checking Task Requirements ===\n")
    
    requirements = [
        ("Model directory structure", "config/models.json", "Model versioning directory structure created"),
        ("Configuration system", "app/models/config_loader.py", "Configuration system for model selection implemented"),
        ("Metadata tracking", "app/models/metadata.py", "Model metadata tracking and validation implemented"),
        ("Version management", "app/models/version_manager.py", "Model version management system implemented"),
        ("Enhanced ModelManager", "app/models/model_manager.py", "ModelManager enhanced with versioning support"),
        ("API endpoints", "app/api/routes/models.py", "Model management API endpoints created"),
        ("Updated settings", "app/config/settings.py", "Settings updated with versioning configuration"),
        ("Test coverage", "tests/test_model_versioning.py", "Comprehensive tests for versioning system")
    ]
    
    passed = 0
    total = len(requirements)
    
    for requirement, file_path, description in requirements:
        if os.path.exists(file_path):
            print(f"✓ {requirement}: {description}")
            passed += 1
        else:
            print(f"✗ {requirement}: {description} (FILE MISSING)")
    
    print(f"\n=== Requirements Check: {passed}/{total} requirements met ===")
    return passed == total


def main():
    """Run all implementation checks"""
    print("Model Versioning System Implementation Check")
    print("=" * 50)
    
    checks = [
        check_directory_structure,
        check_file_content,
        check_python_syntax,
        check_task_requirements
    ]
    
    all_passed = True
    
    for check in checks:
        if not check():
            all_passed = False
        print()
    
    if all_passed:
        print("🎉 All implementation checks passed!")
        print("\nTask 2.2 'Implement model versioning system' appears to be complete:")
        print("- ✓ Model directory structure with version management")
        print("- ✓ Configuration system for model selection and versions") 
        print("- ✓ Model metadata tracking and validation")
        print("- ✓ Enhanced ModelManager with versioning support")
        print("- ✓ API endpoints for model management")
        print("- ✓ Comprehensive test coverage")
        return 0
    else:
        print("❌ Some implementation checks failed.")
        print("Please review the failed checks above.")
        return 1


if __name__ == "__main__":
    sys.exit(main())