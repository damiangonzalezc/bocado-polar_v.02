// context/CartContext.tsx
// Carrito guardado en ms-pedidos. Un visitante usa un carrito anónimo (sesionAnonimaId);
// al iniciar sesión como cliente ese carrito se une al del cliente.

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { CartContext } from './contexts';
import type { CartContextType } from './contexts';
import { useAuth } from '../hooks/useAuth';
import CarritoService from '../services/carritoService';
import StorageService from '../services/storageService';
import { mensajeError } from '../services/apiClient';
import type { Carrito } from '../types';

function nuevoId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
    return `anon-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export const CartProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const { cliente, cargando: cargandoSesion } = useAuth();
    const [carrito, setCarrito] = useState<Carrito | null>(null);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');

    const guardar = (c: Carrito) => {
        setCarrito(c);
        StorageService.setCarritoId(c.id);
        if (c.sesionAnonimaId) StorageService.setSesionAnonima(c.sesionAnonimaId);
    };

    const cargar = useCallback(async () => {
        try {
            if (cliente) {
                let c: Carrito | null = null;
                const previo = StorageService.getCarritoId();
                if (previo) {
                    try {
                        const anterior = await CarritoService.obtener(previo);
                        if (!anterior.clienteId && anterior.items.length > 0) {
                            c = await CarritoService.asociarCliente(previo, cliente.id);
                        }
                    } catch {
                        /* el carrito anterior ya no existe */
                    }
                }
                guardar(c ?? (await CarritoService.obtenerOCrear(cliente.id)));
            } else {
                const sesion = StorageService.getSesionAnonima() ?? nuevoId();
                StorageService.setSesionAnonima(sesion);
                guardar(await CarritoService.obtenerOCrear(undefined, sesion));
            }
            setError('');
        } catch (e) {
            setCarrito(null);
            setError(mensajeError(e, 'No se pudo cargar el carrito.'));
        } finally {
            setCargando(false);
        }
    }, [cliente]);

    useEffect(() => {
        // Carga asíncrona: el estado se actualiza después de responder el backend.
        // oxlint-disable-next-line react/set-state-in-effect
        if (!cargandoSesion) void cargar();
    }, [cargandoSesion, cargar]);

    /** Ejecuta una operación sobre el carrito y devuelve '' o el mensaje de error. */
    const operar = useCallback(async (fn: (id: number) => Promise<Carrito>): Promise<string> => {
        if (!carrito) return error || 'El carrito no está disponible.';
        try {
            setCarrito(await fn(carrito.id));
            return '';
        } catch (e) {
            return mensajeError(e, 'No se pudo actualizar el carrito.');
        }
    }, [carrito, error]);

    const valor = useMemo<CartContextType>(() => ({
        carrito,
        cargando,
        error,
        cantidadItems: carrito?.items.length ?? 0,
        agregar: (productoId, kg) => operar((id) => CarritoService.agregar(id, productoId, kg)),
        fijar: (productoId, kg) => operar((id) => CarritoService.fijar(id, productoId, kg)),
        quitar: (productoId) => operar((id) => CarritoService.quitar(id, productoId)),
        vaciar: () => operar((id) => CarritoService.vaciar(id)),
        refrescar: cargar,
    }), [carrito, cargando, error, operar, cargar]);

    return <CartContext.Provider value={valor}>{children}</CartContext.Provider>;
};
