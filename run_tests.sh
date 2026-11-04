#!/usr/bin/env bash
set -e

echo "=========================================="
echo " 1. Static Analysis: Checkstyle (Product Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/product-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn checkstyle:check

echo "=========================================="
echo " 2. Tests: Unit & Integration (Product Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/product-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn test

echo "=========================================="
echo " 3. Static Analysis: Checkstyle (Order Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/order-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn checkstyle:check

echo "=========================================="
echo " 4. Tests: Unit & Integration (Order Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/order-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn test

echo ""
echo "=========================================="
echo " All Linters and Tests Passed! (100% OK)"
echo "=========================================="
