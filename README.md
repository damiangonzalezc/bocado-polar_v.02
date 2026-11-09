# BOCADO_Polar — Proyecto completo (backend + frontend)

Tienda de pescados y mariscos congelados con cadena de frío. Un solo repositorio con:

```
bocado-polar/
├── backend/    Spring Boot · 13 microservicios + api-gateway (MySQL en XAMPP)
├── frontend/   React 19 + TypeScript + Vite + SWC (consume el gateway)
├── iniciar.sh / iniciar.bat   levanta backend y frontend
└── README.md
```

## Requisitos

- Java 17 o superior y Maven (incluido `mvnw`) · XAMPP con MySQL en el puerto 3306
- Node.js 20 o superior

## Cómo ejecutar

1. **Base de datos:** inicia MySQL en XAMPP. Las 13 bases se crean solas (`createDatabaseIfNotExist`).
2. **Backend:** `cd backend` y ejecuta `iniciar-todo.sh` (Linux/Mac) o `iniciar-todo.bat` (Windows). Gateway en `http://localhost:8080`.
3. **Frontend:**
   ```bash
   cd frontend
   cp .env.example .env      # VITE_API_BASE_URL=http://localhost:8080/api/v1
   npm install
   npm run dev               # http://localhost:5173
   ```

O todo junto desde la raíz: `./iniciar.sh` / `iniciar.bat`.

## Cuentas de prueba

| Rol | Correo | Contraseña | Entra a |
|---|---|---|---|
| Administrador | admin@bocado.cl | PolarAdmin#26 | /admin |
| Vendedor | ventas@bocado.cl | PolarVentas#26 | /admin (pedidos, clientes, marketing) |
| Bodeguero | bodega@bocado.cl | PolarBodega#26 | /admin (inventario, frío) |
| Repartidor | reparto@bocado.cl | PolarReparto#26 | /repartidor |
| Cliente | cliente@bocado.cl | PolarCliente#26 | tienda, pedido, pago, seguimiento |

Pruebas de pago: tarjeta `4111 1111 1111 1111`, cupones `BIENVENIDA10` y `POLAR5000`.

## Frontend: estructura (src/)

| Carpeta | Contenido |
|---|---|
| `types/` | Tipos TypeScript de todos los DTO del backend |
| `services/` | Clases estáticas por microservicio (`pedidoService`, `catalogoService`…), `apiClient` (axios + JWT), `validationService`, `formatService`, `storageService` |
| `context/` | `AuthContext` (sesión JWT + permisos) y `CartContext` (carrito anónimo o del cliente) |
| `hooks/` | `useAuth`, `useCart`, `useForm` (motor de validación), `useApi`, `useToast` |
| `components/` | Componentes reutilizables · `components/admin/` secciones del panel |
| `pages/` | Inicio, Catálogo, Ficha, Carrito, Pedido, Pago, Seguimiento, Cuenta, Admin, Repartidor |
| `styles/` | CSS del prototipo V3 con modo claro/oscuro (`custom.css`, `admin.css`, `repartidor.css`) |
| `tests/` | Configuración de Vitest + Testing Library |

**Validaciones:** las del prototipo (RUT con DV, Luhn, correo .com/.cl, teléfono chileno, contraseña segura, kg en pasos de 0,5 con máximo 12,5 por formato, fechas de entrega…). Se validan al salir del campo, se revalidan en vivo, muestran un resumen y los errores `detalles` del backend se marcan en su campo.

**Sesión:** `/login` y `/registro` son páginas separadas (`/cuenta` redirige a `/login`).

**Modo oscuro:** botón en la cabecera; recuerda la elección y, si no hay, usa la del sistema (`themeService` + `useTheme`).

**Asignación de pedidos:** en *Admin → Pedidos* el administrador elige el repartidor de cada pedido pagado. El pedido queda como parada en la ruta de ese repartidor (ms-rutas) y aparece en su vista `/repartidor` el día de entrega. El cliente ve el repartidor en Seguimiento.

**Rutas protegidas:** `/pedido`, `/pago`, `/seguimiento` requieren sesión; `/admin/*` solo personal (el menú se filtra por permisos); `/repartidor` solo REPARTIDOR/ADMIN.

## Scripts del frontend

| Comando | Uso |
|---|---|
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Compilación de producción (`tsc -b` + Vite) |
| `npm run lint` | oxlint |
| `npm test` / `npm run test:run` / `npm run coverage` | Pruebas unitarias (Vitest) |

## Cambios al backend respecto a la entrega anterior

- `api-gateway` → `JwtFiltro`: `GET /api/v1/stock/**` y `GET /api/v1/lotes/fefo/**` son públicas; `POST /rutas/asignar-pedido` y `PATCH /pedidos/*/repartidor` son solo ADMIN.
- `ms-pedidos`: el pedido guarda `repartidorId`, `repartidorNombre` y `asignadoEn`; nuevo `PATCH /pedidos/{id}/repartidor` y filtro `?repartidorId=`.
- `ms-rutas`: nuevo `POST /rutas/asignar-pedido`; «Generar paradas» ya no mueve pedidos que el admin asignó a otro repartidor.
- Las columnas nuevas se crean solas (`ddl-auto=update`), no hace falta borrar las bases.

Documentación de la API: `backend/README.md`, `backend/API.md` y `backend/POSTMAN.md`.
