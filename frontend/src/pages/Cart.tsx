// pages/Cart.tsx · Carrito en kg con RN-02 (máx. 12,5 kg por formato, de a 0,5 kg) — EP13

import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import ErrorNotice from '../components/ErrorNotice';
import { useCart } from '../hooks/useCart';
import { useToast } from '../hooks/useToast';
import ValidationService, { MAX_KG_FORMATO } from '../services/validationService';
import FormatService from '../services/formatService';
import InventarioService from '../services/inventarioService';
import type { ItemCarrito } from '../types';

const Cart: React.FC = () => {
    const { carrito, cargando, error, fijar, quitar, vaciar, refrescar } = useCart();
    const { mostrar } = useToast();
    const [limites, setLimites] = useState<Record<number, number>>({});
    const [ocupado, setOcupado] = useState(false);
    // Lo que el usuario escribe en cada casilla antes de confirmarlo (al salir del campo).
    const [borradores, setBorradores] = useState<Record<number, string>>({});

    // Límite por producto: el menor entre RN-02 y el stock disponible.
    const ids = (carrito?.items ?? []).map((i) => i.productoId).join(',');
    useEffect(() => {
        if (!ids) return;
        Promise.all(
            ids.split(',').map(async (id) => {
                try {
                    const s = await InventarioService.stock(Number(id));
                    return [Number(id), Math.min(MAX_KG_FORMATO, Math.floor(s.disponibleKg * 2) / 2)] as const;
                } catch {
                    return [Number(id), MAX_KG_FORMATO] as const;
                }
            }),
        ).then((pares) => setLimites(Object.fromEntries(pares)));
    }, [ids]);

    const limite = (i: ItemCarrito) => Math.max(limites[i.productoId] ?? MAX_KG_FORMATO, 0.5);

    const ejecutar = async (accion: () => Promise<string>, exito?: string) => {
        setOcupado(true);
        const err = await accion();
        setOcupado(false);
        if (err) mostrar(err, 'error');
        else if (exito) mostrar(exito);
    };

    const cambiarKg = (i: ItemCarrito, kg: number) => {
        if (kg > limite(i)) {
            mostrar('Llegaste al máximo permitido para este producto.', 'error');
            return;
        }
        void ejecutar(() => fijar(i.productoId, Math.max(0.5, kg)));
    };

    const confirmarCasilla = (i: ItemCarrito) => {
        const valor = borradores[i.productoId];
        if (valor === undefined) return;
        setBorradores((b) => {
            const copia = { ...b };
            delete copia[i.productoId];
            return copia;
        });
        const msg = ValidationService.kg(limite(i))(valor, {});
        if (msg) {
            mostrar(msg, 'error');
            return;
        }
        cambiarKg(i, ValidationService.numero(valor));
    };

    if (cargando) return <Loader texto="Cargando carrito…" />;

    return (
        <>
            <div className="section-title">
                <h1>Carrito</h1>
                <span className="tag">EP13</span>
            </div>
            <div className="notice">RN-02: máximo 12,5 kg por formato. Las cantidades se ajustan de a 0,5 kg.</div>
            <ErrorNotice mensaje={error} onReintentar={refrescar} />
            {!carrito || carrito.items.length === 0 ? (
                !error && <EmptyState mensaje="Tu carrito está vacío." accion={{ texto: 'Ir al catálogo', a: '/catalogo' }} />
            ) : (
                <div className="grid cart-layout">
                    <div className="table-wrap">
                        <table>
                            <caption className="visually-hidden">Productos en el carrito</caption>
                            <thead>
                                <tr>
                                    <th scope="col">Producto</th>
                                    <th scope="col">Precio/kg</th>
                                    <th scope="col">Cantidad (kg)</th>
                                    <th scope="col">Subtotal</th>
                                    <th scope="col"><span className="visually-hidden">Acciones</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                {carrito.items.map((i) => (
                                    <tr key={i.productoId}>
                                        <td>
                                            <strong>{i.nombre}</strong>
                                            <br />
                                            <small className="muted">Máx. {FormatService.kg(limite(i))} kg</small>
                                        </td>
                                        <td>{FormatService.dinero(i.precioKg)}</td>
                                        <td>
                                            <div className="qty">
                                                <button
                                                    type="button"
                                                    className="btn secondary small"
                                                    aria-label={`Quitar 0,5 kg de ${i.nombre}`}
                                                    disabled={ocupado || i.kg <= 0.5}
                                                    onClick={() => cambiarKg(i, i.kg - 0.5)}
                                                >
                                                    −
                                                </button>
                                                <input
                                                    className="input"
                                                    type="number"
                                                    step={0.5}
                                                    min={0.5}
                                                    max={limite(i)}
                                                    value={borradores[i.productoId] ?? String(i.kg)}
                                                    aria-label={`Kilos de ${i.nombre}`}
                                                    onChange={(e) => setBorradores((b) => ({ ...b, [i.productoId]: e.target.value }))}
                                                    onBlur={() => confirmarCasilla(i)}
                                                    onKeyDown={(e) => e.key === 'Enter' && confirmarCasilla(i)}
                                                />
                                                <button
                                                    type="button"
                                                    className="btn secondary small"
                                                    aria-label={`Agregar 0,5 kg de ${i.nombre}`}
                                                    disabled={ocupado}
                                                    onClick={() => cambiarKg(i, i.kg + 0.5)}
                                                >
                                                    +
                                                </button>
                                            </div>
                                        </td>
                                        <td>{FormatService.dinero(i.subtotal)}</td>
                                        <td>
                                            <button
                                                type="button"
                                                className="btn danger small"
                                                aria-label={`Quitar ${i.nombre} del carrito`}
                                                disabled={ocupado}
                                                onClick={() => ejecutar(() => quitar(i.productoId), `${i.nombre} quitado del carrito`)}
                                            >
                                                Quitar
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <aside className="card totals" aria-label="Resumen del carrito">
                        <h2>Resumen</h2>
                        <div><span>Total kilos</span><span>{FormatService.kg(carrito.totalKg)} kg</span></div>
                        <div className="grand"><span>Subtotal</span><span>{FormatService.dinero(carrito.subtotal)}</span></div>
                        <p className="muted">El costo de envío se calcula según tu comuna en el siguiente paso.</p>
                        <Link className="btn" to="/pedido">Continuar</Link>
                        <button type="button" className="btn secondary" disabled={ocupado} onClick={() => ejecutar(vaciar, 'Carrito vaciado')}>
                            Vaciar carrito
                        </button>
                    </aside>
                </div>
            )}
        </>
    );
};

export default Cart;
