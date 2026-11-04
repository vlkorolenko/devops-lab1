# ==========================================
# Stage 1: Build the application with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml checkstyle.xml* ./
RUN mvn dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -Dcheckstyle.skip=true -B

# ==========================================
# Stage 2: Minimal JRE Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install wget for Docker healthcheck
RUN apk add --no-cache wget

# Create non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy compiled jar from builder stage
COPY --from=builder /build/target/product-service-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
