#!/usr/bin/env python3
"""
Test runner script for AI Python Service
"""
import sys
import subprocess

def run_tests():
    """Run all tests"""
    try:
        # Run pytest with coverage
        result = subprocess.run([
            sys.executable, "-m", "pytest",
            "tests/",
            "-v",
            "--tb=short"
        ], check=True)
        
        print("All tests passed!")
        return 0
        
    except subprocess.CalledProcessError as e:
        print(f"Tests failed with exit code: {e.returncode}")
        return e.returncode
    except Exception as e:
        print(f"Error running tests: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(run_tests())