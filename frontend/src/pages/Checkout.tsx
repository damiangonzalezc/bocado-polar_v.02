// pages/Checkout.tsx · Paso 2 "Entrega": datos de despacho, comuna, fecha, horario y cupón (EP14, EP18)

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import Steps from '../components/Steps';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';
import { useApi } from '../hooks/useApi';
import { useForm } from '../hooks/useForm';
import ValidationService from '../services/validationService';
import FormatService from '../services/formatService';
import ConfigService from '../services/configService';
import PedidoService, { HORARIOS, PASOS_COMPRA } from '../services/pedidoService';
import StorageService from '../services/storageService';
import { mensajeError } from '../services/apiClient';
import type { Cotizacion } from '../types';

const Checkout: React.FC = () => {
    const { usuario, cliente } = useAuth();
    const { carrito, cargando } = useCart();
    const navigate = useNavigate();
    const zonas = useApi(() => ConfigService.zonas());
    const [cupon, setCupon] = useState<Cotizacion | null>(null);
    const [aplicando, setAplicando] = useState(false);

    const borrador = StorageService.getBorrador();
    const principal = cliente?.direcciones.find((d) => d.principal) ?? cliente?.direcciones[0];
    const V = ValidationService;

    const form = useForm(
        {
            nombreCliente: { reglas: [V.requerido('El nombre de quien recibe'), V.texto(2, 80)] },
            telefono: { reglas: [V.requerido('El teléfono'), V.telefono()] },
            direccion: { reglas: [V.requerido('La dirección'), V.direccion()] },
            comuna: { reglas: [V.requerido('La comuna')] },
            referencia: { reglas: [V.largo(0, 120)] },
            fechaEntrega: { reglas: [V.requerido('La fecha de entrega'), V.fechaEntrega(1, 14)] },
            horario: { reglas: [V.requerido('El bloque horario')] },
            comentario: { reglas: [V.largo(0, 300)] },
            codigoCupon: { reglas: [V.codigoCupon()], formato: (v) => v.trim().toUpperCase() },
        },
        {
            nombreCliente: borrador?.nombreCliente ?? (usuario ? `${usuario.nombre} ${usuario.apellido}` : ''),
            telefono: borrador?.telefono ?? usuario?.telefono ?? '',
            direccion: borrador?.direccion ?? principal?.calle ?? '',
            comuna: borrador?.comuna ?? principal?.comuna ?? '',
            referencia: borrador?.referencia ?? principal?.referencia ?? '',
            fechaEntrega: borrador?.fechaEntrega ?? '',
            horario: borrador?.horario ?? '',
            comentario: borrador?.comentario ?? '',
            codigoCupon: borrador?.codigoCupon ?? '',
        },
    );

    if (cargando) return <Loader />;

    if (!cliente) {
        return (
            <EmptyState mensaje="Para comprar necesitas una cuenta de cliente. Las cuentas del personal no pueden generar pedidos." accion={{ texto: 'Ir al catálogo', a: '/catalogo' }} />
        );
    }

    if (!carrito || carrito.items.length === 0) {
        return <EmptyState mensaje="No hay productos en tu carrito." accion={{ texto: 'Ir al catálogo', a: '/catalogo' }} />;
    }

    const envio = zonas.datos?.find((z) => z.comuna === form.valores.comuna)?.tarifa;
    const descuento = cupon && cupon.descuento > 0 ? cupon.descuento : 0;
    const total = carrito.subtotal + (envio ?? 0) - descuento;

    const aplicarCupon = async () => {
        const codigo = form.valores.codigoCupon.trim().toUpperCase();
        if (!codigo || V.codigoCupon()(codigo, {})) {
            form.setResumen('Escribe un código de cupón válido (4 a 16 letras o números).');
            return;
        }
        setAplicando(true);
        try {
            const c = await PedidoService.cotizar(carrito.id, form.valores.comuna || 'Concepción', cliente.id, codigo);
            setCupon(c);
            form.setResumen('');
        } catch (e) {
            setCupon(null);
            form.setResumen(mensajeError(e));
        } finally {
            setAplicando(false);
        }
    };

    const enviar = (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        const v = form.valores;
        StorageService.setBorrador({
            nombreCliente: v.nombreCliente.trim(),
            telefono: v.telefono.trim(),
            direccion: v.direccion.trim(),
            comuna: v.comuna,
            referencia: v.referencia.trim(),
            comentario: v.comentario.trim(),
            fechaEntrega: v.fechaEntrega,
            horario: v.horario,
            codigoCupon: cupon && cupon.descuento > 0 ? v.codigoCupon.trim().toUpperCase() : '',
        });
        navigate('/pago');
    };

    return (
        <>
            <div className="section-title">
                <h1>Crear pedido</h1>
                <span className="tag">EP14 · EP18</span>
            </div>
            <Steps pasos={PASOS_COMPRA} actual={1} etiqueta="Pasos de compra" />
            <section className="grid cols-2" aria-label="Datos de entrega y resumen">
                <div className="card">
                    <form className="form" aria-label="Datos de entrega" noValidate onSubmit={enviar}>
                        <FormSummary mensaje={form.resumen} />
                        <FormField id="nombreCliente" label="Cliente" hint="Nombre de quien recibe." error={form.errores.nombreCliente}>
                            <input {...form.campo('nombreCliente')} type="text" autoComplete="name" />
                        </FormField>
                        <FormField id="telefono" label="Teléfono de contacto" hint="Ej: +56 9 1234 5678" error={form.errores.telefono}>
                            <input {...form.campo('telefono')} type="tel" autoComplete="tel" />
                        </FormField>
                        <FormField id="direccion" label="Dirección" hint="Calle y, si tiene, número. Ej: O'Higgins 1234" error={form.errores.direccion}>
                            <input {...form.campo('direccion')} type="text" autoComplete="street-address" />
                        </FormField>
                        <FormField id="comuna" label="Comuna" hint="Define el costo de envío." error={form.errores.comuna}>
                            <select {...form.campo('comuna', 'select')}>
                                <option value="">Selecciona una comuna…</option>
                                {(zonas.datos ?? []).map((z) => (
                                    <option key={z.id} value={z.comuna}>{z.comuna} · envío {FormatService.dinero(z.tarifa)}</option>
                                ))}
                            </select>
                        </FormField>
                        <FormField id="referencia" label="Referencia (opcional)" hint="Ej: depto 3B, portón verde." error={form.errores.referencia}>
                            <input {...form.campo('referencia')} type="text" maxLength={120} />
                        </FormField>
                        <div className="two">
                            <FormField id="fechaEntrega" label="Fecha de entrega" hint="Desde mañana y hasta 14 días." error={form.errores.fechaEntrega}>
                                <input {...form.campo('fechaEntrega')} type="date" min={FormatService.fechaEnDias(1)} max={FormatService.fechaEnDias(14)} />
                            </FormField>
                            <FormField id="horario" label="Bloque horario" hint="Rango en que estarás disponible." error={form.errores.horario}>
                                <select {...form.campo('horario', 'select')}>
                                    <option value="">Selecciona un bloque…</option>
                                    {HORARIOS.map((h) => <option key={h} value={h}>{h}</option>)}
                                </select>
                            </FormField>
                        </div>
                        <FormField id="comentario" label="Comentario de entrega (opcional)" hint="Máximo 300 caracteres." error={form.errores.comentario}>
                            <textarea {...form.campo('comentario')} className={`${form.clase('comentario')} textarea`} maxLength={300} />
                        </FormField>
                        <FormField id="codigoCupon" label="Cupón de descuento (opcional)" hint="Prueba: BIENVENIDA10 o POLAR5000" error={form.errores.codigoCupon}>
                            <div className="input-group">
                                <input {...form.campo('codigoCupon')} type="text" autoComplete="off" style={{ textTransform: 'uppercase' }} />
                                <button type="button" className="btn secondary small" onClick={aplicarCupon} disabled={aplicando}>
                                    {aplicando ? 'Revisando…' : 'Aplicar'}
                                </button>
                            </div>
                        </FormField>
                        {cupon?.mensajeCupon && (
                            <p className={`notice ${cupon.descuento > 0 ? 'success' : 'error'}`} role="status">{cupon.mensajeCupon}</p>
                        )}
                        <button className="btn">Continuar al pago</button>
                    </form>
                </div>
                <aside className="card" aria-label="Resumen del pedido">
                    <h2>Resumen</h2>
                    <div className="totals">
                        {carrito.items.map((i) => (
                            <div key={i.productoId}>
                                <span>{i.nombre} · {FormatService.kg(i.kg)} kg</span>
                                <span>{FormatService.dinero(i.subtotal)}</span>
                            </div>
                        ))}
                        <div><span>Subtotal</span><span>{FormatService.dinero(carrito.subtotal)}</span></div>
                        <div>
                            <span>Envío {form.valores.comuna && `(${form.valores.comuna})`}</span>
                            <span>{envio !== undefined ? FormatService.dinero(envio) : 'Elige comuna'}</span>
                        </div>
                        {descuento > 0 && <div><span>Descuento</span><span>−{FormatService.dinero(descuento)}</span></div>}
                        <div className="grand"><span>Total</span><span>{FormatService.dinero(Math.max(total, 0))}</span></div>
                    </div>
                    <div className="notice">RN-02: máximo 12,5 kg por formato.</div>
                    <div className="notice">RN-03: rotación FEFO (primero en vencer, primero en salir).</div>
                </aside>
            </section>
        </>
    );
};

export default Checkout;
