// components/admin/ColdChainSection.tsx · Cadena de frío: alertas RN-01 y registro de temperaturas (ms-frio)

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import FrioService from '../../services/frioService';
import InventarioService from '../../services/inventarioService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { mensajeError } from '../../services/apiClient';

const ColdChainSection: React.FC = () => {
    const { usuario } = useAuth();
    const { mostrar } = useToast();
    const [verTodas, setVerTodas] = useState(false);
    const alertas = useApi(() => FrioService.alertas(verTodas ? undefined : false), [verTodas]);
    const registros = useApi(() => FrioService.registros());
    const lotes = useApi(() => InventarioService.listarLotes());
    const V = ValidationService;

    const form = useForm(
        {
            loteId: { reglas: [V.requerido('El lote')] },
            grados: { reglas: [V.requerido('La temperatura'), V.rango(-40, 15, '°C')] },
        },
        { loteId: '', grados: '' },
        'frio-',
    );

    const nombreLote = (id: number) => lotes.datos?.find((l) => l.id === id)?.codigoLote ?? `#${id}`;

    const registrar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        try {
            const r = await FrioService.registrar('LOTE', Number(form.valores.loteId), V.numero(form.valores.grados), usuario?.id);
            if (r.alerta) mostrar(`Alerta ${r.alerta.severidad.toLowerCase()}: ${r.alerta.detalle}`, 'error');
            else mostrar('Temperatura registrada: dentro del rango.');
            form.reiniciar();
            void alertas.recargar();
            void registros.recargar();
            void lotes.recargar();
        } catch (err) {
            form.setResumen(mensajeError(err));
        }
    };

    const atender = async (id: number) => {
        try {
            await FrioService.atender(id, usuario?.id);
            mostrar('Alerta marcada como atendida');
            void alertas.recargar();
        } catch (err) {
            mostrar(mensajeError(err), 'error');
        }
    };

    return (
        <section aria-labelledby="frio-titulo">
            <div className="section-title"><h1 id="frio-titulo">Cadena de frío</h1><span className="tag">RN-01</span></div>
            <div className="notice error">
                RN-01: sobre -15°C se genera una advertencia; si un lote sigue sobre el umbral más de 30 minutos, la alerta pasa a crítica y el lote se bloquea.
            </div>

            <div className="grid cols-2">
                <form className="card form" noValidate onSubmit={registrar} aria-label="Registrar temperatura de lote">
                    <h2>Registrar temperatura de lote</h2>
                    <FormSummary mensaje={form.resumen} />
                    <FormField id="frio-loteId" label="Lote" error={form.errores.loteId}>
                        <select {...form.campo('loteId', 'select')}>
                            <option value="">Selecciona…</option>
                            {(lotes.datos ?? []).map((l) => <option key={l.id} value={l.id}>{l.codigoLote} · {l.productoNombre}</option>)}
                        </select>
                    </FormField>
                    <FormField id="frio-grados" label="Temperatura (°C)" hint="Ej: -18" error={form.errores.grados}>
                        <input {...form.campo('grados')} type="number" step={0.1} />
                    </FormField>
                    <button className="btn">Registrar</button>
                </form>
                <article className="card">
                    <h2>Últimas mediciones</h2>
                    <ul className="mini-list">
                        {(registros.datos ?? []).slice(0, 10).map((r) => (
                            <li key={r.id} className={r.grados > -15 ? 'status bad' : undefined}>
                                {FormatService.fechaHora(r.fecha)} · {FormatService.etiqueta(r.origen)} {r.origen === 'LOTE' ? nombreLote(r.referenciaId) : `#${r.referenciaId}`} · {r.grados}°C
                            </li>
                        ))}
                    </ul>
                </article>
            </div>

            <div className="section-title">
                <h2>Alertas</h2>
                <label className="checkbox"><input type="checkbox" checked={verTodas} onChange={(e) => setVerTodas(e.target.checked)} /> Ver también las atendidas</label>
            </div>
            <ErrorNotice mensaje={alertas.error} onReintentar={alertas.recargar} />
            {alertas.cargando ? <Loader /> : (
                <div className="grid">
                    {(alertas.datos ?? []).map((a) => (
                        <article key={a.id} className="card">
                            <span className={`tag ${a.severidad === 'CRITICA' ? 'danger' : 'warning'}`}>{FormatService.etiqueta(a.severidad)}</span>
                            <h3>{FormatService.etiqueta(a.origen)} {a.origen === 'LOTE' ? nombreLote(a.referenciaId) : `#${a.referenciaId}`}</h3>
                            <p>{a.detalle}</p>
                            <p className="muted">{FormatService.fechaHora(a.fecha)}</p>
                            {a.atendida
                                ? <p className="status ok">Atendida</p>
                                : <button type="button" className="btn small" onClick={() => atender(a.id)}>Marcar atendida</button>}
                        </article>
                    ))}
                    {alertas.datos?.length === 0 && <p className="card empty">Sin alertas pendientes. Cadena de frío controlada.</p>}
                </div>
            )}
        </section>
    );
};

export default ColdChainSection;
