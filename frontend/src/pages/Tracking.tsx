// pages/Tracking.tsx · Seguimiento por código y "Mis pedidos" (EP20)

import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import Steps from '../components/Steps';
import StatusBadge from '../components/StatusBadge';
import Loader from '../components/Loader';
import { useAuth } from '../hooks/useAuth';
import { useForm } from '../hooks/useForm';
import { useApi } from '../hooks/useApi';
import { useToast } from '../hooks/useToast';
import ValidationService from '../services/validationService';
import FormatService from '../services/formatService';
import PedidoService from '../services/pedidoService';
import { mensajeError } from '../services/apiClient';
import type { EstadoPedido, Pedido, Seguimiento } from '../types';

type Consulta = { pedido: Pedido; seguimiento: Seguimiento } | { noEncontrado: true };

const CANCELABLES: EstadoPedido[] = ['PENDIENTE_PAGO', 'PAGADO', 'EN_PREPARACION'];

const Tracking: React.FC = () => {
    const { cliente, usuario, esStaff } = useAuth();
    const { mostrar } = useToast();
    const [params, setParams] = useSearchParams();
    const codigoActual = (params.get('codigo') ?? '').trim().toUpperCase();
    const misPedidos = useApi(() => PedidoService.delCliente(cliente!.id), [cliente?.id], Boolean(cliente));

    // Busca el pedido del código que viene en la URL (?codigo=BP-100001).
    const consulta = useApi<Consulta | null>(async () => {
        if (!codigoActual) return null;
        try {
            const p = await PedidoService.porCodigo(codigoActual);
            // Un cliente solo ve sus propios pedidos; el personal ve todos.
            if (!esStaff && cliente && p.clienteId !== cliente.id) return { noEncontrado: true };
            return { pedido: p, seguimiento: await PedidoService.seguimiento(p.id) };
        } catch {
            return { noEncontrado: true };
        }
    }, [codigoActual, cliente?.id, esStaff]);

    const form = useForm(
        { codigo: { reglas: [ValidationService.requerido('El código del pedido'), ValidationService.codigoPedido()], formato: (v) => v.trim().toUpperCase() } },
        { codigo: codigoActual },
    );

    const resultado = consulta.datos && 'pedido' in consulta.datos ? consulta.datos : null;
    const pedido = resultado?.pedido ?? null;
    const seguimiento = resultado?.seguimiento ?? null;
    const noEncontrado = consulta.datos && 'noEncontrado' in consulta.datos ? 'No encontramos un pedido con ese código en tu cuenta.' : '';
    const buscando = consulta.cargando;

    const enviar = (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        setParams({ codigo: form.valores.codigo.trim().toUpperCase() });
    };

    const cancelar = async () => {
        if (!pedido || !window.confirm(`¿Cancelar el pedido ${pedido.codigo}?`)) return;
        try {
            await PedidoService.cancelar(pedido.id, 'Cancelado por el cliente desde Seguimiento', usuario?.id);
            mostrar('Pedido cancelado');
            await consulta.recargar();
            void misPedidos.recargar();
        } catch (err) {
            mostrar(mensajeError(err), 'error');
        }
    };

    const pasosFlujo = seguimiento?.pasos ?? [];
    const indiceActual = pasosFlujo.reduce((acc, p, i) => (p.completado ? i : acc), -1);

    return (
        <>
            <div className="section-title">
                <h1>Seguimiento del pedido</h1>
                <span className="tag">EP20</span>
            </div>

            <section className="card" aria-labelledby="buscar-titulo">
                <h2 id="buscar-titulo">Buscar pedido</h2>
                <form className="form" aria-labelledby="buscar-titulo" noValidate onSubmit={enviar}>
                    <FormSummary mensaje={form.resumen} />
                    <FormField id="codigo" label="Código del pedido" hint="Ej: BP-100001 (viene en tu confirmación)." error={form.errores.codigo}>
                        <input {...form.campo('codigo')} type="text" autoComplete="off" />
                    </FormField>
                    <div><button className="btn" disabled={buscando}>Buscar</button></div>
                </form>
            </section>

            {cliente && (misPedidos.datos?.length ?? 0) > 0 && (
                <section className="card" aria-labelledby="mis-titulo" style={{ marginTop: 18 }}>
                    <h2 id="mis-titulo">Mis pedidos</h2>
                    <div className="table-wrap">
                        <table>
                            <caption className="visually-hidden">Mis pedidos</caption>
                            <thead>
                                <tr><th scope="col">Pedido</th><th scope="col">Fecha</th><th scope="col">Entrega</th><th scope="col">Total</th><th scope="col">Estado</th></tr>
                            </thead>
                            <tbody>
                                {misPedidos.datos!.map((p) => (
                                    <tr key={p.id}>
                                        <td><Link className="link" to={`/seguimiento?codigo=${p.codigo}`}>{p.codigo}</Link></td>
                                        <td>{FormatService.fechaHora(p.creadoEn)}</td>
                                        <td>{FormatService.fecha(p.fechaEntrega)} · {p.horario}</td>
                                        <td>{FormatService.dinero(p.total)}</td>
                                        <td><StatusBadge estado={p.estado} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            <div aria-live="polite" style={{ marginTop: 18 }}>
                {buscando && <Loader texto="Buscando pedido…" />}
                {!buscando && noEncontrado && <div className="card empty">{noEncontrado}</div>}
                {!buscando && !pedido && !noEncontrado && (
                    <div className="card empty">Escribe un código de pedido (ej: BP-100001) o elige uno de tus pedidos para ver su estado.</div>
                )}
                {!buscando && pedido && seguimiento && (
                    <article className="card">
                        <h2>Pedido {pedido.codigo}</h2>
                        <p>Estado: <StatusBadge estado={pedido.estado} /></p>
                        {pedido.estado === 'CANCELADO' || pedido.estado === 'REEMBOLSADO' ? (
                            <p className="notice error">Este pedido fue {FormatService.estadoPedido(pedido.estado).toLowerCase()}.</p>
                        ) : (
                            <Steps
                                etiqueta="Avance del pedido"
                                actual={indiceActual}
                                pasos={pasosFlujo.map((p) => FormatService.estadoPedido(p.estado))}
                            />
                        )}
                        <dl className="specs">
                            <dt>Dirección</dt><dd>{pedido.direccion}, {pedido.comuna}</dd>
                            <dt>Entrega</dt><dd>{FormatService.fecha(pedido.fechaEntrega)} · {pedido.horario}</dd>
                            <dt>Pago</dt><dd>{FormatService.etiqueta(pedido.metodoPago)}</dd>
                            <dt>Repartidor</dt><dd>{pedido.repartidorNombre ?? 'Por asignar'}</dd>
                            <dt>Productos</dt>
                            <dd>
                                {pedido.items.map((i) => `${i.nombre} (${FormatService.kg(i.kg)} kg)`).join(', ')}
                            </dd>
                            {pedido.descuento > 0 && (<><dt>Descuento</dt><dd>−{FormatService.dinero(pedido.descuento)}</dd></>)}
                            <dt>Total</dt><dd>{FormatService.dinero(pedido.total)}</dd>
                        </dl>
                        <h3>Historial</h3>
                        <ul className="mini-list">
                            {seguimiento.historial.map((h) => (
                                <li key={h.id}>
                                    {FormatService.fechaHora(h.fecha)} · {FormatService.estadoPedido(h.estado)}
                                    {h.nota ? ` — ${h.nota}` : ''}
                                </li>
                            ))}
                        </ul>
                        {CANCELABLES.includes(pedido.estado) && (cliente?.id === pedido.clienteId) && (
                            <div className="btn-row">
                                <button type="button" className="btn danger small" onClick={cancelar}>Cancelar pedido</button>
                            </div>
                        )}
                    </article>
                )}
            </div>
        </>
    );
};

export default Tracking;
