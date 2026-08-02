# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Descarga de dependencias
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compilación y extracción de capas (Spring Boot 3.3+)
COPY src ./src
RUN ./mvnw -B -DskipTests package 
RUN java -Djarmode=tools -jar target/products-0.0.1-SNAPSHOT.jar extract --destination layers

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear grupo y usuario del sistema
RUN addgroup -S GrowShop-user && adduser -S -G GrowShop-user GrowShop-user \
    && mkdir -p /app && chown -R GrowShop-user:GrowShop-user /app

# Copiar las capas generadas por jarmode=tools
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/layers/dependencies/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/layers/spring-boot-loader/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/layers/snapshot-dependencies/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/layers/application/ ./

USER GrowShop-user:GrowShop-user

EXPOSE 8080

# Nuevo paquete para JarLauncher en Spring Boot 3.2+ / 3.3+
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]