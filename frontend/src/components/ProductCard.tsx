// components/ProductCard.tsx
// Tarjeta de producto del catálogo (mismo diseño que el prototipo).

import React from 'react';
import { Link } from 'react-router-dom';
import FormatService from '../services/formatService';
import type { Producto, StockProducto } from '../types';

interface ProductCardProps {
    producto: Producto;
    stock?: StockProducto;
    onAgregar: (producto: Producto) => void;
    agregando?: boolean;
}

const ProductCard: React.FC<ProductCardProps> = ({ producto, stock, onAgregar, agregando = false }) => {
    const disponible = stock?.disponibleKg ?? 0;
    const agotado = stock !== undefined && disponible < 1;
    return (
        <article className="card product">
            <img
                className="product__img"
                src={FormatService.imagenProducto(producto)}
                alt={`${producto.nombre}, producto congelado`}
                width={320}
                height={200}
                loading="lazy"
            />
            <span className="tag">{producto.categoria.nombre}</span>
            <h3>{producto.nombre}</h3>
            {stock?.proximoVencimiento ? (
                <p className="muted">
                    Vence <time dateTime={stock.proximoVencimiento}>{FormatService.fecha(stock.proximoVencimiento)}</time> ·{' '}
                    {FormatService.kg(disponible)} kg disponibles
                </p>
            ) : (
                <p className="muted">Origen: {producto.origen ?? '—'}</p>
            )}
            {stock?.critico && !agotado && <span className="tag warning">Últimos kilos</span>}
            <div className="price">{FormatService.dinero(producto.precioKg)} / kg</div>
            <div className="product__actions">
                <Link className="btn secondary" to={`/producto/${producto.id}`}>
                    Ver ficha
                </Link>
                <button
                    type="button"
                    className="btn"
                    onClick={() => onAgregar(producto)}
                    disabled={agotado || agregando}
                    aria-label={agotado ? `${producto.nombre} agotado` : `Agregar 1 kg de ${producto.nombre}`}
                >
                    {agotado ? 'Agotado' : 'Agregar 1 kg'}
                </button>
            </div>
        </article>
    );
};

export default ProductCard;
