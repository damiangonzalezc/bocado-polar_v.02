// context/AuthContext.tsx
// Sesión del usuario contra ms-auth (JWT). El token vive en localStorage y se valida con /auth/me al recargar.

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { AuthContext } from './contexts';
import type { AuthContextType } from './contexts';
import AuthService from '../services/authService';
import ClienteService from '../services/clienteService';
import StorageService from '../services/storageService';
import { EVENTO_SESION_EXPIRADA } from '../services/apiClient';
import type { Cliente, DireccionData, RegistroData, Rol, Seccion, Usuario } from '../types';

const ROLES_STAFF: Rol[] = ['ADMIN', 'VENDEDOR', 'BODEGUERO', 'SOPORTE'];

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [usuario, setUsuario] = useState<Usuario | null>(null);
    const [cliente, setCliente] = useState<Cliente | null>(null);
    const [permisos, setPermisos] = useState<Seccion[]>([]);
    const [cargando, setCargando] = useState(true);

    const limpiarSesion = useCallback(() => {
        StorageService.setToken(null);
        StorageService.setPermisos(null);
        setUsuario(null);
        setCliente(null);
        setPermisos([]);
    }, []);

    const cargarCliente = useCallback(async (u: Usuario | null) => {
        if (!u || u.rol !== 'CLIENTE') {
            setCliente(null);
            return;
        }
        try {
            setCliente(await ClienteService.porUsuario(u.id));
        } catch {
            setCliente(null);
        }
    }, []);

    // Al abrir la app: si hay token, se valida con el backend.
    useEffect(() => {
        let activo = true;
        (async () => {
            if (!StorageService.getToken()) {
                setCargando(false);
                return;
            }
            try {
                const u = await AuthService.me();
                if (!activo) return;
                setUsuario(u);
                setPermisos(StorageService.getPermisos<Seccion>());
                await cargarCliente(u);
            } catch {
                if (activo) limpiarSesion();
            } finally {
                if (activo) setCargando(false);
            }
        })();
        return () => {
            activo = false;
        };
    }, [cargarCliente, limpiarSesion]);

    // Si el backend responde 401 en cualquier llamada, se cierra la sesión local.
    useEffect(() => {
        const alExpirar = () => limpiarSesion();
        window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
        return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    }, [limpiarSesion]);

    const login = useCallback(async (email: string, password: string) => {
        const r = await AuthService.login(email.trim().toLowerCase(), password);
        StorageService.setToken(r.token);
        StorageService.setPermisos(r.permisos);
        setPermisos(r.permisos);
        setUsuario(r.usuario);
        await cargarCliente(r.usuario);
        return r.usuario;
    }, [cargarCliente]);

    const registro = useCallback(async (datos: RegistroData, direccion: DireccionData) => {
        const r = await AuthService.registro(datos);
        StorageService.setToken(r.token);
        StorageService.setPermisos(r.permisos);
        setPermisos(r.permisos);
        setUsuario(r.usuario);
        try {
            const c = await ClienteService.porUsuario(r.usuario.id);
            await ClienteService.agregarDireccion(c.id, { ...direccion, principal: true });
        } catch {
            /* la cuenta ya existe: la dirección se puede agregar después */
        }
        await cargarCliente(r.usuario);
        return r.usuario;
    }, [cargarCliente]);

    const logout = useCallback(async () => {
        try {
            await AuthService.logout();
        } catch {
            /* aunque falle el backend, la sesión local se cierra */
        }
        limpiarSesion();
    }, [limpiarSesion]);

    const recargarCliente = useCallback(() => cargarCliente(usuario), [cargarCliente, usuario]);

    const valor = useMemo<AuthContextType>(() => ({
        usuario,
        cliente,
        permisos,
        cargando,
        estaAutenticado: usuario !== null,
        esStaff: usuario !== null && ROLES_STAFF.includes(usuario.rol),
        tieneRol: (...roles) => usuario !== null && roles.includes(usuario.rol),
        puedeVer: (seccion) => usuario?.rol === 'ADMIN' || permisos.includes(seccion),
        login,
        registro,
        logout,
        recargarCliente,
    }), [usuario, cliente, permisos, cargando, login, registro, logout, recargarCliente]);

    return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
};
