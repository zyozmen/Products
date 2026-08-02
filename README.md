# Products API

## 1. Resumen del proyecto
Este proyecto implementa un microservicio REST para la gestión de productos (CRUD: crear, consultar, actualizar y eliminar), desarrollado como ejercicio académico con enfoque en arquitectura limpia (hexagonal), buenas prácticas de validación y resiliencia.

El sistema expone una API HTTP documentada con OpenAPI/Swagger y utiliza MongoDB para persistencia.

## 2. Objetivo académico
El propósito del proyecto es evidenciar la aplicación integrada de los siguientes conceptos:

- Diseño por capas con separación de responsabilidades.
- Arquitectura Hexagonal (puertos y adaptadores).
- Desarrollo de APIs REST con Spring Boot.
- Persistencia documental con Spring Data MongoDB.
- Validación de datos de entrada.
- Documentación técnica de API con OpenAPI.
- Pruebas unitarias con el ecosistema de Spring Test.

## 3. Stack tecnológico utilizado
### 3.1 Lenguaje y plataforma
- Java 21
- Spring Boot 3.3.5
- Maven (gestión de dependencias y construcción)

### 3.2 Frameworks y librerías principales
- spring-boot-starter-web: construcción de endpoints REST.
- spring-boot-starter-data-mongodb: acceso a datos con MongoDB.
- spring-boot-starter-validation: validación con Jakarta Validation.
- springdoc-openapi-starter-webmvc-ui 2.5.0: documentación OpenAPI y Swagger UI.
- Lombok: reducción de código boilerplate.
- spring-boot-starter-test: pruebas con JUnit 5 y Mockito.

### 3.3 Base de datos
- MongoDB
- Base de datos objetivo: GrowShop.

## 4. Arquitectura del proyecto
La organización del código responde a una arquitectura hexagonal:

- Capa de dominio (domain): entidades/modelos, puertos y excepciones del negocio.
- Capa de aplicación (application): casos de uso y servicios de aplicación.
- Adaptadores de entrada (adapter.in): API REST, DTOs y mapeadores web.
- Configuración transversal (config/exception): OpenAPI, configuración de MongoDB y manejo global de errores.

Esta estructura favorece desacoplamiento, mantenibilidad y testabilidad.

## 5. Requisitos previos
Para construir y ejecutar el proyecto localmente se requiere:

- JDK 21.
- Maven 3.9+.
- MongoDB ejecutándose y accesible desde el host o desde la red Docker configurada.

## 6. Configuración de entorno
La aplicación usa configuración de MongoDB definida en application.properties y puede sobrescribirse al ejecutar la aplicación:

- spring.data.mongodb.host (default: 127.0.0.1)
- spring.data.mongodb.port (default: 27017)
- spring.data.mongodb.database (default: GrowShop)
- spring.data.mongodb.username (default: growShop)
- spring.data.mongodb.password (default: GrowSh0p)
- spring.data.mongodb.authentication-database (default: admin)

Ejemplo en PowerShell usando variables de entorno reconocidas por Spring Boot:

```powershell
$env:SPRING_DATA_MONGODB_HOST="127.0.0.1"
$env:SPRING_DATA_MONGODB_PORT="27017"
$env:SPRING_DATA_MONGODB_DATABASE="GrowShop"
$env:SPRING_DATA_MONGODB_USERNAME="growShop"
$env:SPRING_DATA_MONGODB_PASSWORD="GrowSh0p"
$env:SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE="admin"
```

Nota: el pipeline de Jenkins inyecta la URI de MongoDB mediante la propiedad `spring.data.mongodb.uri` durante la fase de pruebas.

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

También se puede construir y ejecutar el contenedor con Java 21:

```bash
docker build -t products-api:local .
docker run --rm -p 8080:8080 products-api:local
```

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

## 9. Despliegue con Terraform y AWS
El proyecto incluye una configuración base de Terraform para desplegar el servicio en Amazon ECS Fargate.

### 9.1 Recursos provisionados
- Repositorio ECR para almacenar las imágenes Docker del servicio.
- Política de ciclo de vida para eliminar imágenes sin tag y conservar solo las más recientes.
- Cluster ECS, definición de tarea y servicio Fargate para ejecutar la aplicación.
- Grupo de logs de CloudWatch, security group y rol de IAM para la ejecución de tareas.
- Backend remoto en S3 con bloqueo en DynamoDB para guardar el estado de Terraform.

### 9.2 Comandos de despliegue
Desde la raíz del proyecto:

```bash
terraform init
terraform validate
terraform plan -var="image_tag=v1.0.0"
terraform apply -var="image_tag=v1.0.0"
```

> El valor `image_tag` indica qué versión de la imagen ECR se desplegará en ECS.

## 10. Destrucción y limpieza
Para eliminar la infraestructura creada por Terraform se puede ejecutar:

```bash
terraform destroy -auto-approve -var="image_tag=cleanup"
```

luego se puede usar el script de limpieza preparado para AWS:

```bash
bash ./cleanup-aws.sh
```

Este script elimina recursos remanentes como el servicio ECS, el cluster, el repositorio ECR, el security group, el grupo de logs y el estado remoto en S3. Se recomienda ejecutarlo con precaución si el bucket de Terraform contiene datos de importancia.

## 11. Persistencia y ejecución
La aplicación se conecta a MongoDB usando las propiedades `spring.data.mongodb.*` o una URI completa con `spring.data.mongodb.uri`.

Para CI/CD, el proyecto queda alineado con Java 21 en estos puntos:
- Maven compila con Java 21 definido en pom.xml.
- Jenkins usa la herramienta JDK 21 y verifica la versión antes de ejecutar Maven.
- Docker ejecuta la aplicación sobre una imagen base Eclipse Temurin 21.

## 12. Manejo de errores
La API cuenta con un manejador global de excepciones que normaliza respuestas HTTP para casos como:

- Recurso no encontrado (404).
- Errores de validación de entrada (400).
- Servicio no disponible por resiliencia/infraestructura (503).
- Errores no controlados (500).

## 13. Estructura general del repositorio
- pom.xml: definición del proyecto Maven y dependencias.
- src/main/java: código fuente principal.
- src/main/resources/application.properties: configuración de aplicación.
- src/test/java: pruebas.
- target/: artefactos y reportes generados por Maven.
- main.tf y ecs.tf: infraestructura de Terraform para AWS.
- cleanup-aws.sh: script para limpiar recursos AWS generados por la infraestructura.

## 14. Conclusión
El proyecto Products API constituye una implementación académica completa de un microservicio CRUD empresarial, integrando prácticas modernas de ingeniería de software en Java: arquitectura desacoplada, documentación automática, validaciones robustas y pruebas.

Como resultado, el sistema no solo cumple con el objetivo funcional de gestión de productos, sino que también ofrece una base sólida para evolución, mantenimiento y despliegue en entornos reales.
