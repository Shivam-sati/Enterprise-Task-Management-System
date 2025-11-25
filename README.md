# Advanced Task Management System

A cloud-native, microservices-based task management platform built with Spring Boot and Spring Cloud.

## Architecture

The system consists of the following enterprise-grade microservices:

- **Eureka Server** (Port 8761) - Service Discovery
- **API Gateway** (Port 8080) - Routing and Security
- **Auth Service** (Port 8081) - Authentication and Authorization
- **Task Service** (Port 8082) - Core Task Management
- **Notification Service** (Port 8083) - Multi-channel Notifications
- **Collaboration Service** (Port 8084) - Real-time Team Collaboration
- **Analytics Service** (Port 8085) - Productivity Analytics & Insights
- **AI Service** (Port 8086) - AI-powered Task Intelligence
- **React Frontend** (Port 3000) - Modern User Interface

## Infrastructure

- **MongoDB** (Port 27017) - Primary Database
- **Redis** (Port 6379) - Caching and Session Management
- **RabbitMQ** (Port 5672/15672) - Message Queue

## Prerequisites

- Java 11
- Maven 3.8+
- Docker and Docker Compose

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd advanced-task-management
mvn clean package -DskipTests
```

### 2. Start Services

```bash
# Make scripts executable
chmod +x scripts/*.sh

# Start all services
./scripts/start-services.sh
```

### 3. Verify Services

- Eureka Dashboard: http://localhost:8761
- API Gateway Health: http://localhost:8080/actuator/health
- RabbitMQ Management: http://localhost:15672 (admin/password)

### 4. Stop Services

```bash
./scripts/stop-services.sh
```

## Development

### Running Individual Services

Each service can be run independently:

```bash
# Start Eureka Server
cd eureka-server && mvn spring-boot:run

# Start API Gateway
cd api-gateway && mvn spring-boot:run

# Start Auth Service
cd auth-service && mvn spring-boot:run
```

### Building Docker Images

```bash
# Build all services
mvn clean package -DskipTests

# Build and run with Docker Compose
docker-compose up --build
```

## Configuration

### Environment Variables

Services can be configured using environment variables:

- `SPRING_PROFILES_ACTIVE` - Active Spring profile (default: local)
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` - Eureka server URL
- `SPRING_DATA_MONGODB_URI` - MongoDB connection string
- `SPRING_REDIS_HOST` - Redis host
- `SPRING_RABBITMQ_HOST` - RabbitMQ host

### Profiles

- `local` - Local development with embedded services
- `docker` - Docker Compose deployment
- `production` - Production deployment

## API Documentation

Once services are running, API documentation is available at:

- API Gateway: http://localhost:8080/swagger-ui.html
- Individual services: http://localhost:808X/swagger-ui.html

## Monitoring

Health checks are available for all services:

- http://localhost:8080/actuator/health (API Gateway)
- http://localhost:8081/actuator/health (Auth Service)
- http://localhost:8082/actuator/health (Task Service)
- etc.

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run specific service tests
cd auth-service && mvn test
```

## Troubleshooting

### Common Issues

1. **Services not registering with Eureka**
   - Check Eureka server is running
   - Verify network connectivity
   - Check service logs

2. **Database connection issues**
   - Ensure MongoDB is running
   - Check connection string
   - Verify credentials

3. **Port conflicts**
   - Check if ports are already in use
   - Modify port configuration in application.yml

### Logs

View service logs:

```bash
# Docker Compose logs
docker-compose logs -f [service-name]

# Individual service logs
docker logs [container-name]
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and add tests
4. Submit a pull request

## License

This project is licensed under the MIT License.