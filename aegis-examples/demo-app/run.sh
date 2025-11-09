#!/bin/bash

# Aegis Demo Application Launcher
# This script builds and runs the Aegis demo application

set -e

echo "🛡️  Aegis Authentication SDK - Demo Application"
echo "================================================"
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $JAVA_VERSION"
echo ""

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8+."
    exit 1
fi

echo "✅ Maven is installed"
echo ""

# Build the project
echo "Building the project..."
echo "This may take a few minutes on first run..."
echo ""

mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""

# Run the application
echo "Starting Aegis Demo Application..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📱 Access the demo UI at: http://localhost:8080"
echo ""
echo "Test Credentials:"
echo "  Username: demo   | Password: password   | Roles: USER"
echo "  Username: alice  | Password: alice123  | Roles: USER, MANAGER"
echo "  Username: admin  | Password: admin123  | Roles: USER, ADMIN"
echo ""
echo "API Keys:"
echo "  demo-key-alice-12345      (alice - USER)"
echo "  demo-key-admin-67890      (admin - ADMIN)"
echo "  demo-key-service-abc123   (service-bot - SERVICE)"
echo ""
echo "Press Ctrl+C to stop the application"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

java -jar target/demo-app-1.0.0-SNAPSHOT.jar
