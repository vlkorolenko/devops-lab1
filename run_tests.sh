#!/usr/bin/env bash
set -e

echo "=========================================="
echo " Running Unit & Integration Tests (Product Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/product-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn test

echo ""
echo "=========================================="
echo " Running Unit & Integration Tests (Order Service)"
echo "=========================================="
docker run --rm \
  -v "$(pwd)/order-service:/app" \
  -v "$(pwd)/.m2:/root/.m2" \
  -w /app \
  maven:3.9.6-eclipse-temurin-21-alpine \
  mvn test

echo ""
echo "=========================================="
echo " All Tests Passed Successfully! (100% OK)"
echo "=========================================="
