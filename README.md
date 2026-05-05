# Products API

## 1. Resumen del proyecto
Este proyecto implementa un microservicio REST para la gestión de productos (CRUD: crear, consultar, actualizar y eliminar), desarrollado como ejercicio académico con enfoque en arquitectura limpia (hexagonal), buenas prácticas de validación y resiliencia.

El sistema expone una API HTTP documentada con OpenAPI/Swagger y utiliza una base de datos MySQL para persistencia.

## 2. Objetivo académico
El propósito del proyecto es evidenciar la aplicación integrada de los siguientes conceptos:

- Diseño por capas con separación de responsabilidades.
- Arquitectura Hexagonal (puertos y adaptadores).
- Desarrollo de APIs REST con Spring Boot.
- Persistencia relacional con JPA/Hibernate.
- Validación de datos de entrada.
- Mecanismos de tolerancia a fallos (Circuit Breaker y Retry).
- Documentación técnica de API con OpenAPI.
- Pruebas unitarias/integración con el ecosistema de Spring Test.

## 3. Stack tecnológico utilizado
### 3.1 Lenguaje y plataforma
- Java 21
- Spring Boot 3.2.5
- Maven (gestión de dependencias y construcción)

### 3.2 Frameworks y librerías principales
- spring-boot-starter-web: construcción de endpoints REST.
- spring-boot-starter-data-jpa: acceso a datos con JPA/Hibernate.
- spring-boot-starter-validation: validación con Jakarta Validation.
- mysql-connector-j: conector JDBC para MySQL.
- springdoc-openapi-starter-webmvc-ui 2.5.0: documentación OpenAPI y Swagger UI.
- spring-boot-starter-actuator: endpoints operativos/observabilidad.
- resilience4j-spring-boot3 2.2.0: Circuit Breaker y Retry.
- Lombok: reducción de código boilerplate.
- spring-boot-starter-test: pruebas con JUnit 5 y Mockito.

### 3.3 Base de datos
- MySQL 8
- Esquema objetivo: products_db.

## 4. Arquitectura del proyecto
La organización del código responde a una arquitectura hexagonal:

- Capa de dominio (domain): entidades/modelos, puertos y excepciones del negocio.
- Capa de aplicación (application): casos de uso y servicios de aplicación.
- Adaptadores de entrada (adapter.in): API REST, DTOs y mapeadores web.
- Adaptadores de salida (adapter.out): persistencia y mapeadores hacia infraestructura.
- Configuración transversal (config/exception): OpenAPI, manejo global de errores y aspectos no funcionales.

Esta estructura favorece desacoplamiento, mantenibilidad y testabilidad.

## 5. Requisitos previos
Para construir y ejecutar el proyecto localmente se requiere:

- JDK 21.
- Maven 3.9+.
- MySQL ejecutándose y accesible desde el host (por defecto: 127.0.0.1:3306).

## 6. Configuración de entorno
La aplicación usa variables de entorno con valores por defecto definidos en application.properties:

- DB_HOST (default: 127.0.0.1)
- DB_PORT (default: 3306)
- DB_NAME (default: products_db)
- DB_USERNAME (default: USR_PRD_1)
- DB_PASSWORD (default: USR_PSW_SCTR)

Ejemplo en PowerShell (opcional, si desea sobrescribir defaults):

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="products_db"
$env:DB_USERNAME="USR_PRD_1"
$env:DB_PASSWORD="USR_PSW_SCTR"
```

Nota: con la configuración actual, Hibernate utiliza ddl-auto=update, por lo que el esquema se ajusta automáticamente según el modelo.

## 7. Proceso de construcción
### 7.1 Limpieza y compilación
Desde la raíz del proyecto:

```bash
mvn clean compile
```

Resultado esperado:
- Resolución de dependencias.
- Compilación de clases en target/classes.

### 7.2 Ejecución de pruebas
```bash
mvn test
```

Resultado esperado:
- Ejecución de pruebas definidas en src/test/java.
- Reportes en target/surefire-reports.

### 7.3 Empaquetado
```bash
mvn clean package
```

Resultado esperado:
- Generación del artefacto ejecutable en target/.

## 8. Cómo usar el proyecto
### 8.1 Ejecutar la aplicación
Opción A (recomendada en desarrollo):

```bash
mvn spring-boot:run
```

Opción B (jar empaquetado):

```bash
java -jar target/products-0.0.1-SNAPSHOT.jar
```

La API queda disponible en:
- http://localhost:8080

### 8.2 Documentación interactiva
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### 8.3 Endpoints principales (CRUD)
Base path: /api/productos

1. Listar productos
```http
GET /api/productos
```

2. Obtener producto por ID
```http
GET /api/productos/{id}
```

3. Crear producto
```http
POST /api/productos
Content-Type: application/json

{
  "nombre": "Laptop Dell XPS 15",
  "descripcion": "Procesador Intel Core i7, 16GB RAM",
  "precio": 1299.99
}
```

4. Actualizar producto
```http
PUT /api/productos/{id}
Content-Type: application/json

{
  "nombre": "Laptop Dell XPS 15 (2026)",
  "descripcion": "32GB RAM, SSD 1TB",
  "precio": 1499.99
}
```

5. Eliminar producto
```http
DELETE /api/productos/{id}
```

### 8.4 Ejemplos con curl
Crear:
```bash
curl -X POST "http://localhost:8080/api/productos" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado Mecanico","descripcion":"Switch Blue","precio":89.90}'
```

Listar:
```bash
curl "http://localhost:8080/api/productos"
```

## 9. Observabilidad y resiliencia
### 9.1 Actuator
Endpoints expuestos:
- /actuator/health
- /actuator/info
- /actuator/circuitbreakers
- /actuator/retries

### 9.2 Resilience4j
Se implementan políticas de:
- Circuit Breaker para el componente de repositorio de productos.
- Retry ante fallos transitorios de infraestructura.

Esto permite una degradación controlada del servicio cuando existen fallas temporales de conectividad o acceso a datos.

## 10. Manejo de errores
La API cuenta con un manejador global de excepciones que normaliza respuestas HTTP para casos como:

- Recurso no encontrado (404).
- Errores de validación de entrada (400).
- Servicio no disponible por resiliencia/infraestructura (503).
- Errores no controlados (500).

## 11. Estructura general del repositorio
- pom.xml: definición del proyecto Maven y dependencias.
- src/main/java: código fuente principal.
- src/main/resources/application.properties: configuración de aplicación.
- src/test/java: pruebas.
- target/: artefactos y reportes generados por Maven.

## 12. Conclusión
El proyecto Products API constituye una implementación académica completa de un microservicio CRUD empresarial, integrando prácticas modernas de ingeniería de software en Java: arquitectura desacoplada, documentación automática, validaciones robustas, pruebas y mecanismos de resiliencia.

Como resultado, el sistema no solo cumple con el objetivo funcional de gestión de productos, sino que también ofrece una base sólida para evolución, mantenimiento y despliegue en entornos reales.
