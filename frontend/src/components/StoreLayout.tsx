// components/StoreLayout.tsx
// Estructura de las páginas de la tienda: enlace "saltar al contenido", cabecera, contenido y pie.

import React, { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';

const StoreLayout: React.FC = () => {
    const { pathname } = useLocation();
    useEffect(() => {
        window.scrollTo(0, 0);
    }, [pathname]);

    return (
        <div className="app-shell">
            <a className="skip-link" href="#contenido">Saltar al contenido</a>
            <Navbar />
            <main id="contenido" className="container">
                <Outlet />
            </main>
            <Footer />
        </div>
    );
};

export default StoreLayout;
