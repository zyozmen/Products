# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Descarga de dependencias
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compilación y extracción de capas
COPY src ./src
RUN ./mvnw -B -DskipTests package 
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear grupo y usuario del sistema
RUN <<EOF
addgroup -S GrowShop-user && adduser -S -G GrowShop-user GrowShop-user
mkdir -p /app && chown -R GrowShop-user:GrowShop-user /app
EOF

COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/dependencies/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/spring-boot-loader/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/snapshot-dependencies/ ./
COPY --chown=GrowShop-user:GrowShop-user --from=builder /workspace/application/ ./

USER GrowShop-user:GrowShop-user

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]