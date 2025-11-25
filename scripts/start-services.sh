#!/bin/bash

# Start services script for local development

echo "Starting Advanced Task Management System..."

# Build all services
echo "Building all services..."
mvn clean package -DskipTests

# Start infrastructure services first
echo "Starting infrastructure services..."
docker-compose up -d mongodb redis rabbitmq

# Wait for infrastructure services to be ready
echo "Waiting for infrastructure services to be ready..."
sleep 30

# Start Eureka Server
echo "Starting Eureka Server..."
docker-compose up -d eureka-server

# Wait for Eureka Server to be ready
echo "Waiting for Eureka Server to be ready..."
sleep 30

# Start API Gateway
echo "Starting API Gateway..."
docker-compose up -d api-gateway

# Wait for API Gateway to be ready
echo "Waiting for API Gateway to be ready..."
sleep 20

# Start all microservices
echo "Starting all microservices..."
docker-compose up -d auth-service task-service notification-service collaboration-service analytics-service ai-service

# Start frontend
echo "Starting React frontend..."
docker-compose up -d todo-react

echo "All services started successfully!"
echo "🚀 Advanced Task Management System is now running!"
echo ""
echo "📊 Services Dashboard:"
echo "- Eureka Server: http://localhost:8761"
echo "- API Gateway: http://localhost:8080"
echo "- Auth Service: http://localhost:8081"
echo "- Task Service: http://localhost:8082"
echo "- Notification Service: http://localhost:8083"
echo "- Collaboration Service: http://localhost:8084"
echo "- Analytics Service: http://localhost:8085"
echo "- AI Service: http://localhost:8086"
echo "- React Frontend: http://localhost:3000"
echo ""
echo "🗄️ Infrastructure:"
echo "- MongoDB: localhost:27017"
echo "- Redis: localhost:6379"
echo "- RabbitMQ Management: http://localhost:15672 (admin/password)"
echo ""
echo "✅ Your enterprise-grade microservices system is ready!"