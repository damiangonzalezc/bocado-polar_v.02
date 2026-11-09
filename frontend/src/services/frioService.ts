// services/frioService.ts · ms-frio (temperaturas, alertas RN-01 y cuenta regresiva de ruta)

import apiClient from './apiClient';
import type { AlertaFrio, CuentaRegresiva, OrigenTemp, RegistroTemperatura } from '../types';

class FrioService {
    static async registrar(origen: OrigenTemp, referenciaId: number, grados: number, registradoPorId?: number) {
        const { data } = await apiClient.post<{ registro: RegistroTemperatura; alerta?: AlertaFrio }>('/temperaturas', {
            origen,
            referenciaId,
            grados,
            registradoPorId,
        });
        return data;
    }

    static async registros(origen?: OrigenTemp): Promise<RegistroTemperatura[]> {
        const { data } = await apiClient.get<RegistroTemperatura[]>('/temperaturas', { params: origen ? { origen } : {} });
        return data;
    }

    static async alertas(atendida?: boolean): Promise<AlertaFrio[]> {
        const { data } = await apiClient.get<AlertaFrio[]>('/alertas-frio', {
            params: atendida === undefined ? {} : { atendida },
        });
        return data;
    }

    static async atender(id: number, usuarioId?: number): Promise<void> {
        await apiClient.patch(`/alertas-frio/${id}/atender`, { usuarioId });
    }

    static async cuentaRegresiva(turnoId: number): Promise<CuentaRegresiva> {
        const { data } = await apiClient.get<CuentaRegresiva>(`/cuentas-regresivas/turno/${turnoId}`);
        return data;
    }
}

export default FrioService;
