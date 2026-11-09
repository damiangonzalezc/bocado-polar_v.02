// hooks/useForm.ts
// Motor de validación del prototipo (validations.js) como hook de React:
// - Cada campo tiene una lista de reglas; se muestra el mensaje de la primera que falla.
// - El error aparece al salir del campo (blur) y, una vez tocado, se corrige en vivo.
// - Al enviar: valida todo, muestra un resumen ("Revisa 2 campos…") y enfoca el primer error.
// - Los errores que devuelve el backend (detalles) se pueden pintar en los mismos campos.

import { useCallback, useState } from 'react';
import type { ChangeEvent } from 'react';
import type { Regla } from '../services/validationService';

export interface ConfigCampo {
    reglas: Regla[];
    /** Formato que se aplica al salir del campo o al enviar (ej: RUT 12.345.678-5). */
    formato?: (valor: string) => string;
    /** Si es true el formato se aplica mientras se escribe (tarjeta, MM/AA). */
    formatoEnVivo?: boolean;
}

type Elemento = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

export function useForm<K extends string>(
    config: Record<K, ConfigCampo>,
    iniciales: Record<K, string>,
    prefijo = '',
) {
    const [valores, setValoresState] = useState<Record<K, string>>(iniciales);
    const [errores, setErrores] = useState<Partial<Record<K, string>>>({});
    const [tocados, setTocados] = useState<Partial<Record<K, boolean>>>({});
    const [resumen, setResumen] = useState('');

    const nombres = Object.keys(config) as K[];
    const idDe = (n: K) => `${prefijo}${n}`;

    const evaluar = useCallback((nombre: K, vals: Record<K, string>): string => {
        const campo = config[nombre];
        if (!campo) return '';
        for (const regla of campo.reglas) {
            const msg = regla(vals[nombre] ?? '', vals as Record<string, string>);
            if (msg) return msg;
        }
        return '';
    }, [config]);

    const cambiar = (nombre: K, valor: string) => {
        const campo = config[nombre];
        const final = campo?.formato && campo.formatoEnVivo ? campo.formato(valor) : valor;
        const nuevos = { ...valores, [nombre]: final };
        setValoresState(nuevos);
        if (tocados[nombre]) {
            setErrores((e) => ({ ...e, [nombre]: evaluar(nombre, nuevos) }));
        }
    };

    const salir = (nombre: K) => {
        const campo = config[nombre];
        const valor = valores[nombre] ?? '';
        const final = campo?.formato && valor.trim() ? campo.formato(valor) : valor;
        const nuevos = final === valor ? valores : { ...valores, [nombre]: final };
        if (final !== valor) setValoresState(nuevos);
        setTocados((t) => ({ ...t, [nombre]: true }));
        setErrores((e) => ({ ...e, [nombre]: evaluar(nombre, nuevos) }));
    };

    /** Clase visual del campo: input/select + is-invalid / is-valid. */
    const clase = (nombre: K, base: 'input' | 'select' = 'input') => {
        const err = errores[nombre];
        const lleno = (valores[nombre] ?? '').trim() !== '';
        return `${base}${err ? ' is-invalid' : tocados[nombre] && lleno ? ' is-valid' : ''}`;
    };

    /** Props para <input>, <select> o <textarea>. */
    const campo = (nombre: K, base: 'input' | 'select' = 'input') => ({
        id: idDe(nombre),
        name: nombre,
        value: valores[nombre] ?? '',
        className: clase(nombre, base),
        'aria-invalid': errores[nombre] ? true : false,
        'aria-describedby': `${idDe(nombre)}-hint ${idDe(nombre)}-error`,
        onChange: (e: ChangeEvent<Elemento>) => cambiar(nombre, e.target.value),
        onBlur: () => salir(nombre),
    });

    /** Props para una casilla (checkbox). Su valor es '1' si está marcada. */
    const casilla = (nombre: K) => ({
        id: idDe(nombre),
        name: nombre,
        type: 'checkbox' as const,
        checked: valores[nombre] === '1',
        'aria-invalid': errores[nombre] ? true : false,
        'aria-describedby': `${idDe(nombre)}-error`,
        onChange: (e: ChangeEvent<HTMLInputElement>) => {
            const nuevos = { ...valores, [nombre]: e.target.checked ? '1' : '' };
            setValoresState(nuevos);
            setTocados((t) => ({ ...t, [nombre]: true }));
            setErrores((er) => ({ ...er, [nombre]: evaluar(nombre, nuevos) }));
        },
    });

    /** Valida todo el formulario. Devuelve true si no hay errores. */
    const validarTodo = (): boolean => {
        const formateados = { ...valores };
        nombres.forEach((n) => {
            const f = config[n].formato;
            if (f && formateados[n]?.trim()) formateados[n] = f(formateados[n]);
        });
        const nuevos: Partial<Record<K, string>> = {};
        const invalidos: K[] = [];
        nombres.forEach((n) => {
            const msg = evaluar(n, formateados);
            nuevos[n] = msg;
            if (msg) invalidos.push(n);
        });
        setValoresState(formateados);
        setErrores(nuevos);
        setTocados(Object.fromEntries(nombres.map((n) => [n, true])) as Partial<Record<K, boolean>>);
        setResumen(
            invalidos.length
                ? `Revisa ${invalidos.length} campo${invalidos.length > 1 ? 's' : ''} marcado${invalidos.length > 1 ? 's' : ''} antes de continuar.`
                : '',
        );
        if (invalidos.length) document.getElementById(idDe(invalidos[0]))?.focus();
        return invalidos.length === 0;
    };

    /** Pinta los errores por campo que devolvió el backend y un resumen general. */
    const erroresServidor = (detalles: Record<string, string>, mensaje: string) => {
        const propios: Partial<Record<K, string>> = {};
        Object.entries(detalles).forEach(([k, v]) => {
            if ((nombres as string[]).includes(k)) propios[k as K] = v;
        });
        setErrores((e) => ({ ...e, ...propios }));
        setResumen(mensaje);
        const primero = Object.keys(propios)[0] as K | undefined;
        if (primero) document.getElementById(idDe(primero))?.focus();
    };

    const setValores = (parcial: Partial<Record<K, string>>) => setValoresState((v) => ({ ...v, ...parcial }));

    const reiniciar = (nuevos: Record<K, string> = iniciales) => {
        setValoresState(nuevos);
        setErrores({});
        setTocados({});
        setResumen('');
    };

    return {
        valores,
        errores,
        resumen,
        setResumen,
        campo,
        casilla,
        clase,
        cambiar,
        validarTodo,
        erroresServidor,
        setValores,
        reiniciar,
        idDe,
    };
}
