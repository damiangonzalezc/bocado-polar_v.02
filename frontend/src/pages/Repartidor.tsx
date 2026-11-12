// pages/Repartidor.tsx · Ruta del día del repartidor (EP19)
// Turno con temperatura de salida, cuenta regresiva de 90 min (ms-frio), entregas con evidencia
// (foto o firma) y cobro contra entrega, entregas fallidas y cierre de caja (ms-reparto).

import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import ThemeToggle from '../components/ThemeToggle';
import Loader from '../components/Loader';
import SignaturePad from '../components/SignaturePad';
import { useAuth } from '../hooks/useAuth';
import { useForm } from '../hooks/useForm';
import { useToast } from '../hooks/useToast';
import RepartoService from '../services/repartoService';
import RutaService from '../services/rutaService';
import FrioService from '../services/frioService';
import ValidationService from '../services/validationService';
import FormatService from '../services/formatService';
import { estadoHttp, mensajeError } from '../services/apiClient';
import type { CierreCaja, CuentaRegresiva, Parada, Ruta, TipoCobro, TipoIncidencia, Turno } from '../types';
import '../styles/repartidor.css';

const MAX_FOTO_BYTES = 2 * 1024 * 1024;

const Repartidor: React.FC = () => {
    const { usuario, logout } = useAuth();
    const { mostrar } = useToast();
    const [turno, setTurno] = useState<Turno | null>(null);
    const [ruta, setRuta] = useState<Ruta | null>(null);
    const [proximas, setProximas] = useState<Ruta[]>([]);
    // Turno que quedó abierto de un día anterior: no tiene sentido mostrar la cuenta regresiva.
    const [turnoViejo, setTurnoViejo] = useState(false);
    const [cuenta, setCuenta] = useState<CuentaRegresiva | null>(null);
    const [cierre, setCierre] = useState<CierreCaja | null>(null);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [abierta, setAbierta] = useState<{ id: number; modo: 'entregar' | 'fallida' } | null>(null);
    const repartidorId = usuario?.id ?? 0;

    const cargar = useCallback(async () => {
        try {
            const t = await RepartoService.turnoActivo(repartidorId).catch((e) => (estadoHttp(e) === 404 ? null : Promise.reject(e)));
            setTurno(t);
            setTurnoViejo(!!t && new Date(t.inicio).toDateString() !== new Date().toDateString());
            const r = await RutaService.rutaDelDia(repartidorId).catch((e) => (estadoHttp(e) === 404 ? null : Promise.reject(e)));
            setRuta(r);
            setProximas(await RutaService.proximas(repartidorId).catch(() => []));
            setCuenta(t ? await FrioService.cuentaRegresiva(t.id).catch(() => null) : null);
            setError('');
        } catch (e) {
            setError(mensajeError(e));
        } finally {
            setCargando(false);
        }
    }, [repartidorId]);

    useEffect(() => {
        // Carga asíncrona: el estado se actualiza después de responder el backend.
        // oxlint-disable-next-line react/set-state-in-effect
        void cargar();
    }, [cargar]);

    // La cuenta regresiva se actualiza cada minuto mientras haya turno.
    useEffect(() => {
        if (!turno) return;
        const t = setInterval(() => {
            FrioService.cuentaRegresiva(turno.id).then(setCuenta).catch(() => undefined);
        }, 60000);
        return () => clearInterval(t);
    }, [turno]);

    if (cargando) return <Shell onSalir={logout}><Loader texto="Cargando tu ruta…" /></Shell>;

    const paradas = ruta?.paradas ?? [];

    const cuantas = (estado: Parada['estado']) => paradas.filter((p) => p.estado === estado).length;

    return (
        <Shell onSalir={logout}>
            <h1>Ruta del día</h1>
            {error && <p className="notice error" role="alert">{error} <button type="button" className="link" onClick={cargar}>Reintentar</button></p>}

            {cierre && <ResumenCierre cierre={cierre} />}

            {!turno && !cierre && <IniciarTurno repartidorId={repartidorId} ruta={ruta} onIniciado={(t) => { setTurno(t); void cargar(); }} />}

            {turno && (
                <>
                    <p className={`notice ${turno.alertaTemperatura ? 'error' : ''}`}>
                        {ruta ? `Ruta ${ruta.codigo} · ${ruta.vehiculo?.tipo ?? 'Vehículo refrigerado'} ${ruta.vehiculo?.patente ?? ''}` : 'Sin ruta asignada hoy'}
                        {' · '}caja a {turno.tempCajaInicial}°C{turno.alertaTemperatura && ' (sobre el umbral RN-01: revisa la caja)'}
                    </p>
                    {turnoViejo && (
                        <p className="notice warning" role="status">
                            Tienes un turno abierto desde el {FormatService.fechaHora(turno.inicio)}. Ciérralo con «Cerrar turno y rendir caja» y luego inicia el de hoy.
                        </p>
                    )}
                    {cuenta && !turnoViejo && (
                        <p className={`notice ${cuenta.excedida ? 'error' : cuenta.debeAvisar ? 'warning' : 'success'}`} role="status">
                            {cuenta.excedida
                                ? `Se superaron los ${cuenta.limiteMin} min de ruta: riesgo para la cadena de frío.`
                                : `Quedan ${cuenta.minutosRestantes} min de los ${cuenta.limiteMin} permitidos en ruta.`}
                        </p>
                    )}
                </>
            )}

            <div className="driver-stats" aria-label="Resumen de entregas">
                <div><div className="metric">{cuantas('PENDIENTE')}</div><small>Pendientes</small></div>
                <div><div className="metric">{cuantas('ENTREGADA')}</div><small>Entregados</small></div>
                <div><div className="metric">{cuantas('FALLIDA')}</div><small>Fallidos</small></div>
            </div>

            <section aria-label="Entregas asignadas">
                {paradas.length === 0 && <p className="empty">No hay entregas asignadas por ahora.</p>}
                {paradas.map((p) => (
                    <article key={p.id} className={`route-item${p.estado !== 'PENDIENTE' ? ' done' : ''}`}>
                        <h3>{p.orden}. {p.codigoPedido ?? `Pedido ${p.pedidoId}`}</h3>
                        <p>
                            {p.cliente && <><strong>{p.cliente}</strong><br /></>}
                            {p.direccion}, {p.comuna}
                            <br />
                            <small className="muted">{p.horario} {p.referencia && `· ${p.referencia}`} {p.telefono && `· ${p.telefono}`}</small>
                        </p>
                        {p.cobrarAlEntregar && p.estado === 'PENDIENTE' && <p className="tag warning">Cobrar {FormatService.dinero(p.montoACobrar)}</p>}
                        {p.estado === 'ENTREGADA' && <p className="status ok">✔ Entregado</p>}
                        {p.estado === 'FALLIDA' && <p className="status bad">✖ No entregado: {p.motivoFalla}</p>}
                        {p.estado === 'PENDIENTE' && turno && (
                            abierta?.id === p.id ? (
                                abierta.modo === 'entregar' ? (
                                    <FormEntrega parada={p} turno={turno} repartidorId={repartidorId} onCancelar={() => setAbierta(null)} onListo={() => { setAbierta(null); mostrar('Entrega registrada'); void cargar(); }} />
                                ) : (
                                    <FormFallida parada={p} turno={turno} repartidorId={repartidorId} onCancelar={() => setAbierta(null)} onListo={() => { setAbierta(null); mostrar('Incidencia registrada', 'error'); void cargar(); }} />
                                )
                            ) : (
                                <>
                                    <button type="button" className="btn warning driver-btn" onClick={() => setAbierta({ id: p.id, modo: 'entregar' })}>Registrar entrega</button>
                                    <button type="button" className="btn secondary driver-btn" onClick={() => setAbierta({ id: p.id, modo: 'fallida' })}>No se pudo entregar</button>
                                </>
                            )
                        )}
                        {p.estado === 'PENDIENTE' && !turno && <p className="muted">Inicia tu turno para registrar entregas.</p>}
                    </article>
                ))}
            </section>

            {proximas.length > 0 && (
                <section aria-labelledby="proximas-titulo">
                    <h2 id="proximas-titulo">Próximas entregas</h2>
                    <p className="muted">Pedidos que el administrador ya te asignó. Podrás registrarlos el día de la entrega.</p>
                    {proximas.map((r) => (
                        <div key={r.id} className="card">
                            <h3>{FormatService.fecha(r.fecha)} · Ruta {r.codigo}</h3>
                            <p className="muted">
                                Salida {r.horaSalida?.slice(0, 5)} · {r.vehiculo ? `${r.vehiculo.tipo} ${r.vehiculo.patente}` : 'vehículo por asignar'} · {r.paradas.length} parada{r.paradas.length === 1 ? '' : 's'}
                            </p>
                            {r.paradas.map((p) => (
                                <article key={p.id} className="route-item">
                                    <h3>{p.orden}. {p.codigoPedido ?? `Pedido ${p.pedidoId}`}</h3>
                                    <p>
                                        {p.cliente && <><strong>{p.cliente}</strong><br /></>}
                                        {p.direccion}, {p.comuna}
                                        <br />
                                        <small className="muted">{p.horario} {p.referencia && `· ${p.referencia}`}</small>
                                    </p>
                                    {p.cobrarAlEntregar && <p className="tag warning">Cobrar {FormatService.dinero(p.montoACobrar)}</p>}
                                </article>
                            ))}
                        </div>
                    ))}
                </section>
            )}

            {turno && <CerrarTurno turno={turno} onCerrado={(c) => { setCierre(c); setTurno(null); void cargar(); }} />}
            <Link className="btn secondary driver-btn" to="/">Volver al portal</Link>
        </Shell>
    );
};

/* ---------------- Piezas de la vista ---------------- */

const Shell: React.FC<{ children: React.ReactNode; onSalir: () => Promise<void> }> = ({ children, onSalir }) => (
    <>
        <a className="skip-link" href="#contenido">Saltar al contenido</a>
        <header className="driver-header">
            <Link className="brand" to="/"><img src="/img/logo.svg" alt="" width={32} height={32} /> BOCADO_Polar · Repartidor</Link>
            <div className="auth-slot">
                <ThemeToggle />
                <button type="button" className="btn secondary small" onClick={() => void onSalir()}>Salir</button>
            </div>
        </header>
        <main id="contenido" className="container">
            <div className="mobile-card">{children}</div>
        </main>
    </>
);

const IniciarTurno: React.FC<{ repartidorId: number; ruta: Ruta | null; onIniciado: (t: Turno) => void }> = ({ repartidorId, ruta, onIniciado }) => {
    const { mostrar } = useToast();
    const [enviando, setEnviando] = useState(false);
    const form = useForm(
        { temp: { reglas: [ValidationService.requerido('La temperatura de la caja'), ValidationService.rango(-40, 15, '°C')] } },
        { temp: '-18' },
        'turno-',
    );

    const enviar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        setEnviando(true);
        try {
            const t = await RepartoService.iniciarTurno(repartidorId, ValidationService.numero(form.valores.temp));
            mostrar(t.alertaTemperatura ? 'Turno iniciado con alerta de temperatura' : 'Turno iniciado: ¡buen viaje!', t.alertaTemperatura ? 'error' : 'ok');
            onIniciado(t);
        } catch (err) {
            form.setResumen(mensajeError(err));
        } finally {
            setEnviando(false);
        }
    };

    return (
        <form className="form card" noValidate onSubmit={enviar} aria-label="Iniciar turno">
            <h2>Iniciar turno</h2>
            <p className="muted">{ruta ? `Ruta asignada: ${ruta.codigo} · ${ruta.nombre} (${ruta.paradas.length} paradas)` : 'No tienes ruta asignada hoy.'}</p>
            <FormSummary mensaje={form.resumen} />
            <FormField id="turno-temp" label="Temperatura de la caja al salir (°C)" hint="RN-01: debe estar a -15 °C o menos." error={form.errores.temp}>
                <input {...form.campo('temp')} type="number" step={0.5} />
            </FormField>
            <button className="btn driver-btn" disabled={enviando}>{enviando ? 'Iniciando…' : 'Iniciar ruta'}</button>
        </form>
    );
};

interface FormParadaProps {
    parada: Parada;
    turno: Turno;
    repartidorId: number;
    onCancelar: () => void;
    onListo: () => void;
}

const FormEntrega: React.FC<FormParadaProps> = ({ parada, turno, repartidorId, onCancelar, onListo }) => {
    const [foto, setFoto] = useState('');
    const [firma, setFirma] = useState('');
    const [enviando, setEnviando] = useState(false);
    const V = ValidationService;
    const monto = parada.montoACobrar ?? 0;
    const pre = `ent-${parada.id}-`;

    const form = useForm(
        {
            recibidoPor: { reglas: [V.requerido('Quién recibe'), V.texto(2, 80)] },
            temp: { reglas: [V.rango(-40, 15, '°C')] },
            tipoCobro: { reglas: [] },
            pagaCon: {
                reglas: parada.cobrarAlEntregar
                    ? [(v, vals) => {
                        if (vals.tipoCobro !== 'EFECTIVO') return '';
                        if (!v.trim()) return 'Indica con cuánto paga el cliente.';
                        const n = V.numero(v);
                        return Number.isNaN(n) || n < monto ? `Debe ser igual o mayor a ${FormatService.dinero(monto)}.` : '';
                    }]
                    : [],
            },
        },
        { recibidoPor: parada.cliente ?? '', temp: '-18', tipoCobro: 'EFECTIVO', pagaCon: '' },
        pre,
    );

    const leerFoto = (e: React.ChangeEvent<HTMLInputElement>) => {
        const archivo = e.target.files?.[0];
        if (!archivo) return setFoto('');
        if (!archivo.type.startsWith('image/')) return form.setResumen('La evidencia debe ser una imagen.');
        if (archivo.size > MAX_FOTO_BYTES) return form.setResumen('La foto supera los 2 MB. Usa una de menor tamaño.');
        const lector = new FileReader();
        lector.onload = () => setFoto(String(lector.result));
        lector.readAsDataURL(archivo);
    };

    const vuelto = form.valores.tipoCobro === 'EFECTIVO' && form.valores.pagaCon ? V.numero(form.valores.pagaCon) - monto : 0;

    const enviar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        if (!foto && !firma) {
            form.setResumen('La entrega necesita evidencia: toma una foto o pide la firma del cliente.');
            return;
        }
        setEnviando(true);
        try {
            await RepartoService.registrarEntrega({
                repartidorId,
                turnoId: turno.id,
                pedidoId: parada.pedidoId,
                paradaId: parada.id,
                recibidoPor: form.valores.recibidoPor.trim(),
                tempEntrega: form.valores.temp ? V.numero(form.valores.temp) : undefined,
                fotoUrl: foto || undefined,
                firmaUrl: firma || undefined,
                cobro: parada.cobrarAlEntregar
                    ? {
                        tipo: form.valores.tipoCobro as TipoCobro,
                        pagaCon: form.valores.tipoCobro === 'EFECTIVO' ? V.numero(form.valores.pagaCon) : undefined,
                    }
                    : undefined,
            });
            onListo();
        } catch (err) {
            form.setResumen(mensajeError(err));
        } finally {
            setEnviando(false);
        }
    };

    return (
        <form className="form" noValidate onSubmit={enviar} aria-label={`Registrar entrega de ${parada.codigoPedido}`}>
            <FormSummary mensaje={form.resumen} />
            <FormField id={`${pre}recibidoPor`} label="Recibido por" error={form.errores.recibidoPor}>
                <input {...form.campo('recibidoPor')} type="text" />
            </FormField>
            <FormField id={`${pre}temp`} label="Temperatura al entregar (°C)" error={form.errores.temp}>
                <input {...form.campo('temp')} type="number" step={0.5} />
            </FormField>
            {parada.cobrarAlEntregar && (
                <fieldset>
                    <legend>Cobro: {FormatService.dinero(monto)}</legend>
                    <FormField id={`${pre}tipoCobro`} label="Medio de pago">
                        <select {...form.campo('tipoCobro', 'select')}>
                            <option value="EFECTIVO">Efectivo</option>
                            <option value="POS_MOVIL">POS móvil</option>
                            <option value="TRANSFERENCIA">Transferencia</option>
                        </select>
                    </FormField>
                    {form.valores.tipoCobro === 'EFECTIVO' && (
                        <FormField id={`${pre}pagaCon`} label="Paga con" error={form.errores.pagaCon}>
                            <input {...form.campo('pagaCon')} type="number" min={0} step={1000} />
                        </FormField>
                    )}
                    {vuelto > 0 && <p className="status ok">Vuelto: {FormatService.dinero(vuelto)}</p>}
                </fieldset>
            )}
            <div className="field">
                <label htmlFor={`${pre}foto`}>Foto de la entrega</label>
                <input id={`${pre}foto`} className="input" type="file" accept="image/*" capture="environment" onChange={leerFoto} />
                {foto && <img className="preview-img" src={foto} alt="Vista previa de la evidencia" />}
            </div>
            <div className="field">
                <label htmlFor={`${pre}firma`}>Firma del cliente</label>
                <SignaturePad id={`${pre}firma`} onCambio={setFirma} />
            </div>
            <button className="btn warning driver-btn" disabled={enviando}>{enviando ? 'Registrando…' : 'Confirmar entrega'}</button>
            <button type="button" className="btn secondary driver-btn" onClick={onCancelar}>Cancelar</button>
        </form>
    );
};

const FormFallida: React.FC<FormParadaProps> = ({ parada, turno, repartidorId, onCancelar, onListo }) => {
    const [enviando, setEnviando] = useState(false);
    const pre = `fal-${parada.id}-`;
    const form = useForm(
        {
            tipo: { reglas: [ValidationService.requerido('El motivo')] },
            nota: { reglas: [ValidationService.requerido('El detalle'), ValidationService.largo(5, 300)] },
        },
        { tipo: 'CLIENTE_AUSENTE', nota: '' },
        pre,
    );

    const enviar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        setEnviando(true);
        try {
            await RepartoService.registrarFallida({
                repartidorId,
                turnoId: turno.id,
                pedidoId: parada.pedidoId,
                paradaId: parada.id,
                tipo: form.valores.tipo as TipoIncidencia,
                nota: form.valores.nota.trim(),
            });
            onListo();
        } catch (err) {
            form.setResumen(mensajeError(err));
        } finally {
            setEnviando(false);
        }
    };

    return (
        <form className="form" noValidate onSubmit={enviar} aria-label={`Entrega fallida de ${parada.codigoPedido}`}>
            <FormSummary mensaje={form.resumen} />
            <FormField id={`${pre}tipo`} label="Motivo">
                <select {...form.campo('tipo', 'select')}>
                    <option value="CLIENTE_AUSENTE">Cliente ausente</option>
                    <option value="DIRECCION_INCORRECTA">Dirección incorrecta</option>
                    <option value="RECHAZO_PRODUCTO">Rechazó el producto</option>
                    <option value="OTRO">Otro</option>
                </select>
            </FormField>
            <FormField id={`${pre}nota`} label="Detalle" hint="Entre 5 y 300 caracteres." error={form.errores.nota}>
                <textarea {...form.campo('nota')} className={`${form.clase('nota')} textarea`} maxLength={300} />
            </FormField>
            <button className="btn danger driver-btn" disabled={enviando}>{enviando ? 'Registrando…' : 'Registrar incidencia'}</button>
            <button type="button" className="btn secondary driver-btn" onClick={onCancelar}>Cancelar</button>
        </form>
    );
};

const CerrarTurno: React.FC<{ turno: Turno; onCerrado: (c: CierreCaja) => void }> = ({ turno, onCerrado }) => {
    const [abierto, setAbierto] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const form = useForm(
        { efectivo: { reglas: [ValidationService.requerido('El efectivo rendido'), ValidationService.rango(0, 100000000, 'CLP')] } },
        { efectivo: '0' },
        'cierre-',
    );

    if (!abierto) {
        return <button type="button" className="btn secondary driver-btn" onClick={() => setAbierto(true)}>Cerrar turno y rendir caja</button>;
    }

    const enviar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        setEnviando(true);
        try {
            onCerrado(await RepartoService.cerrarTurno(turno.id, ValidationService.numero(form.valores.efectivo)));
        } catch (err) {
            form.setResumen(mensajeError(err));
        } finally {
            setEnviando(false);
        }
    };

    return (
        <form className="form card" noValidate onSubmit={enviar} aria-label="Cerrar turno">
            <h2>Cierre de caja</h2>
            <FormSummary mensaje={form.resumen} />
            <FormField id="cierre-efectivo" label="Efectivo que entregas (CLP)" hint="Suma de los cobros en efectivo del día." error={form.errores.efectivo}>
                <input {...form.campo('efectivo')} type="number" min={0} step={100} />
            </FormField>
            <button className="btn driver-btn" disabled={enviando}>{enviando ? 'Cerrando…' : 'Confirmar cierre'}</button>
            <button type="button" className="btn secondary driver-btn" onClick={() => setAbierto(false)}>Cancelar</button>
        </form>
    );
};

const ResumenCierre: React.FC<{ cierre: CierreCaja }> = ({ cierre }) => (
    <div className={`notice ${cierre.diferencia === 0 ? 'success' : 'warning'}`} role="status">
        <h2>Turno cerrado</h2>
        <dl className="specs">
            <dt>Entregas</dt><dd>{cierre.entregas} ({cierre.fallidas} fallidas)</dd>
            <dt>Efectivo esperado</dt><dd>{FormatService.dinero(cierre.efectivoEsperado)}</dd>
            <dt>Efectivo rendido</dt><dd>{FormatService.dinero(cierre.efectivoRendido)}</dd>
            <dt>Diferencia</dt><dd className={cierre.diferencia === 0 ? 'status ok' : 'status bad'}>{FormatService.dinero(cierre.diferencia)}</dd>
            <dt>POS / Transferencias</dt><dd>{FormatService.dinero(cierre.pos)} / {FormatService.dinero(cierre.transferencia)}</dd>
        </dl>
    </div>
);

export default Repartidor;
