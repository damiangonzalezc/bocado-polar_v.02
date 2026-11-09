// hooks/useAgregarAlCarrito.ts
// Botón "Agregar 1 kg" de las tarjetas: agrega al carrito y muestra el aviso.

import { useState } from 'react';
import { useCart } from './useCart';
import { useToast } from './useToast';
import type { Producto } from '../types';

export function useAgregarAlCarrito() {
    const { agregar: agregarKg } = useCart();
    const { mostrar } = useToast();
    const [agregando, setAgregando] = useState<number | null>(null);

    const agregar = async (producto: Producto, kg = 1) => {
        setAgregando(producto.id);
        const error = await agregarKg(producto.id, kg);
        setAgregando(null);
        mostrar(error || `${String(kg).replace('.', ',')} kg de ${producto.nombre} agregado${kg === 1 ? '' : 's'} al carrito`, error ? 'error' : 'ok');
        return !error;
    };

    return { agregar, agregando };
}
