// pages/Login.tsx · Iniciar sesión (HU001 · HU003)
// Página propia, separada del registro (prototipo V3: login.html).

import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import FormField from '../components/FormField';
import FormSummary from '../components/FormSummary';
import { useAuth } from '../hooks/useAuth';
import { useForm } from '../hooks/useForm';
import { useToast } from '../hooks/useToast';
import ValidationService from '../services/validationService';
import { mensajeError } from '../services/apiClient';
import NavigationService from '../services/navigationService';

const Login: React.FC = () => {
    const { usuario, login } = useAuth();
    const { mostrar } = useToast();
    const navigate = useNavigate();
    const [params] = useSearchParams();
    const volver = NavigationService.destinoSeguro(params.get('volver'));
    const [enviandoL, setEnviandoL] = useState(false);
    const V = ValidationService;

    const log = useForm(
        {
            email: { reglas: [V.requerido('El correo'), V.email()] },
            password: { reglas: [V.requerido('La contraseña')] },
        },
        { email: '', password: '' },
        'login-',
    );

    const ingresar = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!log.validarTodo()) return;
        setEnviandoL(true);
        try {
            const u = await login(log.valores.email, log.valores.password);
            mostrar(`Hola, ${u.nombre}`);
            navigate(u.rol === 'CLIENTE' ? volver ?? NavigationService.paginaPorRol(u.rol) : NavigationService.paginaPorRol(u.rol));
        } catch (err) {
            log.setResumen(mensajeError(err, 'Correo o contraseña incorrectos.'));
        } finally {
            setEnviandoL(false);
        }
    };

    return (
        <>
            <div className="section-title">
                <h1>Iniciar sesión</h1>
                <span className="tag">HU001 · HU003</span>
            </div>
            {usuario && (
                <p className="notice success" role="status">
                    Ya iniciaste sesión como <strong>{usuario.nombre} {usuario.apellido}</strong> ({usuario.rol}).{' '}
                    <Link className="link" to={NavigationService.paginaPorRol(usuario.rol)}>Continuar</Link>
                </p>
            )}
            <section className="card auth-card" aria-labelledby="login-titulo">
                <h2 id="login-titulo">Ingresa a tu cuenta</h2>
                <form className="form" aria-labelledby="login-titulo" noValidate onSubmit={ingresar}>
                    <FormSummary mensaje={log.resumen} />
                    <FormField id="login-email" label="Correo" hint="Prueba: cliente@bocado.cl" error={log.errores.email}>
                        <input {...log.campo('email')} type="email" autoComplete="username" />
                    </FormField>
                    <FormField id="login-password" label="Contraseña" hint="Prueba: PolarCliente#26" error={log.errores.password}>
                        <input {...log.campo('password')} type="password" autoComplete="current-password" />
                    </FormField>
                    <button className="btn" disabled={enviandoL}>{enviandoL ? 'Ingresando…' : 'Ingresar'}</button>
                    <button type="button" className="btn secondary" onClick={() => mostrar('El inicio con Google (HU002) aún no está disponible.', 'error')}>
                        Continuar con Google
                    </button>
                </form>
                <p className="muted">¿No tienes cuenta? <Link className="link" to={volver ? `/registro?volver=${encodeURIComponent(volver)}` : '/registro'}>Crear cuenta</Link></p>
                <p className="muted">
                    Cuentas demo: admin@bocado.cl (PolarAdmin#26), ventas@bocado.cl (PolarVentas#26), bodega@bocado.cl (PolarBodega#26), soporte@bocado.cl (PolarSoporte#26), reparto@bocado.cl y reparto2@bocado.cl (PolarReparto#26).
                </p>
            </section>
        </>
    );
};

export default Login;
