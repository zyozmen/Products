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
- **Recepción (HU-02):** lotes con contenedores por tipo/cepa, inventario en unidades base, código interno automático e historial de abonos con imputación FIFO.
- **Empaquetado (HU-03):** presentaciones con capacidad validada, costo calculado, confirmación de margen negativo y publicación transaccional en `productos_marketplace`.
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
| Puertos de entrada | `domain.port.in` | Casos de uso: `UserUseCase`, `ProductoUseCase`, `CategoryUseCase`, `OrderUseCase`, `SessionUseCase`, `ProveedorUseCase`, `LiquidacionUseCase`. |
| Puertos de salida | `domain.port.out` | Contratos de persistencia: `UserRepositoryPort`, `ProductoRepositoryPort`, `CategoryRepositoryPort`, `OrderRepositoryPort`, `RevokedTokenRepositoryPort`, `ProveedorRepositoryPort`, `LoteRepositoryPort`, `TransaccionProveedorRepositoryPort`. |
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
- **Inactividad:** el límite es parametrizable (`app.session.inactivity-timeout-minutes`, 60 minutos por defecto). Como el servidor no mantiene estado por sesión, el límite se aplica en la renovación: un token que no se renueva dentro de ese lapso ya no puede refrescarse. El cliente debe llamar a `/refresh` mientras haya actividad del usuario.
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
| `SESSION_INACTIVITY_TIMEOUT_MINUTES` | `app.session.inactivity-timeout-minutes` | `60` | Límite de inactividad para renovar sesión. |
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

> **Transacciones:** proveedores y los movimientos de inventario de órdenes usan `@Transactional` con `MongoTransactionManager` (definido en `MongoConfig`), que exige un **replica set** (MongoDB Atlas lo cumple). Un MongoDB local *standalone* fallará en esas operaciones; arránquelo como replica set de un solo nodo.

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
| Productos (lectura) | `/api/productos` y `/api/v1/products` (`GET`, `/{id}`, `/featured`, `/categories`) | Público |
| Productos (escritura) | `/api/productos` y `/api/v1/products` (`POST`, `PUT`, `DELETE`, `/{id}/images`) | `ADMIN` |
| Categorías (lectura) | `/api/v1/categories` (`GET`, `/{id}`, `/{id}/image`) | Público |
| Categorías (escritura) | `/api/v1/categories` (`POST`, `PUT /{id}`) | `ADMIN` |
| Carga unificada de imágenes | `POST /api/v1/images/{id}?target=PRODUCT\|CATEGORY` | `ADMIN` |
| Perfil de usuario | `/api/users/profile` (`GET`, `PUT`) | Autenticado |
| Órdenes del cliente | `/api/orders` (`POST`, `/mine`, `/{id}`) | Autenticado |
| Administración de usuarios | `/api/admin/users` (`GET`, `PUT /{username}/state` con `{"state":"ACTIVO"}`) | `ADMIN` |
| Administración de órdenes | `/api/admin/orders` (listado, `/{id}`, `/{id}/status`, `/pqr`, `/report`) | `ADMIN` |
| Proveedores | `/api/proveedores` (`POST`, `GET /{id}`, `POST /{id}/pagos`) | `ADMIN` |
| Lotes | `/api/lotes` (`POST`) | `ADMIN` |
| Liquidaciones | `/api/liquidaciones/procesar-evento` (`POST`) | `ADMIN` |

### Categorías, referencias e imágenes
- Las categorías se crean de forma independiente de los productos. `POST /api/v1/categories` acepta JSON con `name` obligatorio (hasta 100 caracteres), `description` opcional (hasta 1000) e `imageUrl` opcional (URL HTTP/HTTPS de hasta 2048 caracteres). `PUT /api/v1/categories/{id}` usa el mismo contrato.
- Se almacenan en `categories` mediante `CatalogCategoryDocument`; no se modifica el tipo `CategoryDocument` usado por las categorías embebidas históricas. El campo Java `categoryId` es un `Long` generado atómicamente con `categories_sequence` y se persiste como `_id` numérico de MongoDB. La respuesta lo expone como `id`. `createdAt` y `updatedAt` son `LocalDateTime` en UTC.
- El nombre se guarda sin espacios al inicio o al final y tiene un índice único (sensible a mayúsculas/minúsculas). Un duplicado devuelve `409`. Al arrancar se aseguran ese índice y el índice de `Products.categoryId`; un fallo de creación no se oculta.
- Los productos siguen en **`Products`**, sin cambiar los IDs ni retirar las rutas existentes. En creación/edición se puede enviar `"categoryId": 7`: el servicio verifica que exista antes de guardar (`404` si no existe). No se duplica el nombre de la categoría en esta nueva referencia. Si se omite en edición, se conserva la referencia existente.
- `GET /api/v1/products?categoryId=7` (también `/api/productos?categoryId=7`) filtra por la referencia numérica y admite los filtros y paginación habituales (`page`, `size`, `sort_by`, `sort_dir`, `min_price`, `max_price`, `min_rating`, `max_rating`, `name`). Las respuestas de detalle y catálogo incluyen `categoryId`; el resumen también conserva `category_ids`.
- Por compatibilidad se mantiene la lectura de `categories.category_id` en documentos históricos que todavía no tienen `categoryId`. No se ejecuta una migración automática: antes de asociar datos históricos con el catálogo nuevo, deben reconciliarse los IDs numéricos existentes. `GET /api/productos/categories` y su alias devuelven ahora las categorías de la colección independiente, incluso sin productos; `slug` y `products_count` históricos no se calculan en esa respuesta.
- La carga sigue guardando Base64 en MongoDB, sin filesystem ni proveedor externo. `POST /api/v1/images/{id}` recibe `multipart/form-data` con `target=PRODUCT` o `target=CATEGORY`, `fotoPrincipal` y `fotosSecundarias`. El destino indica si `{id}` es un ID de producto o de categoría. La ruta existente `POST /api/productos/{id}/images` admite el mismo parámetro, con `PRODUCT` por defecto.
- Para productos se admite una principal y hasta 5 secundarias. Para categorías se exige la principal y **se ignoran las secundarias sin procesarlas**. Se aceptan extensiones PNG, JPG/JPEG y WebP con MIME correspondiente, hasta **2 MiB (2 097 152 bytes) por imagen**. El límite HTTP total es 13 MiB. Los archivos inválidos devuelven `400`; si el contenedor rechaza el tamaño multipart, devuelve `413`.
- Al cargar una categoría se devuelve `imageUrl: "/api/v1/categories/{id}/image"`, una ruta pública relativa al origen de la API que entrega los bytes y MIME de la principal. La respuesta JSON no incluye el Base64. En una edición sin `imageUrl` se conserva la imagen anterior; una URL externa nueva reemplaza la imagen almacenada.

Ejemplo (requiere autenticación `ADMIN`):
```http
POST /api/v1/categories
Content-Type: application/json

{
  "name": "Audio",
  "description": "Parlantes y auriculares",
  "imageUrl": "https://cdn.example.com/audio.webp"
}
```

Respuesta `201`:
```json
{ "id": 7, "name": "Audio", "description": "Parlantes y auriculares", "imageUrl": "https://cdn.example.com/audio.webp" }
```

Para asignar una imagen local a esa categoría, enviar `POST /api/v1/images/7` con los campos multipart `target=CATEGORY` y `fotoPrincipal=@audio.webp`.

#### Especificación de imágenes de categorías

| Característica | Especificación |
|---|---|
| Cantidad | Una imagen principal por categoría; las secundarias se ignoran. |
| Tamaño máximo permitido | **2 MiB (2 097 152 bytes)** por archivo. |
| Formatos permitidos | **WebP**, **JPG/JPEG** y **PNG**, con MIME correspondiente: `image/webp`, `image/jpeg` o `image/png`. |
| Formato recomendado | **WebP** para reducir el peso; JPG para fotografías o PNG cuando se necesite compresión sin pérdida. |
| Proporción recomendada | **1:1 (cuadrada)**, con el contenido principal centrado y margen para recortes. |
| Resolución recomendada | **800 × 800 píxeles**, sin ampliar imágenes de menor resolución. |
| Calidad de exportación recomendada | **80–85 % para WebP o JPG**, ajustándola si aparecen artefactos visibles. PNG usa compresión sin pérdida, no un porcentaje de calidad equivalente. |
| Peso objetivo recomendado | **Hasta 200 KB**, siempre que se conserve una buena apariencia; no es un límite del servicio. |
| Color recomendado | **sRGB**, para mantener una apariencia consistente en navegadores. |

**Validaciones actuales:** el servicio comprueba extensión, MIME y tamaño del archivo. No comprueba dimensiones, proporción, calidad visual ni contenido real mediante decodificación. Las recomendaciones de resolución, calidad y peso objetivo son criterios de preparación, no restricciones del backend.

El backend no redimensiona, recomprime ni mejora las imágenes: almacena los bytes recibidos como Base64 y los entrega sin modificar su calidad. La imagen debe prepararse antes de cargarla. Estos límites de carga no verifican imágenes externas indicadas mediante `imageUrl`.

### Órdenes
- El propietario de la orden se toma del token; el cliente no puede indicar el `username`.
- Desde HU-05/T1, `productId` en ventas y PQR es el **ID de la presentación de marketplace**, no el ID numérico del catálogo anterior. El catálogo anterior sigue disponible en sus rutas; no se consulta para crear nuevas órdenes. Las órdenes ya guardadas siguen siendo consultables.
- `POST /api/orders` valida que la presentación esté activa y tenga procedencia en un lote, recalcula precios vigentes e IVA (16 %) y responde `422` si los valores enviados no coinciden. Un ID mal formado responde `400`; una presentación inexistente o inactiva, `404`.
- Cada ítem nuevo guarda `trazabilidad` interna: proveedor, lote, contenedor, cepa, presentación, contenido, unidad base, costo base y costo por presentación. Los datos se resuelven en el servidor; no se aceptan costos del cliente. El costo congelado no cambia al actualizar el catálogo y no se expone en `OrderResponseDTO`.
- Las PQR también requieren `productId` válido y resuelven nombre y costo desde el servidor. `name` es opcional y no se usa como fuente; sus precios e importes comerciales son cero.
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
    { "productId": "650000000000000000000401", "quantity": 2, "unitPrice": 15000, "lineTotal": 30000 }
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

### Catalogo y detalle de marketplace por cepa (HU-05/T1 ampliada)

`GET /api/marketplace/cepas/{cepaId}/presentaciones/{presentacionId}` es público
y devuelve el nombre del producto (tipo/cepa), la presentación seleccionada y
las presentaciones activas con stock positivo de esa misma cepa, incluso de distintos lotes,
contenedores y proveedores. Cada opción conserva su propio ID, SKU, contenido,
unidad base, precio, stock y fotografía; presentaciones de distintos lotes no
se fusionan ni comparten existencias. No devuelve costos ni saldos internos.

Las cepas se administran antes de recibir lotes y se almacenan en `cepas`.
Su ID es un UUID persistido: renombrarlas no cambia sus referencias. El nombre
normalizado (sin espacios extremos, espacios internos colapsados y minusculas)
tiene indice unico. El detalle usa el nombre actual de la entidad, no la copia
historica del contenedor o de marketplace. Las respuestas administrativas de
lotes exponen `cepaId` en cada contenedor.

**Administracion (ADMIN):**
- `POST /api/cepas` con `{"nombre":"Tangerine","categoryIds":[7,8]}` devuelve `201`.
  `categoryIds` es opcional: hasta 100 IDs numericos positivos de categorias existentes.
  Se guardan como referencias en la cepa y son compartidas por todas sus presentaciones.
- `GET /api/cepas?page=0&size=20` devuelve un arreglo y `X-Total-Count`;
  `GET /api/cepas/{id}` devuelve la entidad, incluida su version.
- `PUT /api/cepas/{id}` con `{"nombre":"Nuevo nombre","version":0}` renombra
  conservando el ID y la fotografia. Version desactualizada y nombre duplicado
  normalizado: `409`; cepa inexistente: `404`. No se elimina una cepa referenciada.
  Puede incluir `categoryIds` para reemplazar las categorias; `[]` las elimina y
  omitirlo conserva las anteriores. IDs invalidos o categorias inexistentes: `400`.
  Las respuestas administrativas incluyen `categoryIds`; documentos anteriores
  sin ese campo se leen como `[]`. Renombrar o cargar fotos conserva la asignacion.
- `POST /api/v1/images/{cepaId}?target=CEPA` acepta multipart `fotoPrincipal`.
  Reutiliza el servicio de imagenes: PNG, JPG/JPEG o WebP hasta 2 MiB, con MIME
  correspondiente. Almacena Base64 en la entidad cepa e ignora secundarias.
  PRODUCT y CATEGORY conservan su contrato numerico anterior. Las rutas
  antiguas de imagenes de producto rechazan CEPA para evitar sobrescribir productos.

**Listado publico:** `GET /api/marketplace/presentaciones?buscar=10gr&page=0&size=20`

```json
{
  "content": [{
    "cepaId": "UUID-de-cepa",
    "nombre": "Tangerine",
    "fotografia": {"nombre": "tangerine.webp", "extension": "webp", "filepart": "BASE64"},
    "precioDesde": 8000,
    "presentaciones": [{
      "presentacionId": "650000000000000000000401",
      "sku": "TAN-5",
      "nombre": "Bolsa 5 g",
      "contenidoUnidad": 5,
      "unidadBase": "GRAMOS",
      "precioVenta": 8000,
      "stockDisponible": 12,
      "fotografia": null
    }]
  }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

- Pagina por **cepas**, ordenadas por nombre normalizado y desempate por ID.
  `page >= 0`, `size` entre 1 y 100; valores invalidos o busqueda de mas de
  100 caracteres: `400`. Sin coincidencias o fuera de rango, `content: []`.
- Incluye solo presentaciones `ACTIVO` con stock mayor que cero; excluye cepas
  sin opciones disponibles. `precioDesde` es el menor precio de esas opciones.
- Filtros opcionales: `categoryIds=7&categoryIds=8` (coincide con cualquiera),
  `minPrice=50000` y `maxPrice=100000`. Se combinan con la busqueda mediante AND,
  antes de contar y paginar dentro de MongoDB. Las cepas sin categorias siguen
  visibles sin este filtro, pero no coinciden con una categoria seleccionada.
- El rango de precio es inclusivo y ambos limites deben cumplirse en una misma
  presentacion disponible. No recorta opciones ni recalcula `precioDesde`: una
  cepa puede coincidir por una opcion aunque su precio minimo quede fuera del rango.
  Los precios deben ser no negativos, hasta 15 enteros y 2 decimales, y
  `minPrice <= maxPrice`; hasta 100 IDs de categoria positivos. Valores invalidos: `400`.
- Busca coincidencia parcial literal sin distinguir mayusculas por nombre de
  cepa o presentacion. Tambien interpreta contenido y unidad: `10gr`, `10 g`,
  `10 gramos`, `10ml` o `1 unidad`, reutilizando los alias de unidades existentes.
  No convierte unidades ni interpreta expresiones regulares del usuario.
- Una presentacion coincidente selecciona la cepa **con todas sus opciones
  disponibles**, no solo la coincidencia; el precio minimo tambien usa todas.
- Foto principal de tarjeta: fotografia de la cepa. Si no tiene imagen,
  `fotografia: null`; la aplicacion consumidora decide el placeholder.
  Se conserva ademas la fotografia propia de cada opcion.
- El detalle devuelve tambien `fotografia` de cepa y `precioDesde`. Un par
  cepa/presentacion incorrecto o una seleccion agotada responde `404`.
- La consulta pagina y cuenta dentro de MongoDB, sin cargar todo el catalogo en
  memoria Java. No necesita Atlas Search. La busqueda literal con `$indexOfCP`
  no ofrece relevancia, busqueda difusa ni normalizacion de acentos.

**Datos anteriores:** antes de desplegar, detener escrituras y verificar un
respaldo. Ejecutar primero la previsualizacion de
[reconciliacion HU-05](docs/migrations/hu05-cepas.js), revisarla y aplicar con
`globalThis.APPLY=true` en mongosh. Requiere replica set; no se ejecuta al
arrancar. Crea entidades compartidas desde nombres existentes y completa
referencias y datos de presentacion en marketplace, sin modificar ventas,
existencias ni saldos. Conserva los UUID deterministas anteriores cuando crea
esas entidades. Datos ambiguos detienen la reconciliacion antes de escribir.
Las nuevas recepciones requieren `cepaId`; el contrato solo con `tipoCepa`
ya no se acepta. Los lotes pendientes de reconciliar no pueden empaquetarse.

Para ejecutar las pruebas reales del catalogo, proporcionar
`HU05_TEST_MONGO_URI` apuntando a un MongoDB **aislado de pruebas**. Estas pruebas
crean bases `hu05_test_<UUID>` y eliminan exclusivamente esas bases al terminar.
Sin esa variable se omiten; el resto de las pruebas no requiere MongoDB.

El plan completo y el control de avance están en
[HU-05: plan de integración](docs/hu05-plan-integracion.md). T1 incorpora
trazabilidad y detalle. T2 integra los movimientos de inventario descritos abajo.
Los nuevos estados de PQR, la acumulación diaria y el reporte financiero
pertenecen a T3 y siguientes: no deben considerarse implementados por esta entrega.

### Inventario de ordenes (HU-05/T2)

- Crear una venta en `CREADA` no reserva ni descuenta existencias. Al pasar a
  `PAGADA` se descuenta la cantidad de cada presentacion, tambien en contra
  entrega. `EMPACADA` y `ENTREGADA` no vuelven a descontar.
- Crear una PQR descuenta inmediatamente inventario y mantiene sus valores
  comerciales en cero. En esta etapa conserva el estado inicial `CREADA`;
  crearla directamente en `PAGADA` sigue fuera de esta entrega. El bloqueo de
  cancelacion de ventas pagadas se incorpora posteriormente en HU-04/T2.
- Una cancelacion permitida (venta CREADA o PQR segun su estado) restituye solo el inventario
  previamente descontado. Repetir un cambio de estado ya aplicado devuelve
  `409` y no modifica existencias.
- El descuento usa la trazabilidad interna de cada item: proveedor, lote,
  contenedor, cepa y presentacion. Agrupa items repetidos de la misma
  presentacion; no comparte existencias entre lotes o presentaciones.
- Solo cambia `stockDisponible` de las presentaciones empaquetadas y de
  `productos_marketplace`. No cambia capacidad o cantidad base del contenedor,
  unidades creadas, costos, deuda ni pagos al proveedor. Un saldo del proveedor
  en cero no impide descontar stock.
- El lote, las proyecciones de marketplace y la orden se guardan en una unica
  transaccion MongoDB. Stock insuficiente, presentacion inactiva o proyeccion
  desincronizada devuelven `409` y revierten toda la operacion. Los conflictos
  concurrentes tambien devuelven `409`: consultar la orden antes de reintentar.
  No hay reintentos automaticos ni clave de idempotencia para crear PQR:
  dos POST validos representan dos PQR distintas.
- `inventory_deducted` es una marca interna persistida en la orden, no un campo
  de la API. Las ordenes anteriores sin esta marca no mueven stock
  retroactivamente al empacar, entregar o cancelar. Intentar pagar una orden
  sin trazabilidad valida devuelve `422`; no se infiere procedencia historica.

Las pruebas `InventarioOrdenIntegrationTest` usan `HU05_TEST_MONGO_URI` con un
**replica set aislado de pruebas**. Crean bases `hu05_t2_test_<UUID>` y eliminan
solo esas bases al terminar. Cubren reversion de varios lotes, fallo al guardar
una PQR y competencia real por existencias o por la misma orden. Sin la
variable se omiten. Por ejemplo, tras configurar la URI:

```powershell
.\mvnw.cmd "-Dtest=InventarioOrdenIntegrationTest,InventarioOrdenServiceTest,OrderServiceTest" test
```

### Liquidacion diaria (HU-04/T1)

Consulta administrativa real, independiente del evaluador existente
`POST /api/liquidaciones/procesar-evento` y del reporte comercial.
T1 incorporo la consulta y persistencia base. **T2 conecta solo ventas normales
al pasar a PAGADA**, con la misma transaccion de inventario/orden, y bloquea
cancelaciones de ventas ya pagadas. **T3 reconoce contra entrega al pasar a
ENTREGADA**, usando la fecha de entrega. **T4 reconoce PQR al crearlas y revierte
su costo al cancelarlas en la jornada original**, conservando la auditoria.
Los pagos vinculados quedan pendientes para T5.
No se cambian deuda, reporte comercial ni contratos de abonos.

```http
GET /api/v1/settlements/daily-summary?date=2026-10-23&providerId=6500000000000000000000ab&page=0&size=20
Authorization: Bearer <token ADMIN>
```

| Parametro | Regla |
|---|---|
| `date` | Opcional, fecha ISO `yyyy-MM-dd`; por defecto hoy en `America/Bogota` |
| `providerId` | Opcional, ObjectId de 24 caracteres hexadecimales; mayusculas aceptadas |
| `page` | Base cero, por defecto 0, debe ser >= 0 |
| `size` | Por defecto 20, entre 1 y 100 |

La zona de esta funcionalidad es **fija y no parametrizable**:
`America/Bogota`, UTC-05:00, equivalente a la hora de Lima.
No depende de la zona del navegador, del servidor ni de `app.report.timezone`
(que conserva su comportamiento en el reporte anterior). La fecha de jornada
se devuelve sin hora. Los instantes usan ISO-8601 UTC.

Respuesta real de una base sin movimientos (la fecha cambia segun el dia):

```json
{
  "date": "2026-10-23",
  "timezone": "America/Bogota",
  "activationPending": false,
  "activatedAt": "2026-10-23T20:00:00Z",
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

No se generan proveedores ficticios ni filas con ceros. Cada fila representa un
registro persistido de proveedor/jornada; las jornadas revertidas o con pagos
seguiran visibles. Un proveedor valido sin acumulados devuelve una pagina vacia.
El orden estable es `providerId` ascendente; filtros y paginacion se ejecutan en
MongoDB. El nombre y saldo actual se cargan en lote para los proveedores de la
pagina, sin consultar uno por uno ni cargar todos los proveedores.

Contrato de cada elemento de `content`:

| Campo | Significado |
|---|---|
| `providerId`, `providerName`, `date` | Identificacion, nombre actual y jornada |
| `totalSalesCount` | Unidades vigentes de presentaciones vendidas, no numero de ordenes |
| `totalPqrCount` | Unidades vigentes de presentaciones PQR, descontando las revertidas |
| `salesRecognizedAmount` | Costo bruto reconocido de ventas |
| `pqrRecognizedAmount` | Costo bruto reconocido de PQR |
| `reversedAmount` | Costo revertido, positivo, imputado a la jornada original |
| `grossAccumulatedAmount` | `salesRecognizedAmount + pqrRecognizedAmount` |
| `accumulatedAmount` | `grossAccumulatedAmount - reversedAmount` |
| `linkedPaymentsAmount` | Pagos asociados explicitamente a esa jornada |
| `pendingDailyAmount` | `max(accumulatedAmount - linkedPaymentsAmount, 0)` |
| `providerOutstandingBalance` | Saldo global actual del proveedor, incluso para consultas historicas |
| `suggestedPayableAmount` | `min(pendingDailyAmount, providerOutstandingBalance)` |
| `overpaidAmount` | `max(linkedPaymentsAmount - accumulatedAmount, 0)` |

Los importes son **numeros JSON** calculados con `BigDecimal`, expresados en COP
con dos decimales, sin redondeos silenciosos. No son ingreso comercial ni nueva
deuda. El costo unitario pertenece al futuro detalle de T6, no a esta fila.
El frontend debe mostrar los calculos del servidor, no recalcular importes.

Errores: `401` sin autenticacion valida, `403` sin rol ADMIN, `400` para fechas,
IDs o paginacion invalidos, `500` ante fallos de persistencia o datos financieros
incompletos. Los errores de validacion usan el formato global existente
(`status`, `error`, `message`, `path`, `timestamp`); autenticacion conserva
`message` y, si aplica, `code`. Nunca se devuelve una pagina vacia como sustituto
de una falla.

Persistencia:

- `provider_daily_accumulators`: documento por proveedor/jornada, `providerId`
  ObjectId, `date` como cadena ISO para evitar conversiones de zona horaria,
  cantidades `long` e importes `Decimal128`. No almacena el sugerido ni detalle
  ilimitado. Indice unico `{providerId: 1, date: 1}`; indice `{date: 1,
  providerId: 1}` para consultar un dia completo en orden.
- `daily_settlement_settings`: registro unico `_id: "daily-settlement"`.
  **Por decision confirmada para T2**, el primer arranque que incorpora T2 fija
  automaticamente `activatedAt` si el registro no existe o seguia pendiente
  (`null` en T1). El instante real se guarda en UTC y no cambia en reinicios;
  el ejemplo de respuesta es ilustrativo, no una fecha fija. Si ya habia un
  instante persistido se conserva. No hay endpoint, selector de zona ni
  parametro de activacion.
- Las ordenes creadas antes de ese instante se excluyen aunque se paguen despues.
  Una orden creada exactamente en el instante es elegible. No se borran ventas,
  inventario, deuda ni acumulados existentes: empezar de cero significa no
  importar ni reprocesar historia. No modificar ni eliminar el registro de
  inicio para "reiniciar" la funcionalidad.
- `provider_daily_movements`: movimientos auditables independientes, con orden,
  indice estable del item, tipo `SALE`, `PQR` o `PQR_REVERSAL`, procedencia completa, cantidad, contenido,
  unidad base, costo base snapshot, costo por presentacion, importe, instante de
  reconocimiento y jornada de Bogota. Indice unico
  `{orderId: 1, itemIndex: 1, movementType: 1}`. Las reversiones agregan
  `originalMovementId` como referencia al movimiento PQR original.

#### Reconocimiento de ventas normales (HU-04/T2)

- Al confirmar `PAGADA` sin contra entrega se reconoce
  `cantidad * costoPresentacion` del snapshot guardado al crear la orden, no el
  costo actual del contenedor ni el precio comercial. Se verifica la coherencia
  del snapshot con `contenidoUnidad * costoBasePorUnidad`, con el redondeo de
  dos decimales ya usado en su creacion.
- Items repetidos conservan movimientos separados por indice, pero sus unidades
  e importes se suman al mismo proveedor/jornada. Varios proveedores tienen
  acumulados independientes. El dia corresponde a `paidAt` en Bogota.
- Un proveedor sin saldo o un lote pagado siguen generando costo bruto; el
  sugerido se limita al saldo global vigente en cada consulta.
- Stock, orden, movimientos e incrementos diarios se confirman en una sola
  transaccion MongoDB (requiere replica set). Un fallo revierte todo. No hay
  listener asincrono ni reconocimiento posterior al commit.
- Repetir PAGADA devuelve `409`; el indice unico tambien rechaza un
  reconocimiento duplicado y revierte la transaccion. Conflictos de escritura
  concurrentes devuelven `409`: consultar la orden antes de reintentar; no hay
  reintentos automaticos. Un snapshot invalido devuelve `422`, sin costos cero
  de reemplazo ni escrituras parciales.
- Una venta que paso por PAGADA no se cancela, incluida contra entrega o
  EMPACADA, tambien cuando es anterior al inicio del acumulador. Intentarlo
  devuelve `409` sin restituir stock. CREADA sigue siendo cancelable.
- PQR conserva CREADA, stock y cancelaciones actuales; acumula y revierte
  costos financieros desde T4. Contra entrega sigue descontando stock en PAGADA
  pero no acumula hasta ENTREGADA, incorporado en T3.
  Empacar/entregar una venta normal no vuelve a reconocer costo.
- Para F2 se reutiliza `PUT /api/admin/orders/{id}/status` con el cuerpo actual
  `{"status":"PAGADA"}` (sin `cashOnDelivery: true`). Luego consultar de nuevo
  `daily-summary`. El cliente no debe incrementar totales de forma optimista
  ni ofrecer cancelar ventas ya pagadas.

#### Reconocimiento de contra entrega (HU-04/T3)

- Se reutiliza `PUT /api/admin/orders/{id}/status`. Al confirmar PAGADA con
  `cashOnDelivery: true` se descuenta stock como antes, sin reconocer costo.
  EMPACADA tampoco acumula. No se cambian el instante inicial persistido ni la
  exclusion de ordenes creadas antes de ese inicio.
- Al pasar de EMPACADA a ENTREGADA se validan `cashAmount` y `cardAmount` como
  hasta ahora: ambos obligatorios, no negativos y suma igual al total con la
  tolerancia existente de 0,01. Faltantes o suma incorrecta devuelven `422`;
  negativos devuelven `400`. No se alteran las reglas existentes para total cero.
- El costo reconocido usa cantidad y snapshot original, no el cobro al cliente.
  `timestamp` del movimiento es `deliveredAt` y la jornada se calcula en
  America/Bogota. Una venta pagada un dia y entregada otro acumula solo en el
  dia de entrega. Se mantiene tipo `SALE` y la misma clave unica por orden/item.
- Estado, importes de entrega, movimientos y acumulado se confirman en la misma
  transaccion. Si falla la entrega o persistencia, no quedan reconocimiento,
  importes ni estado parcial. El stock descontado previamente en PAGADA no se
  vuelve a mover ni se restituye al fallar una entrega.
- Entregas repetidas o concurrentes no duplican costo: consultar la orden ante
  `409` antes de reintentar. Entregar una venta normal no repite su costo.
  PQR conserva sus reglas actuales y reconoce costo al crearla desde T4.

Cuerpo para F3, usando los importes reales de la orden:

```json
{"status":"ENTREGADA","cashAmount":30000,"cardAmount":4800}
```

Tras exito, recargar orden y resumen de la fecha de entrega. No calcular ni
incrementar acumulados en el navegador. No se agregan endpoints ni cambios de
contrato JSON para esta etapa.

#### Reconocimiento y reversion de PQR (HU-04/T4)

- `POST /api/admin/orders/pqr` conserva contrato, estado inicial CREADA, precios,
  subtotal, impuestos y total comerciales en cero. Descuenta inventario una vez
  y reconoce `cantidad * costoPresentacion` del snapshot por item, tipo `PQR`,
  usando `createdAt` para la jornada de America/Bogota. No suma ventas ni ingreso
  comercial, ni crea deuda adicional. Reconoce incluso con proveedor sin saldo
  o lote pagado; se conserva el corte global de activacion ya persistido.
- Se reutiliza `PUT /api/admin/orders/{id}/status` para cancelar una PQR CREADA
  o EMPACADA. ENTREGADA sigue sin poder cancelarse. Empacar/entregar una PQR
  no repite su reconocimiento.
- Cancelar restituye stock y crea movimientos `PQR_REVERSAL` separados, enlazados
  por `originalMovementId`. Conservan cantidades, costos y procedencia del
  reconocimiento persistido, aunque los costos actuales hayan cambiado.
  `timestamp` registra la cancelacion real; `date` conserva la jornada original.
- Se disminuye `totalPqrCount` y aumenta `reversedAmount`, sin reducir el bruto
  `pqrRecognizedAmount` ni borrar movimientos o la fila de la jornada.
  `accumulatedAmount`, pendiente y sugerido se recalculan con la reversion.
  No se modifican pagos existentes: un exceso se muestra en `overpaidAmount`,
  sin deshacer transferencias. El registro de pagos vinculados se implementara
  en T5; su conservacion ya se verifica con datos persistidos de prueba.
- Una PQR historica sin movimiento original, incluida una creada entre T2 y T4,
  no genera reversion financiera ni importes negativos. Mantiene su restitucion
  de inventario existente; no hay importacion retroactiva.
- Creacion/cancelacion, orden, stock y movimientos/acumulados comparten la misma
  transaccion. Fallos de reconocimiento o de guardado revierten todo. Un
  acumulado original ausente/inconsistente devuelve `409`, no exito parcial.
  Cancelaciones repetidas o concurrentes no duplican restitucion ni reversion.

Ejemplo para F4 (ADMIN):

```json
{"username":"cliente","items":[{"productId":"ID_PRESENTACION","quantity":2}]}
```

Tras crear o cancelar, recargar orden, stock y
`GET /api/v1/settlements/daily-summary?date=YYYY-MM-DD` para la jornada de
creacion de la PQR en Bogota, no necesariamente hoy. No incrementar, descontar
ni eliminar filas localmente. El detalle paginado de auditoria queda para T6;
no se exponen costos internos en las respuestas de ordenes al cliente.
No reenviar automaticamente el POST ante timeout: verificar si la PQR se creo,
ya que dos creaciones validas representan dos ordenes distintas.

Pruebas de integracion HTTP reales (servidor en puerto aleatorio, JWT real y
MongoDB real, sin mocks de servicios/repositorios):

```powershell
$env:HU04_TEST_MONGO_URI = 'mongodb://127.0.0.1:<puerto-pruebas>/?directConnection=true'
$env:HU05_TEST_MONGO_URI = $env:HU04_TEST_MONGO_URI
.\mvnw.cmd "-Dtest=DailySettlementServiceTest,OrderSettlementServiceTest,OrderServiceTest,DailySettlementIntegrationTest,InventarioOrdenIntegrationTest" test
```

Usar exclusivamente una instancia aislada de pruebas. La suite crea una base
`hu04_t1_test_<UUID>` y elimina solo esa base al terminar. Usa fixtures tecnicas
para verificar precision, indices, filtros y formulas, y ejecuta el flujo real
HTTP de venta normal, contra entrega, creacion/cancelacion PQR y consulta del resumen sin mocks de servicios o
repositorios. No agrega endpoints de datos falsos para frontend. Las pruebas
de inventario/reconocimiento usan bases aisladas `hu05_t2_test_<UUID>`.
Sin la variable correspondiente se omite cada integracion.
Validacion T4: 519 pruebas Surefire aprobadas, sin fallos ni omisiones; cobertura
de lineas JaCoCo 92,90 %. Comando de regresion: `.\mvnw.cmd -DskipITs=true verify`
con ambas variables anteriores y la configuracion general de MongoDB dirigida
a la misma instancia aislada. Se excluyen las cinco pruebas E2E antiguas de
productos por su problema preexistente de escrituras sin autenticacion.
El consumo desde la aplicacion frontend debe validarse como F4 para PQR.

Planes: [backend T1-T8](HU04-tareas-backend.md) y
[frontend F1-F8](HU04-tareas-frontend.md).

### Proveedores, recepción de lotes y liquidación (HU-01 / HU-02)
Colecciones MongoDB: `proveedores`, `lotes` y `transacciones_proveedor`. Los montos son `BigDecimal` con 2 decimales y se guardan como `Decimal128`. Los contenedores se guardan dentro del lote; el historial de movimientos se guarda por separado y solo admite inserciones desde el puerto de persistencia.

La especificación de integración para el frontend está en [docs/proveedores-api-frontend.md](docs/proveedores-api-frontend.md), con contratos, ejemplos, validaciones, tipos TypeScript y flujos de actualización de la interfaz.

- **Crear proveedor** (`POST /api/proveedores`): inicia `totalDeuda`, `totalPagado` y `saldoPendiente` en `0.00`, estado `ACTIVO`; el NIT es único (`422` si se repite).
- **Buscar/listar proveedores** (`GET /api/proveedores?buscar=...&page=0&size=20`): coincidencia parcial literal en `nombre` **o** `nit`, sin distinguir mayúsculas. Elimina espacios al inicio y final; no normaliza acentos, puntuación del NIT ni espacios internos. Sin `buscar`, vacío o solo espacios, lista todos los proveedores. Incluye activos e inactivos; para recepción, seleccionar uno `ACTIVO`. Responde `200` con un arreglo plano de `ProveedorResponseDTO` (campos del proveedor, sin los conteos de lotes exclusivos de la consulta por ID) y encabezado `X-Total-Count` con el total de coincidencias. Orden ascendente por `nombre` y desempate por ID. Paginación obligatoria internamente: `page` desde cero (por defecto `0`), `size` entre `1` y `200` (por defecto `20`); parámetros inválidos devuelven `400`. Sin coincidencias o página fuera de rango: `[]`, conservando el total real en el encabezado.
- **Recibir lote** (`POST /api/lotes`): exige un proveedor activo y al menos un contenedor. `costoTotalLote = Σ(capacidadUnidadesBase × costoBasePorUnidad)`, redondeando cada subtotal a 2 decimales (`HALF_UP`); suma ese costo a `totalDeuda` y registra una transacción `DEUDA_LOTE`. Si hay `abonoInicial > 0` valida que sea `<= costoTotalLote`, lo suma a `totalPagado` y registra `ABONO_INICIAL` asociado al nuevo lote (no a lotes anteriores). `saldoPendiente = totalDeuda - totalPagado`. Lote y contenedores quedan en estado `RECIBIDO`; `estadoPago` es independiente: `PENDIENTE`, `PAGO_PARCIAL` o `PAGADO_TOTAL`. Cada contenedor inicia con `cantidadDisponible = capacidadUnidadesBase`. Todo ocurre en una transacción MongoDB, que requiere replica set (por ejemplo, Atlas).
- **Trazabilidad:** el servidor genera `codigoLote` con formato `LOT-YYYYMMDD-<UUID hexadecimal>` y fecha UTC, protegido por índice único, además de `fechaIngreso` e identificadores de contenedor. El cliente no envía código ni saldos calculados.
- **Consultar lote** (`GET /api/lotes/{id}`): devuelve el lote, los contenedores y sus cantidades disponibles en unidades base. El catálogo de productos no tiene un campo de stock; este inventario se registra en los contenedores, sin convertirlo en unidades de presentación.
- **Listar lotes por proveedor** (`GET /api/proveedores/{id}/lotes`): devuelve un arreglo paginado con la misma estructura de `GET /api/lotes/{id}` y encabezado `X-Total-Count` (total de lotes coincidentes, no contenedores ni presentaciones). Requiere `ADMIN`. Parámetros: `page` desde `0` (predeterminado `0`), `size` entre `1` y `200` (predeterminado `20`), `buscar` opcional y `sort` predeterminado `fechaIngreso,desc`. Admite ordenar por `fechaIngreso`, `codigoLote`, `costoTotalLote` o `saldoPendienteLote`, con dirección `asc` o `desc`; desempata por ID en la misma dirección. Se permite un solo criterio de orden. Parámetros inválidos devuelven `400`; proveedor inexistente, `404`; sin coincidencias o página fuera de rango, `200` con `[]` y el total real.
- **Registrar pago** (`POST /api/proveedores/{id}/pagos`): abono a un lote (`loteId`) o, si se omite, a los lotes pendientes del más antiguo al más reciente. Tipos permitidos: `ABONO` (por defecto), `PAGO_CORTESIA`, `AJUSTE_PQR`. No puede superar el saldo del proveedor ni del lote.
- **FIFO:** si el abono sin lote supera el saldo del más antiguo, lo salda y distribuye el excedente entre los siguientes; genera un movimiento por cada monto aplicado a cada lote. Se ordena por `fechaIngreso` ascendente, con desempate por identificador.
- **Consultar abonos** (`GET /api/proveedores/{id}/abonos?page=0&size=20`): arreglo JSON plano de movimientos paginados, del más reciente al más antiguo (desempate por identificador descendente), con `id`, `fecha` UTC, `monto`, `proveedorId`, `loteId`, `tipo` y `observaciones`. Incluye abonos iniciales y posteriores, cortesías y ajustes; excluye `DEUDA_LOTE`. El encabezado `X-Total-Count` indica el total de movimientos del proveedor, no el tamaño de la página; ya está expuesto por CORS. `page` inicia en `0` (predeterminado `0`) y `size` admite `1` a `200` (predeterminado `20`); parámetros inválidos devuelven `400`. Devuelve `[]` cuando no hay abonos o la página está fuera de rango, conservando el total real, y `404` si no existe el proveedor. No hay endpoints de edición ni eliminación de movimientos. **Cambio de comportamiento:** sin parámetros ahora devuelve únicamente los primeros 20 movimientos, no el historial completo.
- **Procesar evento** (`POST /api/liquidaciones/procesar-evento`): evalúa si una venta/PQR acumula a la liquidación diaria sin modificar saldos ni inventario. Si el lote o el proveedor no tienen saldo pendiente devuelve `acumulaLiquidacion=false` y `montoLiquidable=0.00` (tope de pago); en caso contrario limita el monto al menor saldo. `descontarStock` siempre es `true`: el descuento de inventario lo ejecuta quien invoca el servicio.

#### Consulta de proveedor con cantidades de lotes

`GET /api/proveedores/{id}` requiere rol `ADMIN` y conserva los campos del proveedor (`id`, `nombre`, `nit`, `contacto`, saldos, estado y fechas), agregando dos enteros en la misma raíz del JSON:

| Campo | Criterio |
|---|---|
| `lotesPagados` | Cantidad de lotes del proveedor con `saldoPendienteLote = 0`. |
| `lotesPorPagar` | Cantidad de lotes del proveedor con `saldoPendienteLote > 0`, incluidos los que tienen abonos parciales. |

Los conteos son globales del proveedor, sin paginación ni filtros de búsqueda, y se calculan al consultar, sin cargar contenedores ni fotografías. No dependen del estado de inventario (`RECIBIDO`, `PROCESADO`, etc.). Si no tiene lotes, ambos campos valen `0`; si el proveedor no existe, devuelve `404`. Los lotes históricos deben tener su saldo financiero migrado, igual que para los pagos FIFO.

Ejemplo de los campos financieros y conteos dentro de la respuesta (los demás campos del proveedor se conservan):

```json
{
  "totalDeuda": 8000000.00,
  "totalPagado": 3000000.00,
  "saldoPendiente": 5000000.00,
  "lotesPagados": 3,
  "lotesPorPagar": 2
}
```

Este cambio aplica a la consulta por ID; la búsqueda/listado, la creación y el registro de pagos conservan su respuesta anterior. Después de recibir un lote o registrar un abono, volver a consultar el proveedor para actualizar saldos y conteos.

#### Consulta de lotes para el frontend
Todas estas rutas requieren rol `ADMIN`.

```http
GET /api/proveedores/{id}/lotes
GET /api/proveedores/{id}/lotes?buscar=sativa&page=0&size=10&sort=fechaIngreso,desc
GET /api/proveedores/{id}/lotes?buscar=Bolsa&sort=codigoLote,asc
GET /api/proveedores/{id}/lotes?buscar=SAT-PREM-5G&sort=costoTotalLote,desc
GET /api/proveedores/{id}/lotes?sort=saldoPendienteLote,asc
```

La búsqueda coincide parcialmente con cualquiera de estos campos: nombre de cepa (`contenedores.cepa.nombre`), nombre de presentación o SKU (`contenedores.presentaciones.nombre/sku`). No distingue mayúsculas, elimina espacios exteriores y trata caracteres especiales como texto literal; no normaliza acentos, puntuación ni espacios interiores. Sin `buscar`, vacío o solo espacios, lista todos los lotes del proveedor, tengan o no saldo y presentaciones.

El filtro siempre se aplica al proveedor indicado, antes de paginar; la consulta devuelve cada lote una sola vez aunque varias cepas o presentaciones coincidan. Los lotes incluyen **todos** sus contenedores y presentaciones, no solo los coincidentes, y sus fotografías Base64 cuando existan. Los lotes históricos sin esos detalles aparecen en el listado sin búsqueda, pero no se infieren cepas o nombres de presentación para hacerlos coincidir.

Los nombres de `sort` son los de la API, no los persistidos: usar `fechaIngreso` (mapeado a `fechaRecepcion`) y `costoTotalLote` (mapeado a `costoTotalCalculado`). Enviar `sort=campo,asc` o `sort=campo,desc`; no enviar campos arbitrarios ni múltiples criterios.

En el frontend, usar `URLSearchParams`, reiniciar la página al cambiar búsqueda, proveedor u ordenamiento y leer `X-Total-Count` (ya expuesto por CORS) para calcular `Math.ceil(total / size)`. El valor predeterminado muestra los lotes más recientes primero. Con altas concurrentes, la paginación por desplazamiento puede cambiar entre peticiones: refrescar el listado después de registrar una recepción. La búsqueda parcial sobre campos anidados no requiere Atlas Search, pero puede recorrer documentos del proveedor; la paginación limita el tamaño de respuesta, no todo el costo de búsqueda.

#### Contrato de recepción HU-02

Para el selector de proveedor del frontend:

```http
GET /api/proveedores?buscar=distribuidora&page=0&size=20
GET /api/proveedores?buscar=900123&page=0&size=20
GET /api/proveedores?page=0&size=20
```

Mostrar `nombre` y `nit`; guardar el `id` seleccionado como `proveedorId`. Reiniciar `page=0` al cambiar el texto, codificar el parámetro con `URLSearchParams` y descartar respuestas de búsquedas anteriores. Se recomienda debounce de 300 ms para autocompletar. Leer `X-Total-Count` para calcular páginas; el encabezado ya está expuesto por CORS.

La búsqueda funciona con MongoDB estándar, sin Atlas Search ni índices de búsqueda externos. Se escapan los caracteres del usuario: `.` y `*`, por ejemplo, se buscan como texto literal, no como operadores. La coincidencia parcial sin distinguir mayúsculas puede requerir recorrer documentos; la paginación limita la respuesta, no el costo total de búsqueda. Para volúmenes altos, evaluar un índice de búsqueda dedicado.

```http
POST /api/lotes
Content-Type: application/json

{
  "proveedorId": "60d5ecb8f1d2c2a4c8e9a010",
  "contenedores": [
    {
      "cepaId": "UUID-de-cepa-sativa",
      "unidadBase": "gramo",
      "capacidadUnidadesBase": 1000,
      "costoBasePorUnidad": 5000
    },
    {
      "cepaId": "UUID-de-cepa-indica",
      "unidadBase": "gramo",
      "capacidadUnidadesBase": 500,
      "costoBasePorUnidad": 6000
    }
  ],
  "abonoInicial": 0
}
```

Responde `201` con `costoTotalLote: 8000000.00`, `montoPagadoLote: 0.00`, `saldoPendienteLote: 8000000.00`, `estado: "RECIBIDO"`, `estadoPago: "PENDIENTE"`, código, fecha y los contenedores con sus identificadores, subtotales y cantidades disponibles.

Cada contenedor exige `cepaId` existente y `unidadBase` no vacios, capacidad positiva (hasta 15 enteros y 3 decimales) y costo base positivo (hasta 15 enteros y 2 decimales). El servidor resuelve el nombre de la cepa; el texto enviado por el cliente no es su identidad. `presentacionId` es una referencia opcional al catalogo, no interviene en el calculo de unidades base. El valor total redondeado debe ser positivo. `abonoInicial` es opcional, admite cero y hasta 2 decimales; no puede superar el valor del lote.

Con HU-03, `unidadBase` se normaliza a `GRAMOS`, `MILILITROS` o `UNIDADES`; se mantienen los alias `g`, `gr`, `gramo`, `gramos`, `ml`, `mililitro`, `mililitros`, `unidad` y `unidades`, sin distinguir mayúsculas. Otras unidades devuelven `400`; no se realizan conversiones. Cada contenedor recibe un ObjectId y `codigoContenedor` automático. Opcionalmente puede recibirse `precioBaseUnidad` (no negativo, hasta 15 enteros y 2 decimales), que se guarda en la cepa como referencia, sin alterar costo ni deuda.

Una lista vacía o ausente devuelve `400`, con `"Debe agregar al menos un contenedor con su capacidad en unidades base para registrar el lote"` en `validationErrors`. Un abono excesivo devuelve `422` con, por ejemplo, `"El abono inicial no puede ser mayor al valor total del lote ($4,000,000 COP)"` en `message`; ninguna de estas validaciones escribe datos.

**Cambio incompatible autorizado:** `contenedores` reemplaza a `presentaciones` y el código pasa a generarse en el servidor. Los clientes deben actualizar el cuerpo de recepción y lectura. Los lotes históricos conservan internamente sus presentaciones para no perder el detalle al registrar pagos, pero su consulta devuelve `contenedores: []` y `estado: null` si no tienen esos datos; no se infieren cepas ni inventario ni se migra automáticamente información histórica.

### Empaquetado de contenedores (HU-03)

`POST /api/v1/lotes/{loteId}/contenedores/{contenedorId}/presentaciones` requiere `ADMIN`. Recibe un **arreglo JSON**, no un objeto envolvente:

```json
[
  {
    "sku": "SAT-PREM-5G",
    "nombre": "Bolsa 5g",
    "contenidoUnidad": 5,
    "unidadesCreadas": 100,
    "precioVenta": 45000,
    "activoMarketplace": true,
    "confirmacionMargenNegativo": false,
    "fotografia": {
      "nombre": "sat-prem-5g.jpg",
      "extension": "jpg",
      "filepart": "/9j/..."
    }
  },
  {
    "sku": "SAT-PREM-10G",
    "nombre": "Bolsa 10g",
    "contenidoUnidad": 10,
    "unidadesCreadas": 50,
    "precioVenta": 80000,
    "activoMarketplace": true,
    "confirmacionMargenNegativo": false
  }
]
```

Reglas del contrato:

- El lote y el contenedor deben existir y estar relacionados; de lo contrario, `404`.
- El arreglo no puede estar vacío ni contener elementos nulos. `sku` y `nombre` son obligatorios (máximos 100 y 200 caracteres). `contenidoUnidad` es positivo, con hasta 15 enteros y 3 decimales; `unidadesCreadas` es entero positivo (hasta el límite de `Integer`); `precioVenta` es no negativo, con hasta 15 enteros y 2 decimales. `fotografia` es opcional; si se envía, exige `nombre` (hasta 255 caracteres, con extensión), `extension` (`png`, `jpg`, `jpeg` o `webp`) y `filepart` (Base64 puro válido, sin prefijo `data:`). El contenido decodificado admite hasta **200 KiB (204800 bytes)**; el nombre y la extensión deben coincidir. Una imagen inválida devuelve `400` sin guardar la operación.
- `presentacionId` es opcional; si se envía, debe ser un ObjectId válido de 24 caracteres hexadecimales. Si se omite, se genera en el servidor. Se rechazan identificadores o SKU ya existentes, tanto en el lote como en marketplace, y duplicados en la misma petición (`422`). El SKU se recorta y se compara con distinción de mayúsculas.
- Esta entrega **solo agrega** presentaciones nuevas. No actualiza presentaciones existentes ni reinicia el stock de presentaciones previas.
- Capacidad consumida = `Σ(contenidoUnidad × unidadesCreadas)` de presentaciones existentes y nuevas. El stock vendido no libera capacidad para empaquetar de nuevo. La cantidad sin empaquetar queda en `cantidadDisponible`.
- Si se supera la capacidad, devuelve `400` con `message`: `"El total empaquetado (1005GRAMOS) supera la capacidad máxima del contenedor (1000GRAMOS)"`. Los números se muestran sin separadores ni ceros decimales innecesarios, concatenados con la unidad.
- `costoPresentacionCalculado = contenidoUnidad × costoBaseUnidad`, redondeado a dos decimales con `HALF_UP`. `precioVenta` es el precio público recibido; no se usa para calcular la deuda.
- Si `precioVenta < costoPresentacionCalculado` y no hay confirmación, devuelve `422`: `"Atención: El precio de venta (20000) es menor al costo base de la presentación (25000)"`. El frontend puede confirmar explícitamente y reenviar la petición con `confirmacionMargenNegativo: true`. Igualar el costo no requiere confirmación.
- `stockDisponible` se inicializa con `unidadesCreadas`. Los campos calculados y stock no se reciben del cliente.
- Un documento por presentación se inserta en `productos_marketplace`, incluso si está inactiva (`estado: "INACTIVO"`); `activoMarketplace: true` genera `estado: "ACTIVO"`. Sus campos son `presentacionId`, `loteId`, `proveedorNombre`, `nombreProducto` (`cepa - presentación`), `cepaNombre`, `sku`, `precioVenta`, `stockDisponible`, `fotografia` y `estado`. `fotografia` se almacena embebida con metadatos y Base64, tanto en el lote como en marketplace; no se descarga ni se guarda una URL externa.
- La operación guarda lote y proyecciones en una sola transacción MongoDB. Un fallo o conflicto concurrente cancela toda la operación; los errores se propagan. Se requiere replica set, índices únicos de SKU/presentación y bloqueo optimista del lote. No reintentar automáticamente un POST tras timeout.

**Respuesta:** `201` con el mismo contrato de `GET /api/lotes/{id}`, ampliado con `costoTotalEmpaquetado` y, por contenedor, `codigoContenedor`, `precioBaseUnidad` y `presentaciones`. Cada presentación devuelve `presentacionId`, `sku`, `nombre`, `contenidoUnidad`, `unidadesCreadas`, `stockDisponible`, `costoPresentacionCalculado`, `precioVenta`, `activoMarketplace`, `confirmacionMargenNegativo` y `fotografia` (`null` si no se envió). El estado del lote pasa a `PROCESADO`. Los estados anteriores de pago se conservan.

#### Imágenes de presentaciones

Se reutiliza la representación del servicio existente de imágenes: `{ nombre, extension, filepart }`. El endpoint de empaquetado sigue recibiendo JSON; no es multipart y no utiliza el endpoint `/api/v1/images/{id}`, que continúa dedicado al catálogo legado y categorías, con sus límites existentes. En el ejemplo, `/9j/...` es un marcador: debe reemplazarse por el Base64 completo del archivo. El servidor valida codificación, tamaño y metadatos, pero no decodifica el formato visual para comprobar dimensiones ni correspondencia real de los bytes con la extensión.

El frontend debe validar el archivo antes de enviarlo:

```ts
async function prepararFotografia(file: File) {
  if (!file.size || file.size > 200 * 1024) {
    throw new Error("La imagen debe contener datos y no superar 200 KiB");
  }
  const extension = file.name.split(".").pop()?.toLowerCase();
  if (!extension || !["png", "jpg", "jpeg", "webp"].includes(extension)) {
    throw new Error("Formato permitido: PNG, JPG o WebP");
  }
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => typeof reader.result === "string"
      ? resolve(reader.result)
      : reject(new Error("No se pudo leer la imagen"));
    reader.onerror = () => reject(reader.error ?? new Error("No se pudo leer la imagen"));
    reader.readAsDataURL(file);
  });
  return { nombre: file.name, extension, filepart: dataUrl.slice(dataUrl.indexOf(",") + 1) };
}
```

Para previsualizar la respuesta, construir `data:image/<tipo>;base64,<filepart>`; `jpg` y `jpeg` usan `image/jpeg`. No incluir ese prefijo en `filepart` al enviar la petición. Base64 aumenta el tamaño alrededor de un 33 %: 204800 bytes generan hasta 273068 caracteres. El límite es por imagen; el documento completo del lote sigue sujeto al máximo MongoDB de 16 MiB, por lo que no deben acumularse imágenes grandes ilimitadamente.

**Cambio de contrato:** `fotografia` reemplaza a `fotografiaUrl`; los clientes deben actualizarse. Las URL históricas no se convierten automáticamente ni se descargan desde el servidor. Antes de migrar, convertirlas mediante un proceso controlado al objeto Base64; el script se detiene explícitamente si detecta una URL pendiente para evitar pérdida silenciosa de imágenes.

**Decisión financiera confirmada:** empaquetar no vuelve a cobrar mercancía ni recalcula deuda. `costoTotalLote` en la API (`costoTotalCalculado` en MongoDB) conserva el costo de recepción de toda la capacidad. `costoTotalEmpaquetado` calcula por separado `Σ(costoPresentacionCalculado × unidadesCreadas)` para todas las presentaciones del lote. No se modifican `totalDeuda`, `totalPagado` ni `saldoPendiente` del proveedor, tampoco los saldos del lote. Si el proveedor tenía saldo cero, permanece en cero y se conserva `saldoPendiente = totalDeuda - totalPagado`.

La colección `productos_marketplace` es una proyección independiente: este endpoint no escribe en el catálogo legado de productos, no agrega endpoints públicos de lectura ni modifica el flujo de órdenes o el descuento de stock por ventas. `AGOTADO` queda reservado al flujo de consumo/ventas, no al hecho de terminar de empaquetar.

#### Migración de esquema HU-03

Antes de desplegar sobre datos HU-02, detener las escrituras, realizar y verificar un respaldo y revisar [el script de migración](docs/migrations/hu03-schema.js). **No se ejecuta automáticamente al arrancar ni se ha aplicado desde esta tarea.** El script requiere `mongosh` y replica set, usa una transacción y permite previsualizar sin escribir:

```powershell
mongosh "<URI>" --file docs\migrations\hu03-schema.js
mongosh "<URI>" --eval "globalThis.applyHU03 = true" --file docs\migrations\hu03-schema.js
```

Seleccionar explícitamente la base correcta en la URI. Para bases grandes, planificar una migración por lotes antes de ejecutar: este script está pensado para un volumen que quepa en una transacción y su tiempo límite.

- Proveedor: `contacto.telefono/email` pasa a `telefono/correo`; los saldos pasan a `financiero`; fechas a `createdAt/updatedAt`.
- Lote: `fechaIngreso` pasa a `fechaRecepcion`, `costoTotalLote` a `costoTotalCalculado`; se conserva el valor financiero, no se infiere a partir del empaquetado.
- Contenedor: `id` pasa a `contenedorId` ObjectId; `capacidadUnidadesBase` a `capacidadTotal`; `unidadBase` a `unidadMedida`; nombre/costo/precio de cepa pasan a `cepa.nombre/costoBaseUnidad/precioBaseUnidad`. UUID históricos se convierten a nuevos ObjectId y se imprime la correspondencia: refrescar los IDs en clientes después de migrar.
- Se recupera `abonoInicial` desde el historial inmutable cuando corresponde, sin confundirlo con pagos acumulados. Se conservan saldos/pagos existentes. Las presentaciones pre-HU-02 que no tienen cepa/capacidad permanecen como detalle histórico, sin inventar contenedores.
- Se inicializa `version` para que los documentos existentes puedan actualizarse con bloqueo optimista; se sincronizan proyecciones de presentaciones ya existentes y se rechazan conflictos de SKU/identificador. El arranque crea índices únicos de marketplace y el índice FIFO sobre `fechaRecepcion`.
- Se conservan campos adicionales necesarios para HU-01/HU-02 y concurrencia: `estado` y `version` del proveedor; saldos de lote, estado de pago, costo empaquetado, timestamps y `version`; cantidad sin empaquetar y detalle histórico. El esquema adopta la estructura del JSON adjunto, pero no elimina información contable para hacer coincidir literalmente el ejemplo.

Las APIs anteriores conservan los nombres de campos financieros y de recepción de HU-02; el mapper adapta los nombres persistidos. `EN_RECEPCION` en MongoDB se expone como `RECIBIDO` en la API anterior para compatibilidad. No levantar la versión anterior de la aplicación sobre el esquema migrado; un rollback requiere restaurar el respaldo.

## 9. Manejo de errores
Un manejador global normaliza las respuestas de error:

| Código | Caso |
|---|---|
| `400` | Validación de entrada, reglas de negocio de registro y administrador protegido. |
| `401` | Credenciales inválidas, o sesión vencida (`SESSION_EXPIRED`). |
| `403` | Usuario pendiente de aprobación (`user_pending_approval`), usuario inactivo (`user_inactive`) o rol insuficiente. |
| `404` | Recurso no encontrado. |
| `409` | Conflicto de estado en órdenes, modificación concurrente (bloqueo optimista) o clave única duplicada (por ejemplo, nombre de categoría). |
| `413` | Imagen o solicitud multipart superior al límite permitido. |
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
