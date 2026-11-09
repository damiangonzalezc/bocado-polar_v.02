// components/admin/RoutesSection.tsx · Rutas de despacho, paradas y repartidor (ms-rutas / ms-reparto)

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import RutaService from '../../services/rutaService';
import type { AccionRuta } from '../../services/rutaService';
import RepartoService from '../../services/repartoService';
import ConfigService from '../../services/configService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { erroresDeCampo, mensajeError } from '../../services/apiClient';
import type { EstadoRuta, Ruta } from '../../types';

const CLASE_RUTA: Record<EstadoRuta, string> = { PLANIFICADA: 'pending', EN_CURSO: 'pending', FINALIZADA: 'ok', CANCELADA: 'bad' };

const RoutesSection: React.FC = () => {
    const { mostrar } = useToast();
    const [fecha, setFecha] = useState(FormatService.fechaEnDias(0));
    const [abierta, setAbierta] = useState<number | null>(null);
    const [crear, setCrear] = useState(false);
    const [comunas, setComunas] = useState<string[]>([]);
    const rutas = useApi(() => RutaService.listar(fecha), [fecha]);
    const repartidores = useApi(() => RepartoService.repartidores());
    const vehiculos = useApi(() => RutaService.vehiculos());
    const zonas = useApi(() => ConfigService.zonas());
    const V = ValidationService;

    const form = useForm(
        {
            nombre: { reglas: [V.requerido('El nombre'), V.largo(3, 80)] },
            fecha: { reglas: [V.requerido('La fecha'), V.fechaFutura()] },
            horaSalida: { reglas: [V.requerido('La hora de salida'), V.hora()] },
            repartidorId: { reglas: [V.requerido('El repartidor')] },
            vehiculoId: { reglas: [V.requerido('El vehículo')] },
            notas: { reglas: [V.largo(0, 300)] },
        },
        { nombre: '', fecha: FormatService.fechaEnDias(0), horaSalida: '08:30', repartidorId: '', vehiculoId: '', notas: '' },
        'ruta-',
    );

    const accion = async (r: Ruta, a: AccionRuta | 'generar') => {
        try {
            const nueva = a === 'generar' ? await RutaService.generarParadas(r.id) : await RutaService.accion(r.id, a);
            rutas.setDatos((rutas.datos ?? []).map((x) => (x.id === nueva.id ? nueva : x)));
            mostrar(a === 'generar' ? `${nueva.paradas.length} parada(s) en ${nueva.codigo}` : `Ruta ${nueva.codigo}: ${FormatService.etiqueta(nueva.estado)}`);
            if (a === 'generar') setAbierta(r.id);
        } catch (e) {
            mostrar(mensajeError(e), 'error');
        }
    };

    const guardar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        if (comunas.length === 0) {
            form.setResumen('Elige al menos una comuna para la ruta.');
            return;
        }
        const v = form.valores;
        const rep = repartidores.datos?.find((r) => String(r.id) === v.repartidorId);
        try {
            const r = await RutaService.crear({
                nombre: v.nombre.trim(),
                fecha: v.fecha,
                comunas,
                horaSalida: v.horaSalida,
                repartidorId: Number(v.repartidorId),
                repartidorNombre: rep?.nombre,
                vehiculoId: Number(v.vehiculoId),
                notas: v.notas.trim() || undefined,
            });
            mostrar(`Ruta ${r.codigo} creada`);
            setCrear(false);
            setComunas([]);
            form.reiniciar();
            setFecha(r.fecha);
            void rutas.recargar();
        } catch (err) {
            form.erroresServidor(erroresDeCampo(err), mensajeError(err));
        }
    };

    return (
        <section aria-labelledby="rutas-titulo">
            <div className="section-title">
                <h1 id="rutas-titulo">Rutas</h1>
                <button type="button" className="btn" onClick={() => setCrear(!crear)}>Nueva ruta</button>
            </div>

            {crear && (
                <form className="card form" noValidate onSubmit={guardar} aria-label="Nueva ruta">
                    <h2>Nueva ruta</h2>
                    <FormSummary mensaje={form.resumen} />
                    <div className="two">
                        <FormField id="ruta-nombre" label="Nombre" hint="Ej: Concepción → Chiguayante" error={form.errores.nombre}><input {...form.campo('nombre')} /></FormField>
                        <FormField id="ruta-fecha" label="Fecha" error={form.errores.fecha}><input {...form.campo('fecha')} type="date" min={FormatService.fechaEnDias(0)} /></FormField>
                        <FormField id="ruta-horaSalida" label="Hora de salida" hint="HH:MM" error={form.errores.horaSalida}><input {...form.campo('horaSalida')} type="time" /></FormField>
                        <FormField id="ruta-repartidorId" label="Repartidor a cargo" error={form.errores.repartidorId}>
                            <select {...form.campo('repartidorId', 'select')}>
                                <option value="">Selecciona…</option>
                                {(repartidores.datos ?? []).map((r) => <option key={r.id} value={r.id}>{r.nombre} · {FormatService.etiqueta(r.disponibilidad)}</option>)}
                            </select>
                        </FormField>
                        <FormField id="ruta-vehiculoId" label="Vehículo" error={form.errores.vehiculoId}>
                            <select {...form.campo('vehiculoId', 'select')}>
                                <option value="">Selecciona…</option>
                                {(vehiculos.datos ?? []).filter((v) => v.activo).map((v) => <option key={v.id} value={v.id}>{v.tipo} · {v.patente}</option>)}
                            </select>
                        </FormField>
                        <FormField id="ruta-notas" label="Notas" error={form.errores.notas}><input {...form.campo('notas')} /></FormField>
                    </div>
                    <fieldset>
                        <legend>Comunas</legend>
                        {(zonas.datos ?? []).map((z) => (
                            <label key={z.id} className="option">
                                <input
                                    type="checkbox"
                                    checked={comunas.includes(z.comuna)}
                                    onChange={(e) => setComunas(e.target.checked ? [...comunas, z.comuna] : comunas.filter((c) => c !== z.comuna))}
                                />{' '}
                                {z.comuna}
                            </label>
                        ))}
                    </fieldset>
                    <div className="btn-row">
                        <button className="btn">Crear ruta</button>
                        <button type="button" className="btn secondary" onClick={() => setCrear(false)}>Cancelar</button>
                    </div>
                </form>
            )}

            <div className="toolbar">
                <div className="field">
                    <label htmlFor="r-fecha">Fecha</label>
                    <input id="r-fecha" className="input" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
                </div>
            </div>
            <ErrorNotice mensaje={rutas.error} onReintentar={rutas.recargar} />
            {rutas.cargando ? <Loader /> : (
                <div className="grid">
                    {(rutas.datos ?? []).map((r) => (
                        <article key={r.id} className="card">
                            <h3>Ruta {r.codigo}</h3>
                            <p>{r.nombre}<br /><small className="muted">{r.comunas.join(' · ')} · salida {FormatService.hora(r.horaSalida)}</small></p>
                            <p>{r.repartidorNombre ?? 'Sin repartidor'} · {r.vehiculo ? `${r.vehiculo.tipo} ${r.vehiculo.patente}` : 'sin vehículo'}</p>
                            <p className={`status ${CLASE_RUTA[r.estado]}`}>
                                {FormatService.etiqueta(r.estado)} · {r.paradasPendientes} parada{r.paradasPendientes === 1 ? '' : 's'} pendiente{r.paradasPendientes === 1 ? '' : 's'}
                            </p>
                            <div className="btn-row">
                                {(r.estado === 'PLANIFICADA' || r.estado === 'EN_CURSO') && (
                                    <>
                                        <button type="button" className="btn secondary small" onClick={() => accion(r, 'generar')}>Cargar pedidos</button>
                                        <button type="button" className="btn secondary small" onClick={() => accion(r, 'optimizar')}>Ordenar paradas</button>
                                    </>
                                )}
                                {r.estado === 'PLANIFICADA' && <button type="button" className="btn small" onClick={() => accion(r, 'iniciar')}>Iniciar</button>}
                                {r.estado === 'EN_CURSO' && <button type="button" className="btn small" onClick={() => accion(r, 'finalizar')}>Finalizar</button>}
                                {(r.estado === 'PLANIFICADA' || r.estado === 'EN_CURSO') && (
                                    <button type="button" className="btn danger small" onClick={() => window.confirm(`¿Cancelar la ruta ${r.codigo}?`) && accion(r, 'cancelar')}>Cancelar</button>
                                )}
                                <button type="button" className="link" aria-expanded={abierta === r.id} onClick={() => setAbierta(abierta === r.id ? null : r.id)}>
                                    {abierta === r.id ? 'Ocultar paradas' : `Ver paradas (${r.paradas.length})`}
                                </button>
                            </div>
                            {abierta === r.id && (
                                <ol className="mini-list">
                                    {r.paradas.map((p) => (
                                        <li key={p.id}>
                                            <strong>{p.codigoPedido ?? `#${p.pedidoId}`}</strong> · {p.direccion}, {p.comuna} · {p.horario}{' '}
                                            <span className={`status ${p.estado === 'ENTREGADA' ? 'ok' : p.estado === 'FALLIDA' ? 'bad' : 'pending'}`}>{FormatService.etiqueta(p.estado)}</span>
                                            {p.cobrarAlEntregar && <span className="tag warning">Cobrar {FormatService.dinero(p.montoACobrar)}</span>}
                                        </li>
                                    ))}
                                    {r.paradas.length === 0 && <li className="muted">Sin paradas. Usa “Cargar pedidos”.</li>}
                                </ol>
                            )}
                        </article>
                    ))}
                    {rutas.datos?.length === 0 && <p className="card empty">No hay rutas para esta fecha.</p>}
                </div>
            )}
        </section>
    );
};

export default RoutesSection;
