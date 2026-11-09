// hooks/useApi.ts
// Carga datos del backend con estados de "cargando" y "error". recargar() vuelve a pedirlos.

import { useCallback, useEffect, useRef, useState } from 'react';
import { mensajeError } from '../services/apiClient';

export interface EstadoApi<T> {
    datos: T | null;
    cargando: boolean;
    error: string;
    recargar: () => Promise<void>;
    setDatos: (d: T | null) => void;
}

export function useApi<T>(cargar: () => Promise<T>, deps: unknown[] = [], activo = true): EstadoApi<T> {
    const [datos, setDatos] = useState<T | null>(null);
    const [cargando, setCargando] = useState(activo);
    const [error, setError] = useState('');
    const ultima = useRef(0);
    const fn = useRef(cargar);
    fn.current = cargar;

    const recargar = useCallback(async () => {
        const id = ++ultima.current;
        setCargando(true);
        setError('');
        try {
            const r = await fn.current();
            if (id === ultima.current) setDatos(r);
        } catch (e) {
            if (id === ultima.current) setError(mensajeError(e));
        } finally {
            if (id === ultima.current) setCargando(false);
        }
    }, []);

    useEffect(() => {
        if (activo) void recargar();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activo, recargar, ...deps]);

    return { datos, cargando, error, recargar, setDatos };
}
