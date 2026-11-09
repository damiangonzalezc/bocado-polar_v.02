// services/carritoService.ts · ms-pedidos (carrito en kg)

import apiClient from './apiClient';
import type { Carrito } from '../types';

class CarritoService {
    /** Obtiene o crea el carrito: de un cliente (clienteId) o de un visitante (sesionAnonimaId). */
    static async obtenerOCrear(clienteId?: number, sesionAnonimaId?: string): Promise<Carrito> {
        const { data } = await apiClient.post<Carrito>('/carritos', { clienteId, sesionAnonimaId });
        return data;
    }

    static async obtener(id: number): Promise<Carrito> {
        const { data } = await apiClient.get<Carrito>(`/carritos/${id}`);
        return data;
    }

    static async agregar(id: number, productoId: number, kg: number): Promise<Carrito> {
        const { data } = await apiClient.post<Carrito>(`/carritos/${id}/items`, { productoId, kg });
        return data;
    }

    /** Fija la cantidad exacta (0 kg quita el producto). */
    static async fijar(id: number, productoId: number, kg: number): Promise<Carrito> {
        const { data } = await apiClient.put<Carrito>(`/carritos/${id}/items/${productoId}`, { kg });
        return data;
    }

    static async quitar(id: number, productoId: number): Promise<Carrito> {
        const { data } = await apiClient.delete<Carrito>(`/carritos/${id}/items/${productoId}`);
        return data;
    }

    static async vaciar(id: number): Promise<Carrito> {
        const { data } = await apiClient.delete<Carrito>(`/carritos/${id}/items`);
        return data;
    }

    /** Al iniciar sesión: une el carrito del visitante con el del cliente. */
    static async asociarCliente(id: number, clienteId: number): Promise<Carrito> {
        const { data } = await apiClient.patch<Carrito>(`/carritos/${id}/cliente`, { clienteId });
        return data;
    }
}

export default CarritoService;
