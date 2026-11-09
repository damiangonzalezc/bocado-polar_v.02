// services/marketingService.ts · ms-marketing (cupones y carritos abandonados)

import apiClient from './apiClient';
import type { CarritoAbandonado, Cupon, CuponData } from '../types';

class MarketingService {
    static async cupones(): Promise<Cupon[]> {
        const { data } = await apiClient.get<Cupon[]>('/cupones');
        return data;
    }

    static async crearCupon(datos: CuponData): Promise<Cupon> {
        const { data } = await apiClient.post<Cupon>('/cupones', datos);
        return data;
    }

    static async activarCupon(id: number, activo: boolean): Promise<Cupon> {
        const { data } = await apiClient.patch<Cupon>(`/cupones/${id}/activo`, { activo });
        return data;
    }

    static async abandonados(): Promise<CarritoAbandonado[]> {
        const { data } = await apiClient.get<CarritoAbandonado[]>('/carritos-abandonados');
        return data;
    }

    static async detectarAbandonados(): Promise<CarritoAbandonado[]> {
        const { data } = await apiClient.post<CarritoAbandonado[]>('/carritos-abandonados/detectar', null, { params: { horas: 2 } });
        return data;
    }

    static async enviarRescate(id: number, codigoCupon: string): Promise<CarritoAbandonado> {
        const { data } = await apiClient.post<CarritoAbandonado>(`/carritos-abandonados/${id}/rescate`, { codigoCupon });
        return data;
    }
}

export default MarketingService;
