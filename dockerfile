# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Descarga de dependencias
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Compilación
COPY src ./src
RUN ./mvnw -B -DskipTests package 

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear grupo y usuario del sistema
RUN addgroup -S GrowShop-user && adduser -S -G GrowShop-user GrowShop-user \
    && mkdir -p /app && chown -R GrowShop-user:GrowShop-user /app

# Copiar únicamente el fat-jar generado
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/target/products-0.0.1-SNAPSHOT.jar /app/app.jar

# Script que carga los Secret Files de Render (/etc/secrets) como variables de entorno
COPY --chown=GrowShop-user:GrowShop-user entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

USER GrowShop-user:GrowShop-user

# Render inyecta la variable PORT en tiempo de ejecucion; Spring la usa via server.port=${PORT:8080}
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["/app/entrypoint.sh"]