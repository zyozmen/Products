# Pruebas E2E con Rest Assured

## Objetivo

Estas pruebas ejecutan la aplicación Spring Boot real, realizan peticiones HTTP reales con Rest Assured y persisten los datos en MongoDB. No usan Mockito, MockMvc ni dobles de la API.

## Bases de datos por entorno

- Desarrollo y pruebas E2E: la aplicación usa la misma URI definida por `MONGODB_URI`, `SPRING_DATA_MONGODB_URI`, `SPRING_MONGODB_URI` o `MONGO_URI`. Si no se define ninguna, usa la conexión configurada en `application.properties`.
- El perfil `test` sobrescribe únicamente `spring.data.mongodb.database` con `products-e2e`; no cambia la URI ni levanta contenedores.
- Las pruebas limpian las colecciones de `products-e2e` antes de cada caso. Nunca deben apuntar a una base de desarrollo o producción.

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
- MongoDB accesible desde Jenkins con la conexión de `application.properties`.

## Ejecución local

Pruebas unitarias:

```bash
mvn clean test
```

Este comando ejecuta las clases con sufijo `*Test` mediante Surefire. Sus reportes quedan en `target/surefire-reports`.

Pruebas E2E:

```bash
mvn -B -ntp clean test
mvn -B -ntp -Dit.test=ProductoApiIT failsafe:integration-test failsafe:verify
```

El primer comando compila y ejecuta las pruebas unitarias. El segundo ejecuta únicamente la clase seleccionada por Failsafe contra la base MongoDB configurada para pruebas. En Jenkins, el primer comando corresponde al stage `Unit Tests` y el segundo al stage `E2E Tests`; no se repite `package` entre ambos stages.

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

Un fallo de conexión a MongoDB es un problema de configuración o disponibilidad de la conexión; un fallo HTTP después de arrancar la aplicación corresponde al funcionamiento de la integración. El host configurado debe ser resoluble desde el agente Jenkins. Por ejemplo, `mongo` solo funciona si Jenkins comparte una red Docker donde exista ese alias; para una base externa debe usarse un hostname o IP accesible desde Jenkins.

Jenkins limita el stage E2E a diez minutos y publica los XML de Failsafe como reportes JUnit. Si MongoDB no es accesible, la prueba fallará por timeout del driver y el stage quedará limitado por ese timeout de Jenkins.

## Integración continua

Jenkins ejecuta dos stages antes de construir y desplegar la imagen:

1. `Unit Tests`: `mvn -B -ntp clean test`.
2. `E2E Tests`: ejecuta únicamente Failsafe para `ProductoApiIT` usando las clases ya compiladas en el stage anterior.

Un fallo en cualquiera de los dos stages detiene el pipeline antes del despliegue y permite identificar si el problema pertenece a unitarias o a integración.

El stage E2E no necesita acceso a Docker para ejecutar las pruebas. El agente Jenkins puede seguir usando Docker en stages posteriores para construir la imagen.

El agente usa el volumen Docker nombrado `products-maven-repository` montado en `/root/.m2`. Ese volumen conserva las dependencias y plugins entre ejecuciones del pipeline. La primera ejecución puede descargar dependencias; las siguientes deben reutilizar el cache.

## Buenas prácticas

- No ejecutar las pruebas E2E contra MongoDB de desarrollo.
- Usar SKU y slug únicos dentro de cada escenario.
- Mantener los datos de cada prueba autocontenidos.
- Validar el contrato HTTP, no detalles internos de repositorios o servicios.
- No comparar timestamps contra valores exactos.