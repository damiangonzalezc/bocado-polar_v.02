# BOCADO Polar · Rutas de la API

Todas las rutas se consumen desde el front a través del **API Gateway**: `http://localhost:8080` + la ruta.
También se puede llamar directo a cada microservicio en su puerto (útil con Swagger: `http://localhost:PUERTO/doc/swagger-ui.html`).

Leyenda de acceso (lo aplica el gateway): 🌐 pública · 🔑 requiere token · 👑 solo ADMIN · ⚙️ uso interno entre microservicios (igual pasa por token si se llama vía gateway).

## Autenticación y Usuarios · `ms-auth` · puerto 8081

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 👑 | GET | `/api/v1/auditoria` | ?usuarioId | Últimos 200 accesos, o los de un usuario: GET /api/v1/auditoria?usuarioId=7 |
| 🌐 | POST | `/api/v1/auth/registro` | `RegistroRequest` |  |
| 🌐 | POST | `/api/v1/auth/login` | `LoginRequest` |  |
| 🔑 | POST | `/api/v1/auth/logout` |  |  |
| 🔑 | GET | `/api/v1/auth/me` |  |  |
| 🔑 | GET | `/api/v1/auth/validar` | ?seccion | Ej: GET /api/v1/auth/validar?seccion=inventario |
| 🔑 | PUT | `/api/v1/auth/password` | `CambioPasswordRequest` |  |
| 👑 | GET | `/api/v1/permisos` |  |  |
| 👑 | GET | `/api/v1/permisos/secciones` |  |  |
| 👑 | GET | `/api/v1/permisos/rol/{rol}` |  |  |
| 👑 | PUT | `/api/v1/permisos/rol/{rol}` | `PermisosRequest` | Reemplaza las secciones del rol. Body: { "secciones": ["resumen","pedidos"] } |
| 👑 | GET | `/api/v1/usuarios` | ?rol&estado&q | Ej: GET /api/v1/usuarios?rol=REPARTIDOR&estado=ACTIVO&q=juan |
| 👑 | GET | `/api/v1/usuarios/{id}` |  |  |
| 👑 | POST | `/api/v1/usuarios` | `UsuarioAdminRequest` |  |
| 👑 | PUT | `/api/v1/usuarios/{id}` | `UsuarioUpdateRequest` |  |
| 👑 | PATCH | `/api/v1/usuarios/{id}/estado` | `EstadoRequest` |  |
| 👑 | PATCH | `/api/v1/usuarios/{id}/rol` | `RolRequest` |  |
| 👑 | DELETE | `/api/v1/usuarios/{id}` |  |  |

## Clientes y Perfil (CRM) · `ms-clientes` · puerto 8082

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/clientes` | ?segmento&q | Ej: GET /api/v1/clientes?segmento=VIP&q=soto |
| 🔑 | GET | `/api/v1/clientes/{id}` |  |  |
| 🔑 | GET | `/api/v1/clientes/usuario/{usuarioId}` |  |  |
| 🔑 | POST | `/api/v1/clientes` | `ClienteRequest` |  |
| ⚙️ | POST | `/api/v1/clientes/desde-usuario` | `ClienteRequest` | Uso interno (ms-auth): crea la ficha al registrarse o devuelve la existente. |
| 🔑 | PUT | `/api/v1/clientes/{id}` | `PerfilRequest` |  |
| 🔑 | GET | `/api/v1/clientes/{id}/direcciones` |  |  |
| 🔑 | POST | `/api/v1/clientes/{id}/direcciones` | `DireccionRequest` |  |
| 🔑 | PUT | `/api/v1/clientes/{id}/direcciones/{direccionId}` | `DireccionRequest` |  |
| 🔑 | PATCH | `/api/v1/clientes/{id}/direcciones/{direccionId}/principal` |  |  |
| 🔑 | DELETE | `/api/v1/clientes/{id}/direcciones/{direccionId}` |  |  |
| 🔑 | GET | `/api/v1/clientes/{id}/medios-pago` |  |  |
| 🔑 | POST | `/api/v1/clientes/{id}/medios-pago` | `MedioPagoRequest` |  |
| 🔑 | DELETE | `/api/v1/clientes/{id}/medios-pago/{medioId}` |  |  |
| 🔑 | GET | `/api/v1/clientes/{id}/preferencias` |  |  |
| 🔑 | PUT | `/api/v1/clientes/{id}/preferencias` | `PreferenciaRequest` |  |
| 🔑 | GET | `/api/v1/clientes/{id}/notas` |  |  |
| 🔑 | POST | `/api/v1/clientes/{id}/notas` | `NotaRequest` |  |
| ⚙️ | POST | `/api/v1/clientes/{id}/compras` | `CompraRequest` | Uso interno (ms-pedidos): suma la compra entregada y recalcula el segmento. |
| 🔑 | POST | `/api/v1/clientes/segmentar` |  |  |

## Catálogo y Valoraciones · `ms-catalogo` · puerto 8083

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🌐 | GET | `/api/v1/atributos` |  |  |
| 🌐 | GET | `/api/v1/atributos/{id}` |  |  |
| 🔑 | POST | `/api/v1/atributos` | `AtributoRequest` |  |
| 🔑 | PUT | `/api/v1/atributos/{id}` | `AtributoRequest` |  |
| 🔑 | POST | `/api/v1/atributos/{id}/valores` | `ValorRequest` |  |
| 🔑 | DELETE | `/api/v1/atributos/{id}` |  |  |
| 🌐 | GET | `/api/v1/categorias` |  |  |
| 🌐 | GET | `/api/v1/categorias/{id}` |  |  |
| 🔑 | POST | `/api/v1/categorias` | `CategoriaRequest` |  |
| 🔑 | PUT | `/api/v1/categorias/{id}` | `CategoriaRequest` |  |
| 🔑 | DELETE | `/api/v1/categorias/{id}` |  |  |
| 🌐 | GET | `/api/v1/productos` | ?q&categoriaId&precioMin&precioMax&incluirInactivos&orden | Ej: GET /api/v1/productos?q=salmon&categoriaId=1&precioMin=5000&orden=precio_asc |
| 🌐 | GET | `/api/v1/productos/por-ids` | ?ids | Ej: GET /api/v1/productos/por-ids?ids=1,2,3 (uso interno de ms-pedidos). |
| 🌐 | GET | `/api/v1/productos/sku/{sku}` |  |  |
| 🌐 | GET | `/api/v1/productos/{id}` |  |  |
| 🔑 | POST | `/api/v1/productos` | `ProductoRequest` |  |
| 🔑 | PUT | `/api/v1/productos/{id}` | `ProductoRequest` |  |
| 🔑 | PATCH | `/api/v1/productos/{id}/activo` | `ActivoRequest` |  |
| 🔑 | DELETE | `/api/v1/productos/{id}` |  |  |
| 🔑 | PATCH | `/api/v1/productos/precios` | `AjustePreciosRequest` | Body: { "ids": [1,2], "porcentaje": 5 }  (ids vacío = todos) |
| 🔑 | POST | `/api/v1/productos/{id}/valoraciones` | `ValoracionRequest` |  |
| 🌐 | GET | `/api/v1/productos/{id}/valoraciones` | ?clienteId | Ej: GET /api/v1/productos/1/valoraciones?clienteId=1 |

## Inventario y Lotes · `ms-inventario` · puerto 8084

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/alertas-stock` | ?atendida | Ej: GET /api/v1/alertas-stock?atendida=false |
| 🔑 | POST | `/api/v1/alertas-stock/detectar` |  |  |
| 🔑 | PATCH | `/api/v1/alertas-stock/{id}/resolver` |  |  |
| 🔑 | GET | `/api/v1/lotes` | ?productoId&bloqueado&incluirAgotados | Ej: GET /api/v1/lotes?productoId=1&bloqueado=false&incluirAgotados=true |
| 🔑 | GET | `/api/v1/lotes/{id}` |  |  |
| 🔑 | POST | `/api/v1/lotes` | `LoteRequest` |  |
| 🔑 | PUT | `/api/v1/lotes/{id}` | `LoteUpdateRequest` |  |
| 🔑 | DELETE | `/api/v1/lotes/{id}` |  |  |
| 🔑 | GET | `/api/v1/lotes/fefo/{productoId}` |  | Lotes utilizables del producto, primero el que vence antes. |
| 🔑 | POST | `/api/v1/lotes/{id}/ajustes` | `AjusteRequest` |  |
| 🔑 | POST | `/api/v1/lotes/ajustes/masivo` | `AjusteMasivoRequest` |  |
| 🔑 | PATCH | `/api/v1/lotes/{id}/bloqueo` | `BloqueoRequest` | Lo usa ms-frio (alerta crítica) o el bodeguero. |
| 🔑 | PATCH | `/api/v1/lotes/{id}/desbloqueo` |  |  |
| 🔑 | POST | `/api/v1/reservas` | `ReservaRequest` |  |
| 🔑 | GET | `/api/v1/reservas/pedido/{pedidoId}` |  |  |
| 🔑 | PATCH | `/api/v1/reservas/pedido/{pedidoId}/confirmar` |  |  |
| 🔑 | PATCH | `/api/v1/reservas/pedido/{pedidoId}/liberar` |  |  |
| 🔑 | GET | `/api/v1/stock` |  | Stock de todos los productos (los críticos primero). |
| 🔑 | GET | `/api/v1/stock/{productoId}` |  |  |
| 🔑 | GET | `/api/v1/movimientos` | ?loteId&productoId | Ej: GET /api/v1/movimientos?loteId=3  o  ?productoId=1  (sin filtros: últimos 200). |

## Carrito y Pedidos · `ms-pedidos` · puerto 8085

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🌐 | POST | `/api/v1/carritos` | `CarritoRequest` (opcional) | Obtiene o crea el carrito. Body: {"clienteId":1} o {"sesionAnonimaId":"uuid"} o {} (nuevo visitante). |
| 🌐 | GET | `/api/v1/carritos/inactivos` | ?horas | Uso interno (ms-marketing): carritos con productos sin cambios hace N horas. |
| 🌐 | GET | `/api/v1/carritos/{id}` |  |  |
| 🌐 | GET | `/api/v1/carritos/cliente/{clienteId}` |  |  |
| 🌐 | POST | `/api/v1/carritos/{id}/items` | `ItemCarritoRequest` |  |
| 🌐 | PUT | `/api/v1/carritos/{id}/items/{productoId}` | `CantidadRequest` |  |
| 🌐 | DELETE | `/api/v1/carritos/{id}/items/{productoId}` |  |  |
| 🌐 | DELETE | `/api/v1/carritos/{id}/items` |  |  |
| 🌐 | PATCH | `/api/v1/carritos/{id}/cliente` | `AsociarClienteRequest` | Al iniciar sesión: une el carrito del visitante con el del cliente. |
| 🌐 | POST | `/api/v1/carritos/{id}/volver-a-pedir/{pedidoId}` |  |  |
| 🔑 | POST | `/api/v1/pedidos/cotizar` | `CotizacionRequest` | Resumen de totales antes de confirmar (no crea nada). |
| 🔑 | POST | `/api/v1/pedidos/checkout` | `CheckoutRequest` |  |
| 🔑 | GET | `/api/v1/pedidos` | ?estado&clienteId&comunas&desde&hasta&fechaEntrega&repartidorId&q | Ej: GET /api/v1/pedidos?estado=EN_PREPARACION&comunas=Tomé,Penco&desde=2026-10-01&hasta=2026-10-17&q=BP-1001 |
| 🔑 | GET | `/api/v1/pedidos/{id}` |  |  |
| 🔑 | GET | `/api/v1/pedidos/codigo/{codigo}` |  |  |
| 🔑 | GET | `/api/v1/pedidos/cliente/{clienteId}` |  | Mis pedidos. |
| 🔑 | GET | `/api/v1/pedidos/{id}/seguimiento` |  |  |
| 🔑 | GET | `/api/v1/pedidos/{id}/transiciones` |  | Estados a los que se puede pasar desde el actual (para la lista desplegable del panel). |
| 🔑 | PATCH | `/api/v1/pedidos/{id}/estado` | `CambioEstadoRequest` |  |
| 👑 | PATCH | `/api/v1/pedidos/{id}/repartidor` | `AsignacionRepartidorRequest` | Guarda el repartidor del pedido (lo llama ms-rutas). Si estaba PAGADO pasa a EN_PREPARACION. |
| 🔑 | PATCH | `/api/v1/pedidos/{id}/retroceder` | `MotivoRequest` |  |
| 🔑 | PATCH | `/api/v1/pedidos/{id}/cancelar` | `MotivoRequest` |  |
| ⚙️ | PATCH | `/api/v1/pedidos/{id}/pago` | `PagoEventoRequest` | Uso interno (ms-pagos). |
| 🔑 | GET | `/api/v1/pedidos/{id}/notas` |  |  |
| 🔑 | POST | `/api/v1/pedidos/{id}/notas` | `NotaRequest` |  |
| 🔑 | GET | `/api/v1/pedidos/{id}/etiqueta` |  |  |

## Pagos y Boletas · `ms-pagos` · puerto 8086

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/boletas/{id}` |  |  |
| 🔑 | GET | `/api/v1/boletas/pedido/{pedidoId}` |  |  |
| 🌐 | GET | `/api/v1/boletas/{id}/html` |  | Boleta imprimible: el front puede abrirla en una pestaña nueva (window.open). |
| 🔑 | POST | `/api/v1/pagos` | `IniciarPagoRequest` |  |
| 🔑 | GET | `/api/v1/pagos` | ?estado&metodo |  |
| 🔑 | GET | `/api/v1/pagos/cobros` | ?repartidorId&fecha | Ej: GET /api/v1/pagos/cobros?repartidorId=5&fecha=2026-10-17 (cierre de caja). |
| 🔑 | GET | `/api/v1/pagos/pedido/{pedidoId}` |  |  |
| 🔑 | GET | `/api/v1/pagos/{id}` |  |  |
| 🌐 | POST | `/api/v1/pagos/webhook` | `WebhookRequest` | Simula la respuesta de Webpay: {"pagoId":1,"aprobado":true,"codigoAutorizacion":"123456"} |
| 🔑 | PATCH | `/api/v1/pagos/{id}/confirmar-transferencia` |  |  |
| 🔑 | GET | `/api/v1/pagos/{id}/cobro` |  |  |
| 🔑 | PATCH | `/api/v1/pagos/{id}/cobro-entrega` | `CobroEntregaRequest` |  |
| 🔑 | POST | `/api/v1/pagos/{id}/reembolsos` | `ReembolsoRequest` |  |
| 🔑 | GET | `/api/v1/reembolsos` | ?estado |  |
| 🔑 | PATCH | `/api/v1/reembolsos/{id}/aprobar` | `AprobarRequest` (opcional) |  |
| 🔑 | PATCH | `/api/v1/reembolsos/{id}/rechazar` | `RechazarRequest` |  |

## Promociones y Marketing · `ms-marketing` · puerto 8087

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/canales` |  |  |
| 🔑 | POST | `/api/v1/canales` | `CanalRequest` |  |
| 🔑 | PUT | `/api/v1/canales/{id}` | `CanalRequest` |  |
| 🔑 | DELETE | `/api/v1/canales/{id}` |  |  |
| 🔑 | POST | `/api/v1/canales/visita` | `VisitaCanalRequest` |  |
| 🔑 | POST | `/api/v1/canales/conversion` | `ConversionCanalRequest` |  |
| 🔑 | GET | `/api/v1/carritos-abandonados` | ?recuperado |  |
| 🔑 | POST | `/api/v1/carritos-abandonados` | `AbandonadoRequest` |  |
| 🔑 | POST | `/api/v1/carritos-abandonados/detectar` | ?horas | Busca en ms-pedidos carritos sin movimiento hace N horas. |
| 🔑 | POST | `/api/v1/carritos-abandonados/{id}/rescate` | `RescateRequest` (opcional) | Body opcional: {"codigoCupon":"POLAR5000"} |
| 🔑 | PATCH | `/api/v1/carritos-abandonados/{id}/recuperado` | `RecuperadoRequest` (opcional) |  |
| 🔑 | GET | `/api/v1/cupones` |  |  |
| 🔑 | GET | `/api/v1/cupones/{id}` |  |  |
| 🔑 | GET | `/api/v1/cupones/codigo/{codigo}` |  |  |
| 🔑 | POST | `/api/v1/cupones` | `CuponRequest` |  |
| 🔑 | PUT | `/api/v1/cupones/{id}` | `CuponRequest` |  |
| 🔑 | PATCH | `/api/v1/cupones/{id}/activo` | `ActivoRequest` |  |
| 🔑 | DELETE | `/api/v1/cupones/{id}` |  |  |
| 🌐 | POST | `/api/v1/cupones/validar` | `ValidarCuponRequest` | Body: {"codigo":"BIENVENIDA10","clienteId":1,"subtotal":45000} → {"valido":true,"descuento":4500,...} |
| ⚙️ | POST | `/api/v1/cupones/usos` | `UsoRequest` | Uso interno (ms-pedidos). |
| 🔑 | GET | `/api/v1/cupones/{id}/usos` |  |  |

## Rutas y Despacho · `ms-rutas` · puerto 8088

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/rutas` | ?fecha&estado&repartidorId | Ej: GET /api/v1/rutas?fecha=2026-10-17&estado=PLANIFICADA&repartidorId=5 |
| 🔑 | GET | `/api/v1/rutas/{id}` |  |  |
| 🔑 | GET | `/api/v1/rutas/repartidor/{repartidorId}/hoy` |  | CU-08.06 Mi ruta del día (repartidorId = id de usuario del repartidor). |
| 🔑 | POST | `/api/v1/rutas` | `RutaRequest` |  |
| 🔑 | PUT | `/api/v1/rutas/{id}` | `RutaRequest` |  |
| 🔑 | DELETE | `/api/v1/rutas/{id}` |  |  |
| 🔑 | PATCH | `/api/v1/rutas/{id}/asignar` | `AsignacionRequest` |  |
| 🔑 | PATCH | `/api/v1/rutas/{id}/iniciar` |  |  |
| 🔑 | PATCH | `/api/v1/rutas/{id}/finalizar` |  |  |
| 🔑 | PATCH | `/api/v1/rutas/{id}/cancelar` |  |  |
| 🔑 | PATCH | `/api/v1/rutas/{id}/optimizar` |  |  |
| 👑 | POST | `/api/v1/rutas/asignar-pedido` | `AsignarPedidoRequest` | El admin asigna un pedido a un repartidor: lo deja como parada en la ruta del repartidor para la fecha de entrega (la crea si no existe) y lo saca de otra ruta. |
| 🔑 | GET | `/api/v1/rutas/{id}/paradas` |  |  |
| 🔑 | POST | `/api/v1/rutas/{id}/paradas/generar` |  | Trae de ms-pedidos los pedidos listos de las comunas y fecha de la ruta. |
| 🔑 | POST | `/api/v1/rutas/{id}/paradas` | `ParadaRequest` |  |
| 🔑 | DELETE | `/api/v1/rutas/{id}/paradas/{paradaId}` |  |  |
| 🔑 | GET | `/api/v1/paradas/{paradaId}` |  |  |
| 🔑 | PATCH | `/api/v1/paradas/{paradaId}/entregada` |  |  |
| 🔑 | PATCH | `/api/v1/paradas/{paradaId}/fallida` | `FallaRequest` |  |
| 🔑 | GET | `/api/v1/vehiculos` |  |  |
| 🔑 | GET | `/api/v1/vehiculos/{id}` |  |  |
| 🔑 | POST | `/api/v1/vehiculos` | `VehiculoRequest` |  |
| 🔑 | PUT | `/api/v1/vehiculos/{id}` | `VehiculoRequest` |  |
| 🔑 | DELETE | `/api/v1/vehiculos/{id}` |  |  |

## Reparto en Terreno · `ms-reparto` · puerto 8089

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/repartidores` | ?disponibilidad |  |
| 🔑 | GET | `/api/v1/repartidores/{id}` |  |  |
| 🔑 | POST | `/api/v1/repartidores` | `RepartidorRequest` |  |
| 🔑 | PATCH | `/api/v1/repartidores/{id}/disponibilidad` | `DisponibilidadRequest` |  |
| 🔑 | PATCH | `/api/v1/repartidores/{id}/preferencias` | `PreferenciasRequest` |  |
| 🔑 | POST | `/api/v1/turnos/iniciar` | `IniciarTurnoRequest` |  |
| 🔑 | GET | `/api/v1/turnos/activo/{repartidorId}` |  |  |
| 🔑 | POST | `/api/v1/turnos/{id}/cerrar` | `CerrarTurnoRequest` |  |
| 🔑 | GET | `/api/v1/cierres` | ?repartidorId |  |
| 🔑 | GET | `/api/v1/entregas` | ?repartidorId&fecha |  |
| 🔑 | GET | `/api/v1/entregas/{id}` |  |  |
| 🔑 | GET | `/api/v1/entregas/pedido/{pedidoId}` |  |  |
| 🔑 | POST | `/api/v1/entregas` | `EntregaRequest` |  |
| 🔑 | POST | `/api/v1/entregas/fallida` | `EntregaFallidaRequest` |  |
| 🔑 | POST | `/api/v1/entregas/{id}/calificacion` | `CalificacionRequest` |  |
| 🔑 | GET | `/api/v1/incidencias` | ?repartidorId |  |
| 🔑 | POST | `/api/v1/incidencias` | `IncidenciaRequest` |  |

## Cadena de Frío · `ms-frio` · puerto 8090

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | POST | `/api/v1/temperaturas` | `TemperaturaRequest` | Body: {"origen":"LOTE","referenciaId":3,"grados":-14.2,"registradoPorId":3} |
| 🔑 | GET | `/api/v1/temperaturas` | ?origen&referenciaId | Ej: GET /api/v1/temperaturas?origen=SALIDA_RUTA  ·  ?origen=LOTE&referenciaId=3 |
| 🔑 | GET | `/api/v1/temperaturas/lote/{loteId}` |  |  |
| 🔑 | GET | `/api/v1/alertas-frio` | ?atendida |  |
| 🔑 | PATCH | `/api/v1/alertas-frio/{id}/atender` | `AtenderRequest` (opcional) |  |
| 🔑 | POST | `/api/v1/cuentas-regresivas` | `CuentaRequest` |  |
| 🔑 | GET | `/api/v1/cuentas-regresivas/turno/{turnoId}` |  | Devuelve minutosRestantes, debeAvisar y excedida para el temporizador del repartidor. |

## Configuración y Reglas · `ms-config` · puerto 8091

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🌐 | GET | `/api/v1/configuracion` |  |  |
| 👑 | PUT | `/api/v1/configuracion` | `ConfiguracionRequest` |  |
| 🌐 | GET | `/api/v1/configuracion/iva` |  |  |
| 👑 | GET | `/api/v1/empresas-logistica` |  |  |
| 👑 | GET | `/api/v1/empresas-logistica/{id}` |  |  |
| 👑 | POST | `/api/v1/empresas-logistica` | `EmpresaRequest` |  |
| 👑 | PUT | `/api/v1/empresas-logistica/{id}` | `EmpresaRequest` |  |
| 👑 | PATCH | `/api/v1/empresas-logistica/{id}/activa` | `ActivaRequest` |  |
| 👑 | DELETE | `/api/v1/empresas-logistica/{id}` |  |  |
| 🌐 | GET | `/api/v1/pasarelas` |  |  |
| 🌐 | GET | `/api/v1/pasarelas/disponible` | ?nombre | Ej: GET /api/v1/pasarelas/disponible?nombre=Webpay (lo usa ms-pagos). |
| 👑 | PATCH | `/api/v1/pasarelas/{id}/conectar` |  |  |
| 👑 | PATCH | `/api/v1/pasarelas/{id}/desconectar` |  |  |
| 🔑 | GET | `/api/v1/reglas` |  |  |
| 🔑 | GET | `/api/v1/reglas/{codigo}` |  |  |
| 👑 | POST | `/api/v1/reglas` | `ReglaRequest` |  |
| 👑 | PUT | `/api/v1/reglas/{codigo}` | `ReglaValorRequest` |  |
| 👑 | DELETE | `/api/v1/reglas/{codigo}` |  |  |
| 🌐 | GET | `/api/v1/zonas` | ?activas |  |
| 🌐 | GET | `/api/v1/zonas/tarifa` | ?comuna |  |
| 🌐 | GET | `/api/v1/zonas/{id}` |  |  |
| 👑 | POST | `/api/v1/zonas` | `ZonaRequest` |  |
| 👑 | PUT | `/api/v1/zonas/{id}` | `ZonaRequest` |  |
| 👑 | PATCH | `/api/v1/zonas/{id}/activa` | `ActivaRequest` |  |
| 👑 | DELETE | `/api/v1/zonas/{id}` |  |  |

## Reportes y KPIs · `ms-reportes` · puerto 8092

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| 🔑 | GET | `/api/v1/kpis` | ?periodo | Ej: GET /api/v1/kpis?periodo=SIETE_DIAS  (HOY \| SIETE_DIAS \| MES_ACTUAL \| TREINTA_DIAS) |
| 🔑 | GET | `/api/v1/kpis/top-productos` | ?periodo&limite |  |
| 🔑 | GET | `/api/v1/kpis/ventas-diarias` | ?periodo |  |
| 🔑 | GET | `/api/v1/kpis/alertas-operativas` |  | cantidad = -1 significa que el microservicio correspondiente no respondió. |
| 🔑 | POST | `/api/v1/kpis/snapshot` | ?periodo |  |
| 🔑 | GET | `/api/v1/kpis/historial` |  |  |
| 🌐 | POST | `/api/v1/visitas` | `VisitaRequest` (opcional) | Lo llama el front una vez por sesión del navegador. Body opcional: {"canal":"Instagram"} |
| 🔑 | GET | `/api/v1/visitas` | ?desde&hasta |  |

## Notificaciones · `ms-notificaciones` · puerto 8093

| Acceso | Método | Ruta | Cuerpo (JSON) / parámetros | Descripción |
|---|---|---|---|---|
| ⚙️ | POST | `/api/v1/notificaciones/eventos` | `EventoRequest` | Uso interno: los demás microservicios publican aquí sus eventos. |
| 🔑 | GET | `/api/v1/notificaciones` | ?estado&motivo&canal&destinatario |  |
| 🔑 | GET | `/api/v1/notificaciones/panel` | ?soloNoLeidas | Campana del panel admin: GET /api/v1/notificaciones/panel?soloNoLeidas=true |
| 🔑 | POST | `/api/v1/notificaciones/reintentar-fallidas` |  |  |
| 🔑 | GET | `/api/v1/notificaciones/{id}` |  |  |
| 🔑 | GET | `/api/v1/notificaciones/{id}/historial` |  |  |
| 🔑 | POST | `/api/v1/notificaciones/{id}/reenviar` |  |  |
| 🔑 | PATCH | `/api/v1/notificaciones/{id}/leida` |  |  |
| 🔑 | GET | `/api/v1/plantillas` |  |  |
| 🔑 | GET | `/api/v1/plantillas/{id}` |  |  |
| 🔑 | POST | `/api/v1/plantillas` | `PlantillaRequest` |  |
| 🔑 | PUT | `/api/v1/plantillas/{id}` | `PlantillaRequest` |  |
| 🔑 | DELETE | `/api/v1/plantillas/{id}` |  |  |

## Cuerpos JSON (request)

Campos de cada cuerpo. Las fechas van como `"2026-10-20"`, las horas como `"08:30"`, los montos en CLP sin decimales y los kg con punto decimal (`2.5`).

- **RegistroRequest** (ms-auth) → `nombre`: String, `apellido`: String, `email`: String, `password`: String, `telefono`: String, `rut`: String
- **LoginRequest** (ms-auth) → `email`: String, `password`: String
- **UsuarioAdminRequest** (ms-auth) → `nombre`: String, `apellido`: String, `email`: String, `password`: String, `rol`: Rol, `telefono`: String, `rut`: String
- **UsuarioUpdateRequest** (ms-auth) → `nombre`: String, `apellido`: String, `telefono`: String, `rut`: String
- **CambioPasswordRequest** (ms-auth) → `actual`: String, `nueva`: String
- **EstadoRequest** (ms-auth) → `estado`: EstadoUsuario
- **RolRequest** (ms-auth) → `rol`: Rol
- **PermisosRequest** (ms-auth) → `secciones`: List<String>
- **ClienteRequest** (ms-clientes) → `usuarioId`: Long, `nombre`: String, `apellido`: String, `email`: String, `telefono`: String, `rut`: String
- **PerfilRequest** (ms-clientes) → `nombre`: String, `apellido`: String, `telefono`: String, `rut`: String
- **DireccionRequest** (ms-clientes) → `alias`: String, `calle`: String, `comuna`: String, `referencia`: String, `principal`: Boolean
- **MedioPagoRequest** (ms-clientes) → `alias`: String, `marca`: String, `ultimos4`: String, `vence`: String, `tokenPasarela`: String
- **PreferenciaRequest** (ms-clientes) → `alergias`: String, `notas`: String
- **NotaRequest** (ms-clientes) → `tipo`: TipoNota, `texto`: String, `autorId`: Long
- **CompraRequest** (ms-clientes) → `monto`: BigDecimal, `pedidoId`: Long
- **ProductoRequest** (ms-catalogo) → `sku`: String, `nombre`: String, `descripcion`: String, `categoriaId`: Long, `origen`: String, `alergenos`: String, `imagen`: String, `activo`: Boolean, `precioKg`: BigDecimal, `costoKg`: BigDecimal, `atributos`: Map<String, String>
- **ActivoRequest** (ms-catalogo) → `activo`: Boolean
- **AjustePreciosRequest** (ms-catalogo) → `ids`: List<Long>, `porcentaje`: BigDecimal
- **CategoriaRequest** (ms-catalogo) → `nombre`: String, `descripcion`: String
- **AtributoRequest** (ms-catalogo) → `nombre`: String, `valores`: List<String>
- **ValorRequest** (ms-catalogo) → `valor`: String
- **ValoracionRequest** (ms-catalogo) → `clienteId`: Long, `estrellas`: Double
- **LoteRequest** (ms-inventario) → `productoId`: Long, `codigoLote`: String, `stockKg`: BigDecimal, `vencimiento`: LocalDate, `tempAlmacenaje`: Double, `usuarioId`: Long
- **LoteUpdateRequest** (ms-inventario) → `codigoLote`: String, `vencimiento`: LocalDate, `tempAlmacenaje`: Double
- **AjusteRequest** (ms-inventario) → `tipo`: TipoMovimiento, `kg`: BigDecimal, `motivo`: String, `usuarioId`: Long
- **AjusteMasivoRequest** (ms-inventario) → `ajustes`: List<AjusteMasivoItem>, `usuarioId`: Long
- **BloqueoRequest** (ms-inventario) → `motivo`: String
- **ReservaRequest** (ms-inventario) → `pedidoId`: Long, `items`: List<ItemReserva>
- **CarritoRequest** (ms-pedidos) → `clienteId`: Long, `sesionAnonimaId`: String
- **ItemCarritoRequest** (ms-pedidos) → `productoId`: Long, `kg`: BigDecimal
- **CantidadRequest** (ms-pedidos) → `kg`: BigDecimal
- **AsociarClienteRequest** (ms-pedidos) → `clienteId`: Long
- **CheckoutRequest** (ms-pedidos) → `carritoId`: Long, `clienteId`: Long, `nombreCliente`: String, `email`: String, `telefono`: String, `direccion`: String, `comuna`: String, `referencia`: String, `comentario`: String, `fechaEntrega`: LocalDate, `horario`: String, `metodoPago`: MetodoPago, `codigoCupon`: String
- **CotizacionRequest** (ms-pedidos) → `carritoId`: Long, `comuna`: String, `clienteId`: Long, `codigoCupon`: String
- **CambioEstadoRequest** (ms-pedidos) → `estado`: EstadoPedido, `nota`: String, `usuarioId`: Long
- **MotivoRequest** (ms-pedidos) → `motivo`: String, `usuarioId`: Long
- **PagoEventoRequest** (ms-pedidos) → `evento`: String, `pagoId`: Long, `detalle`: String
- **NotaRequest** (ms-pedidos) → `texto`: String, `autorId`: Long
- **IniciarPagoRequest** (ms-pagos) → `pedidoId`: Long, `metodo`: MetodoPago, `idempotencyKey`: String, `tipoCobro`: TipoCobro, `pagaCon`: BigDecimal
- **WebhookRequest** (ms-pagos) → `pagoId`: Long, `aprobado`: Boolean, `codigoAutorizacion`: String, `motivo`: String
- **CobroEntregaRequest** (ms-pagos) → `repartidorId`: Long, `tipo`: TipoCobro, `pagaCon`: BigDecimal
- **ReembolsoRequest** (ms-pagos) → `motivo`: String, `monto`: BigDecimal, `solicitadoPorId`: Long
- **AprobarRequest** (ms-pagos) → `usuarioId`: Long
- **RechazarRequest** (ms-pagos) → `motivo`: String, `usuarioId`: Long
- **CuponRequest** (ms-marketing) → `codigo`: String, `tipo`: TipoDescuento, `valor`: BigDecimal, `compraMinima`: BigDecimal, `limiteUsos`: Integer, `limitePorCliente`: Integer, `activo`: Boolean, `vence`: LocalDate
- **ActivoRequest** (ms-marketing) → `activo`: Boolean
- **ValidarCuponRequest** (ms-marketing) → `codigo`: String, `clienteId`: Long, `subtotal`: BigDecimal
- **UsoRequest** (ms-marketing) → `codigo`: String, `pedidoId`: Long, `clienteId`: Long
- **CanalRequest** (ms-marketing) → `canal`: String, `visitas`: Integer, `pedidos`: Integer, `ingresos`: BigDecimal
- **VisitaCanalRequest** (ms-marketing) → `canal`: String
- **ConversionCanalRequest** (ms-marketing) → `canal`: String, `monto`: BigDecimal
- **AbandonadoRequest** (ms-marketing) → `cliente`: String, `email`: String, `resumen`: String, `total`: BigDecimal
- **RescateRequest** (ms-marketing) → `codigoCupon`: String
- **RecuperadoRequest** (ms-marketing) → `pedidoId`: Long
- **RutaRequest** (ms-rutas) → `nombre`: String, `fecha`: LocalDate, `comunas`: List<String>, `horaSalida`: LocalTime, `repartidorId`: Long, `repartidorNombre`: String, `vehiculoId`: Long, `notas`: String
- **AsignacionRequest** (ms-rutas) → `repartidorId`: Long, `repartidorNombre`: String, `vehiculoId`: Long
- **VehiculoRequest** (ms-rutas) → `tipo`: String, `patente`: String, `refrigerado`: Boolean, `activo`: Boolean
- **ParadaRequest** (ms-rutas) → `pedidoId`: Long, `codigoPedido`: String, `cliente`: String, `direccion`: String, `comuna`: String, `referencia`: String, `horario`: String, `cobrarAlEntregar`: Boolean, `montoACobrar`: BigDecimal
- **FallaRequest** (ms-rutas) → `motivo`: String
- **RepartidorRequest** (ms-reparto) → `usuarioId`: Long, `nombre`: String, `email`: String, `rut`: String
- **DisponibilidadRequest** (ms-reparto) → `disponibilidad`: Disponibilidad
- **PreferenciasRequest** (ms-reparto) → `sonido`: Boolean, `vibracion`: Boolean
- **IniciarTurnoRequest** (ms-reparto) → `repartidorId`: Long, `tempCajaInicial`: Double
- **CerrarTurnoRequest** (ms-reparto) → `efectivoRendido`: BigDecimal
- **CobroRequest** (ms-reparto) → `tipo`: String, `pagaCon`: BigDecimal
- **EntregaRequest** (ms-reparto) → `repartidorId`: Long, `turnoId`: Long, `pedidoId`: Long, `paradaId`: Long, `recibidoPor`: String, `tempEntrega`: Double, `fotoUrl`: String, `firmaUrl`: String, `comentario`: String, `cobro`: CobroRequest
- **EntregaFallidaRequest** (ms-reparto) → `repartidorId`: Long, `turnoId`: Long, `pedidoId`: Long, `paradaId`: Long, `tipo`: TipoIncidencia, `nota`: String
- **IncidenciaRequest** (ms-reparto) → `tipo`: TipoIncidencia, `nota`: String, `entregaId`: Long, `pedidoId`: Long, `reportadaPorId`: Long
- **CalificacionRequest** (ms-reparto) → `clienteId`: Long, `estrellas`: Integer
- **TemperaturaRequest** (ms-frio) → `origen`: OrigenTemp, `referenciaId`: Long, `grados`: Double, `registradoPorId`: Long
- **CuentaRequest** (ms-frio) → `turnoId`: Long
- **AtenderRequest** (ms-frio) → `usuarioId`: Long
- **ConfiguracionRequest** (ms-config) → `pais`: String, `moneda`: String, `preciosConIva`: Boolean, `iva`: BigDecimal
- **ZonaRequest** (ms-config) → `comuna`: String, `tarifa`: BigDecimal, `activa`: Boolean
- **EmpresaRequest** (ms-config) → `nombre`: String, `tipo`: String, `activa`: Boolean
- **ActivaRequest** (ms-config) → `activa`: Boolean
- **ReglaRequest** (ms-config) → `codigo`: String, `descripcion`: String, `valor`: BigDecimal, `unidad`: String
- **ReglaValorRequest** (ms-config) → `valor`: BigDecimal, `descripcion`: String, `unidad`: String
- **VisitaRequest** (ms-reportes) → `canal`: String
- **EventoRequest** (ms-notificaciones) → `eventoId`: String, `motivo`: MotivoNotificacion, `canal`: Canal, `destinatario`: String, `variables`: Map<String, Object>
- **PlantillaRequest** (ms-notificaciones) → `motivo`: MotivoNotificacion, `asunto`: String, `cuerpo`: String, `activa`: Boolean

