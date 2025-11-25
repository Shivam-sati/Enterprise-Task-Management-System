# AI Python Service

Python-based AI microservice using open source models for task analysis and productivity insights.

## Project Structure

```
ai-python-service/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI application entry point
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes/
│   │       ├── __init__.py
│   │       ├── health.py    # Health check endpoints
│   │       └── ai.py        # AI processing endpoints
│   ├── config/
│   │   ├── __init__.py
│   │   └── settings.py      # Application configuration
│   └── services/
│       ├── __init__.py
│       └── eureka_client.py # Eureka service discovery
├── models/                  # AI model storage
├── config/                  # Configuration files
├── tests/                   # Test files
├── requirements.txt         # Python dependencies
├── Dockerfile              # Container configuration
├── .env.example           # Environment variables template
└── README.md              # This file
```

## Setup

### Local Development

1. Create virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Copy environment configuration:
```bash
cp .env.example .env
```

4. Run the service:
```bash
python -m app.main
```

### Running Tests

1. Install test dependencies (included in requirements.txt):
```bash
pip install -r requirements.txt
```

2. Run tests:
```bash
python -m pytest tests/ -v
```

Or use the test runner script:
```bash
python run_tests.py
```

### Docker

1. Build the image:
```bash
docker build -t ai-python-service .
```

2. Run the container:
```bash
docker run -p 8087:8087 ai-python-service
```

## API Endpoints

### Health Checks
- `GET /health/` - Basic health check
- `GET /health/detailed` - Detailed health check with system metrics

### AI Processing (Placeholders)
- `POST /ai/parse-task` - Parse natural language task description
- `POST /ai/prioritize-tasks` - Prioritize list of tasks
- `POST /ai/insights` - Generate productivity insights

## Configuration

The service uses environment variables with the prefix `AI_SERVICE_`. See `.env.example` for all available options.

## Development Status

This is the foundation setup. The following features will be implemented in subsequent tasks:
- Eureka service registration (Task 1.2)
- Unit tests (Task 1.3)
- AI model integration (Task 2)
- Real AI processing endpoints (Task 3)