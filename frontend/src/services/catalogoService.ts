// services/catalogoService.ts · ms-catalogo (productos, categorías y valoraciones)

import apiClient from './apiClient';
import type { Categoria, Producto, ProductoData, ResumenValoracion, StockProducto } from '../types';

export type OrdenCatalogo = 'nombre' | 'precioAsc' | 'precioDesc' | 'fefo' | 'valoracion';

class CatalogoService {
    static quitarTildes(t: string): string {
        return t.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    /** Filtra y ordena el catálogo en el navegador (búsqueda sin tildes, categoría y orden). */
    static filtrar(
        productos: Producto[],
        stock: Map<number, StockProducto>,
        texto: string,
        categoriaId: string,
        orden: OrdenCatalogo,
    ): Producto[] {
        const q = this.quitarTildes(texto.trim());
        const venc = (p: Producto) => stock.get(p.id)?.proximoVencimiento ?? '9999-12-31';
        const ordenadores: Record<OrdenCatalogo, (a: Producto, b: Producto) => number> = {
            nombre: (a, b) => a.nombre.localeCompare(b.nombre, 'es'),
            precioAsc: (a, b) => a.precioKg - b.precioKg,
            precioDesc: (a, b) => b.precioKg - a.precioKg,
            fefo: (a, b) => venc(a).localeCompare(venc(b)),
            valoracion: (a, b) => b.valoracionProm - a.valoracionProm,
        };
        return productos
            .filter((p) => !q || this.quitarTildes(p.nombre).includes(q) || this.quitarTildes(p.origen ?? '').includes(q))
            .filter((p) => !categoriaId || String(p.categoria.id) === categoriaId)
            .sort(ordenadores[orden]);
    }

    static async listarProductos(incluirInactivos = false): Promise<Producto[]> {
        const { data } = await apiClient.get<Producto[]>('/productos', { params: { incluirInactivos } });
        return data;
    }

    static async obtenerProducto(id: number): Promise<Producto> {
        const { data } = await apiClient.get<Producto>(`/productos/${id}`);
        return data;
    }

    static async crearProducto(datos: ProductoData): Promise<Producto> {
        const { data } = await apiClient.post<Producto>('/productos', datos);
        return data;
    }

    static async actualizarProducto(id: number, datos: ProductoData): Promise<Producto> {
        const { data } = await apiClient.put<Producto>(`/productos/${id}`, datos);
        return data;
    }

    static async cambiarActivo(id: number, activo: boolean): Promise<Producto> {
        const { data } = await apiClient.patch<Producto>(`/productos/${id}/activo`, { activo });
        return data;
    }

    static async eliminarProducto(id: number): Promise<void> {
        await apiClient.delete(`/productos/${id}`);
    }

    static async listarCategorias(): Promise<Categoria[]> {
        const { data } = await apiClient.get<Categoria[]>('/categorias');
        return data;
    }

    static async crearCategoria(nombre: string, descripcion: string): Promise<Categoria> {
        const { data } = await apiClient.post<Categoria>('/categorias', { nombre, descripcion });
        return data;
    }

    static async eliminarCategoria(id: number): Promise<void> {
        await apiClient.delete(`/categorias/${id}`);
    }

    static async resumenValoracion(productoId: number, clienteId?: number): Promise<ResumenValoracion> {
        const { data } = await apiClient.get<ResumenValoracion>(`/productos/${productoId}/valoraciones`, {
            params: clienteId ? { clienteId } : {},
        });
        return data;
    }

    static async valorar(productoId: number, clienteId: number, estrellas: number): Promise<ResumenValoracion> {
        const { data } = await apiClient.post<ResumenValoracion>(`/productos/${productoId}/valoraciones`, { clienteId, estrellas });
        return data;
    }
}

export default CatalogoService;
