// services/reporteService.ts · ms-reportes (KPIs del panel y visitas)

import apiClient from './apiClient';
import StorageService from './storageService';
import type { AlertaOperativa, Kpis, Periodo, TopProducto, VentaDiaria } from '../types';

class ReporteService {
    static async kpis(periodo: Periodo): Promise<Kpis> {
        const { data } = await apiClient.get<Kpis>('/kpis', { params: { periodo } });
        return data;
    }

    static async topProductos(periodo: Periodo): Promise<TopProducto[]> {
        const { data } = await apiClient.get<TopProducto[]>('/kpis/top-productos', { params: { periodo, limite: 5 } });
        return data;
    }

    static async ventasDiarias(periodo: Periodo): Promise<VentaDiaria[]> {
        const { data } = await apiClient.get<VentaDiaria[]>('/kpis/ventas-diarias', { params: { periodo } });
        return data;
    }

    static async alertasOperativas(): Promise<AlertaOperativa[]> {
        const { data } = await apiClient.get<AlertaOperativa[]>('/kpis/alertas-operativas');
        return data;
    }

    /** Registra una visita por sesión del navegador (para la tasa de conversión). No interrumpe si falla. */
    static async registrarVisita(): Promise<void> {
        if (StorageService.visitaRegistrada()) return;
        try {
            await apiClient.post('/visitas', { canal: 'Directo' });
            StorageService.marcarVisita();
        } catch {
            /* sin acción: es solo estadística */
        }
    }
}

export default ReporteService;
