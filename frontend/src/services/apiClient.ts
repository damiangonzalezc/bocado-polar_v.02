// services/apiClient.ts
// Cliente HTTP único (axios) apuntando al API Gateway. Agrega el token y normaliza los errores.

import axios, { AxiosError } from 'axios';
import type { ApiError } from '../types';
import StorageService from './storageService';

export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/** Evento que se dispara cuando el backend responde 401 (sesión vencida o token inválido). */
export const EVENTO_SESION_EXPIRADA = 'bocado:sesion-expirada';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: { 'Content-Type': 'application/json' },
    timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
    const token = StorageService.getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

apiClient.interceptors.response.use(
    (response) => response,
    (error: AxiosError<ApiError>) => {
        const url = error.config?.url ?? '';
        if (error.response?.status === 401 && !url.includes('/auth/login')) {
            window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
        }
        return Promise.reject(error);
    },
);

/** Mensaje legible para mostrar al usuario a partir de cualquier error de la API. */
export function mensajeError(error: unknown, porDefecto = 'Ocurrió un error inesperado.'): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as Partial<ApiError> | undefined;
        if (data?.mensaje) return data.mensaje;
        if (!error.response) return 'No se pudo conectar con el servidor. Revisa que el backend esté encendido.';
        if (error.response.status === 403) return 'No tienes permiso para realizar esta acción.';
    }
    return porDefecto;
}

/** Errores por campo que devuelve el backend en "detalles" (validaciones @Valid). */
export function erroresDeCampo(error: unknown): Record<string, string> {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as Partial<ApiError> | undefined;
        return data?.detalles ?? {};
    }
    return {};
}

/** Código HTTP del error (0 si no hubo respuesta). */
export function estadoHttp(error: unknown): number {
    return axios.isAxiosError(error) ? error.response?.status ?? 0 : 0;
}

export default apiClient;
