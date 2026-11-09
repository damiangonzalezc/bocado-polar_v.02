// pages/Catalog.tsx · Catálogo con búsqueda, filtro por categoría y orden (EP04, EP05)

import React, { useMemo, useState } from 'react';
import ProductCard from '../components/ProductCard';
import Loader from '../components/Loader';
import ErrorNotice from '../components/ErrorNotice';
import { useApi } from '../hooks/useApi';
import { useAgregarAlCarrito } from '../hooks/useAgregarAlCarrito';
import CatalogoService from '../services/catalogoService';
import InventarioService from '../services/inventarioService';
import type { OrdenCatalogo } from '../services/catalogoService';

const Catalog: React.FC = () => {
    const productos = useApi(() => CatalogoService.listarProductos());
    const categorias = useApi(() => CatalogoService.listarCategorias());
    const stock = useApi(() => InventarioService.stockGeneral().catch(() => []));
    const { agregar, agregando } = useAgregarAlCarrito();
    const [q, setQ] = useState('');
    const [cat, setCat] = useState('');
    const [orden, setOrden] = useState<OrdenCatalogo>('nombre');

    const stockPorId = useMemo(() => new Map((stock.datos ?? []).map((s) => [s.productoId, s])), [stock.datos]);
    const resultado = useMemo(
        () => CatalogoService.filtrar(productos.datos ?? [], stockPorId, q, cat, orden),
        [productos.datos, stockPorId, q, cat, orden],
    );

    return (
        <>
            <div className="section-title">
                <h1>Catálogo</h1>
                <span className="tag">EP04 · EP05 · EP06</span>
            </div>
            <form className="filters" role="search" aria-label="Buscar productos" onSubmit={(e) => e.preventDefault()}>
                <div className="field">
                    <label className="visually-hidden" htmlFor="q">Buscar producto</label>
                    <input id="q" type="search" className="input" placeholder="Buscar producto…" autoComplete="off" value={q} onChange={(e) => setQ(e.target.value)} />
                </div>
                <div className="field">
                    <label className="visually-hidden" htmlFor="cat">Categoría</label>
                    <select id="cat" className="select" value={cat} onChange={(e) => setCat(e.target.value)}>
                        <option value="">Todas las categorías</option>
                        {(categorias.datos ?? []).map((c) => (
                            <option key={c.id} value={c.id}>{c.nombre}</option>
                        ))}
                    </select>
                </div>
                <div className="field">
                    <label className="visually-hidden" htmlFor="orden">Ordenar por</label>
                    <select id="orden" className="select" value={orden} onChange={(e) => setOrden(e.target.value as OrdenCatalogo)}>
                        <option value="nombre">Nombre (A-Z)</option>
                        <option value="precioAsc">Precio: menor a mayor</option>
                        <option value="precioDesc">Precio: mayor a menor</option>
                        <option value="fefo">Vencimiento más próximo (FEFO)</option>
                        <option value="valoracion">Mejor valorados</option>
                    </select>
                </div>
            </form>
            <ErrorNotice mensaje={productos.error} onReintentar={productos.recargar} />
            {productos.cargando ? (
                <Loader texto="Cargando catálogo…" />
            ) : (
                <>
                    <p className="muted" aria-live="polite">
                        {resultado.length} producto{resultado.length === 1 ? '' : 's'} encontrado{resultado.length === 1 ? '' : 's'}
                    </p>
                    <div className="grid">
                        {resultado.length ? (
                            resultado.map((p) => (
                                <ProductCard key={p.id} producto={p} stock={stockPorId.get(p.id)} onAgregar={agregar} agregando={agregando === p.id} />
                            ))
                        ) : (
                            <p className="card empty">No encontramos productos con esos filtros.</p>
                        )}
                    </div>
                </>
            )}
        </>
    );
};

export default Catalog;
