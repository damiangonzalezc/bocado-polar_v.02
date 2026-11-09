// services/configService.ts · ms-config (zonas de envío, IVA y reglas de negocio)

import apiClient from './apiClient';
import type { ConfiguracionGlobal, ReglaNegocio, ZonaEnvio } from '../types';

class ConfigService {
    static async zonas(soloActivas = true): Promise<ZonaEnvio[]> {
        const { data } = await apiClient.get<ZonaEnvio[]>('/zonas', { params: soloActivas ? { activas: true } : {} });
        return data;
    }

    static async actualizarZona(zona: ZonaEnvio): Promise<ZonaEnvio> {
        const { data } = await apiClient.put<ZonaEnvio>(`/zonas/${zona.id}`, {
            comuna: zona.comuna,
            tarifa: zona.tarifa,
            activa: zona.activa,
        });
        return data;
    }

    static async crearZona(comuna: string, tarifa: number): Promise<ZonaEnvio> {
        const { data } = await apiClient.post<ZonaEnvio>('/zonas', { comuna, tarifa, activa: true });
        return data;
    }

    static async configuracion(): Promise<ConfiguracionGlobal> {
        const { data } = await apiClient.get<ConfiguracionGlobal>('/configuracion');
        return data;
    }

    static async guardarConfiguracion(c: Omit<ConfiguracionGlobal, 'id'>): Promise<ConfiguracionGlobal> {
        const { data } = await apiClient.put<ConfiguracionGlobal>('/configuracion', c);
        return data;
    }

    static async reglas(): Promise<ReglaNegocio[]> {
        const { data } = await apiClient.get<ReglaNegocio[]>('/reglas');
        return data;
    }

    static async actualizarRegla(codigo: string, valor: number): Promise<ReglaNegocio> {
        const { data } = await apiClient.put<ReglaNegocio>(`/reglas/${codigo}`, { valor });
        return data;
    }
}

export default ConfigService;
