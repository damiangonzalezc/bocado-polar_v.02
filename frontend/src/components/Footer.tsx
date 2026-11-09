// components/Footer.tsx
import React from 'react';
import { Link } from 'react-router-dom';

const Footer: React.FC = () => (
    <footer className="footer">
        <div className="footer-grid">
            <section aria-labelledby="f-marca">
                <h2 id="f-marca">BOCADO_Polar</h2>
                <p>Pescados y mariscos congelados con cadena de frío controlada y entrega refrigerada en el Gran Concepción.</p>
            </section>
            <nav aria-labelledby="f-nav">
                <h2 id="f-nav">Navegación</h2>
                <ul>
                    <li><Link to="/">Inicio</Link></li>
                    <li><Link to="/catalogo">Catálogo</Link></li>
                    <li><Link to="/carrito">Carrito</Link></li>
                    <li><Link to="/seguimiento">Seguimiento de pedido</Link></li>
                    <li><Link to="/login">Iniciar sesión</Link></li>
                    <li><Link to="/registro">Crear cuenta</Link></li>
                </ul>
            </nav>
            <section aria-labelledby="f-contacto">
                <h2 id="f-contacto">Contacto</h2>
                <address>
                    <span>contacto@bocadopolar.cl</span>
                    <span>+56 41 200 0000</span>
                    <span>Concepción, Región del Biobío</span>
                </address>
                <p>
                    <a href="https://www.instagram.com/" target="_blank" rel="noopener noreferrer">Instagram</a> ·{' '}
                    <a href="https://www.facebook.com/" target="_blank" rel="noopener noreferrer">Facebook</a>
                </p>
            </section>
        </div>
        <p className="footer-legal">BOCADO_Polar · Proyecto académico (Duoc UC). Datos de contacto de ejemplo.</p>
    </footer>
);

export default Footer;
