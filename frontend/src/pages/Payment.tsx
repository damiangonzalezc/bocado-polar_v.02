// pages/Payment.tsx · Paso 3 "Pago" y 4 "Confirmación" (EP15, EP16)
// Al confirmar: 1) se crea el pedido en ms-pedidos (reserva stock por FEFO),
// 2) se inicia el pago en ms-pagos y 3) se simula la respuesta de Webpay cuando corresponde.

import React, { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import Steps from '../components/Steps';
import Loader from '../components/Loader';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';
import { useForm } from '../hooks/useForm';
import ValidationService from '../services/validationService';
import FormatService from '../services/formatService';
import PedidoService, { PASOS_COMPRA } from '../services/pedidoService';
import PagoService from '../services/pagoService';
import StorageService from '../services/storageService';
import { mensajeError } from '../services/apiClient';
import type { Cotizacion, MetodoPago, Pago, Pedido, TipoCobro } from '../types';

const METODOS: { valor: MetodoPago; texto: string; aviso: string }[] = [
    { valor: 'WEBPAY', texto: 'Webpay', aviso: 'Serás redirigido a Webpay (simulado en este proyecto).' },
    { valor: 'TRANSFERENCIA', texto: 'Transferencia', aviso: 'Te mostraremos los datos bancarios; el pedido se confirma al recibir la transferencia.' },
    { valor: 'TARJETA', texto: 'Tarjeta de crédito o débito', aviso: 'Los datos de la tarjeta se validan en tu navegador y no se guardan ni se envían.' },
    { valor: 'CONTRA_ENTREGA', texto: 'Pago contra entrega', aviso: 'Pagas al recibir: efectivo, POS móvil o transferencia.' },
];

interface Resultado {
    pedido: Pedido;
    pago: Pago;
    boletaId?: number;
}

const Payment: React.FC = () => {
    const { cliente, usuario } = useAuth();
    const { carrito, refrescar } = useCart();
    const [borrador, setBorradorState] = useState(StorageService.getBorrador());
    const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
    const [metodo, setMetodo] = useState<MetodoPago>(StorageService.getBorrador()?.metodoPago ?? 'WEBPAY');
    const [procesando, setProcesando] = useState('');
    const [resultado, setResultado] = useState<Resultado | null>(null);
    const V = ValidationService;
    const esTarjeta = metodo === 'TARJETA';
    const esEfectivo = metodo === 'CONTRA_ENTREGA';

    const form = useForm(
        {
            titular: { reglas: esTarjeta ? [V.requerido('El titular'), V.texto(2, 60)] : [] },
            tarjeta: { reglas: esTarjeta ? [V.requerido('El número de tarjeta'), V.tarjeta()] : [], formato: V.formatearTarjeta, formatoEnVivo: true },
            vence: { reglas: esTarjeta ? [V.requerido('El vencimiento'), V.vencimientoTarjeta()] : [], formato: V.formatearVencimiento, formatoEnVivo: true },
            cvv: { reglas: esTarjeta ? [V.requerido('El código de seguridad'), V.cvv()] : [] },
            tipoCobro: { reglas: [] },
            pagaCon: {
                reglas: esEfectivo
                    ? [(v, vals) => {
                        if (!v.trim() || vals.tipoCobro !== 'EFECTIVO') return '';
                        const n = V.numero(v);
                        const total = cotizacion?.total ?? 0;
                        return Number.isNaN(n) || n < total ? `Debe ser igual o mayor al total (${FormatService.dinero(total)}).` : '';
                    }]
                    : [],
            },
        },
        { titular: '', tarjeta: '', vence: '', cvv: '', tipoCobro: 'EFECTIVO', pagaCon: '' },
    );

    // Resumen con precios vigentes, envío y cupón (sin crear nada todavía).
    // Si el pedido ya se creó (el pago falló antes), se muestran sus totales reales.
    useEffect(() => {
        if (!borrador) return;
        if (borrador.pedidoId) {
            PedidoService.obtener(borrador.pedidoId)
                .then((p) => setCotizacion({ subtotal: p.subtotal, envio: p.envio, descuento: p.descuento, total: p.total, totalKg: p.totalKg }))
                .catch((e) => form.setResumen(mensajeError(e)));
            return;
        }
        if (!carrito || carrito.items.length === 0) return;
        PedidoService.cotizar(carrito.id, borrador.comuna, cliente?.id, borrador.codigoCupon || undefined)
            .then(setCotizacion)
            .catch((e) => form.setResumen(mensajeError(e)));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [carrito?.id, borrador]);

    if (resultado) return <Confirmacion resultado={resultado} />;
    if (!borrador) return <Navigate to="/pedido" replace />;
    if (!cliente || !usuario) return <Navigate to="/pedido" replace />;

    const pedidoCreado = Boolean(borrador.pedidoId);
    if (!pedidoCreado && (!carrito || carrito.items.length === 0)) return <Navigate to="/carrito" replace />;
    if (!cotizacion && !form.resumen) return <Loader texto="Calculando el total…" />;

    const avisoMetodo = METODOS.find((m) => m.valor === metodo)?.aviso;

    const confirmar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        try {
            // 1) Pedido (solo una vez: si el pago falló antes, se reutiliza)
            let pedidoId = borrador.pedidoId;
            if (!pedidoId) {
                setProcesando('Creando tu pedido y reservando stock…');
                const pedido = await PedidoService.checkout({
                    carritoId: carrito!.id,
                    clienteId: cliente.id,
                    nombreCliente: borrador.nombreCliente,
                    email: usuario.email,
                    telefono: borrador.telefono,
                    direccion: borrador.direccion,
                    comuna: borrador.comuna,
                    referencia: borrador.referencia,
                    comentario: borrador.comentario,
                    fechaEntrega: borrador.fechaEntrega,
                    horario: borrador.horario,
                    metodoPago: metodo,
                    codigoCupon: borrador.codigoCupon,
                });
                pedidoId = pedido.id;
                const nuevo = { ...borrador, pedidoId: pedido.id, pedidoCodigo: pedido.codigo, metodoPago: metodo };
                StorageService.setBorrador(nuevo);
                setBorradorState(nuevo);
                void refrescar();
            }

            // 2) Pago
            setProcesando(metodo === 'WEBPAY' ? 'Conectando con Webpay…' : 'Registrando el pago…');
            let pago = await PagoService.iniciar(
                pedidoId,
                metodo,
                esEfectivo
                    ? { tipoCobro: form.valores.tipoCobro as TipoCobro, pagaCon: form.valores.pagaCon ? V.numero(form.valores.pagaCon) : undefined }
                    : {},
            );

            // 3) Webpay / tarjeta: se simula la confirmación de la pasarela
            if ((metodo === 'WEBPAY' || metodo === 'TARJETA') && pago.estado === 'PENDIENTE') {
                setProcesando('Esperando la confirmación del banco…');
                pago = await PagoService.confirmarPasarela(pago.id, true);
            }
            if (pago.estado === 'RECHAZADO') {
                form.setResumen(`El pago fue rechazado: ${pago.motivoRechazo ?? 'intenta con otro medio de pago'}.`);
                return;
            }

            const pedido = await PedidoService.obtener(pedidoId);
            let boletaId: number | undefined;
            if (pago.estado === 'APROBADO') {
                boletaId = await PagoService.boletaDelPedido(pedidoId).then((b) => b.id).catch(() => undefined);
            }
            StorageService.setBorrador(null);
            setResultado({ pedido, pago, boletaId });
        } catch (err) {
            form.setResumen(mensajeError(err, 'No se pudo completar el pago.'));
        } finally {
            setProcesando('');
        }
    };

    return (
        <>
            <div className="section-title">
                <h1>Pago</h1>
                <span className="tag">EP15 · EP16 · HU031 · HU032</span>
            </div>
            <Steps pasos={PASOS_COMPRA} actual={2} etiqueta="Pasos de compra" />
            <section className="grid cols-2" aria-label="Resumen y método de pago">
                <div className="card">
                    <h2>{borrador.pedidoCodigo ?? 'Tu pedido'}</h2>
                    <p>
                        {borrador.direccion}, {borrador.comuna}
                        <br />
                        {FormatService.fecha(borrador.fechaEntrega)} · {borrador.horario}
                    </p>
                    {cotizacion && (
                        <div className="totals">
                            <div><span>Productos ({FormatService.kg(cotizacion.totalKg)} kg)</span><span>{FormatService.dinero(cotizacion.subtotal)}</span></div>
                            <div><span>Envío</span><span>{FormatService.dinero(cotizacion.envio)}</span></div>
                            {cotizacion.descuento > 0 && <div><span>Descuento {borrador.codigoCupon}</span><span>−{FormatService.dinero(cotizacion.descuento)}</span></div>}
                            <div className="grand"><span>Total</span><span>{FormatService.dinero(cotizacion.total)}</span></div>
                        </div>
                    )}
                    {pedidoCreado && (
                        <p className="notice warning">
                            El pedido {borrador.pedidoCodigo} ya fue creado y espera el pago. Tienes 30 minutos antes de que se libere el stock.
                        </p>
                    )}
                    <p><Link to="/pedido">← Cambiar datos de entrega</Link></p>
                </div>
                <div className="card">
                    <form className="form" aria-label="Método de pago" noValidate onSubmit={confirmar}>
                        <FormSummary mensaje={form.resumen} />
                        <fieldset disabled={pedidoCreado}>
                            <legend>Método de pago</legend>
                            {METODOS.map((m) => (
                                <label key={m.valor} className="option">
                                    <input type="radio" name="metodo" value={m.valor} checked={metodo === m.valor} onChange={() => setMetodo(m.valor)} /> {m.texto}
                                </label>
                            ))}
                        </fieldset>
                        <p className="notice">{avisoMetodo}</p>

                        {esTarjeta && (
                            <div className="form">
                                <FormField id="titular" label="Titular" hint="Como aparece en la tarjeta." error={form.errores.titular}>
                                    <input {...form.campo('titular')} type="text" autoComplete="cc-name" />
                                </FormField>
                                <FormField id="tarjeta" label="Número de tarjeta" hint="Para probar: 4111 1111 1111 1111" error={form.errores.tarjeta}>
                                    <input {...form.campo('tarjeta')} type="text" inputMode="numeric" autoComplete="cc-number" />
                                </FormField>
                                <div className="two">
                                    <FormField id="vence" label="Vencimiento" hint="Formato MM/AA" error={form.errores.vence}>
                                        <input {...form.campo('vence')} type="text" inputMode="numeric" placeholder="MM/AA" autoComplete="cc-exp" />
                                    </FormField>
                                    <FormField id="cvv" label="CVV" hint="3 o 4 dígitos." error={form.errores.cvv}>
                                        <input {...form.campo('cvv')} type="password" inputMode="numeric" maxLength={4} autoComplete="cc-csc" />
                                    </FormField>
                                </div>
                            </div>
                        )}

                        {esEfectivo && (
                            <div className="two">
                                <FormField id="tipoCobro" label="¿Cómo pagarás al recibir?" error={form.errores.tipoCobro}>
                                    <select {...form.campo('tipoCobro', 'select')}>
                                        <option value="EFECTIVO">Efectivo</option>
                                        <option value="POS_MOVIL">POS móvil (tarjeta)</option>
                                        <option value="TRANSFERENCIA">Transferencia</option>
                                    </select>
                                </FormField>
                                {form.valores.tipoCobro === 'EFECTIVO' && (
                                    <FormField id="pagaCon" label="Pagarás con (opcional)" hint="Para llevar vuelto." error={form.errores.pagaCon}>
                                        <input {...form.campo('pagaCon')} type="number" inputMode="numeric" min={0} step={1000} />
                                    </FormField>
                                )}
                            </div>
                        )}

                        <button className="btn" disabled={Boolean(procesando)}>
                            {procesando || 'Confirmar pago'}
                        </button>
                    </form>
                </div>
            </section>
        </>
    );
};

/** Paso 4: confirmación con enlace al seguimiento y a la boleta. */
const Confirmacion: React.FC<{ resultado: Resultado }> = ({ resultado }) => {
    const { pedido, pago, boletaId } = resultado;
    const aprobado = pago.estado === 'APROBADO';
    return (
        <>
            <div className="section-title">
                <h1>Confirmación</h1>
            </div>
            <Steps pasos={PASOS_COMPRA} actual={3} etiqueta="Pasos de compra" />
            <div className={`notice ${aprobado ? 'success' : 'warning'}`} role="status">
                <h2>{aprobado ? 'Pago registrado' : 'Pedido recibido'}</h2>
                <p>
                    Pedido <strong>{pedido.codigo}</strong> · {FormatService.estadoPedido(pedido.estado)} · Total {FormatService.dinero(pedido.total)}.
                </p>
                {pago.instrucciones && !aprobado && <p>{pago.instrucciones}</p>}
                <p>
                    Entrega el {FormatService.fecha(pedido.fechaEntrega)} entre {pedido.horario} en {pedido.direccion}, {pedido.comuna}.
                </p>
                <div className="btn-row">
                    <Link className="btn" to={`/seguimiento?codigo=${pedido.codigo}`}>Ver seguimiento</Link>
                    {boletaId && (
                        <a className="btn secondary" href={PagoService.urlBoleta(boletaId)} target="_blank" rel="noopener noreferrer">
                            Ver boleta
                        </a>
                    )}
                </div>
            </div>
        </>
    );
};

export default Payment;
