// services/pagoService.ts · ms-pagos (pagos simulados, boletas y cobros contra entrega)

import apiClient, { API_BASE_URL } from './apiClient';
import type { Boleta, MetodoPago, Pago, TipoCobro } from '../types';

class PagoService {
    static async iniciar(pedidoId: number, metodo: MetodoPago, extra: { tipoCobro?: TipoCobro; pagaCon?: number } = {}): Promise<Pago> {
        const { data } = await apiClient.post<Pago>('/pagos', {
            pedidoId,
            metodo,
            idempotencyKey: `pedido-${pedidoId}-${metodo}`,
            ...extra,
        });
        return data;
    }

    /** Simula la confirmación de Webpay / tarjeta. */
    static async confirmarPasarela(pagoId: number, aprobado = true): Promise<Pago> {
        const { data } = await apiClient.post<Pago>('/pagos/webhook', {
            pagoId,
            aprobado,
            codigoAutorizacion: String(Math.floor(100000 + Math.random() * 900000)),
        });
        return data;
    }

    static async delPedido(pedidoId: number): Promise<Pago[]> {
        const { data } = await apiClient.get<Pago[]>(`/pagos/pedido/${pedidoId}`);
        return data;
    }

    static async boletaDelPedido(pedidoId: number): Promise<Boleta> {
        const { data } = await apiClient.get<Boleta>(`/boletas/pedido/${pedidoId}`);
        return data;
    }

    /** URL de la boleta imprimible (se abre en una pestaña nueva). */
    static urlBoleta(boletaId: number): string {
        return `${API_BASE_URL}/boletas/${boletaId}/html`;
    }
}

export default PagoService;
