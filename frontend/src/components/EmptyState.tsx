// components/EmptyState.tsx
// Tarjeta vacía con mensaje y acción opcional (ej: "Tu carrito está vacío · Ir al catálogo").

import React from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface EmptyStateProps {
    mensaje: ReactNode;
    accion?: { texto: string; a: string };
    children?: ReactNode;
}

const EmptyState: React.FC<EmptyStateProps> = ({ mensaje, accion, children }) => (
    <div className="card empty">
        <p>{mensaje}</p>
        {children}
        {accion && (
            <Link className="btn" to={accion.a}>
                {accion.texto}
            </Link>
        )}
    </div>
);

export default EmptyState;
