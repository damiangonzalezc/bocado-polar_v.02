// hooks/useAuth.ts
import { useContext } from 'react';
import { AuthContext } from '../context/contexts';
import type { AuthContextType } from '../context/contexts';

export function useAuth(): AuthContextType {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
    return ctx;
}
