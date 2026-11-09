// components/admin/ProductsSection.tsx · Catálogo: productos (crear, editar, activar, eliminar) y categorías

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import CatalogoService from '../../services/catalogoService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { erroresDeCampo, mensajeError } from '../../services/apiClient';
import type { Producto } from '../../types';

const VACIO = { sku: '', nombre: '', categoriaId: '', precioKg: '', costoKg: '', origen: '', alergenos: '', imagen: '', descripcion: '' };

const ProductsSection: React.FC = () => {
    const { mostrar } = useToast();
    const productos = useApi(() => CatalogoService.listarProductos(true));
    const categorias = useApi(() => CatalogoService.listarCategorias());
    const [editando, setEditando] = useState<Producto | null>(null);
    const [mostrarForm, setMostrarForm] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [nuevaCat, setNuevaCat] = useState({ nombre: '', descripcion: '' });
    const V = ValidationService;

    const form = useForm(
        {
            sku: { reglas: [V.requerido('El SKU'), V.sku()], formato: (v) => v.trim().toUpperCase() },
            nombre: { reglas: [V.requerido('El nombre'), V.largo(3, 60)] },
            categoriaId: { reglas: [V.requerido('La categoría')] },
            precioKg: { reglas: [V.requerido('El precio'), V.rango(1, 1000000, 'CLP')] },
            costoKg: { reglas: [V.requerido('El costo'), V.rango(0, 1000000, 'CLP')] },
            origen: { reglas: [V.largo(0, 60)] },
            alergenos: { reglas: [V.largo(0, 100)] },
            imagen: { reglas: [V.largo(0, 255)] },
            descripcion: { reglas: [V.largo(0, 500)] },
        },
        VACIO,
        'prod-',
    );

    const abrir = (p: Producto | null) => {
        setEditando(p);
        setMostrarForm(true);
        form.reiniciar(
            p
                ? {
                    sku: p.sku, nombre: p.nombre, categoriaId: String(p.categoria.id), precioKg: String(p.precioKg), costoKg: String(p.costoKg),
                    origen: p.origen ?? '', alergenos: p.alergenos ?? '', imagen: p.imagen ?? '', descripcion: p.descripcion ?? '',
                }
                : VACIO,
        );
    };

    const guardar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        const v = form.valores;
        const datos = {
            sku: v.sku.trim().toUpperCase(),
            nombre: v.nombre.trim(),
            categoriaId: Number(v.categoriaId),
            precioKg: V.numero(v.precioKg),
            costoKg: V.numero(v.costoKg),
            origen: v.origen.trim() || undefined,
            alergenos: v.alergenos.trim() || undefined,
            imagen: v.imagen.trim() || undefined,
            descripcion: v.descripcion.trim() || undefined,
            atributos: editando?.atributos,
            activo: editando?.activo ?? true,
        };
        setGuardando(true);
        try {
            if (editando) await CatalogoService.actualizarProducto(editando.id, datos);
            else await CatalogoService.crearProducto(datos);
            mostrar(editando ? 'Producto actualizado' : 'Producto creado');
            setMostrarForm(false);
            void productos.recargar();
        } catch (err) {
            form.erroresServidor(erroresDeCampo(err), mensajeError(err));
        } finally {
            setGuardando(false);
        }
    };

    const accion = async (fn: () => Promise<unknown>, exito: string) => {
        try {
            await fn();
            mostrar(exito);
            void productos.recargar();
            void categorias.recargar();
        } catch (err) {
            mostrar(mensajeError(err), 'error');
        }
    };

    const crearCategoria = async (e: React.FormEvent) => {
        e.preventDefault();
        if (nuevaCat.nombre.trim().length < 3) {
            mostrar('El nombre de la categoría debe tener al menos 3 caracteres.', 'error');
            return;
        }
        await accion(() => CatalogoService.crearCategoria(nuevaCat.nombre.trim(), nuevaCat.descripcion.trim()), 'Categoría creada');
        setNuevaCat({ nombre: '', descripcion: '' });
    };

    return (
        <section aria-labelledby="cat-titulo">
            <div className="section-title">
                <h1 id="cat-titulo">Catálogo</h1>
                <button type="button" className="btn" onClick={() => abrir(null)}>Nuevo producto</button>
            </div>

            {mostrarForm && (
                <form className="card form" noValidate onSubmit={guardar} aria-label={editando ? `Editar ${editando.nombre}` : 'Nuevo producto'}>
                    <h2>{editando ? `Editar ${editando.nombre}` : 'Nuevo producto'}</h2>
                    <FormSummary mensaje={form.resumen} />
                    <div className="two">
                        <FormField id="prod-sku" label="SKU" hint="Ej: SAL-001" error={form.errores.sku}><input {...form.campo('sku')} /></FormField>
                        <FormField id="prod-nombre" label="Nombre" error={form.errores.nombre}><input {...form.campo('nombre')} /></FormField>
                        <FormField id="prod-categoriaId" label="Categoría" error={form.errores.categoriaId}>
                            <select {...form.campo('categoriaId', 'select')}>
                                <option value="">Selecciona…</option>
                                {(categorias.datos ?? []).map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                            </select>
                        </FormField>
                        <FormField id="prod-origen" label="Origen" error={form.errores.origen}><input {...form.campo('origen')} /></FormField>
                        <FormField id="prod-precioKg" label="Precio por kg (CLP)" error={form.errores.precioKg}><input {...form.campo('precioKg')} type="number" min={1} /></FormField>
                        <FormField id="prod-costoKg" label="Costo por kg (CLP)" error={form.errores.costoKg}><input {...form.campo('costoKg')} type="number" min={0} /></FormField>
                        <FormField id="prod-alergenos" label="Alérgenos" error={form.errores.alergenos}><input {...form.campo('alergenos')} /></FormField>
                        <FormField id="prod-imagen" label="Imagen" hint="Ej: img/prod-salmon.svg o una URL" error={form.errores.imagen}><input {...form.campo('imagen')} /></FormField>
                    </div>
                    <FormField id="prod-descripcion" label="Descripción" error={form.errores.descripcion}>
                        <textarea {...form.campo('descripcion')} className={`${form.clase('descripcion')} textarea`} />
                    </FormField>
                    <div className="btn-row">
                        <button className="btn" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
                        <button type="button" className="btn secondary" onClick={() => setMostrarForm(false)}>Cancelar</button>
                    </div>
                </form>
            )}

            <ErrorNotice mensaje={productos.error} onReintentar={productos.recargar} />
            {productos.cargando ? <Loader /> : (
                <div className="table-wrap">
                    <table>
                        <caption className="visually-hidden">Productos</caption>
                        <thead>
                            <tr><th scope="col">SKU</th><th scope="col">Producto</th><th scope="col">Categoría</th><th scope="col">Precio/kg</th><th scope="col">Margen</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr>
                        </thead>
                        <tbody>
                            {(productos.datos ?? []).map((p) => (
                                <tr key={p.id}>
                                    <td>{p.sku}</td>
                                    <td>{p.nombre}</td>
                                    <td>{p.categoria.nombre}</td>
                                    <td>{FormatService.dinero(p.precioKg)}</td>
                                    <td>{String(p.margen).replace('.', ',')}%</td>
                                    <td>{p.activo ? <span className="tag">Activo</span> : <span className="tag danger">Inactivo</span>}</td>
                                    <td>
                                        <div className="btn-row" style={{ marginTop: 0 }}>
                                            <button type="button" className="btn secondary small" onClick={() => abrir(p)}>Editar</button>
                                            <button type="button" className="btn secondary small" onClick={() => accion(() => CatalogoService.cambiarActivo(p.id, !p.activo), p.activo ? 'Producto desactivado' : 'Producto activado')}>
                                                {p.activo ? 'Desactivar' : 'Activar'}
                                            </button>
                                            <button type="button" className="btn danger small" onClick={() => window.confirm(`¿Eliminar ${p.nombre}?`) && accion(() => CatalogoService.eliminarProducto(p.id), 'Producto eliminado')}>
                                                Eliminar
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <h2 style={{ marginTop: 28 }}>Categorías</h2>
            <div className="grid cols-2">
                <div className="table-wrap">
                    <table>
                        <thead><tr><th scope="col">Categoría</th><th scope="col">Descripción</th><th scope="col"><span className="visually-hidden">Acciones</span></th></tr></thead>
                        <tbody>
                            {(categorias.datos ?? []).map((c) => (
                                <tr key={c.id}>
                                    <td>{c.nombre}</td>
                                    <td>{c.descripcion}</td>
                                    <td><button type="button" className="btn danger small" onClick={() => window.confirm(`¿Eliminar la categoría ${c.nombre}?`) && accion(() => CatalogoService.eliminarCategoria(c.id), 'Categoría eliminada')}>Eliminar</button></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                <form className="card form" onSubmit={crearCategoria} aria-label="Nueva categoría">
                    <h3>Nueva categoría</h3>
                    <div className="field">
                        <label htmlFor="cat-nombre">Nombre</label>
                        <input id="cat-nombre" className="input" maxLength={40} value={nuevaCat.nombre} onChange={(e) => setNuevaCat({ ...nuevaCat, nombre: e.target.value })} />
                    </div>
                    <div className="field">
                        <label htmlFor="cat-desc">Descripción</label>
                        <input id="cat-desc" className="input" maxLength={200} value={nuevaCat.descripcion} onChange={(e) => setNuevaCat({ ...nuevaCat, descripcion: e.target.value })} />
                    </div>
                    <button className="btn">Crear categoría</button>
                </form>
            </div>
        </section>
    );
};

export default ProductsSection;
