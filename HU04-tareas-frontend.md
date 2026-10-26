# HU-04 - Plan de ejecucion frontend alineado con backend

## Objetivo y modalidad

Integrar una pantalla administrativa de liquidacion diaria, su detalle y pagos,
consumiendo exclusivamente los endpoints y flujos reales del backend.

Cada Fn se desarrolla con la Tn correspondiente del
[plan backend](HU04-tareas-backend.md). La numeracion representa entregas
verticales, no una fase de diseno seguida de una integracion futura.

**No hay mocks, servidores simulados, respuestas hardcodeadas ni datos inventados
para suplir endpoints pendientes.** Se prueban los flujos contra API y MongoDB
reales en un entorno aislado. Una prueba unitaria con dobles no reemplaza esa
validacion de extremo a extremo.

No se conoce aun el repositorio ni la tecnologia frontend. Al ejecutar F1 se
deben identificar sus rutas, componentes, cliente HTTP, autenticacion y pruebas
existentes. Este plan no prescribe un framework ni crea una aplicacion aparte.
Si el frontend no tiene alguno de los flujos administrativos mencionados, su
integracion minima se incluye en la pareja correspondiente y debe concretarse
antes de implementarla.

Las entregas parciales se integran en desarrollo/pruebas, no se anuncian como
funcionalidad completa en produccion. Por decision posterior para T2, el primer
arranque de esa version fija el inicio automaticamente y comienza el
reconocimiento solo de ventas normales nuevas. T8-F8 completa el despliegue,
sin reiniciar el instante de inicio.

## Reglas de experiencia y compatibilidad

- Reutilizar estilos, navegacion, controles de permisos, notificaciones y
  formatos monetarios existentes.
- No modificar el catalogo publico, checkout ni reporte comercial existente.
- Separar costo reconocido al proveedor de ingreso comercial: una PQR no cobra.
- No recalcular la liquidacion en el navegador; mostrar valores del backend.
- Mostrar cantidades como unidades de presentacion, no gramos ni ordenes.
- Trabajar con fecha de negocio fija America/Bogota (hora de Lima), sin selector
  de zona; no inferir el dia con la zona
  local del navegador. Mantener las fechas de jornada como fechas sin hora.
- Preservar precision monetaria segun el contrato acordado; evitar calculos
  financieros con redondeos propios del cliente.
- Diferenciar carga, vacio, error y datos disponibles. Nunca mostrar ceros o
  exito como sustituto de un fallo de consulta.
- Un control oculto no reemplaza autorizacion del servidor.
- No reintentar automaticamente pagos o creacion de PQR tras una respuesta
  incierta. Consultar el estado y notificar; no prometer idempotencia inexistente.

## F1 - Pantalla base conectada al resumen real

**Pareja:** T1. Sin prerrequisitos de otras parejas.

### Trabajo frontend

- Identificar la arquitectura real del frontend y reutilizar cliente HTTP,
  autenticacion, navegacion ADMIN y componentes existentes.
- Acordar con T1 contrato, dinero, fechas, errores y paginacion antes de consumir.
- Agregar acceso ADMIN a "Liquidacion diaria".
- Consumir `GET /api/v1/settlements/daily-summary` con autenticacion real.
- Mostrar la fecha de negocio y las columnas del resumen acordado.
- Integrar carga, estado vacio real, error de red y falta de permisos.
- Preparar lectura de paginacion real; los controles avanzados se completan en F6.
- No presentar como disponibles pagos o detalles aun no implementados.

### Cierre integrado T1-F1

- Un ADMIN entra desde la navegacion real y la pantalla consulta la API real.
- Sin movimientos se muestra un mensaje explicativo, no proveedores ficticios.
- La fecha por defecto es la devuelta/acordada con backend para Bogota.
- Se verifica comportamiento de un usuario sin permisos.
- No queda pendiente "conectar luego al backend".

### Contrato disponible para F1

El backend T1 publica la consulta real descrita en
[README](README.md#liquidacion-diaria-hu-04t1). T1 devolvia
`activationPending: true` y `activatedAt: null`. A partir de T2, el primer
arranque persiste automaticamente el inicio y la respuesta devuelve
`activationPending: false` con `activatedAt`. El estado vacio sin ventas
elegibles es real y no debe reemplazarse con filas ficticias.

## F2 - Venta normal y acumulacion visible

**Pareja:** T2. **Depende de:** T1-F1 cerrada.

### Trabajo frontend

- Integrar el flujo administrativo real de confirmacion de una venta normal.
- Tras confirmacion exitosa, invalidar/actualizar los datos afectados y mostrar
  el acumulado real; no incrementarlo de forma optimista en el cliente.
- Distinguir unidades vendidas, costo reconocido, saldo y sugerido.
- Quitar o deshabilitar cancelar para ventas que ya pasaron por PAGADA, incluso
  en EMPACADA; conservar cancelacion de ventas CREADA.
- Gestionar conflictos del backend sin ocultarlos y refrescar la orden afectada.
- Conservar presentacion y reglas del reporte de ingresos comerciales.

### Cierre integrado T2-F2

- Crear y pagar una venta elegible mediante el flujo real y verificar el resumen.
- Recargar la pagina conserva los valores.
- Con saldo cero se muestra costo reconocido y sugerido cero.
- No se puede cancelar una venta pagada desde UI ni mediante llamada directa.
- Repetir acciones no duplica los importes.

### Contrato disponible para F2

Se reutiliza la creacion de ordenes y
`PUT /api/admin/orders/{id}/status` con `{"status":"PAGADA"}` para venta normal.
Consultar nuevamente el resumen tras confirmar. Las ordenes anteriores a
`activatedAt` se ignoran para acumulacion, no se borran.
Cancelar ventas ya pagadas devuelve `409`, incluso en EMPACADA o contra entrega.
El flujo HTTP backend esta verificado con MongoDB y JWT reales; falta validar
la interfaz F2 en su repositorio.

## F3 - Contra entrega sin reconocimiento anticipado

**Pareja:** T3. **Depende de:** T2-F2 cerrada.

### Trabajo frontend

- Reutilizar la seleccion actual de contra entrega y confirmacion de PAGADA.
- Mostrar que stock descontado no equivale a costo reconocido en esa etapa.
- Integrar la entrega con efectivo/tarjeta segun las validaciones actuales.
- Tras ENTREGADA exitosa, actualizar orden y resumen desde la API.
- Mantener errores de entrega visibles; no mostrar exito si no se confirmo.
- No presentar cancelar como accion disponible para una venta ya pagada.

### Cierre integrado T3-F3

- Confirmar contra entrega en PAGADA no aumenta el acumulado.
- Entregarla correctamente aumenta el costo del dia de entrega.
- Un pago de entrega invalido no altera el resumen.
- Entregar una venta normal no duplica su reconocimiento.

### Contrato disponible para F3

El backend reconoce contra entrega solo al confirmar ENTREGADA con el endpoint
actual de estado, efectivo y tarjeta validos. Ver [contrato T3](README.md#reconocimiento-de-contra-entrega-hu-04t3).
No hay nuevos endpoints ni formatos de respuesta. Tras exito actualizar orden
y resumen del dia de deliveredAt en Bogota; no incrementar totales localmente.
Una entrega fallida no cambia importes/estado/acumulado ni vuelve a mover stock.
Repeticiones y conflictos concurrentes devuelven 409.

## F4 - PQR y cancelacion con efecto sobre la jornada original

**Pareja:** T4. **Depende de:** T3-F3 cerrada.

### Trabajo frontend

- Integrar creacion de PQR con presentacion y cantidad real, sin cobro al cliente.
- Conservar estado inicial CREADA y transiciones actuales de PQR.
- Mostrar el efecto real sobre unidades PQR y costo reconocido del proveedor.
- Integrar cancelacion de PQR solo en estados actualmente permitidos.
- Explicar que la cancelacion devuelve stock y ajusta la jornada original.
- Tras cancelar, actualizar orden, stock mostrado y resumen afectado desde API.
- No borrar filas o historial localmente para simular una reversion.

### Cierre integrado T4-F4

- PQR de dos unidades muestra dos unidades reconocidas y valor comercial cero.
- Cancelar una PQR de otro dia ajusta ese dia, no el acumulado de hoy.
- Una jornada totalmente revertida sigue siendo consultable.
- Repetir la cancelacion produce el resultado real del servidor sin duplicar.

### Contrato real disponible desde T4

- Crear con `POST /api/admin/orders/pqr` (ADMIN), enviando `username` e
  `items: [{productId, quantity}]`; `productId` es el ID de la presentacion.
  No enviar costos internos ni calcular reconocimiento en el navegador.
  Se conserva respuesta de orden, estado CREADA y valores comerciales cero.
- Cancelar con `PUT /api/admin/orders/{id}/status`, cuerpo
  `{"status":"CANCELADA","reason":"Motivo"}`. Solo desde CREADA o EMPACADA;
  ENTREGADA no se puede cancelar. Repetir/conflicto devuelve 409.
- Tras crear, recargar orden, stock y resumen del dia de `createdAt` en Bogota.
  Tras cancelar, recargar esa misma jornada aunque no coincida con hoy.
  Un timeout de creacion exige verificar ordenes antes de repetir el POST;
  no existe idempotencia para dos creaciones validas.
- `totalPqrCount` es cantidad vigente; `pqrRecognizedAmount` conserva el bruto,
  `reversedAmount` identifica lo revertido y `accumulatedAmount` es el neto.
  `totalSalesCount`/`salesRecognizedAmount` no aumentan por PQR.
  No ocultar la fila completamente revertida ni borrar pagos si aparece exceso.
- El resumen sigue en `GET /api/v1/settlements/daily-summary?date=YYYY-MM-DD`.
  La auditoria se persiste desde T4; su consulta paginada llegara en T6.
  El registro de pagos de jornada sigue pendiente de T5.

Backend validado con HTTP/JWT/MongoDB reales; la validacion de F4 en la interfaz
debe completarse en el repositorio frontend antes de autorizar T5-F5.
Ver [contrato completo](README.md#reconocimiento-y-reversion-de-pqr-hu-04t4).

## F5 - Pago general o vinculado a la jornada

**Pareja:** T5. **Depende de:** T4-F4 cerrada.

### Trabajo frontend

- Extender el formulario existente de abonos, sin sustituir su flujo actual.
- Desde liquidacion, permitir asociar el pago al proveedor y fecha seleccionados
  mediante `fechaLiquidacion` en el endpoint actual.
- En un abono general, omitir ese campo; no enviar automaticamente la fecha de hoy.
- Mostrar importe sugerido como referencia editable, no como limite maximo.
- Permitir pagos superiores al sugerido si el backend los admite segun saldos.
- Conservar seleccion de lote o distribucion FIFO actual.
- Antes de confirmar, mostrar proveedor, importe, jornada vinculada y destino
  de imputacion. Mostrar explicitamente cuando el pago supera el sugerido.
- Acordar con T5 validaciones de fechas/jornadas sin inventar restricciones.
- Evitar envios simultaneos desde el formulario; si hay timeout, indicar que se
  debe verificar el abono antes de repetirlo.
- Tras exito, actualizar desde servidor pagos, saldos, pendiente, sugerido y exceso.
- Mostrar exceso pagado sin tratarlo como deuda negativa ni proponer reembolso
  automatico.

### Cierre integrado T5-F5

- Registrar un pago real de jornada y verificar su persistencia tras recarga.
- Verificar pago parcial, superior al sugerido y repartido entre varios lotes.
- Registrar abono general y comprobar que no se asigna a una jornada.
- Cancelar una PQR ya pagada y visualizar el exceso sin borrar el abono.
- Los errores de saldo o concurrencia se muestran sin exito aparente.

## F6 - Navegacion, filtros y detalle auditable

**Pareja:** T6. **Depende de:** T5-F5 cerrada.

### Trabajo frontend

- Completar filtro de fecha, seleccion/busqueda de proveedor y paginacion usando
  APIs reales existentes o acordadas, sin cargar todos los registros en memoria.
- Reiniciar la pagina cuando cambie un filtro y evitar mostrar respuestas tardias
  de una consulta anterior como si fueran actuales.
- Abrir detalle del proveedor/jornada con consulta real paginada.
- Distinguir ventas, PQR, reversiones y pagos.
- Mostrar presentacion, cantidad, costo unitario reconocido e importe cuando
  aplique; no mostrar costo unitario ficticio para pagos.
- Mostrar fecha real del evento y jornada afectada en reversiones.
- Navegar a orden/lote/abono solo cuando exista una ruta autorizada disponible.
- Explicar que el sugerido de una jornada anterior utiliza saldo global actual.
- Mantener filtros y contexto al volver del detalle.

### Cierre integrado T6-F6

- Resumen y detalle concuerdan para varias presentaciones con diferentes costos.
- Filtros, paginas vacias y conteos funcionan contra datos reales.
- Al cambiar filtros rapidamente no se mezclan proveedores o jornadas.
- Los movimientos de cancelacion y los pagos pueden distinguirse y rastrearse.

## F7 - Pruebas integradas y regresion de interfaz

**Pareja:** T7. **Depende de:** T6-F6 cerrada.

### Trabajo frontend

- Ejecutar recorridos de extremo a extremo con API y replica set reales.
- Cubrir venta normal, contra entrega, PQR, cancelacion y pagos de jornada.
- Validar cantidades multiples, varios proveedores, jornadas anteriores, corte
  de activacion y cambio de dia entre navegador y Bogota.
- Probar doble clic, sesiones vencidas, conflictos concurrentes, errores de red
  y respuesta incierta sin reenviar mutaciones automaticamente.
- Comprobar consistencia tras recarga y acciones desde dos sesiones.
- Verificar permisos y que costos no se filtren a pantallas publicas.
- Ejecutar build, analisis de tipos/lint y pruebas existentes segun el proyecto.
- Validar accesibilidad de formularios/tablas y uso en los tamanos de pantalla
  que el producto ya soporte.
- Revisar que catalogo, checkout, reporte comercial y abonos anteriores no cambien.

### Cierre integrado T7-F7

- Recorridos reales aprobados junto con T7.
- No hay pruebas con mocks usadas como reemplazo del recorrido integrado.
- Sin discrepancias de importes, contratos, estados o permisos pendientes.
- Validaciones frontend y backend aprobadas.

## F8 - Despliegue y habilitacion coordinada

**Pareja:** T8. **Depende de:** T7-F7 cerrada.

### Trabajo frontend

- Preparar configuracion del entorno destino sin incrustar secretos.
- Publicar documentacion breve de interpretacion de columnas, fecha de corte,
  pagos generales/vinculados, sugerido editable y exceso pagado.
- Desplegar de forma compatible con el backend y habilitar acceso solo cuando
  la API completa y sus indices/configuracion esten listos.
- Ejecutar en preproduccion el recorrido completo junto con backend.
- Comprobar autenticacion, fecha, filtros y acceso al resumen en el entorno final.
- Acordar como deshabilitar el acceso nuevo ante incidentes sin alterar pantallas
  anteriores ni repetir pagos ya realizados.
- No inventar registros productivos para una demostracion.

### Cierre integrado T8-F8

- UI y backend habilitados con contrato compatible y corte productivo acordado.
- Comprobacion real y autorizada completada.
- Flujos anteriores siguen funcionando y existe guia operativa compartida.

## Regla comun de avance

```text
T1-F1 -> T2-F2 -> T3-F3 -> T4-F4 -> T5-F5 -> T6-F6 -> T7-F7 -> T8-F8
```

Una pareja solo se cierra con codigo persistido de ambos lados, pruebas,
demostracion real integrada, documentacion y confirmacion del responsable.
Si falta una API o una pantalla de la pareja, se resuelve antes de avanzar;
no se sustituye con mocks ni se difiere la integracion a la siguiente entrega.
