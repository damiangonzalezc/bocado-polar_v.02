// components/Navbar.tsx
// Barra superior + navegación principal (como el prototipo V3: sin botón «Menú»),
// contador del carrito, modo oscuro y sesión.

import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import ThemeToggle from './ThemeToggle';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';

interface NavbarProps {
    topbar?: [string, string];
}

const Navbar: React.FC<NavbarProps> = ({ topbar = ['BOCADO_Polar · Productos marinos', 'Cadena de frío · Trazabilidad · Entrega'] }) => {
    const { usuario, esStaff, tieneRol, logout } = useAuth();
    const { cantidadItems } = useCart();
    const navigate = useNavigate();

    const salir = async () => {
        await logout();
        navigate('/');
    };

    return (
        <header>
            <div className="topbar">
                <span>{topbar[0]}</span>
                <span>{topbar[1]}</span>
            </div>
            <nav className="navbar" aria-label="Navegación principal">
                <Link className="brand" to="/">
                    <img src="/img/logo.svg" alt="" width={36} height={36} /> BOCADO_Polar
                </Link>
                <ul className="navlinks" id="menu">
                    <li><NavLink to="/" end>Inicio</NavLink></li>
                    <li><NavLink to="/catalogo">Catálogo</NavLink></li>
                    <li>
                        <NavLink to="/carrito">
                            Carrito <span className="badge" aria-label="productos en el carrito">{cantidadItems}</span>
                        </NavLink>
                    </li>
                    <li><NavLink to="/seguimiento">Seguimiento</NavLink></li>
                    {esStaff && <li><NavLink to="/admin">Panel admin</NavLink></li>}
                    {tieneRol('REPARTIDOR', 'ADMIN') && <li><NavLink to="/repartidor">Repartidor</NavLink></li>}
                    <li><ThemeToggle /></li>
                    <li className="auth-slot">
                        {usuario ? (
                            <>
                                <span className="muted">Hola, {usuario.nombre.split(' ')[0]}</span>
                                <button type="button" className="btn secondary small" onClick={salir}>Salir</button>
                            </>
                        ) : (
                            <>
                                <Link className="btn secondary small" to="/login">Iniciar sesión</Link>
                                <Link className="btn small" to="/registro">Crear cuenta</Link>
                            </>
                        )}
                    </li>
                </ul>
            </nav>
        </header>
    );
};

export default Navbar;
