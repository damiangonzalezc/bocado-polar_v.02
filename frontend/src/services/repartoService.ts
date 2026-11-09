// services/repartoService.ts · ms-reparto (turnos, entregas, incidencias y cierre de caja)

import apiClient from './apiClient';
import type { CierreCaja, EntregaData, EntregaFallidaData, Repartidor, Turno } from '../types';

class RepartoService {
    static async repartidores(): Promise<Repartidor[]> {
        const { data } = await apiClient.get<Repartidor[]>('/repartidores');
        return data;
    }

    static async turnoActivo(repartidorId: number): Promise<Turno> {
        const { data } = await apiClient.get<Turno>(`/turnos/activo/${repartidorId}`);
        return data;
    }

    static async iniciarTurno(repartidorId: number, tempCajaInicial: number): Promise<Turno> {
        const { data } = await apiClient.post<Turno>('/turnos/iniciar', { repartidorId, tempCajaInicial });
        return data;
    }

    static async cerrarTurno(turnoId: number, efectivoRendido: number): Promise<CierreCaja> {
        const { data } = await apiClient.post<CierreCaja>(`/turnos/${turnoId}/cerrar`, { efectivoRendido });
        return data;
    }

    static async registrarEntrega(datos: EntregaData): Promise<void> {
        await apiClient.post('/entregas', datos);
    }

    static async registrarFallida(datos: EntregaFallidaData): Promise<void> {
        await apiClient.post('/entregas/fallida', datos);
    }
}

export default RepartoService;
