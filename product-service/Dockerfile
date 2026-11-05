# ==========================================
# Stage 1: Build the application with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Configure proxy for Maven if running in Docker Desktop environment
RUN if nc -z -w 1 http.docker.internal 3128 2>/dev/null; then \
      mkdir -p /root/.m2 && \
      printf '<settings><proxies><proxy><id>d-http</id><active>true</active><protocol>http</protocol><host>http.docker.internal</host><port>3128</port></proxy><proxy><id>d-https</id><active>true</active><protocol>https</protocol><host>http.docker.internal</host><port>3128</port></proxy></proxies></settings>\n' > /root/.m2/settings.xml; \
    fi

# Copy source and build executable jar
COPY pom.xml checkstyle.xml* ./
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
