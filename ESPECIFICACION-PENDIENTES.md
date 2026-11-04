# Especificacion de avance y pendientes - App Zona Green

Fecha de evaluacion: 4 de noviembre de 2026.

## 1. Objetivo y limites de la evaluacion

Esta reevaluacion contrasta directamente la [propuesta Projecto App Zona Green (1).pdf](<../../../Downloads/Projecto App Zona Green (1).pdf>), fechada el 16 de octubre de 2026, con el codigo backend `zyozmen/Products`, la ejecucion de sus pruebas del 4 de noviembre de 2026 y la evaluacion frontend anterior. Se leyeron las cinco paginas del PDF; las referencias de pagina usan la posicion fisica del archivo, no el numero impreso.

El frontend no esta disponible en este workspace: sus capacidades, pruebas y recorridos reales se conservan como evidencia declarada previamente, no revalidada. El PDF contiene espacios de firma, pero su lectura no acredita firma, aceptacion ni acuerdos posteriores. Las conclusiones siguientes evaluan el alcance propuesto; no constituyen un dictamen de cumplimiento legal.

La propuesta menciona siete modulos, pero enumera ocho. Esta evaluacion utiliza los ocho modulos enumerados; la propuesta y su clausula de garantia deben corregirse para mantener el mismo alcance.

Los porcentajes son estimaciones de cumplimiento, no mediciones de lineas de codigo, horas restantes o certificaciones contractuales.

Se distinguen tres tipos de pendiente:

- **Implementacion:** funcionalidad o contrato que no se encontro completo en el backend, frontend o su conexion.
- **Definicion:** modalidad o criterio de un requisito que debe acordarse antes de desarrollar. Si la solucion existente satisface el requisito y es aceptada, puede cerrarse sin codigo nuevo. Una obligacion explicita no puede eliminarse unilateralmente por marcarla como pendiente de definicion.
- **Validacion:** funcionalidad existente cuyo cierre requiere pruebas de integracion, volumen o aceptacion. No implica volver a implementarla.

Se inspeccionaron controladores, servicios, persistencia, seguridad, configuracion OpenAPI y pruebas del backend. No se consultaron despliegues, cuentas de servicios ni Swagger remoto. No se encontraron archivos `.bru` en este repositorio; esto no demuestra que no existan en otro repositorio o como entrega externa.

Las referencias frontend de las secciones siguientes pertenecen a la evaluacion anterior y a otro repositorio; no representan archivos verificados aqui. Las referencias backend son relativas a este documento y corresponden al codigo inspeccionado. Una prueba unitaria con dobles acredita una regla o contrato local, no su integracion con MongoDB o la interfaz.

### Matriz de alcance de la propuesta

| Requisito explicito | Pagina PDF | Evidencia y resultado de la reevaluacion |
|---|---:|---|
| Acceso seguro, login, logout y tokens/sesiones | 2 | Implementado y probado localmente en backend; faltan carreras de renovacion/revocacion y aceptacion en ambiente final. |
| Administracion de usuarios, roles/permisos, recuperacion de contrasena y referidos | 2 | Registro, estados y roles fijos existen. No se encontro recuperacion/reset administrativo ni referidos en backend. La modalidad de roles y recuperacion debe acordarse, pero el requisito no es opcional. |
| Datos personales y preferencias del usuario | 2 | Perfil propio existe; preferencias solo reportadas en frontend. Deben identificarse y aceptarse, no excluirse del alcance. El PDF no exige sincronizacion entre dispositivos. |
| Catalogo publico/privado, detalle, seleccion y compra con ordenes | 2 | Catalogo publico y compra autenticada existen. Falta confirmar si esta distincion satisface publico/privado o se exige visibilidad diferenciada. No se contrata expresamente una pasarela automatica. |
| Procesamiento, seguimiento de pedidos e historial personal | 2 | Backend implementado con pruebas de reglas. Faltan aceptacion integrada, concurrencia real y evaluacion de volumen. |
| Panel de consulta, reportes de ventas e historial | 3 | Endpoint de reporte existente; definicion de fecha/venta y conciliacion pendientes. El calculo actual carga las ordenes del periodo en memoria. |
| Stock, altas/bajas, existencias y alertas de disponibilidad | 3 | Altas, existencias y movimientos existen. Bajas posteriores y alertas administrativas no se acreditan. Son requisitos explicitos; umbrales configurables y mermas no se especifican. |
| Proveedores, variedades, lotes, presentaciones, costos/precios, liquidacion diaria, deuda/abonos/saldos | 3 | Implementado localmente. Persisten error de parametros del detalle y validaciones reales de pagos/transacciones. Auditoria, PQR y reglas HU-04 son concreciones operativas adicionales, no exigencias detalladas por el PDF. |
| Solucion autonoma, escalable, de alto rendimiento y facil mantenimiento | 2 | Arquitectura y verify aportan evidencia parcial. No hay volumen, latencia, disponibilidad ni restauracion acreditados; se necesitan criterios medibles. El PDF no exige especificamente usar una agregacion MongoDB: es una recomendacion tecnica para satisfacer escalabilidad. |
| Java/Spring Boot, MongoDB Atlas, React y manual de marca | 2-3 | Stack backend comprobado; React declarado previamente. Atlas remoto, manual de marca y validacion visual no se comprobaron. |
| Backend en Render, frontend en Netlify y cuentas del cliente | 3-4 | Configuracion Docker disponible; despliegues y titularidad no verificados. No equivalen a entrega por existir archivos de configuracion. |
| Transferencia completa de propiedad intelectual, codigo e infraestructura; repositorio principal frontend/backend | 2, 4 | Backend disponible. Falta evidencia de repositorio principal/estructura conjunta, transferencia y accesos operativos del cliente. |
| Swagger completo, colecciones Bruno y guia local paso a paso | 4 | Swagger configurado y README disponible; completitud pendiente. No se encontraron colecciones Bruno en este backend. |
| Capacitacion administrativa, documentacion tecnica y manual de usuario | 4 | Obligatorios; no se acreditan capacitacion ni manual final. |
| Pruebas con Zona Green antes del acta, API correcta y datos persistidos | 5 | Pruebas tecnicas parciales, sin sesion de aceptacion ni acta acreditadas. |
| Garantia de 30 dias calendario desde firma del acta final | 4 | Condicion explicita; inicio, canales y evidencia de entrega pendientes. No empieza por desplegar o ejecutar tests. |

La propuesta no asigna pesos, porcentajes ni puntos por requisito. No se encontraron obligaciones explicitas de comisiones por referidos, catalogo exclusivo por rol, avatar, alertas con umbral configurable, ajustes/mermas, exportacion Excel/PDF ni una pasarela de pago. Su ausencia no permite suprimir recuperacion, referidos, preferencias, catalogo publico/privado, bajas o alertas, que si se mencionan.

## 2. Resumen de porcentajes

| Modulo | Completado estimado | Pendiente estimado | Peso en el avance funcional |
|---|---:|---:|---:|
| Autenticacion | 95% | 5% | 8% |
| Gestion de usuarios | 60% | 40% | 12% |
| Gestion de perfil | 90% | 10% | 5% |
| Tienda electronica | 90% | 10% | 18% |
| Administracion de ordenes | 92% | 8% | 15% |
| Reportes de ordenes | 80% | 20% | 10% |
| Gestion de inventario | 70% | 30% | 14% |
| Proveedores, lotes y cuentas por pagar | 90% | 10% | 18% |
| **Avance funcional ponderado** | **83,3% (aproximadamente 83%)** | **16,7%** | **100%** |

Formula: suma de cada porcentaje completado multiplicado por su peso. Los pesos son una convencion de esta evaluacion, considerando el alcance y la complejidad operativa; no forman parte del contrato.

### Porcentajes despues de leer la propuesta

Se conserva **83,3% como estimacion tecnica provisional**, no como porcentaje contractual demostrado. Leer el PDF no crea evidencia nueva de implementacion ni de aceptacion frontend; por ello no se aumenta el avance solo por confirmar el alcance. Los pesos se mantienen para comparar con la evaluacion anterior, pero no fueron aprobados por el cliente ni se derivan del valor comercial.

El resultado contractual verificable es: **los ocho modulos y la entrega integral aun no estan acreditados como completos**. Recuperacion/referidos, bajas/alertas y los entregables finales son brechas explicitas, aunque el promedio funcional sea alto. Los porcentajes no deben utilizarse para autorizar el acta ni inferir pagos o tiempo restante.

### Ajustes derivados de la revision backend anterior

- **Ordenes, 90% -> 92%:** se acreditan parcialmente 2 puntos de la matriz de estados mediante codigo y 23 pruebas de servicio aprobadas. No se cierra la concurrencia real ni la aceptacion del tablero.
- **Reportes, 75% -> 80%:** se acreditan 5 puntos por el endpoint real de reporte y su calculo probado en el servidor. Los otros 5 puntos de escalabilidad permanecen: todavia se cargan todas las ordenes del periodo en memoria.
- **Otros modulos:** mantienen la estimacion anterior. Sus capacidades backend se confirman o se precisan, pero no hay evidencia nueva suficiente para cerrar aceptacion frontend, definiciones de alcance o transacciones reales.
- El avance anterior era 82,5%; el incremento ponderado es `2 x 0,15 + 5 x 0,10 = 0,8` puntos, hasta 83,3%. No se transforma la cobertura tecnica en porcentaje funcional.

**Preparacion para entrega contractual: no cuantificable con la evidencia disponible.** Se retira la cifra heredada de 75%: no tenia una matriz de pesos o evidencias que la sustentara. Despliegue, documentacion final, transferencia, capacitacion y aceptacion son condiciones obligatorias pendientes de acreditar; no se compensan con cobertura de codigo.

Los porcentajes de las tareas siguientes son **puntos porcentuales dentro de su modulo**, no del proyecto completo. Por ejemplo, los 20 puntos pendientes de referidos pertenecen al modulo de usuarios.

### Validacion backend ejecutada en esta revision

```powershell
.\mvnw.cmd "-Dtest=*,!ApiIntegrationTest" "-DskipITs=true" "-Djacoco.append=false" verify
```

- **BUILD SUCCESS:** compilacion, pruebas, empaquetado y comprobacion JaCoCo aprobados.
- Surefire: **452 casos contabilizados, 400 ejecutados satisfactoriamente, 0 fallos, 0 errores y 52 omitidos**. No significa 452 pruebas aprobadas.
- Cobertura de lineas: **96,68% (1779 cubiertas / 1840 evaluadas)**; minimo configurado: 80%. Se genero sin acumular cobertura de ejecuciones previas.
- El calculo excluye DTOs, modelos, mappers, configuracion y la clase principal, segun [pom.xml](pom.xml). No mide cobertura de requisitos ni rendimiento.
- Integraciones omitidas por sus condiciones de entorno: [DailySettlementIntegrationTest](src/test/java/com/zyozmen/products/adapter/out/mongodb/DailySettlementIntegrationTest.java), 12; [InventarioOrdenIntegrationTest](src/test/java/com/zyozmen/products/adapter/out/mongodb/InventarioOrdenIntegrationTest.java), 33; [MarketplaceCatalogoIntegrationTest](src/test/java/com/zyozmen/products/adapter/out/mongodb/MarketplaceCatalogoIntegrationTest.java), 7. Requieren `HU04_TEST_MONGO_URI` y/o `HU05_TEST_MONGO_URI` contra un replica set aislado.
- `skipITs=true` excluye la fase Failsafe. No se acredita una suite E2E completa ni se utilizan como resultado actual los conteos historicos del README.
- No se iniciaron servicios, modificaron bases externas ni comprobaron endpoints de produccion.

## 3. Autenticacion: 95% completado, 5% pendiente

### Lo que ya existe

- Inicio y cierre de sesion.
- Control de acceso por sesion y rutas restringidas.
- Bloqueo de usuarios pendientes o inactivos.
- Expiracion y renovacion de tokens.
- Aviso y cierre de sesion por inactividad.
- Interceptores que coordinan solicitudes durante la renovacion.

### Contraste backend

[AuthController](src/main/java/com/zyozmen/products/adapter/in/web/AuthController.java) publica registro, login, refresh y logout en `/api/auth`. [SessionService](src/main/java/com/zyozmen/products/application/service/SessionService.java) rechaza tokens invalidos/revocados, controla inactividad al renovar, comprueba usuario activo y revoca el token anterior. Sus [7 pruebas](src/test/java/com/zyozmen/products/application/service/SessionServiceTest.java) pasaron.

La inactividad no se mide por cada peticion HTTP: se compara el instante de emision al renovar. [JwtAuthenticationFilter](src/main/java/com/zyozmen/products/security/JwtAuthenticationFilter.java) valida JWT y revocacion, pero no consulta el estado actual del usuario en cada solicitud. Inactivar una cuenta bloquea login/refresh; no queda acreditada la invalidacion inmediata de todos sus tokens existentes. Tampoco se prueba aqui la carrera de dos refresh simultaneos o refresh contra logout. Acordar ese comportamiento antes de cerrar los 3 puntos de aceptacion.

### Definicion del 5% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Aceptacion integral de sesiones | 3% | Definicion y validacion | Acordar plazo efectivo de bloqueo/revocacion y tratamiento de refresh concurrente o simultaneo con logout. Probar login, logout, expiracion, inactividad y cuenta bloqueada con JWT y persistencia reales. Verificar si un token emitido antes de inactivar/cambiar credenciales sigue vigente y corregir solo si contradice la politica aprobada. No restaurar una sesion cerrada fuera de esa politica. |
| Comportamiento en el ambiente final | 2% | Validacion | Verificar HTTPS, proxy, estados 401/403 y renovacion en los dominios de produccion. La interfaz debe distinguir falta de permisos, sesion expirada y fallo de red. |

**No se identifico una nueva funcionalidad concreta equivalente al 5% restante.** El pendiente es de definicion y validacion; solo requiere cambios de codigo si la politica acordada no coincide con el comportamiento actual, no reimplementar login o tokens.

Evidencia frontend previa (rutas del otro repositorio): `src/context/AuthContext.jsx`, `src/services/AuthenticationService.js`, `src/services/httpClient.js`, `src/components/auth/ProtectedRoute.jsx`.

## 4. Gestion de usuarios: 60% completado, 40% pendiente

### Lo que ya existe

- Registro de clientes con datos personales y consentimiento de mayoria de edad.
- Aprobacion de cuentas pendientes.
- Activacion e inactivacion.
- Restricciones de acceso ADMIN/cliente.
- La evaluacion frontend reporta un restablecimiento administrativo de contrasena; **no se confirma como flujo integrado** con este backend.

### Contraste backend

[UserService](src/main/java/com/zyozmen/products/application/service/UserService.java) fuerza CLIENTE/PENDIENTE en el registro, valida duplicidad, cifra la contrasena y permite aprobar/inactivar cuentas. Sus [23 pruebas](src/test/java/com/zyozmen/products/application/service/UserServiceTest.java) pasaron. [AdminUserController](src/main/java/com/zyozmen/products/adapter/in/web/AdminUserController.java) solo publica listado y cambio de estado: no hay endpoint administrativo de reset ni asignacion de roles. El cambio de contrasena disponible pertenece al perfil propio.

No se encontraron codigos de referido, relacion de referidos o tokens de recuperacion en los casos de uso actuales. El listado administrativo devuelve todos los usuarios sin paginacion; debe incluirse en la validacion de volumen, no confundirse con ausencia de administracion.

### Definicion del 40% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Codigos de referido | 20% | Implementacion y definicion de modalidad | Requisito explicito (PDF p. 2). Acordar quien obtiene un codigo, cuando se genera y donde se utiliza. Implementar generacion/consulta, captura y relacion persistente en el flujo aprobado; el registro con asociacion es una propuesta de concrecion, no un detalle fijado por el PDF. Validar codigo inexistente, duplicidad/autorreferencia segun politica y verificar la relacion autorizadamente. |
| Recuperacion de contrasena | 15% | Implementacion y definicion de modalidad | Requisito explicito (PDF p. 2), no opcion de eliminar la recuperacion. Acordar si reset administrativo satisface el metodo prometido o se exige recuperacion autonoma. Para el administrativo, identificar llamada frontend y agregar caso de uso/endpoint ADMIN faltante con politica de credenciales/sesiones. Para el autonomo, implementar solicitud, canal, token temporal de un solo uso y nueva contrasena; probar vencimiento, reutilizacion e invalidez sin revelar cuentas. El cambio de contrasena autenticado del perfil no permite recuperar acceso perdido. |
| Administracion de roles y permisos | 5% | Definicion y validacion; implementacion si hay brecha | Requisito explicito (PDF p. 2), sin exigir un editor dinamico de permisos. Confirmar si ADMIN/CLIENTE y administracion de estados satisfacen el alcance. Documentar y probar matriz de acceso. Si se acuerda asignacion de roles, implementar seleccion autorizada y persistencia evitando elevacion no autorizada. No cerrar solo por tener dos valores en un enum. |

No se consideran contratadas comisiones, premios o descuentos por referidos: deben acordarse como ampliacion si se solicitan.

Evidencia frontend previa (rutas del otro repositorio): `src/components/login/LoginComponent.jsx`, `src/components/admin/users/UserAdminComponent.jsx`, `src/services/UserService.js`.

## 5. Gestion de perfil: 90% completado, 10% pendiente

### Lo que ya existe

- Edicion de nombre, apellido, direccion y telefono.
- Cambio de contrasena.
- Actualizacion del perfil conservado en la sesion.
- Preferencias basicas de tema e idioma en la interfaz.

### Contraste backend

[UserProfileController](src/main/java/com/zyozmen/products/adapter/in/web/UserProfileController.java) obtiene el username del JWT, no del cuerpo enviado. El servicio actualiza solo los campos informados y cifra una nueva contrasena; esto esta cubierto por pruebas de servicio y controlador. No hay preferencias de tema/idioma en el contrato de perfil. Tampoco se revocan sesiones en el caso de uso de cambio de contrasena: su efecto debe acordarse y verificarse; no se da por implementado.

### Definicion del 10% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Alcance y persistencia de preferencias | 5% | Definicion y validacion; implementacion si hay brecha | El PDF p. 2 exige gestionar preferencias, sin enumerarlas ni exigir sincronizacion entre dispositivos. Identificar las preferencias existentes, acordar si cumplen el alcance y probar su conservacion. Si se aprueban locales al navegador, documentarlo; si se acuerdan por cuenta, persistir/recuperar en backend. No excluir todas las preferencias por falta de detalle en el PDF. |
| Aceptacion del perfil | 5% | Validacion | Verificar que las ediciones persistan despues de cerrar e iniciar sesion; que un usuario no pueda editar otro perfil; y que el cambio de contrasena tenga el efecto acordado sobre credenciales y sesiones. |

No se presupone una obligacion de agregar avatar, preferencias de marketing o nuevos campos personales.

Evidencia frontend previa (rutas del otro repositorio): `src/components/profile/ProfileComponent.jsx`, `src/components/dashboard/HeaderComponent.jsx`, `src/services/UserService.js`.

## 6. Tienda electronica: 90% completado, 10% pendiente

### Lo que ya existe

- Catalogo y detalle de productos por presentacion.
- Filtros y paginacion del servidor.
- Precios, fotografias y existencias por presentacion.
- Carrito y formulario de entrega.
- Creacion de orden y apertura de WhatsApp con el pedido.
- Revalidacion del carrito ante cambios de precio o disponibilidad.

### Contraste backend

El catalogo [MarketplaceController](src/main/java/com/zyozmen/products/adapter/in/web/MarketplaceController.java) tiene busqueda, categorias, rango de precio y paginacion; su acceso no exige sesion en la [configuracion actual](src/main/java/com/zyozmen/products/config/SecurityConfig.java). Crear y consultar ordenes propias si exige autenticacion. No existe una politica de catalogo exclusivo por cuenta en estas rutas.

[OrderServiceTest](src/test/java/com/zyozmen/products/application/service/OrderServiceTest.java) prueba el precio vigente, snapshot y rechazo de precios obsoletos, totales inconsistentes y presentaciones inactivas. La creacion de una venta normal no reserva stock: el descuento ocurre en PAGADA; una PQR descuenta al crearse. Por ello crear una orden no garantiza disponibilidad hasta confirmar su pago. Las 7 integraciones de catalogo y las pruebas MongoDB de sobreventa no se ejecutaron en esta revision.

### Definicion del 10% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Catalogo publico/privado | 4% | Definicion y validacion; implementacion si hay brecha | El PDF p. 2 menciona expresamente publico/privado. Obtener decision sobre si catalogo publico con compra autenticada satisface el requisito o si debe restringirse la lectura/visibilidad de productos. Probar la politica en API/UI; implementar visibilidad diferenciada solo si esa es la modalidad acordada. No declarar satisfecha la palabra privado por la sola existencia de login. |
| Aceptacion del checkout | 3% | Validacion | Probar compra con datos reales, producto agotado, cambio de precio, perdida de respuesta y confirmacion repetida. Verificar que el pedido persista y que no se duplique por una repeticion de envio. Confirmar que el flujo por WhatsApp cumple el alcance pactado. |
| Aceptacion visual y contenido final | 3% | Validacion y configuracion | Revisar movil/escritorio contra el manual de marca, imagenes y textos finales. Resolver enlaces de navegacion sin destino real y retirar o identificar contenido de ejemplo visible en la entrega. El cliente debe aprobar las pantallas de tienda y compra. |

Una pasarela de pago automatica, cursos, blog o sistema de resenas no se consideran pendientes contratados solo porque aparezcan referencias visuales: no estan especificados como modulos en la propuesta.

Evidencia frontend previa (rutas del otro repositorio): `src/components/shop/MarketplaceShopList.jsx`, `src/components/shop/MarketplaceProductDetail.jsx`, `src/components/cart/ShoppingCarComponent.jsx`, `src/services/marketplaceService.ts`.

## 7. Administracion de ordenes: 92% completado, 8% pendiente

### Lo que ya existe

- Tablero operativo y contador de ordenes.
- Estados de creacion, pago, empaquetado, entrega y cancelacion.
- Ordenes PQR y contra entrega.
- Dialogos de confirmacion.
- Historial de ordenes personales.
- Actualizacion de inventario y liquidacion asociada a mutaciones.

### Contraste backend

[OrderService](src/main/java/com/zyozmen/products/application/service/OrderService.java) impide saltar estados y cancelar ventas que pasaron por PAGADA, incluso contra entrega o EMPACADA. PQR parte de CREADA con total cero; puede cancelarse en CREADA/EMPACADA, no en ENTREGADA. Contra entrega exige efectivo/tarjeta validos al entregar.

Las 23 pruebas de servicio acreditan reglas, auditoria y aislamiento de consulta propia. Las 33 integraciones de inventario contienen pruebas de rollback, confirmaciones/cancelaciones/entregas concurrentes y sobreventa, pero quedaron omitidas: su existencia no cierra esos escenarios.

El listado administrativo admite `page/size` y conteo, pero sin parametros sigue siendo ilimitado. El historial `/api/orders/mine` devuelve una lista sin paginacion; falta acordar volumen y, si procede, completar paginacion compatible en ambos lados. El listado Mongo ordena por creacion sin desempate por ID, por lo que debe comprobarse la estabilidad entre paginas con timestamps iguales.

### Definicion del 8% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Matriz integrada de estados y concurrencia | 4% | Validacion | Las reglas unitarias estan acreditadas parcialmente. Ejecutar con replica set y UI dos administradores sobre la misma orden, confirmaciones repetidas y cancelacion PQR/contra entrega; incluir fallos al guardar y verificar orden, stock y movimientos despues de recargar. Rechazar estados prohibidos con conflicto explicito sin duplicados ni escrituras parciales. |
| Seguimiento, historial y volumen | 4% | Validacion e implementacion condicional | Comprobar aislamiento de ordenes propias, estados e historial persistidos. Acordar volumen; si el listado ilimitado no lo satisface, paginar `/mine` y el tablero sin truncamientos silenciosos. Probar conteos y orden estable con fechas de creacion iguales, carga, errores y navegacion. |

Este 8% no representa ausencia del Kanban o del historial: son funcionalidades existentes con pendientes de aceptacion integral y volumen.

Evidencia frontend previa (rutas del otro repositorio): `src/components/admin/orders/OrdersKanbanComponent.jsx`, `src/components/orders/MyOrdersComponent.jsx`, `src/services/OrderService.js`.

## 8. Reportes de ordenes: 80% completado, 20% pendiente

### Lo que ya existe

- Consulta por dia, semana y mes.
- Indicadores de ordenes, ventas entregadas, pendientes, cancelaciones y PQR.
- Importes y desglose registrado de efectivo/tarjeta.
- Visualizacion grafica.

### Contraste backend

**Ya existe** `GET /api/admin/orders/report?date=YYYY-MM-DD&period=day|week|month` en [AdminOrdersController](src/main/java/com/zyozmen/products/adapter/in/web/AdminOrdersController.java). [OrderService](src/main/java/com/zyozmen/products/application/service/OrderService.java) calcula semana lunes-lunes y mes calendario con `app.report.timezone` (America/Bogota por defecto).

La seleccion se hace por `created_at`, con inicio inclusivo y fin exclusivo; se cuentan como ventas las ordenes actualmente ENTREGADA que no son PQR. Los importes sin desglose de efectivo/tarjeta se devuelven como `undetailed`: no asignarlos artificialmente a efectivo. Se probaron rangos y exclusion de PQR en servicio; falta la conciliacion con UI y datos reales.

**El endpoint no es una agregacion MongoDB:** consulta todas las ordenes del periodo y recorre la lista en Java. El frontend anterior tambien calcula reportes localmente; no se verifico que haya migrado al endpoint. La zona del reporte comercial es configurable, a diferencia de la liquidacion diaria, cuya zona es fija.

### Definicion del 20% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Definicion de fecha y significado de venta | 10% | Definicion e implementacion condicional | Acordar si el reporte atribuye la venta a creacion, pago o entrega y fijar zona horaria de negocio. Actualmente filtra por creacion. Alinear consultas, etiquetas y pruebas con la definicion aprobada, incluyendo orden creada un dia y pagada/entregada otro. |
| Consulta escalable y consumo del endpoint existente | 5% | Validacion de rendimiento e implementacion tecnica | El PDF p. 2 exige escalabilidad/rendimiento, no un algoritmo concreto. Acordar volumen y objetivos, medir el reporte y eliminar cargas ilimitadas que incumplan esos objetivos; se recomienda agregacion/conteo MongoDB en el endpoint existente. Conectar o verificar consumo frontend, conservando cifras y contrato. Probar periodos vacios, fechas limite y volumen sin truncar ordenes. La paginacion del historial se controla en el modulo de ordenes, sin duplicar puntos. |
| Conciliacion y aceptacion | 5% | Validacion | Comparar reporte con ordenes reales para distintos periodos, PQR, cancelaciones y cobros. Explicar cualquier diferencia entre ingreso comercial, costo reconocido y liquidacion al proveedor; no son la misma magnitud. |

Exportar Excel/PDF no figura como requisito explicito; debe acordarse aparte si se necesita.

Evidencia frontend previa (rutas del otro repositorio): `src/components/admin/orders/OrdersReportComponent.jsx`, `src/components/admin/orders/OrdersReportHelper.js`, `src/hooks/useApiQueries.ts`.

## 9. Gestion de inventario: 70% completado, 30% pendiente

### Lo que ya existe

- Alta de productos y recepcion de lotes.
- Desglose de contenedores y creacion de presentaciones.
- Costos, precios, capacidad y existencias.
- Control de disponibilidad en marketplace y carrito.
- Descuento de stock asociado a ordenes y restitucion en los casos admitidos.
- Activacion de marketplace al crear presentaciones.

### Contraste backend

[PresentacionService](src/main/java/com/zyozmen/products/application/service/PresentacionService.java) valida capacidad total, contenido, unidades, SKU/identificador, costo calculado y confirmacion de margen negativo. Empaquetado y publicacion usan transaccion; 15 pruebas de servicio y una prueba de configuracion transaccional pasaron.

[ProductController](src/main/java/com/zyozmen/products/adapter/in/web/ProductController.java) publica crear, editar y consultar, **no eliminar**. [PresentacionController](src/main/java/com/zyozmen/products/adapter/in/web/PresentacionController.java) solo agrega presentaciones nuevas: `activoMarketplace` se elige al crear, no es un endpoint de retiro posterior. No se encontraron casos de uso de ajustes/mermas o umbrales de stock bajo. La disponibilidad actual filtra presentaciones activas/con stock, pero eso no acredita una alerta administrativa.

Las pruebas unitarias de descuento/restitucion pasaron; la ausencia de sobreventa y la atomicidad en MongoDB siguen pendientes de las integraciones omitidas.

### Definicion del 30% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Bajas y retiro de productos/presentaciones | 12% | Implementacion y definicion de modalidad | Requisito explicito (PDF p. 3). Definir baja logica/retiro y nivel afectado. Implementar caso de uso/endpoint ADMIN y accion frontend para entidades existentes; no basta el flag al crear. Preservar referencias, sincronizar inventario/proyeccion atomicamente y rechazar nuevas compras del retirado. Acordar ordenes CREADA anteriores al retiro y probar stock/auditoria. No se exige borrado fisico de documentos. |
| Alertas de disponibilidad | 10% | Implementacion y definicion de modalidad | Requisito explicito (PDF p. 3). Acordar si un indicador de agotado constituye la alerta solicitada o si se requiere aviso administrativo/stock bajo. Implementar la modalidad faltante mostrando disponibilidad real. Umbral configurable, canales automaticos y notificaciones externas no se presuponen; si se acuerdan, probar permisos, agotado, umbral y recuperacion. Ocultar productos agotados no demuestra por si solo una alerta. |
| Ajustes y mermas | 5% | Definicion e implementacion condicional | Determinar si el control de stock pactado exige ajustes por perdida, conteo o dano. Si se exige, registrar variacion con motivo y responsable, conservar trazabilidad y evitar existencias negativas. Si se excluye, documentarlo y aceptar el cierre sin agregar ese flujo. |
| Conciliacion de existencias | 3% | Validacion | Comparar stock antes/despues de recepcion, desglose, venta y cancelacion permitida. Probar operaciones concurrentes y evitar sobreventa o restitucion duplicada. |

Las presentaciones existentes se muestran principalmente como lectura; no se debe dar por implementado su retiro o edicion por existir el checkbox de activacion durante su creacion.

Los 5 puntos asignados a ajustes/mermas son una reserva de incertidumbre de la evaluacion anterior, **no evidencia de una funcion contratada faltante**. El PDF exige control de stock pero no especifica ese flujo. Al confirmar que no hace falta para los flujos pactados, cerrar esa reserva por definicion sin implementar mermas; si se amplia el alcance, identificar el acuerdo. No usar esos 5 puntos para exigir una nueva funcion unilateralmente.

Evidencia frontend previa (rutas del otro repositorio): `src/components/admin/cepas/CepaManagementPage.tsx`, `src/components/admin/suppliers/DesgloseContenedorForm.tsx`, `src/components/admin/suppliers/SupplierLotsList.tsx`, `src/services/CartService.js`.

## 10. Proveedores, lotes y cuentas por pagar: 90% completado, 10% pendiente

### Lo que ya existe

- Registro y consulta paginada de proveedores.
- Recepcion de lotes y clasificacion de contenedores/productos.
- Presentaciones con costo y precio.
- Deuda, abonos, imputacion por lote y saldo pendiente.
- Liquidacion diaria por proveedor.
- Pagos vinculados a la jornada.
- Detalle auditable paginado que distingue ventas, PQR, reversiones y pagos.
- Lectura exacta de importes y snapshots monetarios.

La incompatibilidad de `orderId` numerico ya se corrigio. Se verificaron una venta real por $20.000 COP y un abono vinculado por $20.000 COP: resumen y detalle coinciden y el pendiente diario es $0. Tambien se verificaron paginas individuales con un movimiento por pagina.

La frase anterior corresponde a la evidencia declarada en la revision frontend: **no se repitio ese recorrido en esta revision backend**.

El PDF p. 3 exige liquidacion diaria y cuentas por pagar, pero no fija los disparadores PAGADA/ENTREGADA, PQR/reversion, FIFO, corte de activacion, zona o formula del sugerido. Esas reglas se contrastan con [HU04-tareas-backend.md](HU04-tareas-backend.md) y los acuerdos operativos previos. Deben incorporarse a la documentacion tecnica final aceptada: el PDF p. 4 utiliza esa documentacion como referencia de garantia. No inventar dichas reglas a partir del texto general de la propuesta.

### Contraste backend

[ProveedorService](src/main/java/com/zyozmen/products/application/service/ProveedorService.java) calcula deuda/abonos, valida saldo del proveedor/lote y pertenencia del lote, reparte FIFO y suma una sola vez el pago vinculado a la jornada. Sus 41 pruebas pasaron, incluyendo pago distribuido y abono sin fecha. `fechaLiquidacion` es opcional y no se infiere del instante de registro; un importe superior al sugerido puede admitirse si respeta saldos.

[OrderSettlementService](src/main/java/com/zyozmen/products/application/service/OrderSettlementService.java) reconoce venta normal en PAGADA, contra entrega en ENTREGADA y PQR al crear, con cantidad y costo snapshot. Excluye ordenes creadas antes del corte persistido. La cancelacion PQR conserva pagos y revierte su jornada original. [DailySettlementService](src/main/java/com/zyozmen/products/application/service/DailySettlementService.java) calcula sugerido con saldo global actual, no historico.

Resumen: `date/providerId` opcionales; detalle: ambos obligatorios; `page` base cero, `size` 1-100. [DailySettlementController](src/main/java/com/zyozmen/products/adapter/in/web/DailySettlementController.java) y el [modelo](src/main/java/com/zyozmen/products/domain/model/DailySettlementDetail.java) ya existen. Los tipos son SALE, PQR, PQR_REVERSAL y PAYMENT; pagos agrupados por correlacion sin costo unitario. Las 20 pruebas de servicio/controlador/adaptador pasaron, pero no ejecutan la agregacion contra MongoDB real.

La integracion de detalle usa inserciones tecnicas de movimientos/pagos, no un POST real de pago de proveedor. Antes de ejecutar toda la suite deben revisarse sus referencias a colecciones antiguas (`provider_daily_accumulators`, `proveedores`, `provider_daily_movements`) frente a los documentos actuales y limpiar solo las colecciones de la base aislada. La configuracion de esa prueba no redefine los nombres de coleccion.

### Definicion del 10% pendiente

| Pendiente | Puntos | Tipo | Especificacion y criterio de cierre |
|---|---:|---|---|
| Parametros obligatorios del detalle | 3% | Correccion backend y validacion | El controlador exige ambos parametros, pero GlobalExceptionHandler no trata MissingServletRequestParameterException y tiene un handler generico de 500: la observacion previa sigue respaldada por el codigo. Agregar tratamiento 400 y pruebas HTTP para omision de cada campo, campos vacios, fecha/ObjectId invalidos y paginacion. Las pruebas actuales llaman directamente al controlador y no acreditan este error de binding. Verificar que la UI muestre el error sin fabricar resultados. |
| Cierre integrado de escenarios financieros | 5% | Validacion y adecuacion de pruebas | Alinear fixtures/colecciones vigentes y ejecutar en replica set aislado; agregar o completar recorrido HTTP/JWT real de POST de pago, no solo insertar transacciones. Probar costos distintos, contra entrega, PQR cancelada otro dia, pagos parciales/FIFO/superiores al sugerido, abono general y exceso. Conciliar stock, deuda, resumen y detalle tras recarga; comprobar pagos conservados y una fila por correlacion. No cambiar el corte ni reprocesar ordenes antiguas. |
| Trazabilidad y consistencia bajo concurrencia | 2% | Validacion | Forzar pagos simultaneos y fallos intermedios, ademas de confirmaciones/cancelaciones. Verificar ausencia de sobrepago, duplicados y escrituras parciales de lote/proveedor/transacciones/jornada. Conservar originalMovementId, timestamp real y date original en reversiones. Las anotaciones transaccionales y versiones no sustituyen esta prueba. Validar tambien respuestas tardias y cambios rapidos de filtros en frontend. |

El historial general de abonos no expone la jornada vinculada; el detalle auditable permite rastrearla. Si el cliente exige verla tambien en el historial general, acordar el cambio de contrato antes de implementarlo.

Evidencia frontend previa (rutas del otro repositorio): `src/components/admin/suppliers/SupplierManagementPage.tsx`, `src/components/admin/suppliers/SupplierPaymentForm.tsx`, `src/components/admin/settlements/DailySettlementPage.tsx`, `src/components/admin/settlements/DailySettlementDetail.tsx`, `src/services/settlementService.ts`.

## 11. Pendientes transversales para entrega contractual

Estos entregables no reciben porcentajes individuales porque no se inspeccionaron todas sus fuentes de evidencia. No deben interpretarse como 0% implementados.

| Entregable | Situacion verificable | Definicion del cierre |
|---|---|---|
| Backend Java/Spring Boot y MongoDB Atlas | Codigo inspeccionado; verify aprobado con 400 pruebas, 52 omitidas y cobertura 96,68%; Atlas remoto no comprobado | Ejecutar integraciones pendientes en replica set aislado, verificar version/esquema/indices y operacion del ambiente acordado. La cobertura no certifica el despliegue. |
| Frontend en Netlify y backend en Render | [dockerfile](dockerfile) y [entrypoint.sh](entrypoint.sh) soportan construccion y carga de configuracion para Render; despliegue/cuentas no comprobados | Verificar dominios, HTTPS, variables, proxy/CORS, arranque, replica set y operacion con servicios a nombre del cliente. La imagen omite tests al construir: exigir verify en el proceso de entrega. |
| Codigo fuente y propiedad intelectual | Backend disponible localmente; frontend documentado en revision previa; transferencia no comprobada | El PDF pp. 2 y 4 exige transferencia total y repositorio principal GitHub con estructura frontend/backend. Entregar esa estructura y accesos/activos del cliente, identificando versiones y cesion. Dos repositorios separados solo sustituyen lo descrito con acuerdo documentado; no asumir equivalencia ni confundir acceso tecnico con titularidad. |
| Swagger | Configuracion [OpenApiConfig](src/main/java/com/zyozmen/products/config/OpenApiConfig.java), anotaciones y rutas Swagger existen; documento generado remoto no revisado | Validar `/v3/api-docs` y `/swagger-ui.html` de la version final con autenticacion, cuerpos, respuestas, errores y paginacion. Verificar que indique realmente como enviar Bearer JWT; la configuracion inspeccionada solo declara metadata, no un esquema de seguridad. |
| Colecciones Bruno | No se encontraron archivos `.bru` en este backend | Localizar entrega externa o crear colecciones ejecutables para las rutas actuales, con ambientes sin secretos y casos positivos/negativos; probarlas en entorno aislado. |
| Guia tecnica | [README](README.md) cubre compilacion, configuracion y contratos; presenta referencias historicas | Alinear rutas documentadas con codigo vigente: por ejemplo, README indica `/api/v1/auth`, pero AuthController publica `/api/auth` y no se encontro un alias en este backend. Actualizar evidencia de pruebas y completar despliegue, backup/restauracion y recuperacion; verificar ejecucion por otro operador. |
| Manual de usuario | No verificado | Documentar flujos de cliente y administrador, restricciones, mensajes y conciliacion de operaciones. |
| Capacitacion | No verificada | Realizar sesion practica con el administrador y registrar entrega de material y dudas resueltas. |
| Pruebas de aceptacion y acta | Validaciones tecnicas parciales disponibles | Ejecutar los ocho modulos con Zona Green, documentar resultados y pendientes, y firmar acta cuando se cumpla el alcance. |
| Garantia | PDF p. 4 establece 30 dias calendario desde la firma del acta final; no se acredita acta firmada | Registrar inicio, canal de reporte y documentacion tecnica de referencia. Corregir sin costo los defectos que desvian esa especificacion; nuevas funciones/disenos/alcance se acuerdan aparte. Corregir la referencia a 7 modulos para que incluya los ocho enumerados. |

### Calidad tecnica pendiente

- Revalidar el typecheck frontend por separado: la evaluacion anterior menciona errores en `src/hooks/useApiQueries.ts` y `src/schemas/profile.test.ts`, pero tambien declara build aprobado. No se confirma aqui si fueron corregidos o si ese build no ejecuta comprobacion de tipos.
- Corregir el tratamiento de parametros obligatorios de detalle y agregar su regresion HTTP. No confundir tests de llamada directa a controlador con binding/autorizacion real.
- Alinear las colecciones de fixtures de integracion con los documentos vigentes y ejecutar las 52 pruebas omitidas en MongoDB aislado. Agregar los recorridos reales de pago y sus fallos/concurrencia donde falte cobertura.
- Repetir verify tras los cambios y ejecutar aceptacion frontend/API/base reales. El umbral backend de 80% ya paso en esta revision; no sigue pendiente repetirlo sin cambios.
- Acordar volumen esperado y objetivos medibles de respuesta antes de certificar "alto rendimiento".
- Verificar respaldos, restauracion, registros operativos y alertas del servicio. No se certifica disponibilidad solo por desplegar en la nube.
- No confundir las pruebas frontend con validacion de transacciones, indices y concurrencia en MongoDB.

La evaluacion frontend anterior declara **308 pruebas en 43 archivos** y build aprobado. Se conserva como antecedente, no como ejecucion de este workspace. La ejecucion backend nueva acredita **400 pruebas aprobadas y 52 omitidas**; ambos resultados son distintos y no se suman como una suite integrada.

### Presupuesto operativo

La evaluacion anterior informa que la propuesta estima MongoDB Atlas con 5 GB por USD 5/mes y cita un rango de USD 8-30 por 30 dias para Flex. No se revalidaron tarifas en esta revision backend: confirmar el plan, su factura y la [documentacion oficial de Atlas Flex](https://www.mongodb.com/docs/atlas/billing/atlas-flex-costs/) vigente.

El PDF p. 3 proyecta **Atlas USD 5/mes, Render USD 25/mes y Netlify USD 0/mes**, total USD 30/mes bajo esos supuestos. Son costos proyectados, no una tarifa actual comprobada ni garantia de rendimiento. Confirmar planes, limites y demanda antes de la entrega. El PDF p. 5 fija desarrollo en **COP 24.000.000** y deja **el esquema de pagos pendiente de establecer**; no derivar cuotas, saldo contractual o desembolsos del 83,3% funcional.

Verificar tambien el plan de Render y los limites de [Netlify](https://www.netlify.com/pricing/). No prometer un costo mensual fijo ni rendimiento garantizado sin validar recursos y demanda.

Estos ajustes no cambian automaticamente el valor de desarrollo de $24.000.000 COP: servicios mensuales, garantia y ampliaciones son conceptos diferentes.

## 12. Orden recomendado para completar el proyecto

1. **Cerrar modalidades y criterios, no eliminar obligaciones:** corregir siete/ocho modulos; concretar recuperacion, referidos, roles/permisos, preferencias, catalogo publico/privado, fecha de reportes, bajas/alertas y objetivos de volumen/rendimiento. Registrar decisiones y su relacion con el PDF.
2. **Corregir contratos y evidencia:** handler 400 para parametros obligatorios de detalle, regresion HTTP, fixtures/colecciones vigentes y rutas documentadas; revalidar el typecheck frontend antes de declararlo defectuoso o cerrado.
3. **Validar lo implementado:** ejecutar las 52 integraciones omitidas con replica set aislado, completar POST de pago real y concurrencia financiera, y conciliar UI/API/base sin mocks.
4. **Completar funciones acordadas:** referidos, alternativa de recuperacion/reset, bajas/alertas y cualquier implementacion condicional confirmada.
5. **Completar escalabilidad:** agregacion MongoDB del reporte existente, consumo frontend y volumen/paginacion/orden estable de listados.
6. **Preparar entrega:** infraestructura, documentacion, transferencia, capacitacion y aceptacion de los ocho modulos.

No se deben desarrollar nuevas funciones solo para rellenar porcentajes. Una definicion aceptada o una validacion satisfactoria puede cerrar puntos pendientes sin cambios de codigo.

## 13. Condicion para declarar 100%

El proyecto se considera completo cuando:

- Los ocho modulos cumplen el alcance explicitamente acordado.
- Recuperacion, referidos, preferencias, publico/privado, bajas y alertas tienen modalidad aprobada y cumplimiento acreditado; no quedan excluidos solo por ambiguedad.
- Los requisitos no explicitos y las ampliaciones tienen decision documentada, sin convertir recomendaciones tecnicas en nuevas obligaciones.
- Los flujos existentes pasan sus pruebas integradas y de aceptacion.
- Los defectos que incumplen el contrato quedan corregidos.
- Codigo, documentacion, cuentas y activos acordados se entregan al cliente.
- Se realizan capacitacion y acta final con condiciones de garantia claras.

La asignacion final de porcentajes debe actualizarse contra esa evidencia, no contra la cantidad de pantallas o pruebas existentes.

### Dictamen de esta reevaluacion

Hay una base funcional avanzada, con backend compilado y probado localmente, pero **no hay evidencia suficiente para declarar entrega completa conforme a la propuesta**. Las brechas prioritarias son recuperacion/referidos, bajas/alertas, modalidades ambiguas, integracion financiera/stock, rendimiento medible y los entregables/aceptacion del cliente.

El **83,3% se conserva exclusivamente como estimacion comparativa**. No se asigna un porcentaje nuevo de cumplimiento contractual: falta revalidar frontend, comprobar decisiones y completar la matriz con resultados de aceptacion por requisito. La sesion con Zona Green prevista en PDF p. 5 debe producir casos, resultados, evidencia de persistencia y pendientes resueltos antes del acta final.
