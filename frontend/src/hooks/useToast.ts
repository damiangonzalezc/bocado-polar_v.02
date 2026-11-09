// hooks/useToast.ts
import { useContext } from 'react';
import { ToastContext } from '../context/contexts';
import type { ToastContextType } from '../context/contexts';

export function useToast(): ToastContextType {
    const ctx = useContext(ToastContext);
    if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>');
    return ctx;
}
