# HU-04 - Plan de ejecucion backend alineado con frontend

## Objetivo y modalidad

Integrar el acumulado diario de liquidacion por proveedor sin duplicar el
inventario, modificar reportes comerciales ni reemplazar el evaluador existente.

Este plan reorganiza las tareas anteriores en entregas verticales. Su numeracion
las sustituye: cada Tn se desarrolla junto con Fn del
[plan frontend](HU04-tareas-frontend.md).

**No se avanza a la siguiente pareja hasta que la actual este integrada y
validada de extremo a extremo contra backend y MongoDB reales.** No se utilizaran
mocks, respuestas simuladas ni datos inventados como sustituto de la integracion.
Se pueden crear datos de prueba mediante los flujos reales en un entorno aislado.
Las pruebas unitarias pueden usar dobles; no sirven como evidencia de integracion
frontend-backend.

Las parejas se integran en un entorno de desarrollo/pruebas. Por confirmacion
posterior al iniciar T2, el primer arranque de T2 guarda automaticamente el
instante inicial si no existia o estaba pendiente en T1. Se conserva tras
reinicios y no se reprocesan ordenes anteriores. Desplegar T2 implica comenzar
el reconocimiento de ventas normales elegibles; no incluye T3-T8.

## Reglas confirmadas

- Venta normal: reconoce costo al pasar a PAGADA.
- Contra entrega: descuenta stock como hoy en PAGADA, pero reconoce costo solo
  al pasar a ENTREGADA.
- PQR: reconoce costo al crearse, conserva CREADA y valores comerciales cero.
- Costo reconocido: snapshot del item multiplicado por su cantidad real.
- El bruto se reconoce incluso con saldo global cero o lote pagado.
- Se impide cancelar ventas que hayan pasado por PAGADA, incluso en EMPACADA.
- Una PQR cancelable devuelve stock y revierte su reconocimiento en la fecha
  original, conservando auditoria. No se habilitan nuevas transiciones de estado.
- Una cancelacion no deshace pagos reales ya registrados.
- Zona de negocio fija, no parametrizable: America/Bogota (UTC-05:00, hora de Lima).
- Se excluyen todas las ordenes creadas antes del instante de activacion, aunque
  se paguen o entreguen despues. No hay reprocesamiento historico.
- Las consultas y pagos de liquidacion son solo para ADMIN.
- El endpoint actual de evaluacion de liquidacion conserva contrato y reglas.

### Formulas del resumen

```text
acumuladoVigente = brutoReconocido - costoRevertido
pendienteDiario = max(acumuladoVigente - pagosVinculados, 0)
sugerido = min(pendienteDiario, saldoGlobalActual)
excesoPagado = max(pagosVinculados - acumuladoVigente, 0)
```

Las unidades mostradas como vendidas/PQR son cantidades de presentaciones,
no gramos ni numero de ordenes. Se distinguen reconocimientos y reversiones.
El costo unitario se muestra por presentacion/costo en el detalle, nunca como un
unico costo representativo del proveedor.

## T1 - Consulta real y pantalla base del resumen diario

**Pareja:** F1. Sin prerrequisitos de otras parejas.

### Trabajo backend

- Definir y publicar el contrato que F1 consumira, incluyendo formatos de fecha,
  dinero, paginacion, errores y orden estable.
- Implementar modelo, puerto, adaptador e indice unico de
  `provider_daily_accumulators` por proveedor y fecha.
- Implementar `GET /api/v1/settlements/daily-summary` con fecha opcional
  (hoy en Bogota), `providerId` opcional, `page` y `size`.
- Devolver resumen con identificacion del proveedor, fecha, unidades de venta y
  PQR vigentes, costos reconocidos/revertidos, acumulado vigente, pagos vinculados,
  pendiente diario, saldo global actual, sugerido y exceso pagado.
- Consultar el saldo real del proveedor y calcular el sugerido al consultar;
  no persistir un sugerido que pueda quedar obsoleto.
- Listar solo jornadas con movimientos o pagos asociados. Un resultado sin
  movimientos es una pagina vacia, no filas sinteticas de proveedores.
- Proteger el endpoint para ADMIN y mantener el formato de errores del proyecto.
- Persistir la activacion pendiente en T1. La decision posterior para T2 es
  establecer el inicio automaticamente una sola vez al arrancar T2.
- Documentar el contrato para el equipo frontend.

### Cierre integrado T1-F1

- Un ADMIN abre la pantalla y consume el endpoint real.
- En una base sin movimientos se visualiza el estado vacio real.
- La fecha por defecto coincide con el dia de Bogota.
- Un usuario sin permisos no puede consultar el resumen.
- No se agregan datos ficticios para aparentar acumulacion: la primera venta
  reconocida se integra en T2-F2.

### Estado backend T1

Implementado el endpoint real, modelo/puerto/adaptador, indices, consulta del
saldo actual, seguridad ADMIN y metadatos de activacion pendiente. Contrato y
comandos de prueba disponibles en [README](README.md#liquidacion-diaria-hu-04t1).
En T1 no se conectaron ventas, PQR ni pagos. El responsable confirmo la
finalizacion de T1 y autorizo iniciar T2; no se declara una pareja integrada
solo por pruebas de backend.

## T2 - Venta normal, acumulacion y bloqueo de cancelacion

**Pareja:** F2. **Depende de:** T1-F1 cerrada.

### Trabajo backend

- Crear persistencia independiente de movimientos auditables con orden, item,
  proveedor, lote, contenedor, cepa, presentacion, cantidad, costo snapshot,
  importe, fecha del evento y jornada.
- Crear claves unicas de reconocimiento para evitar duplicaciones por reintento.
- Conectar venta normal a PAGADA y acumular cantidades/costos por proveedor.
- Excluir contra entrega y PQR de este disparador.
- Reconocer el costo aunque lote/proveedor tengan saldo cero.
- Aplicar corte por fecha de creacion de la orden.
- Mantener orden, inventario y reconocimiento en la misma transaccion.
- Bloquear cancelaciones de ventas que ya pasaron por PAGADA, tambien en
  EMPACADA y tambien para contra entrega. Mantener cancelacion de CREADA.
- Propagar un conflicto explicito ante cancelacion prohibida o concurrencia.
- No cambiar los totales ni criterios del reporte comercial existente.

### Cierre integrado T2-F2

- Desde el flujo real de ordenes se crea y paga una venta normal posterior al
  corte; el resumen muestra cantidad y costo reales del proveedor.
- Repetir la confirmacion no duplica stock ni acumulado.
- La UI no ofrece cancelar una venta pagada; una llamada directa tambien falla.
- Con saldo cero el bruto aparece y el sugerido es cero.
- Una orden anterior al corte conserva su flujo, pero no aporta acumulacion.

### Estado backend T2

Implementados movimientos por item con clave unica, acumulacion transaccional
de ventas normales en PAGADA, inicio automatico persistente y bloqueo de
cancelacion de ventas pagadas. Se reutilizan endpoints de ordenes y consulta.
No se integraron reconocimiento contra entrega, PQR ni pagos de jornada.
Ver [contrato y validacion](README.md#reconocimiento-de-ventas-normales-hu-04t2).
El responsable confirmo la finalizacion de T2 y autorizo T3.
Validacion backend: 497 pruebas Surefire aprobadas, sin fallos ni omisiones,
cobertura de lineas 94,14 %. Se incluyen API HTTP real, transacciones MongoDB,
duplicados, reversion por fallo y concurrencia sobre un mismo acumulado.
Las cinco pruebas E2E antiguas de productos se excluyen de este comando
(`-DskipITs=true`) por el fallo preexistente de escrituras sin autenticacion.

## T3 - Reconocimiento de contra entrega

**Pareja:** F3. **Depende de:** T2-F2 cerrada.

### Trabajo backend

- Conectar ENTREGADA de una venta contra entrega al reconocimiento financiero.
- Usar deliveredAt para determinar la jornada y el snapshot para el costo.
- Reutilizar validacion actual de efectivo/tarjeta; no registrar ingreso ni
  costo de una entrega que falla.
- No reconocer otra vez ventas normales al entregar.
- Mantener pago de entrega, cambio de estado y reconocimiento atomicos.

### Cierre integrado T3-F3

- PAGADA contra entrega descuenta inventario, sin aumentar el acumulado.
- La entrega valida incrementa el acumulado de su dia.
- La UI exige los importes actuales de entrega y muestra errores reales.
- Una entrega repetida o invalida no genera reconocimiento adicional.

### Estado backend T3

Implementado reconocimiento en ENTREGADA para contra entrega, reutilizando
snapshot, clave unica y acumulador de ventas. La jornada usa deliveredAt en
Bogota. No cambia stock al entregar ni las validaciones actuales del cobro.
Contrato para F3 en [README](README.md#reconocimiento-de-contra-entrega-hu-04t3).
El responsable confirmo la finalizacion de T3 y autorizo T4.
Validacion: 505 pruebas Surefire aprobadas, sin fallos ni omisiones; cobertura
de lineas 94,25 %. Incluye flujo HTTP real, cobros invalidos, rollback al guardar,
doble entrega concurrente, fecha de entrega alrededor de medianoche y exclusion
de ordenes anteriores al inicio. Se mantiene la exclusion de las cinco pruebas
E2E antiguas de productos por su problema preexistente de autenticacion.

## T4 - PQR, cancelacion y reversion auditable

**Pareja:** F4. **Depende de:** T3-F3 cerrada.

### Trabajo backend

- Conectar creacion de PQR al acumulador usando cantidad real y costo snapshot.
- Conservar CREADA, ingreso comercial cero y descuento existente de inventario.
- Conectar cancelaciones permitidas de PQR a restitucion y reversion financiera.
- Registrar la reversion como movimiento independiente, enlazado al original,
  con fecha real de cancelacion y jornada original afectada.
- Revertir cantidades y costo una sola vez, sin borrar auditoria.
- No restar un reconocimiento inexistente en PQR anteriores al corte.
- Preservar transaccion y control de concurrencia para stock, orden y acumulado.

### Cierre integrado T4-F4

- Crear una PQR de dos unidades reconoce dos costos y cero ingreso comercial.
- Cancelarla devuelve stock y actualiza la jornada original, aunque sea otro dia.
- El resumen conserva visibilidad de una jornada totalmente revertida.
- Reintentar la cancelacion no duplica la restitucion ni la reversion.

### Estado backend T4

Implementado reconocimiento PQR en CREADA por cantidad real y snapshot, sin
cobro comercial ni deuda adicional. Cancelar desde CREADA/EMPACADA restituye
stock y registra PQR_REVERSAL enlazado al original en su jornada, conservando
bruto, auditoria y pagos. ENTREGADA sigue sin poder cancelarse. Una PQR sin
reconocimiento original no fabrica reversion ni se importa retroactivamente.
Contrato F4 en [README](README.md#reconocimiento-y-reversion-de-pqr-hu-04t4).

Validacion: 519 pruebas Surefire aprobadas, sin fallos ni omisiones; cobertura
de lineas 92,90 %. Incluye HTTP real de creacion/cancelacion, cantidades e items
repetidos, varios proveedores, saldo cero, jornada original, costos modificados,
pagos conservados y exceso resultante, rollback de creacion/cancelacion,
acumulados inconsistentes/ausentes y cancelaciones concurrentes. Se mantiene
la exclusion de las cinco pruebas E2E antiguas de productos por autenticacion.
La integracion visual F4 corresponde al repositorio frontend. No se inicia T5
sin confirmacion del responsable; no se agrego registro de pagos de jornada.

## T5 - Pagos vinculados a una jornada

**Pareja:** F5. **Depende de:** T4-F4 cerrada.

### Trabajo backend

- Extender `POST /api/proveedores/{id}/pagos` con `fechaLiquidacion` opcional.
- Conservar respuesta y comportamiento de solicitudes sin ese campo.
- En solicitudes vinculadas, registrar proveedor, jornada e identificador de
  correlacion del pago; relacionarlo con las imputaciones a lotes.
- Evitar contar varias veces un pago distribuido entre lotes.
- Actualizar pagos vinculados y recalcular pendiente, sugerido y exceso.
- Permitir pagos parciales o superiores al sugerido, respetando saldo global,
  saldo del lote cuando se especifica y distribucion FIFO actual en caso contrario.
- Guardar deuda, transacciones de pago y vinculo diario en una misma transaccion.
- No inferir fecha de liquidacion a partir de la fecha de registro del abono.
- No deshacer un pago cuando se cancele una PQR; reflejar el exceso resultante.
- Documentar con F5 validaciones de fechas/jornadas para pagos vinculados antes
  de implementarlas; no introducir restricciones de negocio no confirmadas.

### Cierre integrado T5-F5

- El formulario real registra un pago asociado a la jornada seleccionada.
- Un pago repartido entre lotes cuenta una vez por su total.
- Se acepta un importe superior al sugerido si cumple los limites actuales.
- Un abono general reduce saldo global, sin aumentar pagos de una jornada.
- Cancelar una PQR ya pagada conserva el abono y muestra exceso.
- Una recarga completa conserva los resultados.

## T6 - Detalle auditable y filtros completos

**Pareja:** F6. **Depende de:** T5-F5 cerrada.

### Trabajo backend

- Publicar una consulta ADMIN paginada de detalle por proveedor y fecha, acordada
  con F6; propuesta: `GET /api/v1/settlements/daily-detail`.
- Devolver reconocimientos, reversiones y pagos con sus referencias, cantidades,
  costos aplicables y marcas de tiempo. No asignar costo unitario a un pago.
- Para reversiones distinguir fecha de registro y jornada afectada.
- Mostrar procedencias y referencias necesarias para navegar a ordenes/lotes
  autorizados; no exponer informacion de costos en endpoints publicos.
- Completar filtros de fecha/proveedor y paginacion de resumen y detalle con
  conteos en base de datos y orden estable.
- Evitar arreglos de detalle ilimitados en el documento del acumulador.

### Cierre integrado T6-F6

- Se navega del resumen al detalle real y se verifican sus importes.
- Busquedas, cambios de fecha/proveedor y paginas conservan resultados correctos.
- El costo de cada presentacion se distingue incluso con costos diferentes.
- El historico indica que el sugerido usa saldo global actual, no historico.

## T7 - Validacion integral, concurrencia y regresion

**Pareja:** F7. **Depende de:** T6-F6 cerrada.

### Trabajo backend

- Ejecutar pruebas unitarias e integracion sobre MongoDB replica set aislado.
- Verificar varios proveedores/lotes, items repetidos, snapshot inmutable,
  fechas alrededor de medianoche, corte de activacion y jornadas anteriores.
- Forzar pagos/confirmaciones/cancelaciones concurrentes y fallos intermedios;
  comprobar ausencia de sobrepago, doble reconocimiento y escrituras parciales.
- Validar explicitamente errores reales y recuperacion del cliente sin convertir
  fallos en respuestas exitosas.
- Ejecutar regresion de inventario, marketplace, pagos generales, estados no
  afectados, reporte comercial y evaluador previo.
- Verificar permisos ADMIN, contratos y ausencia de costos en respuestas publicas.

### Cierre integrado T7-F7

- Pruebas de extremo a extremo con la UI, API y base reales aprobadas.
- Estado persistido comprobado despues de recargar y ante concurrencia.
- Build, suite y umbral de cobertura del backend aprobados.
- No se avanza con diferencias de contrato o errores de integracion pendientes.

## T8 - Despliegue coordinado y activacion

**Pareja:** F8. **Depende de:** T7-F7 cerrada.

### Trabajo backend

- Actualizar documentacion, contratos, indices y procedimiento de operacion.
- Verificar el inicio productivo persistido automaticamente al desplegar T2,
  sin reemplazarlo al desplegar nuevas etapas. No migrar ni reprocesar ordenes
  anteriores.
- Preparar orden de despliegue compatible: backend antes de habilitar la nueva UI.
- Verificar backup, replica set, indices y configuracion de zona/corte.
- Validar junto al frontend un recorrido completo en preproduccion.
- Definir recuperacion ante fallos sin borrar auditoria ni repetir pagos.
- Activar y ejecutar comprobaciones autorizadas sin fabricar ventas productivas.

### Cierre integrado T8-F8

- Pantalla y API estan habilitadas en el entorno destino con la misma version
  de contrato.
- Los flujos anteriores siguen operativos y los registros nuevos elegibles
  aparecen correctamente en el resumen.
- Existe evidencia de validacion y procedimiento operativo compartido.

## Regla comun de avance

```text
T1-F1 -> T2-F2 -> T3-F3 -> T4-F4 -> T5-F5 -> T6-F6 -> T7-F7 -> T8-F8
```

Cada pareja requiere codigo persistido, pruebas apropiadas, demostracion
integrada, documentacion actualizada y confirmacion del responsable antes de
iniciar la siguiente. Entregar solo endpoints o solo pantallas no cierra la pareja.
