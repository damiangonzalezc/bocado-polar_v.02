// pages/Home.tsx · Inicio (hero, cómo comprar, destacados FEFO, ventajas y video)

import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import Loader from '../components/Loader';
import ErrorNotice from '../components/ErrorNotice';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../hooks/useAuth';
import { useAgregarAlCarrito } from '../hooks/useAgregarAlCarrito';
import CatalogoService from '../services/catalogoService';
import InventarioService from '../services/inventarioService';
import Steps from '../components/Steps';

const Home: React.FC = () => {
    const { usuario } = useAuth();
    const productos = useApi(() => CatalogoService.listarProductos());
    const stock = useApi(() => InventarioService.stockGeneral().catch(() => []));
    const { agregar, agregando } = useAgregarAlCarrito();

    // Destacados: los de vencimiento más próximo con stock (rotación FEFO).
    const destacados = useMemo(() => {
        const porId = new Map((stock.datos ?? []).map((s) => [s.productoId, s]));
        return [...(productos.datos ?? [])]
            .filter((p) => (porId.get(p.id)?.disponibleKg ?? 0) >= 1)
            .sort((a, b) => (porId.get(a.id)?.proximoVencimiento ?? '9999').localeCompare(porId.get(b.id)?.proximoVencimiento ?? '9999'))
            .slice(0, 3)
            .map((p) => ({ p, s: porId.get(p.id) }));
    }, [productos.datos, stock.datos]);

    return (
        <>
            <section className="hero" aria-labelledby="hero-titulo">
                <div>
                    <h1 id="hero-titulo">BOCADO_Polar</h1>
                    <p className="lead">
                        Pescados y mariscos congelados con cadena de frío controlada, trazabilidad por lote y entrega refrigerada
                        en Concepción, Chiguayante, Tomé y Penco.
                    </p>
                    <div className="btn-row">
                        <Link className="btn warning" to="/catalogo">Ir al catálogo</Link>
                        {!usuario && <Link className="btn secondary" to="/registro">Crear cuenta</Link>}
                    </div>
                </div>
                <img src="/img/hero.svg" alt="Ilustración de un salmón sobre olas" width={520} height={340} />
            </section>

            <section aria-labelledby="flujo-titulo">
                <div className="section-title"><h2 id="flujo-titulo">Cómo comprar</h2></div>
                <Steps
                    etiqueta="Flujo de compra"
                    actual={-1}
                    pasos={['1 Registro', '2 Catálogo', '3 Carrito', '4 Pedido', '5 Pago', '6 Seguimiento', '7 Entrega']}
                />
            </section>

            <section aria-labelledby="dest-titulo">
                <div className="section-title">
                    <h2 id="dest-titulo">Productos destacados</h2>
                    <Link className="btn secondary small" to="/catalogo">Ver todo el catálogo</Link>
                </div>
                <p className="muted">Lotes con vencimiento más próximo: salen primero por rotación FEFO.</p>
                <ErrorNotice mensaje={productos.error} onReintentar={productos.recargar} />
                {productos.cargando ? (
                    <Loader />
                ) : (
                    <div className="grid">
                        {destacados.map(({ p, s }) => (
                            <ProductCard key={p.id} producto={p} stock={s} onAgregar={agregar} agregando={agregando === p.id} />
                        ))}
                    </div>
                )}
            </section>

            <section aria-labelledby="ventajas-titulo">
                <div className="section-title"><h2 id="ventajas-titulo">Por qué BOCADO_Polar</h2></div>
                <div className="grid">
                    <article className="card">
                        <h3>❄️ Cadena de frío</h3>
                        <p>Cada lote se almacena y transporta entre -14°C y -18°C, con alerta si supera -15°C (RN-01).</p>
                        <span className="tag">EP06</span>
                    </article>
                    <article className="card">
                        <h3>🏷️ Trazabilidad FEFO</h3>
                        <p>Primero en vencer, primero en salir: el lote y su vencimiento visibles en cada producto.</p>
                        <span className="tag">EP08</span>
                    </article>
                    <article className="card">
                        <h3>🚚 Entrega refrigerada</h3>
                        <p>Elige comuna y bloque horario, y sigue tu pedido desde el pago hasta la puerta.</p>
                        <span className="tag">EP18 · EP20</span>
                    </article>
                </div>
            </section>

            <section aria-labelledby="video-titulo">
                <div className="section-title"><h2 id="video-titulo">Conoce nuestra cadena de frío</h2></div>
                <figure className="video-box">
                    <video controls preload="metadata" poster="/img/poster-video.jpg" width={640} height={360}>
                        <source src="/video/cadena-de-frio.mp4" type="video/mp4" />
                        <track kind="captions" src="/video/cadena-de-frio.vtt" srcLang="es" label="Español" default />
                        Tu navegador no puede reproducir video. <a href="/video/cadena-de-frio.mp4">Descarga el archivo</a>.
                    </video>
                    <figcaption>Video de presentación (9 segundos, con subtítulos).</figcaption>
                </figure>
            </section>
        </>
    );
};

export default Home;
