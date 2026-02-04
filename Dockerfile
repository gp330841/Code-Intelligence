# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder

WORKDIR /app

# 1. Copy build files first to cache dependencies
COPY build.gradle settings.gradle ./

# 2. Download dependencies (this layer will be cached unless build.gradle changes)
# We run a dry-run build or dependencies task. 'dependencies' is safer.
RUN gradle dependencies --no-daemon

# 3. Copy source and build
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose web port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
