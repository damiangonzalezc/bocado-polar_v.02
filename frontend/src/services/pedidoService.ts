// services/pedidoService.ts · ms-pedidos (checkout, estados, seguimiento)

import apiClient from './apiClient';
import { limpiar } from './authService';
import type { CheckoutData, Cotizacion, EstadoPedido, NotaInterna, Pedido, Seguimiento } from '../types';

/** Transiciones permitidas hacia adelante (las mismas que valida ms-pedidos). */
export const TRANSICIONES: Record<EstadoPedido, EstadoPedido[]> = {
    PENDIENTE_PAGO: ['PAGADO', 'EN_PREPARACION', 'CANCELADO'],
    PAGADO: ['EN_PREPARACION', 'CANCELADO', 'REEMBOLSADO'],
    EN_PREPARACION: ['ENVIADO', 'CANCELADO', 'REEMBOLSADO'],
    ENVIADO: ['ENTREGADO', 'REEMBOLSADO'],
    ENTREGADO: ['REEMBOLSADO'],
    CANCELADO: ['REEMBOLSADO'],
    REEMBOLSADO: [],
};

export const HORARIOS = ['09:00 - 12:00', '12:00 - 15:00', '15:00 - 18:00'] as const;

export const PASOS_COMPRA = ['1 Pedido', '2 Entrega', '3 Pago', '4 Confirmación'];

/** Estados en que el admin puede asignar o cambiar el repartidor (igual que ms-pedidos). */
export const puedeAsignarse = (p: Pedido): boolean =>
    p.estado === 'PAGADO' || p.estado === 'EN_PREPARACION' || p.estado === 'ENVIADO' ||
    (p.estado === 'PENDIENTE_PAGO' && p.metodoPago === 'CONTRA_ENTREGA');

class PedidoService {
    static async cotizar(carritoId: number, comuna: string, clienteId?: number, codigoCupon?: string): Promise<Cotizacion> {
        const { data } = await apiClient.post<Cotizacion>('/pedidos/cotizar', { carritoId, comuna, clienteId, codigoCupon });
        return data;
    }

    static async checkout(datos: CheckoutData): Promise<Pedido> {
        const { data } = await apiClient.post<Pedido>('/pedidos/checkout', datos);
        return data;
    }

    static async listar(filtros: { estado?: EstadoPedido | ''; q?: string; desde?: string; hasta?: string } = {}): Promise<Pedido[]> {
        const { data } = await apiClient.get<Pedido[]>('/pedidos', { params: limpiar(filtros) });
        return data;
    }

    static async obtener(id: number): Promise<Pedido> {
        const { data } = await apiClient.get<Pedido>(`/pedidos/${id}`);
        return data;
    }

    static async porCodigo(codigo: string): Promise<Pedido> {
        const { data } = await apiClient.get<Pedido>(`/pedidos/codigo/${encodeURIComponent(codigo.trim().toUpperCase())}`);
        return data;
    }

    static async delCliente(clienteId: number): Promise<Pedido[]> {
        const { data } = await apiClient.get<Pedido[]>(`/pedidos/cliente/${clienteId}`);
        return data;
    }

    static async seguimiento(id: number): Promise<Seguimiento> {
        const { data } = await apiClient.get<Seguimiento>(`/pedidos/${id}/seguimiento`);
        return data;
    }

    static async cambiarEstado(id: number, estado: EstadoPedido, nota?: string, usuarioId?: number): Promise<Pedido> {
        const { data } = await apiClient.patch<Pedido>(`/pedidos/${id}/estado`, { estado, nota, usuarioId });
        return data;
    }

    static async retroceder(id: number, motivo: string, usuarioId?: number): Promise<Pedido> {
        const { data } = await apiClient.patch<Pedido>(`/pedidos/${id}/retroceder`, { motivo, usuarioId });
        return data;
    }

    static async cancelar(id: number, motivo: string, usuarioId?: number): Promise<Pedido> {
        const { data } = await apiClient.patch<Pedido>(`/pedidos/${id}/cancelar`, { motivo, usuarioId });
        return data;
    }

    static async agregarNota(id: number, texto: string, autorId?: number): Promise<NotaInterna> {
        const { data } = await apiClient.post<NotaInterna>(`/pedidos/${id}/notas`, { texto, autorId });
        return data;
    }
}

export default PedidoService;
