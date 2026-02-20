# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY keza-common/pom.xml keza-common/
COPY keza-infrastructure/pom.xml keza-infrastructure/
COPY keza-user/pom.xml keza-user/
COPY keza-campaign/pom.xml keza-campaign/
COPY keza-investment/pom.xml keza-investment/
COPY keza-payment/pom.xml keza-payment/
COPY keza-ai/pom.xml keza-ai/
COPY keza-marketplace/pom.xml keza-marketplace/
COPY keza-notification/pom.xml keza-notification/
COPY keza-admin/pom.xml keza-admin/
COPY keza-app/pom.xml keza-app/

# Download dependencies (cached layer)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -q || true

# Copy source and build
COPY . .
RUN mvn clean package -DskipTests -B -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S keza && adduser -S keza -G keza
WORKDIR /app

COPY --from=builder /app/keza-app/target/*.jar app.jar

RUN chown -R keza:keza /app
USER keza

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
