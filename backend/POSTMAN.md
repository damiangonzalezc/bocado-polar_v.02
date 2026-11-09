# BOCADO Polar · Pruebas en Postman

Consultas listas para copiar en Postman, ordenadas por microservicio.

## Antes de empezar

1. Enciende MySQL en XAMPP y levanta los servicios (`iniciar-todo.bat`). Levanta **ms-config** primero.
2. En Postman, en cada consulta con cuerpo, elige **Body → raw → JSON**.
3. Los ids de los ejemplos son los de los **datos de ejemplo** (base recién creada).

**Dos formas de llamar:**

| Forma | URL | Token |
|---|---|---|
| Directo al microservicio (recomendado para probar) | `http://localhost:PUERTO/api/v1/...` | No se pide |
| A través del gateway (como lo hará React) | `http://localhost:8080/api/v1/...` | Sí, salvo las rutas públicas |

Para usar el gateway: haz el login (sección 1), copia el `token` y en Postman ve a **Authorization → Bearer Token** y pégalo. Para las rutas de administrador inicia sesión con `admin@bocado.cl`.

**Variables sugeridas** (Postman → Environments): crea `auth`, `clientes`, etc. con cada URL base, o usa una sola `gw = http://localhost:8080` y la variable `token`.

| Servicio | Puerto | URL base |
|---|---|---|
| ms-auth | 8081 | `http://localhost:8081/api/v1` |
| ms-clientes | 8082 | `http://localhost:8082/api/v1` |
| ms-catalogo | 8083 | `http://localhost:8083/api/v1` |
| ms-inventario | 8084 | `http://localhost:8084/api/v1` |
| ms-pedidos | 8085 | `http://localhost:8085/api/v1` |
| ms-pagos | 8086 | `http://localhost:8086/api/v1` |
| ms-marketing | 8087 | `http://localhost:8087/api/v1` |
| ms-rutas | 8088 | `http://localhost:8088/api/v1` |
| ms-reparto | 8089 | `http://localhost:8089/api/v1` |
| ms-frio | 8090 | `http://localhost:8090/api/v1` |
| ms-config | 8091 | `http://localhost:8091/api/v1` |
| ms-reportes | 8092 | `http://localhost:8092/api/v1` |
| ms-notificaciones | 8093 | `http://localhost:8093/api/v1` |

> Las fechas van como `"2026-10-25"` y las horas como `"08:30"`. La `fechaEntrega` de un pedido debe estar entre mañana y 14 días más: ajústala al día en que pruebes.

---

## 1. ms-auth · 8081

**Iniciar sesión**
`POST http://localhost:8081/api/v1/auth/login`
```json
{
  "email": "admin@bocado.cl",
  "password": "PolarAdmin#26"
}
```
Respuesta: `token`, `usuario` y `permisos`. Otras cuentas: `cliente@bocado.cl / PolarCliente#26`, `reparto@bocado.cl / PolarReparto#26`, `bodega@bocado.cl / PolarBodega#26`.

**Registrar cliente**
`POST http://localhost:8081/api/v1/auth/registro`
```json
{
  "nombre": "Camila",
  "apellido": "Fuentes",
  "email": "camila.fuentes@gmail.com",
  "password": "Camila123",
  "telefono": "+56 9 8765 4321",
  "rut": "11.111.111-1"
}
```
El correo debe terminar en un solo `.com` o `.cl`. La contraseña necesita 8 caracteres, una mayúscula, una minúscula y un número.

**Datos del usuario con sesión** (header `Authorization: Bearer <token>`)
`GET http://localhost:8081/api/v1/auth/me`

**Validar token y permiso de una sección**
`GET http://localhost:8081/api/v1/auth/validar?seccion=inventario`

**Cambiar contraseña** (con token)
`PUT http://localhost:8081/api/v1/auth/password`
```json
{
  "actual": "PolarCliente#26",
  "nueva": "Cliente456"
}
```

**Cerrar sesión** (con token)
`POST http://localhost:8081/api/v1/auth/logout`

**Listar usuarios**
`GET http://localhost:8081/api/v1/usuarios?rol=REPARTIDOR&estado=ACTIVO`

**Crear usuario del personal**
`POST http://localhost:8081/api/v1/usuarios`
```json
{
  "nombre": "Pedro",
  "apellido": "Muñoz",
  "email": "pedro.munoz@bocado.cl",
  "password": "PolarReparto#26",
  "rol": "REPARTIDOR",
  "telefono": "+56 9 5555 6666",
  "rut": "12.345.678-5"
}
```
Roles: `CLIENTE`, `REPARTIDOR`, `VENDEDOR`, `BODEGUERO`, `SOPORTE`.

**Editar usuario**
`PUT http://localhost:8081/api/v1/usuarios/2`
```json
{
  "nombre": "Equipo",
  "apellido": "Ventas Sur",
  "telefono": "+56 9 2222 3333",
  "rut": ""
}
```

**Cambiar estado** (`ACTIVO`, `BLOQUEADO`, `SUSPENDIDO`, `ELIMINADO`)
`PATCH http://localhost:8081/api/v1/usuarios/4/estado`
```json
{ "estado": "SUSPENDIDO" }
```
Con el usuario 1 (admin original) responde 409.

**Cambiar rol**
`PATCH http://localhost:8081/api/v1/usuarios/4/rol`
```json
{ "rol": "VENDEDOR" }
```

**Eliminar usuario** (queda en estado ELIMINADO)
`DELETE http://localhost:8081/api/v1/usuarios/4`

**Permisos por rol**
`GET http://localhost:8081/api/v1/permisos`
`GET http://localhost:8081/api/v1/permisos/secciones`

`PUT http://localhost:8081/api/v1/permisos/rol/SOPORTE`
```json
{ "secciones": ["resumen", "pedidos", "clientes", "marketing"] }
```

**Auditoría de accesos**
`GET http://localhost:8081/api/v1/auditoria?usuarioId=7`

---

## 2. ms-clientes · 8082

**Directorio CRM**
`GET http://localhost:8082/api/v1/clientes?segmento=VIP&q=azul`

**Ficha por id / por usuario de ms-auth**
`GET http://localhost:8082/api/v1/clientes/1`
`GET http://localhost:8082/api/v1/clientes/usuario/7`

**Crear cliente (ej. B2B)**
`POST http://localhost:8082/api/v1/clientes`
```json
{
  "nombre": "Restaurante El Ancla",
  "apellido": "",
  "email": "compras@elancla.cl",
  "telefono": "+56 9 7777 8888",
  "rut": ""
}
```

**Editar perfil**
`PUT http://localhost:8082/api/v1/clientes/1`
```json
{
  "nombre": "Cliente",
  "apellido": "Demo",
  "telefono": "+56 9 4444 4444",
  "rut": "11111111-1"
}
```

**Direcciones**
`GET http://localhost:8082/api/v1/clientes/1/direcciones`

`POST http://localhost:8082/api/v1/clientes/1/direcciones`
```json
{
  "alias": "Departamento",
  "calle": "Camino a Lota s/n",
  "comuna": "Chiguayante",
  "referencia": "Portón verde",
  "principal": false
}
```

`PUT http://localhost:8082/api/v1/clientes/1/direcciones/1` (mismo cuerpo)
`PATCH http://localhost:8082/api/v1/clientes/1/direcciones/2/principal`
`DELETE http://localhost:8082/api/v1/clientes/1/direcciones/2`

**Medios de pago** (solo últimos 4 dígitos y token de la pasarela)
`POST http://localhost:8082/api/v1/clientes/1/medios-pago`
```json
{
  "alias": "Mi Mastercard",
  "marca": "Mastercard",
  "ultimos4": "5454",
  "vence": "11/28",
  "tokenPasarela": "tok_demo_mc_5454"
}
```
`GET http://localhost:8082/api/v1/clientes/1/medios-pago`
`DELETE http://localhost:8082/api/v1/clientes/1/medios-pago/1`

**Preferencias**
`PUT http://localhost:8082/api/v1/clientes/1/preferencias`
```json
{
  "alergias": "Mariscos",
  "notas": "Prefiero entregas en la mañana."
}
```

**Notas de soporte / ventas** (`INCIDENCIA`, `DEVOLUCION`, `RECLAMO`, `CONSULTA`)
`POST http://localhost:8082/api/v1/clientes/2/notas`
```json
{
  "tipo": "RECLAMO",
  "texto": "Reporta producto con escarcha. Se ofreció cambio.",
  "autorId": 4
}
```

**Registrar compra** (lo usa ms-pedidos al entregar)
`POST http://localhost:8082/api/v1/clientes/3/compras`
```json
{ "monto": 52000, "pedidoId": 4 }
```

**Recalcular segmentos**
`POST http://localhost:8082/api/v1/clientes/segmentar`

---

## 3. ms-catalogo · 8083

**Buscar productos** (orden: `nombre`, `precio_asc`, `precio_desc`, `valoracion`)
`GET http://localhost:8083/api/v1/productos?q=salmon&categoriaId=1&orden=precio_asc`
`GET http://localhost:8083/api/v1/productos/1`
`GET http://localhost:8083/api/v1/productos/sku/SAL-001`
`GET http://localhost:8083/api/v1/productos/por-ids?ids=1,2,3`

**Crear producto**
`POST http://localhost:8083/api/v1/productos`
```json
{
  "sku": "COR-007",
  "nombre": "Corvina en Filete",
  "descripcion": "Filete sin piel, congelado individualmente.",
  "categoriaId": 1,
  "origen": "Arica",
  "alergenos": "Pescado",
  "imagen": "img/prod-corvina.svg",
  "activo": true,
  "precioKg": 13990,
  "costoKg": 9100,
  "atributos": {
    "Marca": "BOCADO Polar",
    "Formato": "Bolsa 1 kg",
    "Calibre": "Mediano"
  }
}
```
`PUT http://localhost:8083/api/v1/productos/7` (mismo cuerpo)

**Activar / desactivar**
`PATCH http://localhost:8083/api/v1/productos/7/activo`
```json
{ "activo": false }
```

**Eliminar** (409 si aún tiene kg en bodega)
`DELETE http://localhost:8083/api/v1/productos/7`

**Ajuste masivo de precios** (`ids` vacío = todos)
`PATCH http://localhost:8083/api/v1/productos/precios`
```json
{ "ids": [1, 2], "porcentaje": 5 }
```

**Valorar producto** (0,5 a 5 en pasos de 0,5)
`POST http://localhost:8083/api/v1/productos/1/valoraciones`
```json
{ "clienteId": 1, "estrellas": 4.5 }
```
`GET http://localhost:8083/api/v1/productos/1/valoraciones?clienteId=1`

**Categorías**
`GET http://localhost:8083/api/v1/categorias`

`POST http://localhost:8083/api/v1/categorias`
```json
{ "nombre": "Ahumados", "descripcion": "Pescados ahumados en frío." }
```
`PUT http://localhost:8083/api/v1/categorias/4` (mismo cuerpo)
`DELETE http://localhost:8083/api/v1/categorias/4`

**Atributos**
`GET http://localhost:8083/api/v1/atributos`

`POST http://localhost:8083/api/v1/atributos`
```json
{ "nombre": "Corte", "valores": ["Filete", "Trozo", "Entero"] }
```

`POST http://localhost:8083/api/v1/atributos/1/valores`
```json
{ "valor": "Extra grande" }
```

---

## 4. ms-inventario · 8084

**Lotes**
`GET http://localhost:8084/api/v1/lotes?productoId=1&incluirAgotados=true`
`GET http://localhost:8084/api/v1/lotes/1`
`GET http://localhost:8084/api/v1/lotes/fefo/1`

**Registrar lote** (el producto debe existir en ms-catalogo)
`POST http://localhost:8084/api/v1/lotes`
```json
{
  "productoId": 2,
  "codigoLote": "REI-2610-C",
  "stockKg": 40,
  "vencimiento": "2026-12-15",
  "tempAlmacenaje": -18,
  "usuarioId": 3
}
```

**Editar lote**
`PUT http://localhost:8084/api/v1/lotes/8`
```json
{
  "codigoLote": "REI-2610-C",
  "vencimiento": "2026-12-20",
  "tempAlmacenaje": -19
}
```

**Ajustar stock** (`ENTRADA` suma, `SALIDA` y `MERMA` restan, `AJUSTE` acepta + o -)
`POST http://localhost:8084/api/v1/lotes/1/ajustes`
```json
{
  "tipo": "MERMA",
  "kg": 2.5,
  "motivo": "Bolsa rota en bodega",
  "usuarioId": 3
}
```

**Ajuste masivo**
`POST http://localhost:8084/api/v1/lotes/ajustes/masivo`
```json
{
  "usuarioId": 3,
  "ajustes": [
    { "loteId": 1, "tipo": "ENTRADA", "kg": 10, "motivo": "Recepción proveedor" },
    { "loteId": 3, "tipo": "AJUSTE", "kg": -1.5, "motivo": "Conteo físico" }
  ]
}
```

**Bloquear / desbloquear lote**
`PATCH http://localhost:8084/api/v1/lotes/7/bloqueo`
```json
{ "motivo": "Temperatura sobre -15 °C" }
```
`PATCH http://localhost:8084/api/v1/lotes/7/desbloqueo`

**Stock y movimientos**
`GET http://localhost:8084/api/v1/stock`
`GET http://localhost:8084/api/v1/stock/6`
`GET http://localhost:8084/api/v1/movimientos?productoId=1`

**Reservas** (las usa ms-pedidos)
`POST http://localhost:8084/api/v1/reservas`
```json
{
  "pedidoId": 999,
  "items": [
    { "productoId": 1, "kg": 3.5 },
    { "productoId": 5, "kg": 2 }
  ]
}
```
`GET http://localhost:8084/api/v1/reservas/pedido/999`
`PATCH http://localhost:8084/api/v1/reservas/pedido/999/confirmar`
`PATCH http://localhost:8084/api/v1/reservas/pedido/999/liberar`

**Alertas de stock crítico**
`GET http://localhost:8084/api/v1/alertas-stock?atendida=false`
`POST http://localhost:8084/api/v1/alertas-stock/detectar`
`PATCH http://localhost:8084/api/v1/alertas-stock/1/resolver`

---

## 5. ms-pedidos · 8085

**Carrito de visitante** (body vacío `{}`) o de cliente
`POST http://localhost:8085/api/v1/carritos`
```json
{ "clienteId": 1 }
```
`GET http://localhost:8085/api/v1/carritos/1`
`GET http://localhost:8085/api/v1/carritos/cliente/1`

**Agregar producto** (pasos de 0,5 kg; máximo 12,5 kg por producto)
`POST http://localhost:8085/api/v1/carritos/1/items`
```json
{ "productoId": 1, "kg": 2.5 }
```

**Cambiar cantidad** (0 quita el producto)
`PUT http://localhost:8085/api/v1/carritos/1/items/1`
```json
{ "kg": 3 }
```
`DELETE http://localhost:8085/api/v1/carritos/1/items/1`
`DELETE http://localhost:8085/api/v1/carritos/1/items`

**Pasar carrito de visitante al cliente**
`PATCH http://localhost:8085/api/v1/carritos/2/cliente`
```json
{ "clienteId": 1 }
```

**Volver a pedir**
`POST http://localhost:8085/api/v1/carritos/1/volver-a-pedir/1`

**Cotizar** (no guarda nada)
`POST http://localhost:8085/api/v1/pedidos/cotizar`
```json
{
  "carritoId": 1,
  "comuna": "Concepción",
  "clienteId": 1,
  "codigoCupon": "BIENVENIDA10"
}
```

**Confirmar pedido (checkout)** — el carrito debe tener productos
`POST http://localhost:8085/api/v1/pedidos/checkout`
```json
{
  "carritoId": 1,
  "clienteId": 1,
  "nombreCliente": "Cliente Demo",
  "email": "cliente@bocado.cl",
  "telefono": "+56 9 4444 4444",
  "direccion": "O'Higgins 1234",
  "comuna": "Concepción",
  "referencia": "Tocar timbre 3B",
  "comentario": "Dejar con conserjería si no estoy",
  "fechaEntrega": "2026-10-25",
  "horario": "09:00 - 12:00",
  "metodoPago": "WEBPAY",
  "codigoCupon": "BIENVENIDA10"
}
```
Horarios: `09:00 - 12:00`, `12:00 - 15:00`, `15:00 - 18:00`. Métodos: `WEBPAY`, `TARJETA`, `TRANSFERENCIA`, `CONTRA_ENTREGA`. Comunas: Concepción, Chiguayante, Tomé, Penco.

**Consultar pedidos**
`GET http://localhost:8085/api/v1/pedidos?estado=EN_PREPARACION`
`GET http://localhost:8085/api/v1/pedidos?comunas=Tomé,Penco&desde=2026-10-01&hasta=2026-10-31`
`GET http://localhost:8085/api/v1/pedidos/1`
`GET http://localhost:8085/api/v1/pedidos/codigo/BP-100001`
`GET http://localhost:8085/api/v1/pedidos/cliente/1`
`GET http://localhost:8085/api/v1/pedidos/1/seguimiento`
`GET http://localhost:8085/api/v1/pedidos/1/transiciones`
`GET http://localhost:8085/api/v1/pedidos/1/etiqueta`

**Cambiar estado**
`PATCH http://localhost:8085/api/v1/pedidos/1/estado`
```json
{
  "estado": "ENVIADO",
  "nota": "Salió en ruta R-001",
  "usuarioId": 2
}
```
Estados: `PENDIENTE_PAGO`, `PAGADO`, `EN_PREPARACION`, `ENVIADO`, `ENTREGADO`, `CANCELADO`, `REEMBOLSADO`.

**Retroceder al estado anterior**
`PATCH http://localhost:8085/api/v1/pedidos/1/retroceder`
```json
{ "motivo": "Se marcó por error", "usuarioId": 2 }
```

**Cancelar**
`PATCH http://localhost:8085/api/v1/pedidos/5/cancelar`
```json
{ "motivo": "El cliente se arrepintió", "usuarioId": 4 }
```

**Evento de pago** (lo usa ms-pagos: `APROBADO`, `RECHAZADO`, `CONTRA_ENTREGA`, `REEMBOLSADO`)
`PATCH http://localhost:8085/api/v1/pedidos/5/pago`
```json
{ "evento": "APROBADO", "pagoId": 1, "detalle": "WEBPAY" }
```

**Notas internas**
`POST http://localhost:8085/api/v1/pedidos/1/notas`
```json
{ "texto": "Cliente pidió llamar antes de llegar", "autorId": 4 }
```
`GET http://localhost:8085/api/v1/pedidos/1/notas`

---

## 6. ms-pagos · 8086

**Iniciar pago** (el pedido debe estar `PENDIENTE_PAGO`)
`POST http://localhost:8086/api/v1/pagos`
```json
{
  "pedidoId": 5,
  "metodo": "WEBPAY",
  "idempotencyKey": "pago-pedido-5-intento-1"
}
```

Contra entrega:
```json
{
  "pedidoId": 5,
  "metodo": "CONTRA_ENTREGA",
  "tipoCobro": "EFECTIVO",
  "pagaCon": 30000
}
```

**Simular respuesta de Webpay**
`POST http://localhost:8086/api/v1/pagos/webhook`
```json
{
  "pagoId": 7,
  "aprobado": true,
  "codigoAutorizacion": "123456"
}
```
Rechazo: `{ "pagoId": 7, "aprobado": false, "motivo": "Fondos insuficientes" }`

**Confirmar transferencia**
`PATCH http://localhost:8086/api/v1/pagos/7/confirmar-transferencia`

**Cobro al entregar** (`EFECTIVO`, `POS_MOVIL`, `TRANSFERENCIA`)
`PATCH http://localhost:8086/api/v1/pagos/2/cobro-entrega`
```json
{
  "repartidorId": 5,
  "tipo": "EFECTIVO",
  "pagaCon": 200000
}
```

**Consultas**
`GET http://localhost:8086/api/v1/pagos?estado=APROBADO&metodo=WEBPAY`
`GET http://localhost:8086/api/v1/pagos/1`
`GET http://localhost:8086/api/v1/pagos/pedido/2`
`GET http://localhost:8086/api/v1/pagos/2/cobro`
`GET http://localhost:8086/api/v1/pagos/cobros?repartidorId=6&fecha=2026-10-22`

**Boletas**
`GET http://localhost:8086/api/v1/boletas/1`
`GET http://localhost:8086/api/v1/boletas/pedido/1`
`GET http://localhost:8086/api/v1/boletas/1/html` (se ve mejor en el navegador)

**Reembolsos**
`POST http://localhost:8086/api/v1/pagos/1/reembolsos`
```json
{
  "motivo": "Producto llegó descongelado",
  "monto": 48455,
  "solicitadoPorId": 4
}
```
`GET http://localhost:8086/api/v1/reembolsos?estado=SOLICITADO`

`PATCH http://localhost:8086/api/v1/reembolsos/1/aprobar`
```json
{ "usuarioId": 1 }
```

`PATCH http://localhost:8086/api/v1/reembolsos/1/rechazar`
```json
{ "motivo": "Fuera de plazo", "usuarioId": 1 }
```

---

## 7. ms-marketing · 8087

**Cupones**
`GET http://localhost:8087/api/v1/cupones`
`GET http://localhost:8087/api/v1/cupones/codigo/POLAR5000`

`POST http://localhost:8087/api/v1/cupones`
```json
{
  "codigo": "PRIMAVERA15",
  "tipo": "PORCENTAJE",
  "valor": 15,
  "compraMinima": 20000,
  "limiteUsos": 50,
  "limitePorCliente": 1,
  "activo": true,
  "vence": "2026-12-31"
}
```
Tipos: `PORCENTAJE` o `MONTO_FIJO`. `PUT http://localhost:8087/api/v1/cupones/4` usa el mismo cuerpo.

`PATCH http://localhost:8087/api/v1/cupones/4/activo`
```json
{ "activo": false }
```
`DELETE http://localhost:8087/api/v1/cupones/4`

**Validar cupón**
`POST http://localhost:8087/api/v1/cupones/validar`
```json
{
  "codigo": "BIENVENIDA10",
  "clienteId": 1,
  "subtotal": 45000
}
```

**Registrar uso** (lo usa ms-pedidos)
`POST http://localhost:8087/api/v1/cupones/usos`
```json
{ "codigo": "POLAR5000", "pedidoId": 999, "clienteId": 1 }
```
`GET http://localhost:8087/api/v1/cupones/2/usos`

**Canales**
`GET http://localhost:8087/api/v1/canales`

`POST http://localhost:8087/api/v1/canales`
```json
{ "canal": "TikTok", "visitas": 0, "pedidos": 0, "ingresos": 0 }
```

`POST http://localhost:8087/api/v1/canales/visita`
```json
{ "canal": "Instagram" }
```

`POST http://localhost:8087/api/v1/canales/conversion`
```json
{ "canal": "Instagram", "monto": 38970 }
```

**Carritos abandonados**
`GET http://localhost:8087/api/v1/carritos-abandonados?recuperado=false`
`POST http://localhost:8087/api/v1/carritos-abandonados/detectar?horas=2`

`POST http://localhost:8087/api/v1/carritos-abandonados`
```json
{
  "cliente": "Juan Pérez",
  "email": "juan.perez@correo.cl",
  "resumen": "Merluza Austral · 3 kg",
  "total": 20970
}
```

`POST http://localhost:8087/api/v1/carritos-abandonados/1/rescate`
```json
{ "codigoCupon": "POLAR5000" }
```

`PATCH http://localhost:8087/api/v1/carritos-abandonados/1/recuperado`
```json
{ "pedidoId": 10 }
```

---

## 8. ms-rutas · 8088

**Rutas**
`GET http://localhost:8088/api/v1/rutas?fecha=2026-10-22`
`GET http://localhost:8088/api/v1/rutas/1`
`GET http://localhost:8088/api/v1/rutas/repartidor/5/hoy`

**Crear ruta**
`POST http://localhost:8088/api/v1/rutas`
```json
{
  "nombre": "Concepción centro",
  "fecha": "2026-10-23",
  "comunas": ["Concepción"],
  "horaSalida": "08:30",
  "repartidorId": 5,
  "repartidorNombre": "Repartidor Demo",
  "vehiculoId": 1,
  "notas": "Salida desde bodega central."
}
```
`PUT http://localhost:8088/api/v1/rutas/3` (mismo cuerpo, solo si está PLANIFICADA)
`DELETE http://localhost:8088/api/v1/rutas/3`

**Asignar repartidor y vehículo**
`PATCH http://localhost:8088/api/v1/rutas/3/asignar`
```json
{ "repartidorId": 6, "repartidorNombre": "Rodrigo Norte", "vehiculoId": 2 }
```

**Asignar un pedido a un repartidor (solo ADMIN por el gateway)**
`POST http://localhost:8088/api/v1/rutas/asignar-pedido`
```json
{ "pedidoId": 3, "repartidorId": 5, "repartidorNombre": "Repartidor Demo", "usuarioId": 1 }
```
Agrega el pedido como parada en la ruta de ese repartidor para la fecha de entrega (si no tiene, crea una con un camión refrigerado libre), lo quita de la ruta donde estaba y guarda el repartidor en el pedido. Solo pedidos Pagados, En preparación, Enviados o contra entrega. Ver los pedidos de un repartidor: `GET http://localhost:8085/api/v1/pedidos?repartidorId=5`.

**Cambiar estado de la ruta**
`PATCH http://localhost:8088/api/v1/rutas/2/iniciar`
`PATCH http://localhost:8088/api/v1/rutas/2/finalizar`
`PATCH http://localhost:8088/api/v1/rutas/2/cancelar`
`PATCH http://localhost:8088/api/v1/rutas/2/optimizar`

**Paradas**
`GET http://localhost:8088/api/v1/rutas/1/paradas`
`POST http://localhost:8088/api/v1/rutas/3/paradas/generar` (trae los pedidos listos de sus comunas y fecha)

`POST http://localhost:8088/api/v1/rutas/3/paradas`
```json
{
  "pedidoId": 4,
  "codigoPedido": "BP-100004",
  "cliente": "Pedro Araya",
  "direccion": "Av. Collao 1550",
  "comuna": "Concepción",
  "referencia": "",
  "horario": "09:00 - 12:00",
  "cobrarAlEntregar": false,
  "montoACobrar": null
}
```
`DELETE http://localhost:8088/api/v1/rutas/3/paradas/6`
`PATCH http://localhost:8088/api/v1/paradas/1/entregada`

`PATCH http://localhost:8088/api/v1/paradas/1/fallida`
```json
{ "motivo": "Cliente ausente" }
```

**Vehículos**
`GET http://localhost:8088/api/v1/vehiculos`

`POST http://localhost:8088/api/v1/vehiculos`
```json
{
  "tipo": "Camioneta refrigerada",
  "patente": "RTWZ-55",
  "refrigerado": true,
  "activo": true
}
```
`PUT http://localhost:8088/api/v1/vehiculos/3` (mismo cuerpo)
`DELETE http://localhost:8088/api/v1/vehiculos/3`

---

## 9. ms-reparto · 8089

> `repartidorId` es el id de usuario de ms-auth (5 y 6 en los datos de ejemplo).

**Repartidores**
`GET http://localhost:8089/api/v1/repartidores?disponibilidad=DISPONIBLE`
`GET http://localhost:8089/api/v1/repartidores/5`

`POST http://localhost:8089/api/v1/repartidores`
```json
{
  "usuarioId": 8,
  "nombre": "Pedro Muñoz",
  "email": "pedro.munoz@bocado.cl",
  "rut": "12345678-5"
}
```

`PATCH http://localhost:8089/api/v1/repartidores/6/disponibilidad`
```json
{ "disponibilidad": "DISPONIBLE" }
```
Valores: `DISPONIBLE`, `EN_RUTA`, `FUERA_SERVICIO`.

`PATCH http://localhost:8089/api/v1/repartidores/5/preferencias`
```json
{ "sonido": true, "vibracion": false }
```

**Iniciar turno** (registra la temperatura de la caja e inicia la cuenta de 90 min)
`POST http://localhost:8089/api/v1/turnos/iniciar`
```json
{ "repartidorId": 6, "tempCajaInicial": -18 }
```
`GET http://localhost:8089/api/v1/turnos/activo/5`

**Registrar entrega** (necesita foto o firma; si es contra entrega, también el cobro)
`POST http://localhost:8089/api/v1/entregas`
```json
{
  "repartidorId": 5,
  "turnoId": 1,
  "pedidoId": 2,
  "paradaId": 2,
  "recibidoPor": "Encargado de cocina",
  "tempEntrega": -17,
  "fotoUrl": "https://ejemplo.cl/evidencia/bp-100002.jpg",
  "firmaUrl": "",
  "comentario": "Recibido en portón azul",
  "cobro": {
    "tipo": "EFECTIVO",
    "pagaCon": 200000
  }
}
```
Si el pedido ya está pagado, omite `"cobro"`. El pedido debe estar `ENVIADO`.

**Entrega fallida** (`CLIENTE_AUSENTE`, `DIRECCION_INCORRECTA`, `RECHAZO_PRODUCTO`, `OTRO`)
`POST http://localhost:8089/api/v1/entregas/fallida`
```json
{
  "repartidorId": 5,
  "turnoId": 1,
  "pedidoId": 1,
  "paradaId": 1,
  "tipo": "CLIENTE_AUSENTE",
  "nota": "Nadie responde el timbre"
}
```

**Consultas de entregas**
`GET http://localhost:8089/api/v1/entregas?repartidorId=6&fecha=2026-10-22`
`GET http://localhost:8089/api/v1/entregas/1`
`GET http://localhost:8089/api/v1/entregas/pedido/6`

**Calificar al repartidor** (1 a 5)
`POST http://localhost:8089/api/v1/entregas/1/calificacion`
```json
{ "clienteId": 4, "estrellas": 5 }
```

**Reportar incidencia**
`POST http://localhost:8089/api/v1/incidencias`
```json
{
  "tipo": "OTRO",
  "nota": "Neumático pinchado, retraso de 20 min",
  "entregaId": null,
  "pedidoId": 3,
  "reportadaPorId": 5
}
```
`GET http://localhost:8089/api/v1/incidencias?repartidorId=5`

**Cerrar turno y rendir caja**
`POST http://localhost:8089/api/v1/turnos/1/cerrar`
```json
{ "efectivoRendido": 183370 }
```
`GET http://localhost:8089/api/v1/cierres?repartidorId=5`

---

## 10. ms-frio · 8090

**Registrar temperatura** (`LOTE` → id del lote, `SALIDA_RUTA` → id del turno, `ENTREGA` → id del pedido)
`POST http://localhost:8090/api/v1/temperaturas`
```json
{
  "origen": "LOTE",
  "referenciaId": 3,
  "grados": -14.2,
  "registradoPorId": 3
}
```
Sobre -15 °C crea una advertencia. Si un lote sigue sobre el umbral 30 minutos o más, la alerta pasa a crítica y el lote se bloquea.

**Consultas**
`GET http://localhost:8090/api/v1/temperaturas?origen=LOTE&referenciaId=7`
`GET http://localhost:8090/api/v1/temperaturas/lote/7`
`GET http://localhost:8090/api/v1/alertas-frio?atendida=false`

**Atender alerta**
`PATCH http://localhost:8090/api/v1/alertas-frio/1/atender`
```json
{ "usuarioId": 3 }
```

**Cuenta regresiva de 90 minutos**
`POST http://localhost:8090/api/v1/cuentas-regresivas`
```json
{ "turnoId": 2 }
```
`GET http://localhost:8090/api/v1/cuentas-regresivas/turno/1`

---

## 11. ms-config · 8091

**Configuración global**
`GET http://localhost:8091/api/v1/configuracion`
`GET http://localhost:8091/api/v1/configuracion/iva`

`PUT http://localhost:8091/api/v1/configuracion`
```json
{
  "pais": "Chile",
  "moneda": "CLP",
  "preciosConIva": true,
  "iva": 19
}
```

**Zonas de envío**
`GET http://localhost:8091/api/v1/zonas?activas=true`
`GET http://localhost:8091/api/v1/zonas/tarifa?comuna=Tomé`

`POST http://localhost:8091/api/v1/zonas`
```json
{ "comuna": "Talcahuano", "tarifa": 3990, "activa": true }
```
`PUT http://localhost:8091/api/v1/zonas/5` (mismo cuerpo)

`PATCH http://localhost:8091/api/v1/zonas/5/activa`
```json
{ "activa": false }
```
`DELETE http://localhost:8091/api/v1/zonas/5`

**Empresas de logística**
`GET http://localhost:8091/api/v1/empresas-logistica`

`POST http://localhost:8091/api/v1/empresas-logistica`
```json
{ "nombre": "Chilexpress Frío", "tipo": "Courier con cadena de frío", "activa": false }
```

`PATCH http://localhost:8091/api/v1/empresas-logistica/2/activa`
```json
{ "activa": true }
```

**Pasarelas de pago**
`GET http://localhost:8091/api/v1/pasarelas`
`GET http://localhost:8091/api/v1/pasarelas/disponible?nombre=Webpay`
`PATCH http://localhost:8091/api/v1/pasarelas/2/conectar`
`PATCH http://localhost:8091/api/v1/pasarelas/2/desconectar`

**Reglas de negocio**
`GET http://localhost:8091/api/v1/reglas`
`GET http://localhost:8091/api/v1/reglas/STOCK_CRITICO`

`PUT http://localhost:8091/api/v1/reglas/STOCK_CRITICO`
```json
{ "valor": 40, "descripcion": "Bajo estos kg disponibles el producto queda en stock crítico", "unidad": "kg" }
```

`POST http://localhost:8091/api/v1/reglas`
```json
{
  "codigo": "ENVIO_GRATIS_DESDE",
  "descripcion": "Monto desde el cual el envío es gratis",
  "valor": 80000,
  "unidad": "CLP"
}
```
`DELETE http://localhost:8091/api/v1/reglas/ENVIO_GRATIS_DESDE`

---

## 12. ms-reportes · 8092

Períodos: `HOY`, `SIETE_DIAS`, `MES_ACTUAL`, `TREINTA_DIAS`. Necesita ms-pedidos encendido.

`GET http://localhost:8092/api/v1/kpis?periodo=SIETE_DIAS`
`GET http://localhost:8092/api/v1/kpis/top-productos?periodo=TREINTA_DIAS&limite=5`
`GET http://localhost:8092/api/v1/kpis/ventas-diarias?periodo=TREINTA_DIAS`
`GET http://localhost:8092/api/v1/kpis/alertas-operativas`
`POST http://localhost:8092/api/v1/kpis/snapshot?periodo=MES_ACTUAL`
`GET http://localhost:8092/api/v1/kpis/historial`

**Registrar visita** (body opcional)
`POST http://localhost:8092/api/v1/visitas`
```json
{ "canal": "Instagram" }
```
`GET http://localhost:8092/api/v1/visitas?desde=2026-10-01&hasta=2026-10-22`

---

## 13. ms-notificaciones · 8093

**Enviar un evento** (los correos se simulan y aparecen en la consola del servicio)
`POST http://localhost:8093/api/v1/notificaciones/eventos`
```json
{
  "eventoId": "prueba-001",
  "motivo": "PEDIDO_CONFIRMADO",
  "canal": "CORREO",
  "destinatario": "cliente@bocado.cl",
  "variables": {
    "cliente": "Cliente Demo",
    "codigo": "BP-100001",
    "total": 48455,
    "fechaEntrega": "2026-10-25",
    "horario": "09:00 - 12:00"
  }
}
```
Motivos: `PEDIDO_CONFIRMADO`, `CAMBIO_ESTADO`, `ALERTA_FRIO`, `ALERTA_STOCK`, `INCIDENCIA_REPARTO`, `RESCATE_CARRITO`, `GENERAL`. Canales: `CORREO`, `PANEL_ADMIN` (usar destinatario `"admin"`), `SMS`.

**Consultas**
`GET http://localhost:8093/api/v1/notificaciones?estado=ENVIADA&motivo=CAMBIO_ESTADO`
`GET http://localhost:8093/api/v1/notificaciones/panel?soloNoLeidas=true`
`GET http://localhost:8093/api/v1/notificaciones/1`
`GET http://localhost:8093/api/v1/notificaciones/1/historial`
`PATCH http://localhost:8093/api/v1/notificaciones/1/leida`
`POST http://localhost:8093/api/v1/notificaciones/1/reenviar`
`POST http://localhost:8093/api/v1/notificaciones/reintentar-fallidas`

**Plantillas** (variables con doble llave)
`GET http://localhost:8093/api/v1/plantillas`

`PUT http://localhost:8093/api/v1/plantillas/1`
```json
{
  "motivo": "PEDIDO_CONFIRMADO",
  "asunto": "¡Gracias {{cliente}}! Recibimos tu pedido {{codigo}}",
  "cuerpo": "Hola {{cliente}}, tu pedido {{codigo}} por ${{total}} llegará el {{fechaEntrega}} entre {{horario}}.",
  "activa": true
}
```

---

## Prueba completa de una compra (en orden)

| # | Consulta | Qué verificar |
|---|---|---|
| 1 | `POST :8085/api/v1/carritos` con `{ "clienteId": 1 }` | Anota el `id` del carrito |
| 2 | `POST :8085/api/v1/carritos/{id}/items` con `{ "productoId": 1, "kg": 2 }` | `subtotal` = 25980 |
| 3 | `POST :8085/api/v1/pedidos/checkout` (cuerpo de la sección 5) | Estado `PENDIENTE_PAGO`; anota el `id` |
| 4 | `GET :8084/api/v1/reservas/pedido/{id}` | Reserva en estado `RESERVADA` |
| 5 | `POST :8086/api/v1/pagos` con `{ "pedidoId": {id}, "metodo": "WEBPAY" }` | Anota el `id` del pago |
| 6 | `POST :8086/api/v1/pagos/webhook` con `{ "pagoId": {pago}, "aprobado": true }` | Pago `APROBADO` |
| 7 | `GET :8085/api/v1/pedidos/{id}/seguimiento` | Pedido `PAGADO` |
| 8 | `GET :8084/api/v1/reservas/pedido/{id}` | Reserva `CONFIRMADA` y stock descontado |
| 9 | `GET :8086/api/v1/boletas/pedido/{id}` | Boleta con neto e IVA |
| 10 | `GET :8093/api/v1/notificaciones?destinatario=cliente@bocado.cl` | Correos de confirmación y cambio de estado |

## Respuestas de error comunes

| Código | Significado | Ejemplo |
|---|---|---|
| 400 | Datos inválidos | Falta un campo, formato de correo o kg incorrecto |
| 401 | Sin sesión (gateway) | Falta el Bearer Token o expiró |
| 403 | Sin permiso | Usuario no ADMIN en `/usuarios` |
| 404 | No existe | Id de pedido o producto inexistente |
| 409 | Regla de negocio | Sin stock, cupón usado, cambio de estado no permitido |
| 503 | Otro servicio apagado | ms-pedidos llama a ms-inventario y no está levantado |
