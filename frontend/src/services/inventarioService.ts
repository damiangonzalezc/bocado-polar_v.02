// services/inventarioService.ts · ms-inventario (stock, lotes FEFO, ajustes y alertas)

import apiClient from './apiClient';
import type { AlertaStock, Lote, LoteData, StockProducto, TipoMovimiento } from '../types';

class InventarioService {
    /** Público: stock disponible de todos los productos. */
    static async stockGeneral(): Promise<StockProducto[]> {
        const { data } = await apiClient.get<StockProducto[]>('/stock');
        return data;
    }

    static async stock(productoId: number): Promise<StockProducto> {
        const { data } = await apiClient.get<StockProducto>(`/stock/${productoId}`);
        return data;
    }

    /** Público: lotes utilizables del producto, primero el que vence antes (FEFO). */
    static async lotesFefo(productoId: number): Promise<Lote[]> {
        const { data } = await apiClient.get<Lote[]>(`/lotes/fefo/${productoId}`);
        return data;
    }

    static async listarLotes(incluirAgotados = false): Promise<Lote[]> {
        const { data } = await apiClient.get<Lote[]>('/lotes', { params: { incluirAgotados } });
        return data;
    }

    static async registrarLote(datos: LoteData): Promise<Lote> {
        const { data } = await apiClient.post<Lote>('/lotes', datos);
        return data;
    }

    static async ajustar(loteId: number, tipo: TipoMovimiento, kg: number, motivo: string, usuarioId?: number): Promise<void> {
        await apiClient.post(`/lotes/${loteId}/ajustes`, { tipo, kg, motivo, usuarioId });
    }

    static async bloquear(loteId: number, motivo: string): Promise<Lote> {
        const { data } = await apiClient.patch<Lote>(`/lotes/${loteId}/bloqueo`, { motivo });
        return data;
    }

    static async desbloquear(loteId: number): Promise<Lote> {
        const { data } = await apiClient.patch<Lote>(`/lotes/${loteId}/desbloqueo`);
        return data;
    }

    static async alertas(atendida?: boolean): Promise<AlertaStock[]> {
        const { data } = await apiClient.get<AlertaStock[]>('/alertas-stock', {
            params: atendida === undefined ? {} : { atendida },
        });
        return data;
    }

    static async resolverAlerta(id: number): Promise<void> {
        await apiClient.patch(`/alertas-stock/${id}/resolver`);
    }
}

export default InventarioService;
