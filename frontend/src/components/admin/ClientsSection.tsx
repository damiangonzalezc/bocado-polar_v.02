// components/admin/ClientsSection.tsx · Directorio CRM con segmentos y notas de soporte (ms-clientes)

import React, { useMemo, useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import ClienteService from '../../services/clienteService';
import FormatService from '../../services/formatService';
import { mensajeError } from '../../services/apiClient';
import type { SegmentoCliente, TipoNota } from '../../types';

const ClientsSection: React.FC = () => {
    const { usuario } = useAuth();
    const { mostrar } = useToast();
    const [segmento, setSegmento] = useState<SegmentoCliente | ''>('');
    const [q, setQ] = useState('');
    const [elegido, setElegido] = useState<number | null>(null);
    const [nota, setNota] = useState<{ tipo: TipoNota; texto: string }>({ tipo: 'CONSULTA', texto: '' });
    const clientes = useApi(() => ClienteService.listar({ segmento }), [segmento]);

    const lista = useMemo(() => {
        const t = q.trim().toLowerCase();
        return (clientes.datos ?? []).filter((c) => !t || `${c.nombre} ${c.apellido ?? ''} ${c.email}`.toLowerCase().includes(t));
    }, [clientes.datos, q]);
    const cliente = lista.find((c) => c.id === elegido) ?? null;

    const guardarNota = async () => {
        if (!cliente) return;
        if (nota.texto.trim().length < 3) return mostrar('La nota debe tener al menos 3 caracteres.', 'error');
        try {
            await ClienteService.agregarNota(cliente.id, nota.tipo, nota.texto.trim(), usuario?.id);
            setNota({ tipo: 'CONSULTA', texto: '' });
            mostrar('Nota registrada');
            void clientes.recargar();
        } catch (e) {
            mostrar(mensajeError(e), 'error');
        }
    };

    return (
        <section aria-labelledby="crm-titulo">
            <div className="section-title"><h1 id="crm-titulo">Clientes (CRM)</h1><span className="tag">Segmentos VIP · Frecuente · Nuevo</span></div>
            <div className="toolbar">
                <div className="field">
                    <label htmlFor="c-seg">Segmento</label>
                    <select id="c-seg" className="select" value={segmento} onChange={(e) => setSegmento(e.target.value as SegmentoCliente | '')}>
                        <option value="">Todos</option>
                        <option value="VIP">VIP</option>
                        <option value="FRECUENTE">Frecuente</option>
                        <option value="NUEVO">Nuevo</option>
                    </select>
                </div>
                <div className="field">
                    <label htmlFor="c-q">Buscar</label>
                    <input id="c-q" className="input" type="search" placeholder="Nombre o correo" value={q} onChange={(e) => setQ(e.target.value)} />
                </div>
            </div>
            <ErrorNotice mensaje={clientes.error} onReintentar={clientes.recargar} />
            {clientes.cargando ? <Loader /> : (
                <div className="grid cart-layout">
                    <div className="table-wrap">
                        <table>
                            <thead><tr><th scope="col">Cliente</th><th scope="col">Segmento</th><th scope="col">Pedidos</th><th scope="col">Gasto total</th></tr></thead>
                            <tbody>
                                {lista.map((c) => (
                                    <tr key={c.id}>
                                        <td>
                                            <button type="button" className="link" onClick={() => setElegido(c.id)}>{c.nombre} {c.apellido}</button>
                                            <br /><small className="muted">{c.email}</small>
                                        </td>
                                        <td><span className={`tag${c.segmento === 'VIP' ? ' warning' : ''}`}>{FormatService.etiqueta(c.segmento)}</span></td>
                                        <td>{c.totalPedidos}</td>
                                        <td>{FormatService.dinero(c.gastoTotal)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <aside className="card" aria-label="Ficha del cliente">
                        {!cliente ? <p className="muted">Elige un cliente para ver su ficha.</p> : (
                            <>
                                <h2>{cliente.nombre} {cliente.apellido}</h2>
                                <p>{cliente.email}<br />{cliente.telefono ?? 'Sin teléfono'}</p>
                                <h3>Direcciones</h3>
                                <ul className="mini-list">
                                    {cliente.direcciones.map((d) => <li key={d.id}>{d.alias}: {d.calle}, {d.comuna}{d.principal ? ' (principal)' : ''}</li>)}
                                </ul>
                                {cliente.preferencia?.alergias && <p className="notice warning">Alergias: {cliente.preferencia.alergias}</p>}
                                <h3>Notas</h3>
                                <ul className="mini-list">
                                    {cliente.notas.map((n) => <li key={n.id}>{FormatService.fecha(n.fecha)} · <strong>{FormatService.etiqueta(n.tipo)}</strong>: {n.texto}</li>)}
                                    {cliente.notas.length === 0 && <li className="muted">Sin notas.</li>}
                                </ul>
                                <div className="form">
                                    <div className="field">
                                        <label htmlFor="n-tipo">Tipo de nota</label>
                                        <select id="n-tipo" className="select" value={nota.tipo} onChange={(e) => setNota({ ...nota, tipo: e.target.value as TipoNota })}>
                                            <option value="CONSULTA">Consulta</option>
                                            <option value="RECLAMO">Reclamo</option>
                                            <option value="DEVOLUCION">Devolución</option>
                                            <option value="INCIDENCIA">Incidencia</option>
                                        </select>
                                    </div>
                                    <div className="field">
                                        <label htmlFor="n-texto">Detalle</label>
                                        <textarea id="n-texto" className="input textarea" maxLength={500} value={nota.texto} onChange={(e) => setNota({ ...nota, texto: e.target.value })} />
                                    </div>
                                    <button type="button" className="btn" onClick={guardarNota}>Registrar nota</button>
                                </div>
                            </>
                        )}
                    </aside>
                </div>
            )}
        </section>
    );
};

export default ClientsSection;
