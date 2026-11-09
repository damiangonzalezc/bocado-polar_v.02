// services/clienteService.ts · ms-clientes (ficha CRM, direcciones y notas)

import apiClient from './apiClient';
import { limpiar } from './authService';
import type { Cliente, Direccion, DireccionData, NotaCliente, SegmentoCliente, TipoNota } from '../types';

class ClienteService {
    static async porUsuario(usuarioId: number): Promise<Cliente> {
        const { data } = await apiClient.get<Cliente>(`/clientes/usuario/${usuarioId}`);
        return data;
    }

    static async listar(filtros: { segmento?: SegmentoCliente | ''; q?: string } = {}): Promise<Cliente[]> {
        const { data } = await apiClient.get<Cliente[]>('/clientes', { params: limpiar(filtros) });
        return data;
    }

    static async agregarDireccion(clienteId: number, datos: DireccionData): Promise<Direccion> {
        const { data } = await apiClient.post<Direccion>(`/clientes/${clienteId}/direcciones`, datos);
        return data;
    }

    static async agregarNota(clienteId: number, tipo: TipoNota, texto: string, autorId?: number): Promise<NotaCliente> {
        const { data } = await apiClient.post<NotaCliente>(`/clientes/${clienteId}/notas`, { tipo, texto, autorId });
        return data;
    }
}

export default ClienteService;
