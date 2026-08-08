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

USER GrowShop-user:GrowShop-user

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]