// types/index.ts
// Tipos que calzan con el JSON que devuelve el backend (microservicios Spring Boot).
// Las fechas llegan como texto: LocalDate "2026-11-07", Instant "2026-11-07T21:30:00Z", LocalTime "08:30:00".

/* ============ Comunes ============ */

export interface ApiError {
    timestamp?: string;
    status: number;
    error?: string;
    mensaje: string;
    ruta?: string;
    detalles?: Record<string, string>;
}

/* ============ ms-auth ============ */

export type Rol = 'CLIENTE' | 'REPARTIDOR' | 'VENDEDOR' | 'BODEGUERO' | 'SOPORTE' | 'ADMIN';
export type EstadoUsuario = 'ACTIVO' | 'BLOQUEADO' | 'ELIMINADO' | 'SUSPENDIDO';

/** Secciones del panel admin (permisos por rol). */
export type Seccion =
    | 'resumen' | 'pedidos' | 'inventario' | 'categorias' | 'clientes'
    | 'marketing' | 'rutas' | 'frio' | 'usuarios' | 'configuracion';

export interface Usuario {
    id: number;
    nombre: string;
    apellido: string;
    email: string;
    rut?: string;
    telefono?: string;
    rol: Rol;
    estado: EstadoUsuario;
    protegido: boolean;
    creadoEn?: string;
}

export interface AuthResponse {
    token: string;
    tipo: string;
    expiraEn: string;
    usuario: Usuario;
    permisos: Seccion[];
}

export interface RegistroData {
    nombre: string;
    apellido: string;
    email: string;
    password: string;
    telefono: string;
    rut: string;
}

export interface UsuarioAdminData {
    nombre: string;
    apellido: string;
    email: string;
    password: string;
    rol: Rol;
    telefono: string;
    rut: string;
}

/* ============ ms-clientes ============ */

export type SegmentoCliente = 'NUEVO' | 'FRECUENTE' | 'VIP';
export type TipoNota = 'INCIDENCIA' | 'DEVOLUCION' | 'RECLAMO' | 'CONSULTA';

export interface Direccion {
    id: number;
    alias: string;
    calle: string;
    comuna: string;
    referencia?: string;
    principal: boolean;
}

export interface NotaCliente {
    id: number;
    tipo: TipoNota;
    texto: string;
    autorId?: number;
    fecha: string;
}

export interface Cliente {
    id: number;
    usuarioId?: number;
    nombre: string;
    apellido?: string;
    email: string;
    telefono?: string;
    rut?: string;
    segmento: SegmentoCliente;
    gastoTotal: number;
    totalPedidos: number;
    creadoEn?: string;
    direcciones: Direccion[];
    notas: NotaCliente[];
    preferencia?: { id: number; alergias?: string; notas?: string };
}

export interface DireccionData {
    alias: string;
    calle: string;
    comuna: string;
    referencia?: string;
    principal?: boolean;
}

/* ============ ms-catalogo ============ */

export interface Categoria {
    id: number;
    nombre: string;
    descripcion?: string;
}

export interface Producto {
    id: number;
    sku: string;
    nombre: string;
    descripcion?: string;
    categoria: Categoria;
    origen?: string;
    alergenos?: string;
    imagen?: string;
    activo: boolean;
    precioKg: number;
    costoKg: number;
    valoracionProm: number;
    valoracionVotos: number;
    /** Promedio redondeado a media estrella. */
    valoracion: number;
    margen: number;
    atributos: Record<string, string>;
}

export interface ProductoData {
    sku: string;
    nombre: string;
    descripcion?: string;
    categoriaId: number;
    origen?: string;
    alergenos?: string;
    imagen?: string;
    activo?: boolean;
    precioKg: number;
    costoKg: number;
    atributos?: Record<string, string>;
}

export interface ResumenValoracion {
    productoId: number;
    promedio: number;
    votos: number;
    mia: number;
}

/* ============ ms-inventario ============ */

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'MERMA';

export interface Lote {
    id: number;
    productoId: number;
    productoNombre?: string;
    codigoLote: string;
    tempAlmacenaje?: number;
    bloqueado: boolean;
    motivoBloqueo?: string;
    stockKg: number;
    reservadoKg: number;
    disponibleKg: number;
    vencimiento: string;
    diasParaVencer: number;
    ingreso?: string;
}

export interface StockProducto {
    productoId: number;
    productoNombre?: string;
    stockKg: number;
    reservadoKg: number;
    disponibleKg: number;
    lotes: number;
    critico: boolean;
    proximoVencimiento?: string;
}

export interface AlertaStock {
    id: number;
    productoId: number;
    productoNombre?: string;
    stockActual: number;
    atendida: boolean;
    fecha: string;
}

export interface LoteData {
    productoId: number;
    codigoLote: string;
    stockKg: number;
    vencimiento: string;
    tempAlmacenaje?: number;
    usuarioId?: number;
}

/* ============ ms-pedidos ============ */

export type EstadoPedido =
    | 'PENDIENTE_PAGO' | 'PAGADO' | 'EN_PREPARACION' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO' | 'REEMBOLSADO';
export type MetodoPago = 'WEBPAY' | 'TARJETA' | 'TRANSFERENCIA' | 'CONTRA_ENTREGA';

export interface ItemCarrito {
    id: number;
    productoId: number;
    nombre: string;
    imagen?: string;
    kg: number;
    precioKg: number;
    subtotal: number;
}

export interface Carrito {
    id: number;
    clienteId?: number;
    sesionAnonimaId?: string;
    items: ItemCarrito[];
    totalKg: number;
    subtotal: number;
}

export interface ItemPedido {
    id: number;
    productoId: number;
    nombre: string;
    precioKg: number;
    kg: number;
    subtotal: number;
}

export interface HistorialEstado {
    id: number;
    estado: EstadoPedido;
    nota?: string;
    usuarioId?: number;
    fecha: string;
    esReversion: boolean;
}

export interface NotaInterna {
    id: number;
    texto: string;
    autorId?: number;
    fecha: string;
}

export interface Pedido {
    id: number;
    codigo: string;
    clienteId: number;
    nombreCliente: string;
    email: string;
    telefono?: string;
    direccion: string;
    comuna: string;
    referencia?: string;
    comentario?: string;
    fechaEntrega: string;
    horario: string;
    metodoPago: MetodoPago;
    pagoId?: number;
    estado: EstadoPedido;
    subtotal: number;
    envio: number;
    descuento: number;
    total: number;
    totalKg: number;
    codigoCupon?: string;
    /** Repartidor que asignó el administrador (id de usuario). */
    repartidorId?: number | null;
    repartidorNombre?: string | null;
    asignadoEn?: string | null;
    creadoEn: string;
    items: ItemPedido[];
    historial: HistorialEstado[];
    notas: NotaInterna[];
}

export interface CheckoutData {
    carritoId: number;
    clienteId: number;
    nombreCliente: string;
    email: string;
    telefono: string;
    direccion: string;
    comuna: string;
    referencia: string;
    comentario: string;
    fechaEntrega: string;
    horario: string;
    metodoPago: MetodoPago;
    codigoCupon: string;
}

export interface Cotizacion {
    subtotal: number;
    envio: number;
    descuento: number;
    total: number;
    totalKg: number;
    mensajeCupon?: string;
}

export interface PasoSeguimiento {
    estado: EstadoPedido;
    completado: boolean;
    fecha?: string;
}

export interface Seguimiento {
    pedidoId: number;
    codigo: string;
    estado: EstadoPedido;
    fechaEntrega: string;
    horario: string;
    pasos: PasoSeguimiento[];
    historial: HistorialEstado[];
}

/** Datos de entrega que se guardan entre el paso "Pedido" y el paso "Pago". */
export interface BorradorPedido {
    nombreCliente: string;
    telefono: string;
    direccion: string;
    comuna: string;
    referencia: string;
    comentario: string;
    fechaEntrega: string;
    horario: string;
    codigoCupon: string;
    /** Se llena cuando el pedido ya se creó (para no duplicarlo si el pago falla). */
    pedidoId?: number;
    pedidoCodigo?: string;
    metodoPago?: MetodoPago;
}

/* ============ ms-pagos ============ */

export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO';
export type TipoCobro = 'EFECTIVO' | 'POS_MOVIL' | 'TRANSFERENCIA';

export interface Pago {
    id: number;
    pedidoId: number;
    codigoPedido?: string;
    metodo: MetodoPago;
    estado: EstadoPago;
    monto: number;
    pasarela?: string;
    referencia?: string;
    instrucciones?: string;
    motivoRechazo?: string;
    fecha: string;
}

export interface Boleta {
    id: number;
    numero: string;
    pagoId: number;
    pedidoId: number;
    neto: number;
    iva: number;
    tasaIva: number;
    total: number;
    emision: string;
}

/* ============ ms-marketing ============ */

export type TipoDescuento = 'PORCENTAJE' | 'MONTO_FIJO';

export interface Cupon {
    id: number;
    codigo: string;
    tipo: TipoDescuento;
    valor: number;
    compraMinima: number;
    limiteUsos: number;
    usos: number;
    limitePorCliente: number;
    activo: boolean;
    vence: string;
}

export interface CuponData {
    codigo: string;
    tipo: TipoDescuento;
    valor: number;
    compraMinima: number;
    limiteUsos: number;
    limitePorCliente: number;
    activo: boolean;
    vence: string;
}

export interface CarritoAbandonado {
    id: number;
    cliente?: string;
    email?: string;
    resumen: string;
    total: number;
    fecha: string;
    correoEnviado: boolean;
    cuponOfrecido?: string;
    recuperado: boolean;
}

/* ============ ms-rutas ============ */

export type EstadoRuta = 'PLANIFICADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA';
export type EstadoParada = 'PENDIENTE' | 'ENTREGADA' | 'FALLIDA';

export interface Vehiculo {
    id: number;
    tipo: string;
    patente: string;
    refrigerado: boolean;
    activo: boolean;
}

export interface Parada {
    id: number;
    pedidoId: number;
    codigoPedido?: string;
    cliente?: string;
    telefono?: string;
    direccion: string;
    comuna: string;
    referencia?: string;
    horario?: string;
    cobrarAlEntregar: boolean;
    montoACobrar?: number;
    orden: number;
    estado: EstadoParada;
    motivoFalla?: string;
}

export interface Ruta {
    id: number;
    codigo: string;
    nombre: string;
    fecha: string;
    comunas: string[];
    horaSalida: string;
    repartidorId?: number;
    repartidorNombre?: string;
    vehiculo?: Vehiculo;
    estado: EstadoRuta;
    notas?: string;
    paradas: Parada[];
    paradasPendientes: number;
}

export interface RutaData {
    nombre: string;
    fecha: string;
    comunas: string[];
    horaSalida: string;
    repartidorId?: number;
    repartidorNombre?: string;
    vehiculoId?: number;
    notas?: string;
}

/* ============ ms-reparto ============ */

export type Disponibilidad = 'DISPONIBLE' | 'EN_RUTA' | 'FUERA_SERVICIO';
export type TipoIncidencia = 'CLIENTE_AUSENTE' | 'DIRECCION_INCORRECTA' | 'RECHAZO_PRODUCTO' | 'OTRO';

export interface Repartidor {
    id: number;
    nombre: string;
    email?: string;
    rut?: string;
    disponibilidad: Disponibilidad;
    calificacionProm: number;
    votos: number;
}

export interface Turno {
    id: number;
    repartidorId: number;
    rutaId?: number;
    inicio: string;
    fin?: string;
    tempCajaInicial: number;
    alertaTemperatura: boolean;
}

export interface CierreCaja {
    id: number;
    repartidorId: number;
    turnoId: number;
    fecha: string;
    entregas: number;
    fallidas: number;
    efectivoEsperado: number;
    efectivoRendido: number;
    diferencia: number;
    pos: number;
    transferencia: number;
}

export interface EntregaData {
    repartidorId: number;
    turnoId?: number;
    pedidoId: number;
    paradaId?: number;
    recibidoPor: string;
    tempEntrega?: number;
    fotoUrl?: string;
    firmaUrl?: string;
    comentario?: string;
    cobro?: { tipo: TipoCobro; pagaCon?: number };
}

export interface EntregaFallidaData {
    repartidorId: number;
    turnoId?: number;
    pedidoId: number;
    paradaId?: number;
    tipo: TipoIncidencia;
    nota: string;
}

/* ============ ms-frio ============ */

export type OrigenTemp = 'LOTE' | 'SALIDA_RUTA' | 'ENTREGA';
export type Severidad = 'ADVERTENCIA' | 'CRITICA';

export interface RegistroTemperatura {
    id: number;
    origen: OrigenTemp;
    referenciaId: number;
    grados: number;
    fecha: string;
    registradoPorId?: number;
}

export interface AlertaFrio {
    id: number;
    registroId: number;
    origen: OrigenTemp;
    referenciaId: number;
    severidad: Severidad;
    detalle: string;
    minutosSobreUmbral: number;
    atendida: boolean;
    fecha: string;
}

export interface CuentaRegresiva {
    id: number;
    turnoId: number;
    inicio: string;
    limiteMin: number;
    umbralAvisoMin: number;
    minutosRestantes: number;
    debeAvisar: boolean;
    excedida: boolean;
}

/* ============ ms-config ============ */

export interface ZonaEnvio {
    id: number;
    comuna: string;
    tarifa: number;
    activa: boolean;
}

export interface ReglaNegocio {
    codigo: string;
    descripcion: string;
    valor: number;
    unidad?: string;
    vigenteDesde?: string;
}

export interface ConfiguracionGlobal {
    id: number;
    pais: string;
    moneda: string;
    preciosConIva: boolean;
    iva: number;
}

/* ============ ms-reportes ============ */

export type Periodo = 'HOY' | 'SIETE_DIAS' | 'MES_ACTUAL' | 'TREINTA_DIAS';

export interface Kpis {
    periodo: Periodo;
    desde: string;
    hasta: string;
    ordenes: number;
    visitas: number;
    conversion: number;
    ventasBrutas: number;
    ventasNetas: number;
    ticketPromedio: number;
    comparacion: {
        ventasBrutasPct?: number;
        ordenesPct?: number;
        ticketPromedioPct?: number;
        conversionPct?: number;
    };
}

export interface TopProducto {
    productoId: number;
    nombre: string;
    kg: number;
    ingresos: number;
}

export interface AlertaOperativa {
    tipo: 'STOCK_CRITICO' | 'FRIO' | 'INCIDENCIA' | 'PEDIDO_ATRASADO';
    cantidad: number;
    enlace: string;
}

export interface VentaDiaria {
    fecha: string;
    ordenes: number;
    ventas: number;
    visitas: number;
}
