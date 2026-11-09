// hooks/useTheme.ts · Tema actual y acción para alternarlo.

import { useCallback, useEffect, useState } from 'react';
import ThemeService, { type Tema } from '../services/themeService';

const EVENTO = 'bocado:tema';

export const useTheme = () => {
    const [tema, setTema] = useState<Tema>(() => ThemeService.inicial());

    // Mantiene sincronizados todos los botones de tema y sigue al sistema si el usuario no eligió.
    useEffect(() => {
        ThemeService.aplicar(tema);
        const alCambiar = (e: Event) => setTema((e as CustomEvent<Tema>).detail);
        const media = window.matchMedia?.('(prefers-color-scheme: dark)');
        const alSistema = () => {
            if (!ThemeService.guardado()) setTema(ThemeService.delSistema());
        };
        window.addEventListener(EVENTO, alCambiar);
        media?.addEventListener?.('change', alSistema);
        return () => {
            window.removeEventListener(EVENTO, alCambiar);
            media?.removeEventListener?.('change', alSistema);
        };
    }, [tema]);

    const alternar = useCallback(() => {
        const nuevo = ThemeService.opuesto(tema);
        ThemeService.guardar(nuevo);
        ThemeService.aplicar(nuevo);
        window.dispatchEvent(new CustomEvent<Tema>(EVENTO, { detail: nuevo }));
    }, [tema]);

    return { tema, oscuro: tema === 'dark', alternar };
};
