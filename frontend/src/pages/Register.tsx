// pages/Register.tsx · Crear cuenta de cliente (HU001 · HU005 · HU006)
// Página propia, separada del inicio de sesión (prototipo V3: registro.html).

import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import { useAuth } from '../hooks/useAuth';
import { useForm } from '../hooks/useForm';
import { useToast } from '../hooks/useToast';
import { useApi } from '../hooks/useApi';
import ValidationService from '../services/validationService';
import ConfigService from '../services/configService';
import { erroresDeCampo, mensajeError } from '../services/apiClient';
import NavigationService from '../services/navigationService';

const REGLAS_PW: { clave: 'len' | 'may' | 'min' | 'num'; texto: string }[] = [
    { clave: 'len', texto: '8 o más caracteres' },
    { clave: 'may', texto: 'Una mayúscula' },
    { clave: 'min', texto: 'Una minúscula' },
    { clave: 'num', texto: 'Un número' },
];

const Register: React.FC = () => {
    const { usuario, registro } = useAuth();
    const { mostrar } = useToast();
    const navigate = useNavigate();
    const [params] = useSearchParams();
    const volver = NavigationService.destinoSeguro(params.get('volver'));
    const zonas = useApi(() => ConfigService.zonas());
    const [enviandoR, setEnviandoR] = useState(false);
    const V = ValidationService;

    const reg = useForm(
        {
            nombre: { reglas: [V.requerido('El nombre'), V.texto(2, 30)] },
            apellido: { reglas: [V.requerido('El apellido'), V.texto(2, 30)] },
            rut: { reglas: [V.requerido('El RUT'), V.rut()], formato: V.formatearRut.bind(V) },
            email: { reglas: [V.requerido('El correo'), V.email()] },
            telefono: { reglas: [V.requerido('El teléfono'), V.telefono()] },
            password: { reglas: [V.requerido('La contraseña'), V.password()] },
            password2: { reglas: [V.requerido('La confirmación'), V.coincide('password')] },
            direccion: { reglas: [V.requerido('La dirección'), V.direccion()] },
            comuna: { reglas: [V.requerido('La comuna')] },
            terminos: { reglas: [V.requerido('Debes aceptar los términos')] },
        },
        { nombre: '', apellido: '', rut: '', email: '', telefono: '', password: '', password2: '', direccion: '', comuna: '', terminos: '' },
    );

    const pw = V.analizarContrasena(reg.valores.password);

    const registrar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!reg.validarTodo()) return;
        const v = reg.valores;
        setEnviandoR(true);
        try {
            await registro(
                {
                    nombre: v.nombre.trim(),
                    apellido: v.apellido.trim(),
                    email: v.email.trim().toLowerCase(),
                    password: v.password,
                    telefono: v.telefono.trim(),
                    rut: V.rutParaApi(v.rut),
                },
                { alias: 'Casa', calle: v.direccion.trim(), comuna: v.comuna },
            );
            mostrar('Cuenta creada. ¡Bienvenido/a!');
            navigate(volver ?? '/catalogo');
        } catch (err) {
            reg.erroresServidor(erroresDeCampo(err), mensajeError(err, 'No se pudo crear la cuenta.'));
        } finally {
            setEnviandoR(false);
        }
    };

    return (
        <>
            <div className="section-title">
                <h1>Crear cuenta</h1>
                <span className="tag">HU001 · HU005 · HU006</span>
            </div>
            {usuario && (
                <p className="notice success" role="status">
                    Ya iniciaste sesión como <strong>{usuario.nombre} {usuario.apellido}</strong> ({usuario.rol}).{' '}
                    <Link className="link" to={NavigationService.paginaPorRol(usuario.rol)}>Continuar</Link>
                </p>
            )}
            <section className="card auth-card" aria-labelledby="registro-titulo">
                <h2 id="registro-titulo">Tus datos</h2>
                <form className="form" aria-labelledby="registro-titulo" noValidate onSubmit={registrar}>
                    <FormSummary mensaje={reg.resumen} />
                    <div className="two">
                        <FormField id="nombre" label="Nombre" hint="2 a 30 letras." error={reg.errores.nombre}>
                            <input {...reg.campo('nombre')} type="text" maxLength={30} autoComplete="given-name" />
                        </FormField>
                        <FormField id="apellido" label="Apellido" hint="2 a 30 letras." error={reg.errores.apellido}>
                            <input {...reg.campo('apellido')} type="text" maxLength={30} autoComplete="family-name" />
                        </FormField>
                    </div>
                    <FormField id="rut" label="RUT" hint="Con o sin puntos. Ej: 12.345.678-5" error={reg.errores.rut}>
                        <input {...reg.campo('rut')} type="text" maxLength={12} />
                    </FormField>
                    <FormField id="email" label="Correo" hint="Debe terminar en .com o .cl (un solo final). Lo usarás para iniciar sesión." error={reg.errores.email}>
                        <input {...reg.campo('email')} type="email" autoComplete="email" />
                    </FormField>
                    <FormField id="telefono" label="Teléfono" hint="Celular chileno: +56 9 1234 5678" error={reg.errores.telefono}>
                        <input {...reg.campo('telefono')} type="tel" autoComplete="tel" />
                    </FormField>
                    <FormField id="password" label="Contraseña" hint="Mínimo 8 caracteres." error={reg.errores.password}>
                        <input {...reg.campo('password')} type="password" autoComplete="new-password" />
                    </FormField>
                    <ul className="pw-rules" aria-label="Requisitos de la contraseña">
                        {REGLAS_PW.map((r) => (
                            <li key={r.clave} className={pw[r.clave] ? 'ok' : undefined}>{r.texto}</li>
                        ))}
                    </ul>
                    <FormField id="password2" label="Repite la contraseña" hint="Debe ser igual a la anterior." error={reg.errores.password2}>
                        <input {...reg.campo('password2')} type="password" autoComplete="new-password" />
                    </FormField>
                    <div className="two">
                        <FormField id="direccion" label="Dirección de entrega" hint="Calle y, si tiene, número. Ej: O'Higgins 1234 o Camino a Lota." error={reg.errores.direccion}>
                            <input {...reg.campo('direccion')} type="text" autoComplete="street-address" />
                        </FormField>
                        <FormField id="comuna" label="Comuna" hint="Comunas con despacho." error={reg.errores.comuna}>
                            <select {...reg.campo('comuna', 'select')}>
                                <option value="">Selecciona…</option>
                                {(zonas.datos ?? []).map((z) => <option key={z.id} value={z.comuna}>{z.comuna}</option>)}
                            </select>
                        </FormField>
                    </div>
                    <FormField id="terminos" label="Acepto los términos y la política de privacidad." error={reg.errores.terminos} checkbox>
                        <input {...reg.casilla('terminos')} />
                    </FormField>
                    <button className="btn" disabled={enviandoR}>{enviandoR ? 'Creando cuenta…' : 'Registrarme'}</button>
                </form>
                <p className="muted">¿Ya tienes cuenta? <Link className="link" to={volver ? `/login?volver=${encodeURIComponent(volver)}` : '/login'}>Inicia sesión</Link></p>
            </section>
        </>
    );
};

export default Register;
