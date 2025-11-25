#!/bin/bash

# Stop services script

echo "Stopping Advanced Task Management System..."

# Stop all services
docker-compose down

# Remove volumes (optional - uncomment if you want to clean data)
# docker-compose down -v

echo "All services stopped successfully!"