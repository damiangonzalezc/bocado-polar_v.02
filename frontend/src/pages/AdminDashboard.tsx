// pages/AdminDashboard.tsx
// Panel administrativo: barra lateral con las secciones que permite el rol (permisos de ms-auth)
// y el contenido de la sección elegida. Cada sección está en components/admin/.

import React from 'react';
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../hooks/useAuth';
import type { Seccion } from '../types';
import '../styles/admin.css';

interface SeccionAdmin {
    id: Seccion;
    ruta: string;
    nombre: string;
}

const SECCIONES_ADMIN: SeccionAdmin[] = [
    { id: 'resumen', ruta: '/admin', nombre: 'Resumen y KPIs' },
    { id: 'pedidos', ruta: '/admin/pedidos', nombre: 'Pedidos' },
    { id: 'categorias', ruta: '/admin/catalogo', nombre: 'Catálogo' },
    { id: 'inventario', ruta: '/admin/inventario', nombre: 'Inventario y lotes' },
    { id: 'frio', ruta: '/admin/frio', nombre: 'Cadena de frío' },
    { id: 'rutas', ruta: '/admin/rutas', nombre: 'Rutas' },
    { id: 'clientes', ruta: '/admin/clientes', nombre: 'Clientes (CRM)' },
    { id: 'marketing', ruta: '/admin/marketing', nombre: 'Marketing' },
    { id: 'usuarios', ruta: '/admin/usuarios', nombre: 'Usuarios y roles' },
    { id: 'configuracion', ruta: '/admin/configuracion', nombre: 'Configuración' },
];

const AdminDashboard: React.FC = () => {
    const { usuario, puedeVer } = useAuth();
    const { pathname } = useLocation();
    const visibles = SECCIONES_ADMIN.filter((s) => puedeVer(s.id));
    const actual = SECCIONES_ADMIN.find((s) => s.ruta === pathname);

    // Si la URL es una sección sin permiso, se lleva a la primera permitida.
    if (actual && !puedeVer(actual.id) && visibles.length > 0) {
        return <Navigate to={visibles[0].ruta} replace />;
    }

    return (
        <>
            <a className="skip-link" href="#contenido">Saltar al contenido</a>
            <Navbar topbar={['BOCADO_Polar · Administración', 'RBAC · Auditoría · Trazabilidad']} />
            <div className="sidebar-layout">
                <aside className="sidebar" aria-label="Secciones del panel">
                    <h2>{usuario?.rol ?? 'ADMIN'}</h2>
                    {visibles.map((s) => (
                        <NavLink key={s.id} to={s.ruta} end>
                            {s.nombre}
                        </NavLink>
                    ))}
                    {visibles.length === 0 && <p className="muted">Tu rol no tiene secciones asignadas.</p>}
                </aside>
                <main id="contenido" className="content">
                    <Outlet />
                </main>
            </div>
        </>
    );
};

export default AdminDashboard;
