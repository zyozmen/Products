# Products API

Microservicio REST para la gestión de catálogo de productos, usuarios, órdenes de compra y sesiones de la plataforma **GrowShop**. Está construido con Java 21, Spring Boot 3 y MongoDB, siguiendo una arquitectura hexagonal (puertos y adaptadores).

## Tabla de contenido
1. [Capacidades](#1-capacidades)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Arquitectura](#3-arquitectura)
4. [Seguridad y gestión de sesión](#4-seguridad-y-gestión-de-sesión)
5. [Configuración](#5-configuración)
6. [Construcción y pruebas](#6-construcción-y-pruebas)
7. [Ejecución](#7-ejecución)
8. [Referencia de la API](#8-referencia-de-la-api)
9. [Manejo de errores](#9-manejo-de-errores)
10. [Despliegue](#10-despliegue)
11. [Estructura del repositorio](#11-estructura-del-repositorio)

## 1. Capacidades
- **Catálogo:** consulta, creación, actualización y eliminación de productos, categorías, productos destacados y carga de imágenes.
- **Usuarios:** registro público (cuenta en estado `PENDIENTE` hasta aprobación), perfil propio y administración de estados (`ACTIVO`, `INACTIVO`, `PENDIENTE`) exclusiva del rol `ADMIN`.
- **Autenticación y sesión:** login con JWT, renovación de token, cierre de sesión con revocación en servidor y control de inactividad parametrizable.
- **Órdenes:** creación con validación de precios e IVA en backend, consulta por usuario, gestión administrativa con auditoría de estados, PQR y reportes.
- **Proveedores y saldos (HU-01):** alta de proveedores, recepción de lotes con cálculo de costo y abono inicial opcional, abonos posteriores, saldos transaccionales y evaluación de la liquidación diaria con tope de pago.
- **Operación:** documentación OpenAPI/Swagger, endpoints de Actuator, imagen Docker lista para producción.

## 2. Stack tecnológico
| Componente | Tecnología |
|---|---|
| Lenguaje / plataforma | Java 21, Spring Boot 3.3.5 |
| Construcción | Maven (wrapper incluido) |
| Persistencia | MongoDB (Spring Data MongoDB) |
| Seguridad | Spring Security (stateless), JWT con jjwt 0.12.6, BCrypt |
| Validación | Jakarta Validation |
| Documentación | springdoc-openapi 2.5.0 (Swagger UI) |
| Observabilidad | Spring Boot Actuator |
| Calidad | JUnit 5, Mockito, JaCoCo (cobertura mínima de líneas 80 %), SonarQube |

## 3. Arquitectura
El código sigue una arquitectura hexagonal bajo `com.zyozmen.products`:

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Dominio | `domain.model`, `domain.exception` | Modelos y excepciones de negocio, sin dependencias de framework. |
| Puertos de entrada | `domain.port.in` | Casos de uso: `UserUseCase`, `ProductoUseCase`, `OrderUseCase`, `SessionUseCase`, `ProveedorUseCase`, `LiquidacionUseCase`. |
| Puertos de salida | `domain.port.out` | Contratos de persistencia: `UserRepositoryPort`, `ProductoRepositoryPort`, `OrderRepositoryPort`, `RevokedTokenRepositoryPort`, `ProveedorRepositoryPort`, `LoteRepositoryPort`, `TransaccionProveedorRepositoryPort`. |
| Aplicación | `application.service` | Implementación de los casos de uso. |
| Adaptadores de entrada | `adapter.in.web` | Controladores REST, DTOs y mapeadores web. |
| Adaptadores de salida | `adapter.out.mongodb` | Documentos, repositorios, mapeadores y adaptadores MongoDB. |
| Seguridad | `security` | `JwtService` y `JwtAuthenticationFilter`. |
| Transversal | `config`, `exception` | Seguridad, CORS, OpenAPI, MongoDB y manejo global de errores. |

## 4. Seguridad y gestión de sesión
La API es stateless y autentica con tokens JWT enviados en el encabezado `Authorization: Bearer <token>`.

- **Emisión:** el token se firma con HMAC y se emite con una vigencia de **1 hora** (`exp`). Incluye los claims `sub` (usuario), `role`, `iat` y `jti` (identificador único para revocación).
- **Autorización:** los roles son `ADMIN` y `CLIENTE`. Las rutas `/api/admin/**` y las escrituras de `/api/productos/**` y todo `/api/proveedores/**`, `/api/lotes/**` y `/api/liquidaciones/**` requieren `ADMIN`; `/api/orders/**` y `/api/users/**` requieren autenticación.
- **Revocación:** al cerrar sesión o renovar, el `jti` se registra en la colección `revoked_tokens`. Un índice TTL elimina cada registro cuando el token habría expirado, por lo que la lista no crece indefinidamente.
- **Inactividad:** el límite es parametrizable (`app.session.inactivity-timeout-minutes`, 15 minutos por defecto). Como el servidor no mantiene estado por sesión, el límite se aplica en la renovación: un token que no se renueva dentro de ese lapso ya no puede refrescarse. El cliente debe llamar a `/refresh` mientras haya actividad del usuario.
- **Respuesta de sesión vencida:** cualquier token expirado, revocado o inactivo se rechaza con `401` y el cuerpo:

```json
{ "code": "SESSION_EXPIRED", "message": "Inactivity limit reached" }
```

### Endpoints de sesión
Disponibles en `/api/v1/auth` (también se mantiene el alias `/api/auth`).

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Registro público (requiere `email`). Siempre crea una cuenta con rol `CLIENTE` en estado `PENDIENTE` y envía un correo de bienvenida de forma asíncrona; un fallo de envío no afecta el registro. |
| `POST` | `/api/v1/auth/login` | Autentica por **correo electrónico** y contraseña; retorna el JWT y el perfil del usuario. |
| `POST` | `/api/v1/auth/refresh` | Valida el token actual (firma, expiración, revocación, inactividad, usuario activo), lo revoca y emite uno nuevo de 1 hora. |
| `POST` | `/api/v1/auth/logout` | Revoca el token. Es idempotente. Responde `204`; con `{"reason":"INACTIVITY"}` responde `401 SESSION_EXPIRED` tras revocar. |

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"zyozmen@gmail.com","password":"<password>"}'

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Authorization: Bearer <token>"

curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"INACTIVITY"}'
```

## 5. Configuración
Toda la configuración se define en `src/main/resources/application.properties` y se sobrescribe mediante variables de entorno.

| Variable | Propiedad | Valor por defecto | Descripción |
|---|---|---|---|
| `PORT` | `server.port` | `8080` | Puerto HTTP. |
| `SPRING_DATA_MONGODB_URI` | `spring.data.mongodb.uri` | URI local | Cadena de conexión a MongoDB. |
| `MONGO_DATABASE` | `spring.data.mongodb.database` | `GrowShop` | Base de datos. |
| `JWT_SECRET` | `app.jwt.secret` | clave de desarrollo | Clave de firma de los JWT. **Obligatoria en producción.** |
| `JWT_EXPIRATION_MS` | `app.jwt.expiration-ms` | `3600000` (1 h) | Vigencia del token. |
| `SESSION_INACTIVITY_TIMEOUT_MINUTES` | `app.session.inactivity-timeout-minutes` | `15` | Límite de inactividad para renovar sesión. |
| `MAIL_USERNAME` | `spring.mail.username` / `app.mail.from` | `zonagreen9@gmail.com` | Cuenta SMTP y remitente de los correos. |
| `MAIL_PASSWORD` | `spring.mail.password` | _(vacío)_ | App Password de Gmail (requiere verificación en 2 pasos). Nunca se versiona. |
| `MAIL_ENABLED` | `app.mail.enabled` | `true` | Activa o desactiva el envío de correos. |
| `ADMIN_USERNAME` | `app.admin.username` | `zyozmen` | Username del administrador principal (protegido contra inactivación). El usuario ya no se siembra: se inserta manualmente en la colección `Users`. |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | `*` | Orígenes permitidos; restringir en producción. |
| `LOG_LEVEL_MONGO_TEMPLATE`, `LOG_LEVEL_MONGO_COMMAND` | `logging.level.*` | `INFO` | Nivel de logs de MongoDB. `DEBUG` expone datos de las consultas. |

### Gestión de secretos
- Nunca versione credenciales, URIs con contraseña ni la clave `JWT_SECRET`.
- En plataformas como Render, los *Secret Files* montados en `/etc/secrets` se cargan automáticamente como variables de entorno mediante `entrypoint.sh`.

### Conexión a MongoDB
Ejemplo para MongoDB Atlas (PowerShell):

```powershell
$env:SPRING_DATA_MONGODB_URI="mongodb+srv://<usuario>:<password>@<cluster>.mongodb.net/GrowShop?retryWrites=true&w=majority"
$env:MONGO_DATABASE="GrowShop"
```

Sin variables definidas, la aplicación usa una instancia local en `localhost:27017`.

> **Transacciones:** el módulo de proveedores usa `@Transactional` con `MongoTransactionManager` (definido en `MongoConfig`), que exige un **replica set** (MongoDB Atlas lo cumple). Un MongoDB local *standalone* fallará en esas operaciones; arránquelo como replica set de un solo nodo.

## 6. Construcción y pruebas
Requisitos: JDK 21 y una instancia de MongoDB accesible (Maven se ejecuta con el wrapper `mvnw`).

```bash
./mvnw clean compile     # compilación
./mvnw test              # pruebas unitarias + reporte JaCoCo
./mvnw clean package     # genera target/products-0.0.1-SNAPSHOT.jar
./mvnw clean verify      # pruebas + reporte + validación del mínimo de cobertura
```

El reporte de cobertura se genera en `target/site/jacoco` y la fase `verify` exige al menos 80 % de cobertura de líneas. `test` y `package` no ejecutan la regla `jacoco-check`; para validar el umbral localmente o en CI se debe ejecutar `./mvnw clean verify` (en Windows: `.\mvnw.cmd clean verify`). Las clases de `dto`, `model`, `mapper`, `config` y la clase `Application` se excluyen del cálculo.

## 7. Ejecución
```bash
./mvnw spring-boot:run
# o con el artefacto empaquetado
java -jar target/products-0.0.1-SNAPSHOT.jar
```

Con Docker (imagen multi-etapa sobre Eclipse Temurin 21, ejecutada con usuario sin privilegios):

```bash
docker build -t products-api:local .
docker run --rm -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI="<uri>" \
  -e JWT_SECRET="<secreto>" \
  products-api:local
```

Documentación interactiva:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 8. Referencia de la API
La especificación completa y actualizada está en Swagger UI. Resumen de rutas:

| Recurso | Ruta base | Acceso |
|---|---|---|
| Autenticación | `/api/v1/auth` | Público |
| Productos (lectura) | `/api/productos` (`GET`, `/{id}`, `/featured`, `/categories`) | Público |
| Productos (escritura) | `/api/productos` (`POST`, `PUT`, `DELETE`, `/{id}/images`) | `ADMIN` |
| Perfil de usuario | `/api/users/profile` (`GET`, `PUT`) | Autenticado |
| Órdenes del cliente | `/api/orders` (`POST`, `/mine`, `/{id}`) | Autenticado |
| Administración de usuarios | `/api/admin/users` (`GET`, `PUT /{username}/state` con `{"state":"ACTIVO"}`) | `ADMIN` |
| Administración de órdenes | `/api/admin/orders` (listado, `/{id}`, `/{id}/status`, `/pqr`, `/report`) | `ADMIN` |
| Proveedores | `/api/proveedores` (`POST`, `GET /{id}`, `POST /{id}/pagos`) | `ADMIN` |
| Lotes | `/api/lotes` (`POST`) | `ADMIN` |
| Liquidaciones | `/api/liquidaciones/procesar-evento` (`POST`) | `ADMIN` |

### Órdenes
- El propietario de la orden se toma del token; el cliente no puede indicar el `username`.
- `POST /api/orders` valida que cada producto esté activo, recalcula precios vigentes e IVA (16 %) y responde `422` si los valores enviados no coinciden.
- Una orden de otro usuario responde `404`.
- `GET /api/admin/orders` admite los filtros `status`, `from`, `to` (formato `YYYY-MM-DD`), `username`, `page` y `size`; con paginación devuelve una lista plana y el encabezado `X-Total-Count`.
- Estados válidos: `CREADA`, `PAGADA`, `EMPACADA`, `ENTREGADA` y `CANCELADA`. Los cambios de estado se auditan y las transiciones no permitidas responden `409`.
- Los ítems y el historial de estados se almacenan embebidos en el documento de la orden para garantizar escrituras atómicas.

Ejemplo de creación:
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    { "productId": "12", "name": "Producto", "quantity": 2, "unitPrice": 15000, "lineTotal": 30000 }
  ],
  "delivery": {
    "recipientName": "Juan Pérez",
    "address": "Calle 10 # 20-30",
    "addressComplement": "",
    "contactPhone": "3001234567"
  },
  "subtotal": 30000,
  "taxAmount": 4800,
  "total": 34800
}
```

### Proveedores, lotes y liquidación (HU-01)
Colecciones MongoDB: `proveedores`, `lotes` y `transacciones_proveedor`. Los montos son `BigDecimal` con 2 decimales y se guardan como `Decimal128`. La guía detallada para el frontend (campos, validaciones, ejemplos y tipos TypeScript) está en [docs/proveedores-api-frontend.md](docs/proveedores-api-frontend.md).

- **Crear proveedor** (`POST /api/proveedores`): inicia `totalDeuda`, `totalPagado` y `saldoPendiente` en `0.00`, estado `ACTIVO`; el NIT es único (`422` si se repite).
- **Recibir lote** (`POST /api/lotes`): `costoTotalLote = Σ(cantidadUnidades × costoUnitario)`; suma ese costo a `totalDeuda` y registra una transacción `DEUDA_LOTE`. Si hay `abonoInicial > 0` valida que sea `<= costoTotalLote`, lo suma a `totalPagado` y registra `ABONO_INICIAL`. `saldoPendiente = max(0, totalDeuda - totalPagado)`. El lote queda `PENDIENTE`, `PAGO_PARCIAL` o `PAGADO_TOTAL` (saldo del lote en cero). Todo ocurre en una transacción.
- **Registrar pago** (`POST /api/proveedores/{id}/pagos`): abono a un lote (`loteId`) o, si se omite, a los lotes pendientes del más antiguo al más reciente. Tipos permitidos: `ABONO` (por defecto), `PAGO_CORTESIA`, `AJUSTE_PQR`. No puede superar el saldo del proveedor ni del lote.
- **Procesar evento** (`POST /api/liquidaciones/procesar-evento`): evalúa si una venta/PQR acumula a la liquidación diaria sin modificar saldos ni inventario. Si el lote o el proveedor no tienen saldo pendiente devuelve `acumulaLiquidacion=false` y `montoLiquidable=0.00` (tope de pago); en caso contrario limita el monto al menor saldo. `descontarStock` siempre es `true`: el descuento de inventario lo ejecuta quien invoca el servicio.

## 9. Manejo de errores
Un manejador global normaliza las respuestas de error:

| Código | Caso |
|---|---|
| `400` | Validación de entrada, reglas de negocio de registro y administrador protegido. |
| `401` | Credenciales inválidas, o sesión vencida (`SESSION_EXPIRED`). |
| `403` | Usuario pendiente de aprobación (`user_pending_approval`), usuario inactivo (`user_inactive`) o rol insuficiente. |
| `404` | Recurso no encontrado. |
| `409` | Conflicto de estado en órdenes o modificación concurrente (bloqueo optimista). |
| `422` | Orden con precios o totales inconsistentes, o regla financiera de proveedores violada (NIT/lote duplicado, abono mayor al costo o al saldo, proveedor inactivo). |
| `503` | Servicio o infraestructura no disponible. |
| `500` | Error no controlado. |

## 10. Despliegue
- La imagen Docker (`dockerfile`) compila el proyecto en una etapa y ejecuta únicamente el JAR en una imagen JRE 21, con `MaxRAMPercentage=75` y un usuario sin privilegios.
- El `entrypoint.sh` carga los secretos montados en `/etc/secrets` antes de iniciar la aplicación, y la aplicación escucha en la variable `PORT` que inyecta la plataforma.
- Verifique la salud del servicio mediante `/actuator/health`.
- El análisis estático se integra con SonarQube (`sonar-maven-plugin`) usando el reporte de JaCoCo.

## 11. Estructura del repositorio
```text
├── pom.xml                  Definición Maven y dependencias
├── dockerfile               Imagen multi-etapa de producción
├── entrypoint.sh            Carga de secretos y arranque
├── src/main/java            Código fuente (hexagonal)
├── src/main/resources       application.properties
├── src/test/java            Pruebas unitarias (incl. ProveedorServiceTest y LiquidacionServiceTest)
└── docs/                    Documentación (guía de la API de proveedores para frontend)
```
