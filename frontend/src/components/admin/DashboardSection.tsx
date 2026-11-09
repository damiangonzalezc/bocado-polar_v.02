// components/admin/DashboardSection.tsx · Resumen y KPIs (ms-reportes)

import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import { useApi } from '../../hooks/useApi';
import ReporteService from '../../services/reporteService';
import FormatService from '../../services/formatService';
import type { AlertaOperativa, Periodo } from '../../types';

const PERIODOS: { valor: Periodo; texto: string }[] = [
    { valor: 'HOY', texto: 'Hoy' },
    { valor: 'SIETE_DIAS', texto: 'Últimos 7 días' },
    { valor: 'MES_ACTUAL', texto: 'Mes actual' },
    { valor: 'TREINTA_DIAS', texto: 'Últimos 30 días' },
];

const NOMBRE_ALERTA: Record<AlertaOperativa['tipo'], string> = {
    STOCK_CRITICO: 'Stock crítico',
    FRIO: 'Alertas de frío',
    INCIDENCIA: 'Incidencias de reparto hoy',
    PEDIDO_ATRASADO: 'Pedidos atrasados',
};

const Variacion: React.FC<{ pct?: number }> = ({ pct }) =>
    pct === undefined || pct === null ? (
        <small className="muted">sin período anterior</small>
    ) : (
        <small className={pct >= 0 ? 'status ok' : 'status bad'}>{FormatService.porcentaje(pct)} vs período anterior</small>
    );

const DashboardSection: React.FC = () => {
    const [periodo, setPeriodo] = useState<Periodo>('SIETE_DIAS');
    const kpis = useApi(() => ReporteService.kpis(periodo), [periodo]);
    const top = useApi(() => ReporteService.topProductos(periodo), [periodo]);
    const ventas = useApi(() => ReporteService.ventasDiarias(periodo === 'HOY' ? 'SIETE_DIAS' : periodo), [periodo]);
    const alertas = useApi(() => ReporteService.alertasOperativas());
    const maxVenta = Math.max(1, ...(ventas.datos ?? []).map((v) => v.ventas));
    const k = kpis.datos;

    return (
        <section aria-labelledby="dash-titulo">
            <div className="section-title">
                <h1 id="dash-titulo">Panel administrativo</h1>
                <div className="field">
                    <label className="visually-hidden" htmlFor="periodo">Período</label>
                    <select id="periodo" className="select" value={periodo} onChange={(e) => setPeriodo(e.target.value as Periodo)}>
                        {PERIODOS.map((p) => <option key={p.valor} value={p.valor}>{p.texto}</option>)}
                    </select>
                </div>
            </div>
            <ErrorNotice mensaje={kpis.error} onReintentar={kpis.recargar} />
            {kpis.cargando ? (
                <Loader texto="Calculando indicadores…" />
            ) : (
                k && (
                    <div className="grid cols-4">
                        <div className="card kpi"><span>Ventas brutas</span><div className="metric">{FormatService.dinero(k.ventasBrutas)}</div><Variacion pct={k.comparacion?.ventasBrutasPct} /></div>
                        <div className="card kpi"><span>Órdenes</span><div className="metric">{k.ordenes}</div><Variacion pct={k.comparacion?.ordenesPct} /></div>
                        <div className="card kpi"><span>Ticket promedio</span><div className="metric">{FormatService.dinero(k.ticketPromedio)}</div><Variacion pct={k.comparacion?.ticketPromedioPct} /></div>
                        <div className="card kpi"><span>Conversión ({k.visitas} visitas)</span><div className="metric">{String(k.conversion).replace('.', ',')}%</div><Variacion pct={k.comparacion?.conversionPct} /></div>
                    </div>
                )
            )}
            {k && <p className="muted">Ventas netas (sin IVA): {FormatService.dinero(k.ventasNetas)} · {FormatService.fecha(k.desde)} al {FormatService.fecha(k.hasta)}</p>}

            <h2>Alertas operativas</h2>
            <div className="grid cols-4">
                {(alertas.datos ?? []).map((a) => (
                    <Link key={a.tipo} to={a.enlace} className={`card kpi${a.cantidad > 0 ? ' alert' : ''}`}>
                        <span>{NOMBRE_ALERTA[a.tipo]}</span>
                        <div className="metric">{a.cantidad < 0 ? '—' : a.cantidad}</div>
                        {a.cantidad < 0 && <small className="muted">servicio sin respuesta</small>}
                    </Link>
                ))}
            </div>

            <div className="grid cols-2" style={{ marginTop: 18 }}>
                <article className="card">
                    <h2>Ventas por día</h2>
                    <div className="bars" role="img" aria-label="Gráfico de ventas diarias">
                        {(ventas.datos ?? []).map((v) => (
                            <span key={v.fecha} style={{ height: `${(v.ventas / maxVenta) * 100}%` }} title={`${FormatService.fecha(v.fecha)}: ${FormatService.dinero(v.ventas)} (${v.ordenes} órdenes)`} />
                        ))}
                    </div>
                    <p className="muted">La última barra (amarilla) es hoy.</p>
                </article>
                <article className="card">
                    <h2>Productos más vendidos</h2>
                    <div className="table-wrap">
                        <table>
                            <thead><tr><th scope="col">Producto</th><th scope="col">Kg</th><th scope="col">Ingresos</th></tr></thead>
                            <tbody>
                                {(top.datos ?? []).map((t) => (
                                    <tr key={t.productoId}><td>{t.nombre}</td><td>{FormatService.kg(t.kg)}</td><td>{FormatService.dinero(t.ingresos)}</td></tr>
                                ))}
                                {top.datos?.length === 0 && <tr><td colSpan={3} className="muted">Sin ventas en el período.</td></tr>}
                            </tbody>
                        </table>
                    </div>
                </article>
            </div>
        </section>
    );
};

export default DashboardSection;
