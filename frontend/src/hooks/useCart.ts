// hooks/useCart.ts
import { useContext } from 'react';
import { CartContext } from '../context/contexts';
import type { CartContextType } from '../context/contexts';

export function useCart(): CartContextType {
    const ctx = useContext(CartContext);
    if (!ctx) throw new Error('useCart debe usarse dentro de <CartProvider>');
    return ctx;
}
