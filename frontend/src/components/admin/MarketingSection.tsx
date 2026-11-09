// components/admin/MarketingSection.tsx · Cupones y rescate de carritos abandonados (ms-marketing)

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import MarketingService from '../../services/marketingService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { erroresDeCampo, mensajeError } from '../../services/apiClient';
import type { TipoDescuento } from '../../types';

const MarketingSection: React.FC = () => {
    const { mostrar } = useToast();
    const cupones = useApi(() => MarketingService.cupones());
    const abandonados = useApi(() => MarketingService.abandonados());
    const [cuponRescate, setCuponRescate] = useState<Record<number, string>>({});
    const V = ValidationService;

    const form = useForm(
        {
            codigo: { reglas: [V.requerido('El código'), V.codigoCupon()], formato: (v) => v.trim().toUpperCase() },
            tipo: { reglas: [V.requerido('El tipo')] },
            valor: {
                reglas: [V.requerido('El valor'), (v, vals) => (vals.tipo === 'PORCENTAJE' ? V.rango(1, 100, '%')(v, vals) : V.rango(1, 1000000, 'CLP')(v, vals))],
            },
            compraMinima: { reglas: [V.rango(0, 10000000, 'CLP')] },
            limiteUsos: { reglas: [V.requerido('El límite de usos'), V.entero(1, 100000)] },
            vence: { reglas: [V.requerido('El vencimiento'), V.fechaFutura()] },
        },
        { codigo: '', tipo: 'PORCENTAJE', valor: '', compraMinima: '0', limiteUsos: '100', vence: '' },
        'cup-',
    );

    const crear = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        const v = form.valores;
        try {
            await MarketingService.crearCupon({
                codigo: v.codigo.trim().toUpperCase(),
                tipo: v.tipo as TipoDescuento,
                valor: V.numero(v.valor),
                compraMinima: V.numero(v.compraMinima || '0'),
                limiteUsos: Number(v.limiteUsos),
                limitePorCliente: 1,
                activo: true,
                vence: v.vence,
            });
            mostrar('Cupón creado');
            form.reiniciar();
            void cupones.recargar();
        } catch (err) {
            form.erroresServidor(erroresDeCampo(err), mensajeError(err));
        }
    };

    const accion = async (fn: () => Promise<unknown>, exito: string, recargar: () => Promise<void>) => {
        try {
            await fn();
            mostrar(exito);
            void recargar();
        } catch (err) {
            mostrar(mensajeError(err), 'error');
        }
    };

    return (
        <section aria-labelledby="mkt-titulo">
            <div className="section-title"><h1 id="mkt-titulo">Marketing y promociones</h1></div>
            <div className="grid cart-layout">
                <div>
                    <h2>Cupones</h2>
                    <ErrorNotice mensaje={cupones.error} onReintentar={cupones.recargar} />
                    {cupones.cargando ? <Loader /> : (
                        <div className="table-wrap">
                            <table>
                                <thead><tr><th scope="col">Código</th><th scope="col">Descuento</th><th scope="col">Usos</th><th scope="col">Vence</th><th scope="col">Estado</th></tr></thead>
                                <tbody>
                                    {(cupones.datos ?? []).map((c) => (
                                        <tr key={c.id}>
                                            <td><strong>{c.codigo}</strong></td>
                                            <td>{c.tipo === 'PORCENTAJE' ? `${c.valor}%` : FormatService.dinero(c.valor)}{c.compraMinima > 0 && <><br /><small className="muted">mín. {FormatService.dinero(c.compraMinima)}</small></>}</td>
                                            <td>{c.usos} / {c.limiteUsos}</td>
                                            <td>{FormatService.fecha(c.vence)} {FormatService.diasHasta(c.vence) < 0 && <span className="tag danger">Vencido</span>}</td>
                                            <td>
                                                <button type="button" className={`btn small ${c.activo ? 'secondary' : ''}`} onClick={() => accion(() => MarketingService.activarCupon(c.id, !c.activo), c.activo ? 'Cupón desactivado' : 'Cupón activado', cupones.recargar)}>
                                                    {c.activo ? 'Desactivar' : 'Activar'}
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
                <form className="card form" noValidate onSubmit={crear} aria-label="Nuevo cupón">
                    <h2>Nuevo cupón</h2>
                    <FormSummary mensaje={form.resumen} />
                    <FormField id="cup-codigo" label="Código" hint="4 a 16 letras o números." error={form.errores.codigo}><input {...form.campo('codigo')} /></FormField>
                    <FormField id="cup-tipo" label="Tipo" error={form.errores.tipo}>
                        <select {...form.campo('tipo', 'select')}>
                            <option value="PORCENTAJE">Porcentaje</option>
                            <option value="MONTO_FIJO">Monto fijo</option>
                        </select>
                    </FormField>
                    <FormField id="cup-valor" label={form.valores.tipo === 'PORCENTAJE' ? 'Descuento (%)' : 'Descuento (CLP)'} error={form.errores.valor}><input {...form.campo('valor')} type="number" min={1} /></FormField>
                    <FormField id="cup-compraMinima" label="Compra mínima (CLP)" error={form.errores.compraMinima}><input {...form.campo('compraMinima')} type="number" min={0} /></FormField>
                    <FormField id="cup-limiteUsos" label="Límite de usos" error={form.errores.limiteUsos}><input {...form.campo('limiteUsos')} type="number" min={1} /></FormField>
                    <FormField id="cup-vence" label="Vence" error={form.errores.vence}><input {...form.campo('vence')} type="date" min={FormatService.fechaEnDias(0)} /></FormField>
                    <button className="btn">Crear cupón</button>
                </form>
            </div>

            <div className="section-title">
                <h2>Carritos abandonados</h2>
                <button type="button" className="btn secondary small" onClick={() => accion(() => MarketingService.detectarAbandonados(), 'Búsqueda de carritos abandonados lista', abandonados.recargar)}>
                    Detectar ahora
                </button>
            </div>
            {abandonados.cargando ? <Loader /> : (
                <div className="table-wrap">
                    <table>
                        <thead><tr><th scope="col">Cliente</th><th scope="col">Carrito</th><th scope="col">Total</th><th scope="col">Rescate</th></tr></thead>
                        <tbody>
                            {(abandonados.datos ?? []).map((a) => (
                                <tr key={a.id}>
                                    <td>{a.cliente ?? 'Visitante'}<br /><small className="muted">{a.email ?? 'sin correo'} · {FormatService.fechaHora(a.fecha)}</small></td>
                                    <td>{a.resumen}</td>
                                    <td>{FormatService.dinero(a.total)}</td>
                                    <td>
                                        {a.recuperado ? <span className="status ok">Recuperado</span>
                                            : a.correoEnviado ? <span className="status ok">Correo enviado{a.cuponOfrecido ? ` (${a.cuponOfrecido})` : ''}</span>
                                                : a.email ? (
                                                    <div className="qty">
                                                        <label className="visually-hidden" htmlFor={`resc-${a.id}`}>Cupón para {a.cliente}</label>
                                                        <select id={`resc-${a.id}`} className="select small" value={cuponRescate[a.id] ?? ''} onChange={(e) => setCuponRescate({ ...cuponRescate, [a.id]: e.target.value })}>
                                                            <option value="">Sin cupón</option>
                                                            {(cupones.datos ?? []).filter((c) => c.activo).map((c) => <option key={c.id} value={c.codigo}>{c.codigo}</option>)}
                                                        </select>
                                                        <button type="button" className="btn small" onClick={() => accion(() => MarketingService.enviarRescate(a.id, cuponRescate[a.id] ?? ''), 'Correo de rescate enviado', abandonados.recargar)}>Enviar</button>
                                                    </div>
                                                ) : <span className="muted">Sin correo</span>}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
};

export default MarketingSection;
