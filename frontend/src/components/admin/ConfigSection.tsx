// components/admin/ConfigSection.tsx · Configuración: IVA, zonas de envío y reglas de negocio (ms-config)

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import ConfigService from '../../services/configService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { mensajeError } from '../../services/apiClient';
import type { ZonaEnvio } from '../../types';

const ConfigSection: React.FC = () => {
    const { mostrar } = useToast();
    const config = useApi(() => ConfigService.configuracion());
    const zonas = useApi(() => ConfigService.zonas(false));
    const reglas = useApi(() => ConfigService.reglas());
    const [ivaEditado, setIva] = useState<string | null>(null);
    const [tarifas, setTarifas] = useState<Record<number, string>>({});
    const [valores, setValores] = useState<Record<string, string>>({});
    const [nueva, setNueva] = useState({ comuna: '', tarifa: '' });

    const iva = ivaEditado ?? String(config.datos?.iva ?? '');

    const ejecutar = async (fn: () => Promise<unknown>, exito: string, recargar: () => Promise<void>) => {
        try {
            await fn();
            mostrar(exito);
            void recargar();
        } catch (e) {
            mostrar(mensajeError(e), 'error');
        }
    };

    const guardarIva = () => {
        const n = ValidationService.numero(iva);
        if (Number.isNaN(n) || n < 0 || n > 100) return mostrar('El IVA debe estar entre 0 y 100.', 'error');
        const c = config.datos!;
        void ejecutar(() => ConfigService.guardarConfiguracion({ pais: c.pais, moneda: c.moneda, preciosConIva: c.preciosConIva, iva: n }), 'Configuración guardada', config.recargar);
    };

    const guardarZona = (z: ZonaEnvio, cambios: Partial<ZonaEnvio>) => {
        const tarifa = cambios.tarifa ?? z.tarifa;
        if (Number.isNaN(tarifa) || tarifa < 0) return mostrar('La tarifa debe ser 0 o mayor.', 'error');
        void ejecutar(() => ConfigService.actualizarZona({ ...z, ...cambios }), `Zona ${z.comuna} actualizada`, zonas.recargar);
    };

    const crearZona = () => {
        const tarifa = ValidationService.numero(nueva.tarifa);
        if (nueva.comuna.trim().length < 3 || Number.isNaN(tarifa) || tarifa < 0) return mostrar('Escribe la comuna y una tarifa válida.', 'error');
        void ejecutar(() => ConfigService.crearZona(nueva.comuna.trim(), tarifa), 'Zona creada', zonas.recargar).then(() => setNueva({ comuna: '', tarifa: '' }));
    };

    return (
        <section aria-labelledby="conf-titulo">
            <div className="section-title"><h1 id="conf-titulo">Configuración</h1><span className="tag">Solo administrador</span></div>
            <ErrorNotice mensaje={config.error || zonas.error || reglas.error} onReintentar={() => { void config.recargar(); void zonas.recargar(); void reglas.recargar(); }} />
            {config.cargando ? <Loader /> : config.datos && (
                <div className="card inline-form">
                    <div className="field"><span>País y moneda</span><strong>{config.datos.pais} · {config.datos.moneda}</strong></div>
                    <div className="field">
                        <label htmlFor="cfg-iva">IVA (%)</label>
                        <input id="cfg-iva" className="input" type="number" min={0} max={100} value={iva} onChange={(e) => setIva(e.target.value)} />
                    </div>
                    <button type="button" className="btn" onClick={guardarIva}>Guardar IVA</button>
                </div>
            )}

            <h2 style={{ marginTop: 24 }}>Zonas de envío</h2>
            <div className="table-wrap">
                <table>
                    <thead><tr><th scope="col">Comuna</th><th scope="col">Tarifa</th><th scope="col">Estado</th><th scope="col"><span className="visually-hidden">Acciones</span></th></tr></thead>
                    <tbody>
                        {(zonas.datos ?? []).map((z) => (
                            <tr key={z.id}>
                                <td>{z.comuna}</td>
                                <td>
                                    <label className="visually-hidden" htmlFor={`tar-${z.id}`}>Tarifa de {z.comuna}</label>
                                    <input id={`tar-${z.id}`} className="input small" type="number" min={0} step={10} value={tarifas[z.id] ?? String(z.tarifa)} onChange={(e) => setTarifas({ ...tarifas, [z.id]: e.target.value })} />
                                </td>
                                <td>{z.activa ? <span className="tag">Activa</span> : <span className="tag danger">Inactiva</span>}</td>
                                <td>
                                    <div className="btn-row" style={{ marginTop: 0 }}>
                                        <button type="button" className="btn secondary small" onClick={() => guardarZona(z, { tarifa: ValidationService.numero(tarifas[z.id] ?? String(z.tarifa)) })}>Guardar</button>
                                        <button type="button" className="btn secondary small" onClick={() => guardarZona(z, { activa: !z.activa })}>{z.activa ? 'Desactivar' : 'Activar'}</button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        <tr>
                            <td><input className="input small" placeholder="Nueva comuna" aria-label="Nueva comuna" value={nueva.comuna} onChange={(e) => setNueva({ ...nueva, comuna: e.target.value })} /></td>
                            <td><input className="input small" type="number" min={0} placeholder="Tarifa" aria-label="Tarifa de la nueva comuna" value={nueva.tarifa} onChange={(e) => setNueva({ ...nueva, tarifa: e.target.value })} /></td>
                            <td colSpan={2}><button type="button" className="btn small" onClick={crearZona}>Agregar zona</button></td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <h2 style={{ marginTop: 24 }}>Reglas de negocio</h2>
            <div className="table-wrap">
                <table>
                    <thead><tr><th scope="col">Código</th><th scope="col">Descripción</th><th scope="col">Valor</th><th scope="col"><span className="visually-hidden">Acciones</span></th></tr></thead>
                    <tbody>
                        {(reglas.datos ?? []).map((r) => (
                            <tr key={r.codigo}>
                                <td><strong>{r.codigo}</strong></td>
                                <td>{r.descripcion}<br /><small className="muted">Vigente desde {FormatService.fecha(r.vigenteDesde)}</small></td>
                                <td>
                                    <div className="qty">
                                        <input className="input small" type="number" step="any" aria-label={`Valor de ${r.codigo}`} value={valores[r.codigo] ?? String(r.valor)} onChange={(e) => setValores({ ...valores, [r.codigo]: e.target.value })} />
                                        <span className="muted">{r.unidad}</span>
                                    </div>
                                </td>
                                <td>
                                    <button
                                        type="button"
                                        className="btn secondary small"
                                        onClick={() => {
                                            const n = ValidationService.numero(valores[r.codigo] ?? String(r.valor));
                                            if (Number.isNaN(n)) return mostrar('Ingresa un número válido.', 'error');
                                            void ejecutar(() => ConfigService.actualizarRegla(r.codigo, n), `Regla ${r.codigo} actualizada`, reglas.recargar);
                                        }}
                                    >
                                        Guardar
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
};

export default ConfigSection;
