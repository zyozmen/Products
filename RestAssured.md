# Pruebas E2E con Rest Assured

## Objetivo

Estas pruebas ejecutan la aplicación Spring Boot real, realizan peticiones HTTP reales con Rest Assured y persisten los datos en MongoDB. No usan Mockito, MockMvc ni dobles de la API.

## Bases de datos por entorno

- Desarrollo y pruebas E2E: la aplicación usa la URI definida por `MONGODB_URI`, `SPRING_DATA_MONGODB_URI`, `SPRING_MONGODB_URI` o `MONGO_URI`. Si no se define ninguna, usa la conexión configurada en `application.properties`.
- Las pruebas E2E no levantan contenedores ni sobrescriben la URI o la base de datos. Deben ejecutarse con la conexión de una base de pruebas configurada en `application.properties` o mediante variables de entorno.
- Las pruebas limpian las colecciones de la base configurada antes de cada caso. Nunca deben apuntar a una base de desarrollo o producción.

## Dependencias

- Rest Assured `5.5.0` para peticiones y aserciones HTTP.
- Maven Failsafe `3.2.5` para ejecutar clases `*IT` durante `verify`.

## Estructura

```text
src/test/resources/application-test.properties
src/test/java/com/zyozmen/products/E2E/ApiIntegrationTest.java
src/test/java/com/zyozmen/products/E2E/ProductoApiIT.java
```

`ApiIntegrationTest` configura un puerto HTTP aleatorio, usa el `MongoTemplate` conectado con la configuración normal de Spring y limpia la base antes de cada prueba. `ProductoApiIT` valida creación, consulta, eliminación, errores de validación y recursos inexistentes.

## Requisitos

- JDK 21.
- Maven 3.9+ o `mvnw`.
- MongoDB de pruebas accesible con la configuración de `application.properties`.

## Ejecución local

Pruebas unitarias:

```bash
mvn clean test
```

Este comando ejecuta las clases con sufijo `*Test` mediante Surefire. Sus reportes quedan en `target/surefire-reports`.

Pruebas E2E:

```bash
mvn -DskipTests package
mvn -Dit.test=ProductoApiIT failsafe:integration-test failsafe:verify
```

El primer comando compila y empaqueta sin ejecutar pruebas. El segundo ejecuta únicamente las clases seleccionadas por Failsafe contra la base MongoDB configurada para pruebas.

Ejecutar solamente la suite E2E:

```bash
mvn -Dit.test=ProductoApiIT verify
```

Para ejecutar unitarias y E2E juntas en local:

```bash
mvn clean verify
```

Los reportes unitarios se generan en `target/surefire-reports` y los E2E en `target/failsafe-reports`.

## Diagnóstico de fallos

| Stage | Reporte | Interpretación |
|---|---|---|
| `Unit Tests` | `target/surefire-reports` | Fallo de lógica aislada, reglas de negocio, mapeos o validaciones unitarias |
| `E2E Tests` | `target/failsafe-reports` | Fallo de arranque, conexión a MongoDB, configuración, HTTP, persistencia o contrato de la API |

Un fallo de conexión a MongoDB es un problema de configuración o disponibilidad de la base de pruebas; un fallo HTTP después de arrancar la aplicación corresponde al funcionamiento de la integración.

## Integración continua

Jenkins ejecuta dos stages antes de construir y desplegar la imagen:

1. `Unit Tests`: `mvn clean test`.
2. `E2E Tests`: empaqueta con `-DskipTests` y ejecuta únicamente Failsafe para `ProductoApiIT`.

Un fallo en cualquiera de los dos stages detiene el pipeline antes del despliegue y permite identificar si el problema pertenece a unitarias o a integración.

El stage E2E no necesita acceso a Docker para ejecutar las pruebas. El agente Jenkins puede seguir usando Docker en stages posteriores para construir la imagen.

## Buenas prácticas

- No ejecutar las pruebas E2E contra MongoDB de desarrollo.
- Usar SKU y slug únicos dentro de cada escenario.
- Mantener los datos de cada prueba autocontenidos.
- Validar el contrato HTTP, no detalles internos de repositorios o servicios.
- No comparar timestamps contra valores exactos.