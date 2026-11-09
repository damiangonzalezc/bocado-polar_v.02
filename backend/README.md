# BOCADO Polar · Backend (Spring Boot + microservicios)

Backend de la tienda de productos marinos congelados BOCADO Polar. Son **13 microservicios** Spring Boot (uno por cada diagrama de clases) más un **API Gateway**. Cada microservicio tiene su **propia base de datos MySQL** (XAMPP). No usa Docker.

El front (React + TypeScript + Vite + SWC) se conecta a **una sola URL**: `http://localhost:8080/api/v1`.

> La lista completa de rutas, quién puede usarlas y los campos de cada cuerpo JSON está en **[API.md](API.md)**.

---

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Java (JDK) | 17 o superior (probado para 17, 21 y 25) |
| XAMPP | MySQL encendido en el puerto **3306**, usuario `root` **sin contraseña** |
| Maven | No hace falta instalarlo: se usa `mvnw` (incluido) |

Versiones del proyecto (igual que el ejemplo `unitarias` del profesor): Spring Boot **3.5.0**, Lombok, Spring Data JPA, conector MySQL, springdoc (Swagger). El gateway usa Spring Cloud Gateway 2025.0.0.

## 2. Cómo levantarlo

1. Abre **XAMPP** y presiona **Start** en MySQL.
2. Desde la carpeta del proyecto ejecuta **`iniciar-todo.bat`** (Windows) o `./iniciar-todo.sh` (macOS/Linux).
   El script compila todo y abre una ventana por servicio. La primera vez Maven descarga dependencias (tarda unos minutos).
3. Listo: el front usa `http://localhost:8080/api/v1`.

**No hay que crear las bases a mano**: cada servicio crea la suya (`createDatabaseIfNotExist=true`) y Hibernate crea las tablas. Si prefieres crearlas tú, está `bd/crear_bases.sql` para phpMyAdmin.

**Desde VS Code** (con *Extension Pack for Java* y *Spring Boot Dashboard*): abre la carpeta raíz y ejecuta cada `XxxApplication.java`. Levanta primero **ms-config**, luego los demás y al final **api-gateway**.

Para compilar y correr las pruebas de todo: `mvnw clean install` (o `mvnw test`).

## 3. Microservicios, puertos y bases

| Servicio | Puerto | Base de datos | Rutas que atiende (`/api/v1/...`) |
|---|---|---|---|
| **api-gateway** | **8080** | — | Todas (reenvía al servicio que corresponde) |
| ms-auth | 8081 | bocado_auth | `auth`, `usuarios`, `permisos`, `auditoria` |
| ms-clientes | 8082 | bocado_clientes | `clientes` |
| ms-catalogo | 8083 | bocado_catalogo | `productos`, `categorias`, `atributos` |
| ms-inventario | 8084 | bocado_inventario | `lotes`, `stock`, `movimientos`, `reservas`, `alertas-stock` |
| ms-pedidos | 8085 | bocado_pedidos | `carritos`, `pedidos` |
| ms-pagos | 8086 | bocado_pagos | `pagos`, `boletas`, `reembolsos` |
| ms-marketing | 8087 | bocado_marketing | `cupones`, `canales`, `carritos-abandonados` |
| ms-rutas | 8088 | bocado_rutas | `rutas`, `paradas`, `vehiculos` |
| ms-reparto | 8089 | bocado_reparto | `repartidores`, `turnos`, `entregas`, `incidencias`, `cierres` |
| ms-frio | 8090 | bocado_frio | `temperaturas`, `alertas-frio`, `cuentas-regresivas` |
| ms-config | 8091 | bocado_config | `configuracion`, `zonas`, `empresas-logistica`, `pasarelas`, `reglas` |
| ms-reportes | 8092 | bocado_reportes | `kpis`, `visitas` |
| ms-notificaciones | 8093 | bocado_notificaciones | `notificaciones`, `plantillas` |

Swagger de cada servicio: `http://localhost:PUERTO/doc/swagger-ui.html` (por ejemplo `http://localhost:8085/doc/swagger-ui.html`).

## 4. Estructura de cada microservicio

Igual que el proyecto de ejemplo, con algunas carpetas extra:

```
ms-pedidos/src/main/java/cl/bocadopolar/pedidos/
├── controller/   rutas REST (@RestController)
├── service/      reglas de negocio
├── repository/   acceso a MySQL (JpaRepository)
├── model/        entidades JPA y enums (según el diagrama de clases)
├── dto/          cuerpos de entrada/salida con validaciones (@NotBlank, @Size...)
├── client/       llamadas REST a otros microservicios (RestClient)
├── config/       CORS, Swagger y datos de ejemplo (DataLoader)
└── exception/    errores con su código HTTP (404, 400, 409...)
```

**Cómo se comunican:** por REST síncrono (`RestClient`). Las llamadas obligatorias (ej.: reservar stock al confirmar un pedido) cortan la operación si fallan; las informativas (correos, estadísticas) se intentan y, si fallan, solo quedan en el log. Las URL de los demás servicios están en el `application.properties` de cada uno (`bocado.servicios.*`).

**Reglas de negocio** (en ms-config, editables desde el panel): `MAX_KG_FORMATO` 12,5 kg · `TEMP_MAX_ALERTA` -15 °C · `MIN_SOSTENIDO_CRITICO` 30 min · `STOCK_CRITICO` 35 kg · `RUTA_MAX_MIN` 90 min · `RESERVA_MIN` 30 min · `DIAS_MAX_ENTREGA` 14 · segmentos VIP/Frecuente.

## 5. Datos de ejemplo y cuentas de prueba

Al iniciar con las tablas vacías, cada servicio carga los datos del prototipo (productos, lotes, ~70 pedidos de 40 días para los KPI, rutas de hoy, cupones, etc.). Se desactiva con `bocado.semilla.habilitada=false`.

| Rol | Correo | Contraseña | id usuario |
|---|---|---|---|
| Administrador (protegido) | admin@bocado.cl | PolarAdmin#26 | 1 |
| Vendedor | ventas@bocado.cl | PolarVentas#26 | 2 |
| Bodeguero | bodega@bocado.cl | PolarBodega#26 | 3 |
| Soporte | soporte@bocado.cl | PolarSoporte#26 | 4 |
| Repartidor (ruta R-001) | reparto@bocado.cl | PolarReparto#26 | 5 |
| Repartidor (ruta R-002) | reparto2@bocado.cl | PolarReparto#26 | 6 |
| Cliente (clienteId 1) | cliente@bocado.cl | PolarCliente#26 | 7 |

Cupones: `BIENVENIDA10` (10 %), `POLAR5000` ($5.000). `VERANO20` está vencido.

**Identificadores entre servicios (importante para el front):**
- `usuarioId` = id de ms-auth. Viene en el login (`usuario.id`).
- `repartidorId` = el **mismo** id de usuario del repartidor (5 y 6).
- `clienteId` = id de la ficha en ms-clientes. Se obtiene después del login con `GET /api/v1/clientes/usuario/{usuarioId}` (el Cliente Demo es el 1).

> Para volver a los datos de ejemplo, borra las 13 bases en phpMyAdmin y reinicia los servicios **todos juntos** (los ids de ejemplo se cruzan entre servicios).

## 6. Seguridad (JWT)

1. `POST /api/v1/auth/login` → devuelve `token`, `usuario` y `permisos` (secciones del panel que puede ver).
2. El front envía `Authorization: Bearer <token>` en cada llamada.
3. El gateway valida el token. Rutas **públicas**: login, registro, catálogo (GET), carrito, zonas/tarifas, validar cupón, visitas y webhook de pago. Rutas **solo ADMIN**: usuarios, permisos, auditoría y cambios de configuración.
4. Las contraseñas se guardan con BCrypt. Una cuenta Bloqueada/Suspendida/Eliminada no puede iniciar sesión, y el admin original no se puede bloquear ni cambiar de rol.

Para probar en Postman sin token: en `api-gateway/src/main/resources/application.properties` pon `bocado.seguridad.habilitada=false`.

## 7. Flujo de compra (orden de llamadas)

```text
1. POST /api/v1/carritos                       {}  → carrito de visitante (guarda id y sesionAnonimaId en localStorage)
2. POST /api/v1/carritos/{id}/items            {"productoId":1,"kg":2.5}
3. POST /api/v1/auth/login                     {"email":"cliente@bocado.cl","password":"PolarCliente#26"}
4. GET  /api/v1/clientes/usuario/7             → clienteId = 1 (y sus direcciones)
5. PATCH /api/v1/carritos/{id}/cliente         {"clienteId":1}   (une el carrito del visitante con el del cliente)
6. POST /api/v1/pedidos/cotizar                {"carritoId":1,"comuna":"Concepción","clienteId":1,"codigoCupon":"BIENVENIDA10"}
7. POST /api/v1/pedidos/checkout               {"carritoId":1,"clienteId":1,"nombreCliente":"Cliente Demo",
                                                "email":"cliente@bocado.cl","telefono":"+56 9 4444 4444",
                                                "direccion":"O'Higgins 1234","comuna":"Concepción",
                                                "referencia":"Tocar timbre 3B","comentario":"Dejar con conserje",
                                                "fechaEntrega":"2026-10-20","horario":"09:00 - 12:00",
                                                "metodoPago":"WEBPAY","codigoCupon":"BIENVENIDA10"}
                                               → pedido PENDIENTE_PAGO y stock reservado por FEFO (30 min)
8. POST /api/v1/pagos                          {"pedidoId":<id del paso 7>,"metodo":"WEBPAY"}
9. POST /api/v1/pagos/webhook                  {"pagoId":<id del paso 8>,"aprobado":true}   (simula a Webpay)
                                               → pedido PAGADO, stock descontado, boleta emitida
10. GET /api/v1/boletas/pedido/{pedidoId}   ·   GET /api/v1/pedidos/{pedidoId}/seguimiento
```

Contra entrega: en el paso 8 usa `{"pedidoId":<id>,"metodo":"CONTRA_ENTREGA","tipoCobro":"EFECTIVO","pagaCon":50000}`; el pedido pasa a EN_PREPARACION y el repartidor cobra al entregar (`POST /api/v1/entregas`).

Horarios válidos: `09:00 - 12:00`, `12:00 - 15:00`, `15:00 - 18:00`. Estados del pedido: `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO`, más `CANCELADO` y `REEMBOLSADO`. `PATCH /pedidos/{id}/retroceder` vuelve al estado anterior y queda en el historial.

## 8. Conexión con React + TypeScript + Vite

`.env` del front:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

`src/services/api.ts` (mismo patrón que `conexion_react_springboot`, agregando el token):

```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('bocadoToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Todos los errores llegan con el mismo formato: { status, error, mensaje, ruta, detalles }
export const mensajeError = (e: unknown) =>
  axios.isAxiosError(e) ? e.response?.data?.mensaje ?? 'No se pudo conectar con el servidor' : 'Error inesperado';
```

CORS ya está permitido para `http://localhost:5173` (y 3000) en el gateway y en cada servicio (`bocado.cors.origenes`).

**Formato de errores** (todos los servicios):

```json
{ "timestamp": "2026-10-17T23:10:00Z", "status": 400, "error": "Bad Request",
  "mensaje": "Hay campos con errores", "ruta": "/api/v1/auth/registro",
  "detalles": { "password": "Mínimo 8 caracteres, una mayúscula, una minúscula y un número" } }
```

Códigos: 400 datos inválidos · 401 sin sesión · 403 sin permiso · 404 no existe · 409 regla de negocio (sin stock, cupón usado, transición de estado no permitida) · 503 otro microservicio apagado.

## 9. Simulaciones (proyecto académico)

- **Pagos**: Webpay se simula con `POST /api/v1/pagos/webhook`. No se guardan números de tarjeta, solo alias, marca, últimos 4 dígitos y un token.
- **Correos**: se guardan en ms-notificaciones y se muestran en su consola como `[CORREO SIMULADO]`. Para enviarlos de verdad, configura `spring.mail.*` en ms-notificaciones y cambia `bocado.correo.habilitado=true`.
- **Boleta**: `GET /api/v1/boletas/{id}/html` devuelve la boleta lista para imprimir (o guardar como PDF desde el navegador).
- **Orden de paradas**: por cercanía entre comunas desde la bodega (coordenadas aproximadas), igual que el prototipo.
