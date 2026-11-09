// components/admin/OrdersSection.tsx · Pedidos: filtros, cambio de estado, retroceso, notas (ms-pedidos)
// y asignación de cada pedido a un repartidor (solo ADMIN; ms-rutas lo deja en la ruta del repartidor).

import React, { useMemo, useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import StatusBadge from '../StatusBadge';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import PedidoService, { TRANSICIONES, puedeAsignarse } from '../../services/pedidoService';
import RutaService from '../../services/rutaService';
import RepartoService from '../../services/repartoService';
import FormatService from '../../services/formatService';
import { mensajeError } from '../../services/apiClient';
import type { EstadoPedido, Pedido, Repartidor } from '../../types';

const ESTADOS: EstadoPedido[] = ['PENDIENTE_PAGO', 'PAGADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'CANCELADO', 'REEMBOLSADO'];

type FiltroAsignacion = '' | 'SIN' | 'CON';

const OrdersSection: React.FC = () => {
    const { usuario, tieneRol } = useAuth();
    const esAdmin = tieneRol('ADMIN');
    const { mostrar } = useToast();
    const [estado, setEstado] = useState<EstadoPedido | ''>('');
    const [q, setQ] = useState('');
    const [abierto, setAbierto] = useState<number | null>(null);
    const [siguiente, setSiguiente] = useState<Record<number, EstadoPedido>>({});
    const [motivo, setMotivo] = useState('');
    const [nota, setNota] = useState('');
    const [ocupado, setOcupado] = useState(false);
    const [asignacion, setAsignacion] = useState<FiltroAsignacion>('');
    const [elegido, setElegido] = useState<Record<number, string>>({});
    const pedidos = useApi(() => PedidoService.listar({ estado }), [estado]);
    const repartidores = useApi(() => RepartoService.repartidores());

    const lista = useMemo(() => {
        const t = q.trim().toLowerCase();
        return (pedidos.datos ?? []).filter(
            (p) =>
                (!t || p.codigo.toLowerCase().includes(t) || p.nombreCliente.toLowerCase().includes(t) || p.comuna.toLowerCase().includes(t)) &&
                (asignacion === '' || (asignacion === 'SIN' ? !p.repartidorId && puedeAsignarse(p) : !!p.repartidorId)),
        );
    }, [pedidos.datos, q, asignacion]);

    const pendientesDeAsignar = (pedidos.datos ?? []).filter((p) => !p.repartidorId && puedeAsignarse(p)).length;

    const reemplazar = (p: Pedido) => pedidos.setDatos((pedidos.datos ?? []).map((x) => (x.id === p.id ? p : x)));

    const ejecutar = async (fn: () => Promise<Pedido>, exito: string) => {
        setOcupado(true);
        try {
            reemplazar(await fn());
            mostrar(exito);
            return true;
        } catch (e) {
            mostrar(mensajeError(e), 'error');
            return false;
        } finally {
            setOcupado(false);
        }
    };

    const cambiar = (p: Pedido) => {
        const nuevo = siguiente[p.id] ?? TRANSICIONES[p.estado][0];
        if (!nuevo) return;
        void ejecutar(() => PedidoService.cambiarEstado(p.id, nuevo, 'Cambio desde el panel', usuario?.id), `${p.codigo} → ${FormatService.estadoPedido(nuevo)}`);
    };

    const asignar = async (p: Pedido) => {
        const id = Number(elegido[p.id] ?? p.repartidorId ?? '');
        const r = (repartidores.datos ?? []).find((x) => x.id === id);
        if (!r) {
            mostrar(`Elige un repartidor para ${p.codigo}.`, 'error');
            return;
        }
        if (r.id === p.repartidorId) {
            mostrar(`${p.codigo} ya está asignado a ${r.nombre}.`, 'error');
            return;
        }
        const ok = await ejecutar(async () => {
            await RutaService.asignarPedido(p.id, r.id, r.nombre, usuario?.id);
            return PedidoService.obtener(p.id);
        }, `${p.codigo} asignado a ${r.nombre}`);
        if (ok) setElegido((e) => { const n = { ...e }; delete n[p.id]; return n; });
    };

    const retroceder = async (p: Pedido) => {
        if (motivo.trim().length < 3) {
            mostrar('Escribe el motivo del retroceso (mínimo 3 caracteres).', 'error');
            return;
        }
        if (await ejecutar(() => PedidoService.retroceder(p.id, motivo.trim(), usuario?.id), `${p.codigo} volvió al estado anterior`)) setMotivo('');
    };

    const agregarNota = async (p: Pedido) => {
        if (nota.trim().length < 3) {
            mostrar('La nota debe tener al menos 3 caracteres.', 'error');
            return;
        }
        try {
            await PedidoService.agregarNota(p.id, nota.trim(), usuario?.id);
            reemplazar(await PedidoService.obtener(p.id));
            setNota('');
            mostrar('Nota agregada');
        } catch (e) {
            mostrar(mensajeError(e), 'error');
        }
    };

    return (
        <section aria-labelledby="pedidos-titulo">
            <div className="section-title"><h1 id="pedidos-titulo">Pedidos</h1><span className="tag">EP14 · EP20</span></div>
            <div className="toolbar">
                <div className="field">
                    <label htmlFor="f-estado">Estado</label>
                    <select id="f-estado" className="select" value={estado} onChange={(e) => setEstado(e.target.value as EstadoPedido | '')}>
                        <option value="">Todos</option>
                        {ESTADOS.map((s) => <option key={s} value={s}>{FormatService.estadoPedido(s)}</option>)}
                    </select>
                </div>
                <div className="field">
                    <label htmlFor="f-q">Buscar</label>
                    <input id="f-q" className="input" type="search" placeholder="Código, cliente o comuna" value={q} onChange={(e) => setQ(e.target.value)} />
                </div>
                <div className="field">
                    <label htmlFor="f-asig">Repartidor</label>
                    <select id="f-asig" className="select" value={asignacion} onChange={(e) => setAsignacion(e.target.value as FiltroAsignacion)}>
                        <option value="">Todos</option>
                        <option value="SIN">Sin asignar</option>
                        <option value="CON">Asignados</option>
                    </select>
                </div>
                <button type="button" className="btn secondary" onClick={() => pedidos.recargar()}>Actualizar</button>
            </div>
            {pendientesDeAsignar > 0 && (
                <p className="notice warning" role="status">
                    Hay {pendientesDeAsignar} pedido{pendientesDeAsignar === 1 ? '' : 's'} sin repartidor.{' '}
                    {esAdmin ? 'Asígnalos en la columna «Repartidor».' : 'Solo el administrador puede asignarlos.'}
                </p>
            )}
            <ErrorNotice mensaje={repartidores.error} onReintentar={repartidores.recargar} />
            <ErrorNotice mensaje={pedidos.error} onReintentar={pedidos.recargar} />
            {pedidos.cargando ? (
                <Loader />
            ) : (
                <div className="table-wrap">
                    <table>
                        <caption className="visually-hidden">Pedidos</caption>
                        <thead>
                            <tr>
                                <th scope="col">Pedido</th><th scope="col">Cliente</th><th scope="col">Comuna</th><th scope="col">Entrega</th>
                                <th scope="col">Total</th><th scope="col">Estado</th><th scope="col">Repartidor</th><th scope="col">Acción</th>
                            </tr>
                        </thead>
                        <tbody>
                            {lista.map((p) => {
                                const opciones = TRANSICIONES[p.estado];
                                return (
                                    <React.Fragment key={p.id}>
                                        <tr>
                                            <td>
                                                <button type="button" className="link" aria-expanded={abierto === p.id} onClick={() => setAbierto(abierto === p.id ? null : p.id)}>
                                                    {p.codigo}
                                                </button>
                                            </td>
                                            <td>{p.nombreCliente}</td>
                                            <td>{p.comuna}</td>
                                            <td>{FormatService.fecha(p.fechaEntrega)}<br /><small className="muted">{p.horario}</small></td>
                                            <td>{FormatService.dinero(p.total)}</td>
                                            <td><StatusBadge estado={p.estado} /></td>
                                            <td>
                                                <CeldaRepartidor
                                                    pedido={p}
                                                    esAdmin={esAdmin}
                                                    repartidores={repartidores.datos ?? []}
                                                    valor={elegido[p.id] ?? (p.repartidorId ? String(p.repartidorId) : '')}
                                                    onElegir={(v) => setElegido({ ...elegido, [p.id]: v })}
                                                    onAsignar={() => void asignar(p)}
                                                    ocupado={ocupado}
                                                />
                                            </td>
                                            <td>
                                                {opciones.length ? (
                                                    <div className="qty">
                                                        <label className="visually-hidden" htmlFor={`sig-${p.id}`}>Nuevo estado de {p.codigo}</label>
                                                        <select
                                                            id={`sig-${p.id}`}
                                                            className="select small"
                                                            value={siguiente[p.id] ?? opciones[0]}
                                                            onChange={(e) => setSiguiente({ ...siguiente, [p.id]: e.target.value as EstadoPedido })}
                                                        >
                                                            {opciones.map((o) => <option key={o} value={o}>{FormatService.estadoPedido(o)}</option>)}
                                                        </select>
                                                        <button type="button" className="btn secondary small" disabled={ocupado} onClick={() => cambiar(p)}>Cambiar</button>
                                                    </div>
                                                ) : '—'}
                                            </td>
                                        </tr>
                                        {abierto === p.id && (
                                            <tr className="detail-row">
                                                <td colSpan={8}>
                                                    <div className="grid cols-2">
                                                        <div>
                                                            <h3>Detalle</h3>
                                                            <ul className="mini-list">
                                                                {p.items.map((i) => <li key={i.id}>{i.nombre} · {FormatService.kg(i.kg)} kg · {FormatService.dinero(i.subtotal)}</li>)}
                                                            </ul>
                                                            <p>{p.direccion}, {p.comuna}{p.referencia ? ` (${p.referencia})` : ''} · {p.telefono ?? 'sin teléfono'}</p>
                                                            {p.comentario && <p className="muted">Comentario: {p.comentario}</p>}
                                                            <p>Pago: {FormatService.etiqueta(p.metodoPago)} {p.codigoCupon && `· Cupón ${p.codigoCupon}`}</p>
                                                            <p>
                                                                Repartidor: {p.repartidorNombre
                                                                    ? <><strong>{p.repartidorNombre}</strong>{p.asignadoEn ? ` (asignado ${FormatService.fechaHora(p.asignadoEn)})` : ''}</>
                                                                    : <span className="muted">sin asignar</span>}
                                                            </p>
                                                            <h3>Historial</h3>
                                                            <ul className="mini-list">
                                                                {p.historial.map((h) => (
                                                                    <li key={h.id}>{FormatService.fechaHora(h.fecha)} · {h.esReversion ? '↩ ' : ''}{FormatService.estadoPedido(h.estado)}{h.nota ? ` — ${h.nota}` : ''}</li>
                                                                ))}
                                                            </ul>
                                                            <div className="inline-form">
                                                                <div className="field">
                                                                    <label htmlFor={`mot-${p.id}`}>Motivo para retroceder</label>
                                                                    <input id={`mot-${p.id}`} className="input small" value={motivo} maxLength={300} onChange={(e) => setMotivo(e.target.value)} />
                                                                </div>
                                                                <button type="button" className="btn warning small" disabled={ocupado || p.historial.length < 2} onClick={() => retroceder(p)}>Retroceder estado</button>
                                                            </div>
                                                        </div>
                                                        <div>
                                                            <h3>Notas internas</h3>
                                                            <ul className="mini-list">
                                                                {p.notas.map((n) => <li key={n.id}>{FormatService.fechaHora(n.fecha)} · {n.texto}</li>)}
                                                                {p.notas.length === 0 && <li className="muted">Sin notas.</li>}
                                                            </ul>
                                                            <div className="inline-form">
                                                                <div className="field">
                                                                    <label htmlFor={`nota-${p.id}`}>Nueva nota</label>
                                                                    <input id={`nota-${p.id}`} className="input small" value={nota} maxLength={500} onChange={(e) => setNota(e.target.value)} />
                                                                </div>
                                                                <button type="button" className="btn small" onClick={() => agregarNota(p)}>Agregar nota</button>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                );
                            })}
                            {lista.length === 0 && <tr><td colSpan={8} className="muted">No hay pedidos con esos filtros.</td></tr>}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
};

/* ---------- Celda «Repartidor»: el admin elige y asigna; el resto solo ve el nombre ---------- */

interface CeldaRepartidorProps {
    pedido: Pedido;
    esAdmin: boolean;
    repartidores: Repartidor[];
    valor: string;
    onElegir: (v: string) => void;
    onAsignar: () => void;
    ocupado: boolean;
}

const CeldaRepartidor: React.FC<CeldaRepartidorProps> = ({ pedido: p, esAdmin, repartidores, valor, onElegir, onAsignar, ocupado }) => {
    if (!esAdmin || !puedeAsignarse(p)) {
        return p.repartidorNombre ? <span>{p.repartidorNombre}</span> : <span className="muted">—</span>;
    }
    return (
        <div className="input-group">
            <label className="visually-hidden" htmlFor={`rep-${p.id}`}>Repartidor de {p.codigo}</label>
            <select id={`rep-${p.id}`} className="select small" value={valor} onChange={(e) => onElegir(e.target.value)}>
                <option value="">{p.repartidorId ? '' : 'Sin asignar'}</option>
                {repartidores.map((r) => (
                    <option key={r.id} value={r.id} disabled={r.disponibilidad === 'FUERA_SERVICIO'}>
                        {r.nombre}{r.disponibilidad === 'FUERA_SERVICIO' ? ' (fuera de servicio)' : ''}
                    </option>
                ))}
            </select>
            <button type="button" className="btn small" disabled={ocupado || !valor || Number(valor) === p.repartidorId} onClick={onAsignar}>
                {p.repartidorId ? 'Reasignar' : 'Asignar'}
            </button>
        </div>
    );
};

export default OrdersSection;
