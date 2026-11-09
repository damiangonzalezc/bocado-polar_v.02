// components/admin/InventorySection.tsx · Inventario por lote en orden FEFO, ajustes, bloqueo y alertas (ms-inventario)

import React, { useMemo, useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import InventarioService from '../../services/inventarioService';
import CatalogoService from '../../services/catalogoService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { erroresDeCampo, mensajeError } from '../../services/apiClient';
import type { Lote, TipoMovimiento } from '../../types';

const VenceTag: React.FC<{ dias: number }> = ({ dias }) =>
    dias < 0 ? <span className="tag danger">Vencido</span>
        : dias <= 7 ? <span className="tag warning">Vence en {dias} d</span>
            : <span className="tag">Vence en {dias} d</span>;

const InventorySection: React.FC = () => {
    const { usuario } = useAuth();
    const { mostrar } = useToast();
    const lotes = useApi(() => InventarioService.listarLotes(true));
    const stock = useApi(() => InventarioService.stockGeneral());
    const alertas = useApi(() => InventarioService.alertas(false));
    const productos = useApi(() => CatalogoService.listarProductos(true));
    const [ajuste, setAjuste] = useState<{ lote: Lote; tipo: TipoMovimiento; kg: string; motivo: string } | null>(null);
    const [registrar, setRegistrar] = useState(false);
    const V = ValidationService;

    const ordenFefo = useMemo(
        () => [...(lotes.datos ?? [])].sort((a, b) => a.vencimiento.localeCompare(b.vencimiento)),
        [lotes.datos],
    );

    const form = useForm(
        {
            productoId: { reglas: [V.requerido('El producto')] },
            codigoLote: { reglas: [V.requerido('El código de lote'), V.codigoLote()], formato: (v) => v.trim().toUpperCase() },
            stockKg: { reglas: [V.requerido('Los kilos'), V.rango(0.5, 100000, 'kg')] },
            vencimiento: { reglas: [V.requerido('El vencimiento'), V.fechaFutura()] },
            tempAlmacenaje: { reglas: [V.rango(-40, 0, '°C')] },
        },
        { productoId: '', codigoLote: '', stockKg: '', vencimiento: '', tempAlmacenaje: '-18' },
        'lote-',
    );

    const recargarTodo = () => {
        void lotes.recargar();
        void stock.recargar();
        void alertas.recargar();
    };

    const accion = async (fn: () => Promise<unknown>, exito: string) => {
        try {
            await fn();
            mostrar(exito);
            recargarTodo();
            return true;
        } catch (e) {
            mostrar(mensajeError(e), 'error');
            return false;
        }
    };

    const guardarLote = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        const v = form.valores;
        try {
            await InventarioService.registrarLote({
                productoId: Number(v.productoId),
                codigoLote: v.codigoLote.trim().toUpperCase(),
                stockKg: V.numero(v.stockKg),
                vencimiento: v.vencimiento,
                tempAlmacenaje: v.tempAlmacenaje ? V.numero(v.tempAlmacenaje) : undefined,
                usuarioId: usuario?.id,
            });
            mostrar('Lote registrado');
            setRegistrar(false);
            form.reiniciar();
            recargarTodo();
        } catch (err) {
            form.erroresServidor(erroresDeCampo(err), mensajeError(err));
        }
    };

    const guardarAjuste = async () => {
        if (!ajuste) return;
        const kg = V.numero(ajuste.kg);
        if (Number.isNaN(kg) || kg === 0) return mostrar('Ingresa los kilos del ajuste (distinto de 0).', 'error');
        if (ajuste.motivo.trim().length < 3) return mostrar('Escribe el motivo del ajuste.', 'error');
        if (await accion(() => InventarioService.ajustar(ajuste.lote.id, ajuste.tipo, kg, ajuste.motivo.trim(), usuario?.id), 'Stock ajustado')) setAjuste(null);
    };

    const bloquear = (l: Lote) => {
        const motivo = window.prompt(`Motivo del bloqueo del lote ${l.codigoLote}:`, 'Revisión de cadena de frío');
        if (motivo && motivo.trim()) void accion(() => InventarioService.bloquear(l.id, motivo.trim()), 'Lote bloqueado');
    };

    return (
        <section aria-labelledby="inv-titulo">
            <div className="section-title">
                <h1 id="inv-titulo">Inventario y lotes (orden FEFO)</h1>
                <button type="button" className="btn" onClick={() => setRegistrar(!registrar)}>Registrar lote</button>
            </div>

            {(alertas.datos ?? []).length > 0 && (
                <div className="notice error" role="alert">
                    <strong>Stock crítico:</strong>{' '}
                    {alertas.datos!.map((a) => (
                        <span key={a.id}>
                            {a.productoNombre ?? `Producto ${a.productoId}`} ({FormatService.kg(a.stockActual)} kg){' '}
                            <button type="button" className="link" onClick={() => accion(() => InventarioService.resolverAlerta(a.id), 'Alerta resuelta')}>resolver</button>
                            {' · '}
                        </span>
                    ))}
                </div>
            )}

            {registrar && (
                <form className="card form" noValidate onSubmit={guardarLote} aria-label="Registrar lote">
                    <h2>Nuevo lote</h2>
                    <FormSummary mensaje={form.resumen} />
                    <div className="two">
                        <FormField id="lote-productoId" label="Producto" error={form.errores.productoId}>
                            <select {...form.campo('productoId', 'select')}>
                                <option value="">Selecciona…</option>
                                {(productos.datos ?? []).map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                            </select>
                        </FormField>
                        <FormField id="lote-codigoLote" label="Código de lote" hint="Ej: SAL-2611-A" error={form.errores.codigoLote}><input {...form.campo('codigoLote')} /></FormField>
                        <FormField id="lote-stockKg" label="Kilos" error={form.errores.stockKg}><input {...form.campo('stockKg')} type="number" step={0.5} min={0.5} /></FormField>
                        <FormField id="lote-vencimiento" label="Vencimiento" error={form.errores.vencimiento}><input {...form.campo('vencimiento')} type="date" min={FormatService.fechaEnDias(0)} /></FormField>
                        <FormField id="lote-tempAlmacenaje" label="Temperatura de ingreso (°C)" hint="RN-01: debe ser -15 °C o menos." error={form.errores.tempAlmacenaje}><input {...form.campo('tempAlmacenaje')} type="number" step={0.5} /></FormField>
                    </div>
                    <div className="btn-row">
                        <button className="btn">Guardar lote</button>
                        <button type="button" className="btn secondary" onClick={() => setRegistrar(false)}>Cancelar</button>
                    </div>
                </form>
            )}

            <h2>Stock por producto</h2>
            <div className="table-wrap">
                <table>
                    <thead><tr><th scope="col">Producto</th><th scope="col">Stock</th><th scope="col">Reservado</th><th scope="col">Disponible</th><th scope="col">Lotes</th><th scope="col">Próx. vencimiento</th></tr></thead>
                    <tbody>
                        {(stock.datos ?? []).map((s) => (
                            <tr key={s.productoId}>
                                <td>{s.productoNombre ?? `Producto ${s.productoId}`}</td>
                                <td>{FormatService.kg(s.stockKg)} kg</td>
                                <td>{FormatService.kg(s.reservadoKg)} kg</td>
                                <td>{FormatService.kg(s.disponibleKg)} kg {s.critico && <span className="tag danger">Crítico</span>}</td>
                                <td>{s.lotes}</td>
                                <td>{FormatService.fecha(s.proximoVencimiento)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <h2 style={{ marginTop: 24 }}>Lotes</h2>
            <ErrorNotice mensaje={lotes.error} onReintentar={lotes.recargar} />
            {lotes.cargando ? <Loader /> : (
                <div className="table-wrap">
                    <table>
                        <caption className="visually-hidden">Inventario por lote</caption>
                        <thead><tr><th scope="col">Producto</th><th scope="col">Lote</th><th scope="col">Stock</th><th scope="col">Vence</th><th scope="col">FEFO</th><th scope="col">Temp.</th><th scope="col">Acciones</th></tr></thead>
                        <tbody>
                            {ordenFefo.map((l, i) => (
                                <React.Fragment key={l.id}>
                                    <tr>
                                        <td>{l.productoNombre}</td>
                                        <td>{l.codigoLote} {l.bloqueado && <span className="tag danger" title={l.motivoBloqueo}>Bloqueado</span>}</td>
                                        <td>{FormatService.kg(l.stockKg)} kg<br /><small className="muted">{FormatService.kg(l.reservadoKg)} kg reservados</small></td>
                                        <td><time dateTime={l.vencimiento}>{FormatService.fecha(l.vencimiento)}</time> <VenceTag dias={l.diasParaVencer} /></td>
                                        <td>{i === 0 ? <span className="tag warning">Salir primero</span> : `Orden ${i + 1}`}</td>
                                        <td className={l.tempAlmacenaje !== undefined && l.tempAlmacenaje > -15 ? 'status bad' : undefined}>{l.tempAlmacenaje ?? '—'}°C</td>
                                        <td>
                                            <div className="btn-row" style={{ marginTop: 0 }}>
                                                <button type="button" className="btn secondary small" onClick={() => setAjuste({ lote: l, tipo: 'AJUSTE', kg: '', motivo: '' })}>Ajustar</button>
                                                {l.bloqueado
                                                    ? <button type="button" className="btn secondary small" onClick={() => accion(() => InventarioService.desbloquear(l.id), 'Lote desbloqueado')}>Desbloquear</button>
                                                    : <button type="button" className="btn danger small" onClick={() => bloquear(l)}>Bloquear</button>}
                                            </div>
                                        </td>
                                    </tr>
                                    {ajuste?.lote.id === l.id && (
                                        <tr className="detail-row">
                                            <td colSpan={7}>
                                                <div className="inline-form">
                                                    <div className="field">
                                                        <label htmlFor="aj-tipo">Tipo</label>
                                                        <select id="aj-tipo" className="select small" value={ajuste.tipo} onChange={(e) => setAjuste({ ...ajuste, tipo: e.target.value as TipoMovimiento })}>
                                                            <option value="AJUSTE">Ajuste (+ o −)</option>
                                                            <option value="ENTRADA">Entrada</option>
                                                            <option value="SALIDA">Salida</option>
                                                            <option value="MERMA">Merma</option>
                                                        </select>
                                                    </div>
                                                    <div className="field">
                                                        <label htmlFor="aj-kg">Kilos</label>
                                                        <input id="aj-kg" className="input small" type="number" step={0.5} value={ajuste.kg} onChange={(e) => setAjuste({ ...ajuste, kg: e.target.value })} />
                                                    </div>
                                                    <div className="field">
                                                        <label htmlFor="aj-motivo">Motivo</label>
                                                        <input id="aj-motivo" className="input small" maxLength={200} value={ajuste.motivo} onChange={(e) => setAjuste({ ...ajuste, motivo: e.target.value })} />
                                                    </div>
                                                    <div className="btn-row">
                                                        <button type="button" className="btn small" onClick={guardarAjuste}>Guardar ajuste</button>
                                                        <button type="button" className="btn secondary small" onClick={() => setAjuste(null)}>Cancelar</button>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </React.Fragment>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
};

export default InventorySection;
