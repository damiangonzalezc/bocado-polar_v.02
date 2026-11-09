// context/contexts.ts
// Objetos de contexto y sus tipos. Los Providers están en AuthContext.tsx, CartContext.tsx y
// components/ToastNotification.tsx; los hooks para usarlos están en la carpeta hooks/.

import { createContext } from 'react';
import type { Carrito, Cliente, DireccionData, RegistroData, Rol, Seccion, Usuario } from '../types';

/* ---------- Sesión ---------- */
export interface AuthContextType {
    usuario: Usuario | null;
    /** Ficha CRM del usuario (solo rol CLIENTE). Se usa su id para carrito y pedidos. */
    cliente: Cliente | null;
    permisos: Seccion[];
    cargando: boolean;
    estaAutenticado: boolean;
    /** Personal del panel: ADMIN, VENDEDOR, BODEGUERO o SOPORTE. */
    esStaff: boolean;
    tieneRol: (...roles: Rol[]) => boolean;
    puedeVer: (seccion: Seccion) => boolean;
    login: (email: string, password: string) => Promise<Usuario>;
    registro: (datos: RegistroData, direccion: DireccionData) => Promise<Usuario>;
    logout: () => Promise<void>;
    recargarCliente: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | null>(null);

/* ---------- Carrito ---------- */
export interface CartContextType {
    carrito: Carrito | null;
    cargando: boolean;
    error: string;
    cantidadItems: number;
    /** Devuelve '' si se pudo o el mensaje de error (igual que el prototipo). */
    agregar: (productoId: number, kg: number) => Promise<string>;
    fijar: (productoId: number, kg: number) => Promise<string>;
    quitar: (productoId: number) => Promise<string>;
    vaciar: () => Promise<string>;
    refrescar: () => Promise<void>;
}

export const CartContext = createContext<CartContextType | null>(null);

/* ---------- Avisos (toast) ---------- */
export type TipoToast = 'ok' | 'error' | 'info';

export interface ToastContextType {
    mostrar: (mensaje: string, tipo?: TipoToast) => void;
}

export const ToastContext = createContext<ToastContextType | null>(null);
