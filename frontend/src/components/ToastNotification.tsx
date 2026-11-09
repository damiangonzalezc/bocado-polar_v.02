// components/ToastNotification.tsx
// Avisos no bloqueantes (reemplazan a alert), con región accesible aria-live.

import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { ToastContext } from '../context/contexts';
import type { TipoToast } from '../context/contexts';

interface Toast {
    id: number;
    mensaje: string;
    tipo: TipoToast;
}

export const ToastProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [toasts, setToasts] = useState<Toast[]>([]);
    const contador = useRef(0);

    const mostrar = useCallback((mensaje: string, tipo: TipoToast = 'ok') => {
        const id = ++contador.current;
        setToasts((t) => [...t, { id, mensaje, tipo }]);
        setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3500);
    }, []);

    const valor = useMemo(() => ({ mostrar }), [mostrar]);

    return (
        <ToastContext.Provider value={valor}>
            {children}
            <div className="toast-region" role="status" aria-live="polite">
                {toasts.map((t) => (
                    <div key={t.id} className={`toast ${t.tipo === 'info' ? '' : t.tipo}`}>
                        {t.mensaje}
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
};
