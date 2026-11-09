// components/ProtectedRoute.tsx
// Protege una vista por sesión y rol. Si no corresponde muestra "Acceso restringido" (como el prototipo).

import React from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import Loader from './Loader';
import type { Rol } from '../types';

interface ProtectedRouteProps {
    children: ReactNode;
    roles?: Rol[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, roles }) => {
    const { usuario, cargando } = useAuth();
    const { pathname } = useLocation();

    if (cargando) return <Loader texto="Verificando sesión…" />;

    if (usuario && (!roles || roles.includes(usuario.rol))) return <>{children}</>;

    return (
        <section className="card empty" aria-labelledby="acceso-titulo">
            <h1 id="acceso-titulo">Acceso restringido</h1>
            <p>
                {usuario
                    ? `Tu rol (${usuario.rol}) no tiene permiso para ver esta sección.`
                    : 'Debes iniciar sesión para ver esta sección.'}
            </p>
            {!usuario && (
                <div className="btn-row">
                    <Link className="btn" to={`/login?volver=${encodeURIComponent(pathname)}`}>Ir a iniciar sesión</Link>
                    <Link className="btn secondary" to={`/registro?volver=${encodeURIComponent(pathname)}`}>Crear cuenta</Link>
                </div>
            )}
        </section>
    );
};

export default ProtectedRoute;
