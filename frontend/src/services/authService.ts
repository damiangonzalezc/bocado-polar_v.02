// services/authService.ts · ms-auth (registro, login, sesión, usuarios y permisos)

import apiClient from './apiClient';
import type { AuthResponse, EstadoUsuario, RegistroData, Rol, Usuario, UsuarioAdminData } from '../types';

class AuthService {
    static async login(email: string, password: string): Promise<AuthResponse> {
        const { data } = await apiClient.post<AuthResponse>('/auth/login', { email, password });
        return data;
    }

    static async registro(datos: RegistroData): Promise<AuthResponse> {
        const { data } = await apiClient.post<AuthResponse>('/auth/registro', datos);
        return data;
    }

    static async me(): Promise<Usuario> {
        const { data } = await apiClient.get<Usuario>('/auth/me');
        return data;
    }

    static async logout(): Promise<void> {
        await apiClient.post('/auth/logout');
    }


    /* ---------- Administración de usuarios (solo ADMIN) ---------- */

    static async listarUsuarios(filtros: { rol?: Rol | ''; estado?: EstadoUsuario | ''; q?: string } = {}): Promise<Usuario[]> {
        const { data } = await apiClient.get<Usuario[]>('/usuarios', { params: limpiar(filtros) });
        return data;
    }

    static async crearUsuario(datos: UsuarioAdminData): Promise<Usuario> {
        const { data } = await apiClient.post<Usuario>('/usuarios', datos);
        return data;
    }

    static async cambiarEstado(id: number, estado: EstadoUsuario): Promise<Usuario> {
        const { data } = await apiClient.patch<Usuario>(`/usuarios/${id}/estado`, { estado });
        return data;
    }

    static async cambiarRol(id: number, rol: Rol): Promise<Usuario> {
        const { data } = await apiClient.patch<Usuario>(`/usuarios/${id}/rol`, { rol });
        return data;
    }
}

/** Quita filtros vacíos para no enviarlos como parámetros. */
export function limpiar<T extends Record<string, unknown>>(obj: T): Partial<T> {
    return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== undefined && v !== null)) as Partial<T>;
}

export default AuthService;
