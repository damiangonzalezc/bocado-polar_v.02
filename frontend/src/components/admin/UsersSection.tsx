// components/admin/UsersSection.tsx · Usuarios y roles: estados Activo/Bloqueado/Suspendido/Eliminado (ms-auth, solo ADMIN)

import React, { useState } from 'react';
import Loader from '../Loader';
import ErrorNotice from '../ErrorNotice';
import FormField from '../FormField';
import FormSummary from '../FormSummary';
import { useApi } from '../../hooks/useApi';
import { useForm } from '../../hooks/useForm';
import { useToast } from '../../hooks/useToast';
import AuthService from '../../services/authService';
import ValidationService from '../../services/validationService';
import FormatService from '../../services/formatService';
import { erroresDeCampo, mensajeError } from '../../services/apiClient';
import type { EstadoUsuario, Rol, Usuario } from '../../types';

const ESTADOS: EstadoUsuario[] = ['ACTIVO', 'BLOQUEADO', 'SUSPENDIDO', 'ELIMINADO'];
const ROLES_ASIGNABLES: Rol[] = ['CLIENTE', 'REPARTIDOR', 'VENDEDOR', 'BODEGUERO', 'SOPORTE'];

const UsersSection: React.FC = () => {
    const { mostrar } = useToast();
    const [rol, setRol] = useState<Rol | ''>('');
    const [crear, setCrear] = useState(false);
    const usuarios = useApi(() => AuthService.listarUsuarios({ rol }), [rol]);
    const V = ValidationService;

    const form = useForm(
        {
            nombre: { reglas: [V.requerido('El nombre'), V.texto(2, 30)] },
            apellido: { reglas: [V.requerido('El apellido'), V.texto(2, 30)] },
            email: { reglas: [V.requerido('El correo'), V.email()] },
            telefono: { reglas: [V.telefono()] },
            rut: { reglas: [V.rut()], formato: V.formatearRut.bind(V) },
            rol: { reglas: [V.requerido('El rol')] },
            password: { reglas: [V.requerido('La contraseña'), V.password()] },
        },
        { nombre: '', apellido: '', email: '', telefono: '', rut: '', rol: 'VENDEDOR', password: '' },
        'usr-',
    );

    const actualizar = async (u: Usuario, fn: () => Promise<Usuario>, exito: string) => {
        try {
            const nuevo = await fn();
            usuarios.setDatos((usuarios.datos ?? []).map((x) => (x.id === u.id ? nuevo : x)));
            mostrar(exito);
        } catch (e) {
            mostrar(mensajeError(e), 'error');
        }
    };

    const guardar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.validarTodo()) return;
        const v = form.valores;
        try {
            await AuthService.crearUsuario({
                nombre: v.nombre.trim(),
                apellido: v.apellido.trim(),
                email: v.email.trim().toLowerCase(),
                telefono: v.telefono.trim(),
                rut: v.rut ? V.rutParaApi(v.rut) : '',
                rol: v.rol as Rol,
                password: v.password,
            });
            mostrar('Usuario creado');
            setCrear(false);
            form.reiniciar();
            void usuarios.recargar();
        } catch (err) {
            form.erroresServidor(erroresDeCampo(err), mensajeError(err));
        }
    };

    return (
        <section aria-labelledby="usuarios-titulo">
            <div className="section-title">
                <h1 id="usuarios-titulo">Usuarios y roles</h1>
                <button type="button" className="btn" onClick={() => setCrear(!crear)}>Nuevo usuario</button>
            </div>

            {crear && (
                <form className="card form" noValidate onSubmit={guardar} aria-label="Nuevo usuario">
                    <h2>Nuevo usuario</h2>
                    <FormSummary mensaje={form.resumen} />
                    <div className="two">
                        <FormField id="usr-nombre" label="Nombre" error={form.errores.nombre}><input {...form.campo('nombre')} /></FormField>
                        <FormField id="usr-apellido" label="Apellido" error={form.errores.apellido}><input {...form.campo('apellido')} /></FormField>
                        <FormField id="usr-email" label="Correo" hint="Termina en .cl o .com" error={form.errores.email}><input {...form.campo('email')} type="email" /></FormField>
                        <FormField id="usr-rol" label="Rol" error={form.errores.rol}>
                            <select {...form.campo('rol', 'select')}>
                                {ROLES_ASIGNABLES.map((r) => <option key={r} value={r}>{FormatService.etiqueta(r)}</option>)}
                            </select>
                        </FormField>
                        <FormField id="usr-telefono" label="Teléfono (opcional)" error={form.errores.telefono}><input {...form.campo('telefono')} type="tel" /></FormField>
                        <FormField id="usr-rut" label="RUT (opcional)" error={form.errores.rut}><input {...form.campo('rut')} maxLength={12} /></FormField>
                        <FormField id="usr-password" label="Contraseña inicial" hint="8+ caracteres, mayúscula, minúscula y número." error={form.errores.password}><input {...form.campo('password')} type="password" autoComplete="new-password" /></FormField>
                    </div>
                    <div className="btn-row">
                        <button className="btn">Crear usuario</button>
                        <button type="button" className="btn secondary" onClick={() => setCrear(false)}>Cancelar</button>
                    </div>
                </form>
            )}

            <div className="toolbar">
                <div className="field">
                    <label htmlFor="u-rol">Rol</label>
                    <select id="u-rol" className="select" value={rol} onChange={(e) => setRol(e.target.value as Rol | '')}>
                        <option value="">Todos</option>
                        {(['ADMIN', ...ROLES_ASIGNABLES] as Rol[]).map((r) => <option key={r} value={r}>{FormatService.etiqueta(r)}</option>)}
                    </select>
                </div>
            </div>
            <ErrorNotice mensaje={usuarios.error} onReintentar={usuarios.recargar} />
            {usuarios.cargando ? <Loader /> : (
                <div className="table-wrap">
                    <table>
                        <caption className="visually-hidden">Usuarios y roles</caption>
                        <thead><tr><th scope="col">Correo</th><th scope="col">Nombre</th><th scope="col">Rol</th><th scope="col">Estado</th></tr></thead>
                        <tbody>
                            {(usuarios.datos ?? []).map((u) => (
                                <tr key={u.id}>
                                    <td>{u.email} {u.protegido && <span className="tag">Protegido</span>}</td>
                                    <td>{u.nombre} {u.apellido}</td>
                                    <td>
                                        {u.protegido || u.rol === 'ADMIN' ? FormatService.etiqueta(u.rol) : (
                                            <select className="select small" aria-label={`Rol de ${u.email}`} value={u.rol} onChange={(e) => actualizar(u, () => AuthService.cambiarRol(u.id, e.target.value as Rol), 'Rol actualizado')}>
                                                {ROLES_ASIGNABLES.map((r) => <option key={r} value={r}>{FormatService.etiqueta(r)}</option>)}
                                            </select>
                                        )}
                                    </td>
                                    <td>
                                        {u.protegido ? <span className="status ok">Activo</span> : (
                                            <select
                                                className={`select small${u.estado === 'ACTIVO' ? '' : ' is-invalid'}`}
                                                aria-label={`Estado de ${u.email}`}
                                                value={u.estado}
                                                onChange={(e) => actualizar(u, () => AuthService.cambiarEstado(u.id, e.target.value as EstadoUsuario), 'Estado actualizado')}
                                            >
                                                {ESTADOS.map((s) => <option key={s} value={s}>{FormatService.etiqueta(s)}</option>)}
                                            </select>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
};

export default UsersSection;
