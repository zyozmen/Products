FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia el JAR generado por Maven
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]