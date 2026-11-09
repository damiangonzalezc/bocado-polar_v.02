// services/rutaService.ts · ms-rutas (rutas, paradas y vehículos)

import apiClient from './apiClient';
import type { Ruta, RutaData, Vehiculo } from '../types';

export type AccionRuta = 'iniciar' | 'finalizar' | 'cancelar' | 'optimizar';

class RutaService {
    static async listar(fecha?: string): Promise<Ruta[]> {
        const { data } = await apiClient.get<Ruta[]>('/rutas', { params: fecha ? { fecha } : {} });
        return data;
    }

    static async rutaDelDia(repartidorId: number): Promise<Ruta> {
        const { data } = await apiClient.get<Ruta>(`/rutas/repartidor/${repartidorId}/hoy`);
        return data;
    }

    static async crear(datos: RutaData): Promise<Ruta> {
        const { data } = await apiClient.post<Ruta>('/rutas', datos);
        return data;
    }

    static async accion(id: number, accion: AccionRuta): Promise<Ruta> {
        const { data } = await apiClient.patch<Ruta>(`/rutas/${id}/${accion}`);
        return data;
    }

    /**
     * El admin asigna (o reasigna) un pedido a un repartidor: queda como parada en la ruta de ese
     * repartidor para la fecha de entrega. Si el pedido estaba «Pagado» pasa a «En preparación».
     */
    static async asignarPedido(pedidoId: number, repartidorId: number, repartidorNombre?: string, usuarioId?: number): Promise<Ruta> {
        const { data } = await apiClient.post<Ruta>('/rutas/asignar-pedido', { pedidoId, repartidorId, repartidorNombre, usuarioId });
        return data;
    }

    static async generarParadas(id: number): Promise<Ruta> {
        const { data } = await apiClient.post<Ruta>(`/rutas/${id}/paradas/generar`);
        return data;
    }

    static async vehiculos(): Promise<Vehiculo[]> {
        const { data } = await apiClient.get<Vehiculo[]>('/vehiculos');
        return data;
    }
}

export default RutaService;
