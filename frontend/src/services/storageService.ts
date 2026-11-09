// services/storageService.ts
// Acceso seguro a localStorage / sessionStorage (no falla si el navegador lo bloquea).

import type { BorradorPedido } from '../types';

const KEYS = {
    token: 'bocadoToken',
    permisos: 'bocadoPermisos',
    carritoId: 'bocadoCarritoId',
    sesionAnonima: 'bocadoSesionAnonima',
    borrador: 'bocadoBorradorPedido',
    visita: 'bocadoVisita',
} as const;

class StorageService {
    private static leer(store: Storage, clave: string): string | null {
        try {
            return store.getItem(clave);
        } catch {
            return null;
        }
    }

    private static guardar(store: Storage, clave: string, valor: string | null): void {
        try {
            if (valor === null) store.removeItem(clave);
            else store.setItem(clave, valor);
        } catch {
            /* almacenamiento no disponible: se ignora */
        }
    }

    /* ---------- Sesión (token JWT) ---------- */
    static getToken(): string | null {
        return this.leer(localStorage, KEYS.token);
    }

    static setToken(token: string | null): void {
        this.guardar(localStorage, KEYS.token, token);
    }

    /** Secciones del panel que vio el login (solo para pintar el menú; el backend valida igual). */
    static getPermisos<T extends string>(): T[] {
        const v = this.leer(localStorage, KEYS.permisos);
        try {
            return v ? (JSON.parse(v) as T[]) : [];
        } catch {
            return [];
        }
    }

    static setPermisos(permisos: string[] | null): void {
        this.guardar(localStorage, KEYS.permisos, permisos === null ? null : JSON.stringify(permisos));
    }

    /* ---------- Carrito ---------- */
    static getCarritoId(): number | null {
        const v = this.leer(localStorage, KEYS.carritoId);
        return v ? Number(v) : null;
    }

    static setCarritoId(id: number | null): void {
        this.guardar(localStorage, KEYS.carritoId, id === null ? null : String(id));
    }

    static getSesionAnonima(): string | null {
        return this.leer(localStorage, KEYS.sesionAnonima);
    }

    static setSesionAnonima(id: string | null): void {
        this.guardar(localStorage, KEYS.sesionAnonima, id);
    }

    /* ---------- Borrador del pedido (entre "Pedido" y "Pago") ---------- */
    static getBorrador(): BorradorPedido | null {
        const v = this.leer(sessionStorage, KEYS.borrador);
        if (!v) return null;
        try {
            return JSON.parse(v) as BorradorPedido;
        } catch {
            return null;
        }
    }

    static setBorrador(b: BorradorPedido | null): void {
        this.guardar(sessionStorage, KEYS.borrador, b === null ? null : JSON.stringify(b));
    }

    /* ---------- Visitas (una por sesión del navegador) ---------- */
    static visitaRegistrada(): boolean {
        return this.leer(sessionStorage, KEYS.visita) === '1';
    }

    static marcarVisita(): void {
        this.guardar(sessionStorage, KEYS.visita, '1');
    }
}

export default StorageService;
