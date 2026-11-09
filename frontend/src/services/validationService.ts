// services/validationService.ts
// Validaciones del prototipo (validations.js) llevadas a TypeScript.
// - Funciones puras (validarRut, validarTarjeta, ...) fáciles de probar.
// - "Reglas": devuelven '' si el valor es válido o el mensaje de error personalizado.
//   Los campos vacíos solo los revisa la regla "requerido".

/** Una regla recibe el valor del campo y todos los valores del formulario. */
export type Regla = (valor: string, valores: Record<string, string>) => string;

export interface AnalisisContrasena {
    len: boolean;
    may: boolean;
    min: boolean;
    num: boolean;
}

/** RN-02: máximo de kilos por producto/formato en el carrito. */
export const MAX_KG_FORMATO = 12.5;

const LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' -]+$/;

class ValidationService {
    /* =================== Funciones puras =================== */

    static limpiarRut(rut: string): string {
        return String(rut).replace(/[^0-9kK]/g, '').toUpperCase();
    }

    /** RUT chileno con dígito verificador (módulo 11). */
    static validarRut(rut: string): boolean {
        const r = this.limpiarRut(rut);
        if (r.length < 8 || r.length > 9) return false;
        const cuerpo = r.slice(0, -1);
        const dv = r.slice(-1);
        if (!/^\d+$/.test(cuerpo)) return false;
        let suma = 0;
        let mult = 2;
        for (let i = cuerpo.length - 1; i >= 0; i--) {
            suma += Number(cuerpo[i]) * mult;
            mult = mult === 7 ? 2 : mult + 1;
        }
        const esperado = 11 - (suma % 11);
        const dvCalculado = esperado === 11 ? '0' : esperado === 10 ? 'K' : String(esperado);
        return dv === dvCalculado;
    }

    /** "123456785" → "12.345.678-5" */
    static formatearRut(rut: string): string {
        const r = this.limpiarRut(rut);
        if (r.length < 2) return r;
        return r.slice(0, -1).replace(/\B(?=(\d{3})+(?!\d))/g, '.') + '-' + r.slice(-1);
    }

    /** Formato que espera el backend: sin puntos y con guion (12345678-5). */
    static rutParaApi(rut: string): string {
        const r = this.limpiarRut(rut);
        return r.length < 2 ? '' : `${r.slice(0, -1)}-${r.slice(-1)}`;
    }

    /** Algoritmo de Luhn para números de tarjeta. */
    static validarTarjeta(numero: string): boolean {
        const n = String(numero).replace(/\s/g, '');
        if (!/^\d{13,19}$/.test(n)) return false;
        let suma = 0;
        let doble = false;
        for (let i = n.length - 1; i >= 0; i--) {
            let d = Number(n[i]);
            if (doble) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            suma += d;
            doble = !doble;
        }
        return suma % 10 === 0;
    }

    static formatearTarjeta(v: string): string {
        return v.replace(/\D/g, '').slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ');
    }

    static formatearVencimiento(v: string): string {
        const d = v.replace(/\D/g, '').slice(0, 4);
        return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
    }

    static analizarContrasena(pw: string): AnalisisContrasena {
        return {
            len: pw.length >= 8,
            may: /[A-Z]/.test(pw),
            min: /[a-z]/.test(pw),
            num: /\d/.test(pw),
        };
    }

    static validarContrasena(pw: string): boolean {
        const r = this.analizarContrasena(pw);
        return r.len && r.may && r.min && r.num;
    }

    /**
     * Correo como lo exige ms-auth: termina en .com o .cl y ese final aparece una sola vez
     * (no se permite .com.cl ni .cl.com).
     */
    static mensajeCorreo(email: string): string {
        const v = email.trim().toLowerCase();
        const m = /^([^\s@]+)@([^\s@]+)$/.exec(v);
        const generico = 'Escribe un correo válido, por ejemplo nombre@dominio.cl.';
        if (!m) return generico;
        const [, local, dominio] = m;
        const etiquetas = dominio.split('.');
        if (!/^[a-z0-9._%+-]+$/.test(local) || /^\.|\.$|\.\./.test(local)) return generico;
        if (etiquetas.length < 2 || etiquetas.some((e) => !/^[a-z0-9-]+$/.test(e))) return generico;
        const final = etiquetas[etiquetas.length - 1];
        if (final !== 'com' && final !== 'cl') return 'El correo debe terminar en .com o .cl.';
        if (etiquetas.slice(0, -1).some((e) => e === 'com' || e === 'cl')) {
            return 'Usa un solo .com o .cl: no se permite .com.cl ni terminaciones repetidas.';
        }
        return '';
    }

    static validarTelefono(tel: string): boolean {
        return /^(\+?56)?\s?9\s?\d{4}\s?\d{4}$/.test(tel.trim());
    }

    /** Número con coma o punto decimal: "1,5" → 1.5 */
    static numero(v: string): number {
        return Number(String(v).trim().replace(',', '.'));
    }

    /** Días entre hoy y una fecha "YYYY-MM-DD". */
    static diasDesdeHoy(fecha: string): number {
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        return Math.round((new Date(fecha + 'T00:00:00').getTime() - hoy.getTime()) / 86400000);
    }

    /* =================== Reglas para formularios =================== */

    static requerido(etiqueta = 'Este campo'): Regla {
        return (v) => (v.trim() ? '' : `${etiqueta} es un dato obligatorio.`);
    }

    /** Solo letras y espacios, con largo mínimo y máximo. */
    static texto(min = 2, max = 30): Regla {
        return (v) => {
            const t = v.trim();
            if (!t) return '';
            if (!LETRAS.test(t)) return 'Usa solo letras y espacios.';
            return t.length >= min && t.length <= max ? '' : `Debe tener entre ${min} y ${max} caracteres (tiene ${t.length}).`;
        };
    }

    static largo(min: number, max: number): Regla {
        return (v) => {
            const n = v.trim().length;
            if (!n) return '';
            return n >= min && n <= max ? '' : `Debe tener entre ${min} y ${max} caracteres (tiene ${n}).`;
        };
    }

    static rut(): Regla {
        return (v) => (!v.trim() || ValidationService.validarRut(v) ? '' : 'El RUT no es válido. Revisa el dígito verificador (ej: 12.345.678-5).');
    }

    static email(): Regla {
        return (v) => (!v.trim() ? '' : ValidationService.mensajeCorreo(v));
    }

    static telefono(): Regla {
        return (v) => (!v.trim() || ValidationService.validarTelefono(v) ? '' : 'Teléfono chileno inválido. Formato: +56 9 1234 5678.');
    }

    /** Dirección (prototipo V3): calle con o sin número (ej: O'Higgins 1234 o Camino a Lota). */
    static direccion(): Regla {
        return (v) => {
            const t = v.trim();
            if (!t) return '';
            return t.length >= 5 && t.length <= 100 && /[A-Za-zÁÉÍÓÚÑáéíóúñ]{3}/.test(t)
                ? ''
                : 'Escribe la calle (5 a 100 caracteres). El número es opcional.';
        };
    }

    static password(): Regla {
        return (v) =>
            !v || ValidationService.validarContrasena(v)
                ? ''
                : 'La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número.';
    }

    static coincide(otroCampo: string, mensaje = 'Las contraseñas no coinciden.'): Regla {
        return (v, valores) => (!v || v === valores[otroCampo] ? '' : mensaje);
    }

    static tarjeta(): Regla {
        return (v) =>
            !v.trim() || ValidationService.validarTarjeta(v)
                ? ''
                : 'Número de tarjeta inválido. Revisa los dígitos (prueba: 4111 1111 1111 1111).';
    }

    static vencimientoTarjeta(): Regla {
        return (v) => {
            if (!v.trim()) return '';
            const m = /^(\d{2})\/(\d{2})$/.exec(v.trim());
            if (!m || Number(m[1]) < 1 || Number(m[1]) > 12) return 'Usa el formato MM/AA (ej: 08/28).';
            const vence = new Date(2000 + Number(m[2]), Number(m[1]), 0, 23, 59);
            return vence >= new Date() ? '' : 'La tarjeta está vencida.';
        };
    }

    static cvv(): Regla {
        return (v) => (!v.trim() || /^\d{3,4}$/.test(v.trim()) ? '' : 'El código de seguridad tiene 3 o 4 dígitos.');
    }

    /** Kilos en pasos de 0,5 con un máximo (RN-02 o el stock disponible). */
    static kg(max: number = MAX_KG_FORMATO): Regla {
        return (v) => {
            if (!v.trim()) return '';
            const n = ValidationService.numero(v);
            if (Number.isNaN(n)) return 'Ingresa un número (ej: 1,5).';
            if (n < 0.5) return 'La cantidad mínima es 0,5 kg.';
            if (n > max) return `Máximo ${String(max).replace('.', ',')} kg por formato (RN-02) o el stock disponible.`;
            return Math.round(n * 2) === n * 2 ? '' : 'Usa múltiplos de 0,5 kg (0,5 · 1 · 1,5 …).';
        };
    }

    /** Fecha entre hoy + minDias y hoy + maxDias. */
    static fechaEntrega(minDias = 1, maxDias = 14): Regla {
        return (v) => {
            if (!v) return '';
            const dias = ValidationService.diasDesdeHoy(v);
            return dias >= minDias && dias <= maxDias ? '' : `Elige una fecha entre mañana y dentro de ${maxDias} días.`;
        };
    }

    static fechaFutura(): Regla {
        return (v) => (!v || ValidationService.diasDesdeHoy(v) >= 0 ? '' : 'La fecha no puede estar en el pasado.');
    }

    static codigoPedido(): Regla {
        return (v) => (!v.trim() || /^BP-\d{6}$/i.test(v.trim()) ? '' : 'El código tiene el formato BP-123456.');
    }

    static codigoCupon(): Regla {
        return (v) => (!v.trim() || /^[A-Za-z0-9]{4,16}$/.test(v.trim()) ? '' : 'El código usa 4 a 16 letras o números, sin espacios.');
    }

    static sku(): Regla {
        return (v) => (!v.trim() || /^[A-Z0-9-]{3,12}$/.test(v.trim()) ? '' : 'El SKU usa 3 a 12 letras MAYÚSCULAS, números o guiones (ej: SAL-001).');
    }

    static codigoLote(): Regla {
        return (v) => (!v.trim() || /^[A-Z0-9-]{4,16}$/.test(v.trim()) ? '' : 'El lote usa 4 a 16 letras MAYÚSCULAS, números o guiones (ej: SAL-2609-A).');
    }

    static patente(): Regla {
        return (v) => (!v.trim() || /^[A-Za-z]{2,4}-?\d{2,4}$/.test(v.trim()) ? '' : 'Patente inválida. Ejemplo: KLTY-21 o AB-1234.');
    }

    static hora(): Regla {
        return (v) => (!v.trim() || /^([01]\d|2[0-3]):[0-5]\d$/.test(v.trim()) ? '' : 'Usa el formato HH:MM (ej: 08:30).');
    }

    /** Número dentro de un rango (acepta coma decimal). */
    static rango(min: number, max: number, unidad = ''): Regla {
        return (v) => {
            if (!v.trim()) return '';
            const n = ValidationService.numero(v);
            if (Number.isNaN(n)) return 'Ingresa un número válido.';
            return n >= min && n <= max ? '' : `Debe estar entre ${min} y ${max}${unidad ? ' ' + unidad : ''}.`;
        };
    }

    static entero(min: number, max: number): Regla {
        return (v) => {
            if (!v.trim()) return '';
            const n = Number(v);
            if (!Number.isInteger(n)) return 'Ingresa un número entero.';
            return n >= min && n <= max ? '' : `Debe estar entre ${min} y ${max}.`;
        };
    }
}

export default ValidationService;
