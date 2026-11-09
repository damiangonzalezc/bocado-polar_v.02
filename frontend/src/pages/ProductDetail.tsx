// pages/ProductDetail.tsx · Ficha técnica: lote FEFO, vencimiento, temperatura, alérgenos y valoración

import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import FormField from '../components/FormField';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import StarRating from '../components/StarRating';
import { useApi } from '../hooks/useApi';
import { useForm } from '../hooks/useForm';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';
import { useAgregarAlCarrito } from '../hooks/useAgregarAlCarrito';
import { useToast } from '../hooks/useToast';
import CatalogoService from '../services/catalogoService';
import InventarioService from '../services/inventarioService';
import ValidationService, { MAX_KG_FORMATO } from '../services/validationService';
import FormatService from '../services/formatService';
import { mensajeError } from '../services/apiClient';
import type { ResumenValoracion } from '../types';

const ProductDetail: React.FC = () => {
    const id = Number(useParams().id);
    const { cliente } = useAuth();
    const { carrito } = useCart();
    const { mostrar } = useToast();
    const { agregar, agregando } = useAgregarAlCarrito();

    const producto = useApi(() => CatalogoService.obtenerProducto(id), [id]);
    const lotes = useApi(() => InventarioService.lotesFefo(id).catch(() => []), [id]);
    const stock = useApi(() => InventarioService.stock(id).catch(() => null), [id]);
    const [valoracion, setValoracion] = useState<ResumenValoracion | null>(null);
    const [votando, setVotando] = useState(false);

    useEffect(() => {
        CatalogoService.resumenValoracion(id, cliente?.id).then(setValoracion).catch(() => setValoracion(null));
    }, [id, cliente?.id]);

    // Límite: RN-02 (12,5 kg) o el stock disponible, descontando lo que ya está en el carrito.
    const enCarrito = carrito?.items.find((i) => i.productoId === id)?.kg ?? 0;
    const disponible = stock.datos?.disponibleKg ?? MAX_KG_FORMATO;
    const limite = Math.max(0, Math.floor(Math.min(MAX_KG_FORMATO, disponible) * 2) / 2 - enCarrito);

    const form = useForm(
        { cantidad: { reglas: [ValidationService.requerido('La cantidad'), ValidationService.kg(limite)] } },
        { cantidad: '1' },
    );

    if (producto.cargando) return <Loader texto="Cargando ficha…" />;
    if (producto.error || !producto.datos) {
        return <EmptyState mensaje="No encontramos ese producto." accion={{ texto: 'Volver al catálogo', a: '/catalogo' }} />;
    }

    const p = producto.datos;
    const lote = lotes.datos?.[0];

    const enviar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        await agregar(p, ValidationService.numero(form.valores.cantidad));
    };

    const votar = async (estrellas: number) => {
        if (!cliente) return;
        setVotando(true);
        try {
            setValoracion(await CatalogoService.valorar(p.id, cliente.id, estrellas));
            mostrar('¡Gracias por tu valoración!');
        } catch (err) {
            mostrar(mensajeError(err), 'error');
        } finally {
            setVotando(false);
        }
    };

    return (
        <>
            <div className="section-title">
                <h1>Ficha técnica</h1>
                <span className="tag">HU011 · HU016 · HU017</span>
            </div>
            <section className="card" aria-live="polite">
                <div className="grid cols-2">
                    <img className="detail-img" src={FormatService.imagenProducto(p)} alt={`${p.nombre}, producto congelado`} width={320} height={200} />
                    <div>
                        <span className="tag">{p.categoria.nombre}</span>
                        <h2>{p.nombre}</h2>
                        <div className="price">{FormatService.dinero(p.precioKg)} / kg</div>
                        {valoracion && (
                            <StarRating
                                valor={valoracion.promedio}
                                votos={valoracion.votos}
                                elegida={valoracion.mia}
                                onElegir={cliente ? votar : undefined}
                                deshabilitado={votando}
                            />
                        )}
                        {!cliente && <p className="muted">Inicia sesión como cliente para valorar este producto.</p>}
                        <dl className="specs">
                            <dt>Lote</dt><dd>{lote?.codigoLote ?? 'Sin lote disponible'}</dd>
                            <dt>Vencimiento</dt>
                            <dd>{lote ? <time dateTime={lote.vencimiento}>{FormatService.fecha(lote.vencimiento)}</time> : '—'}</dd>
                            <dt>Temperatura</dt><dd>{lote?.tempAlmacenaje !== undefined ? `${lote.tempAlmacenaje}°C` : '—'}</dd>
                            <dt>Origen</dt><dd>{p.origen ?? '—'}</dd>
                            <dt>Stock</dt><dd>{FormatService.kg(stock.datos?.disponibleKg ?? 0)} kg</dd>
                            {Object.entries(p.atributos ?? {}).map(([k, v]) => (
                                <React.Fragment key={k}><dt>{k}</dt><dd>{v}</dd></React.Fragment>
                            ))}
                        </dl>
                        <h3>Calidad y alérgenos</h3>
                        <p>
                            Contiene: <strong>{p.alergenos ?? 'sin información'}</strong>. Se despacha en vehículo refrigerado y con la
                            cadena de frío registrada por lote.
                        </p>
                        {limite >= 0.5 ? (
                            <form className="form" aria-label={`Agregar ${p.nombre} al carrito`} noValidate onSubmit={enviar}>
                                <FormField
                                    id="cantidad"
                                    label="Cantidad (kg)"
                                    hint={`Entre 0,5 y ${FormatService.kg(limite)} kg, de a 0,5 kg.`}
                                    error={form.errores.cantidad}
                                >
                                    <input {...form.campo('cantidad')} type="number" inputMode="decimal" step={0.5} min={0.5} max={limite} />
                                </FormField>
                                <div className="btn-row">
                                    <button className="btn" disabled={agregando === p.id}>Agregar al carrito</button>
                                    <Link className="btn secondary" to="/carrito">Ver carrito</Link>
                                </div>
                            </form>
                        ) : (
                            <p className="notice warning">
                                {enCarrito > 0 ? 'Ya tienes el máximo permitido de este producto en tu carrito.' : 'Producto sin stock por ahora.'}
                            </p>
                        )}
                    </div>
                </div>
            </section>
            <p><Link to="/catalogo">← Volver al catálogo</Link></p>
        </>
    );
};

export default ProductDetail;
