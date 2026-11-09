// services/formatService.ts
// Formatos de moneda, kilos y fechas en estilo chileno.

import type { EstadoPedido, Producto } from '../types';

const clp = new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 });

const NOMBRE_ESTADO: Record<EstadoPedido, string> = {
    PENDIENTE_PAGO: 'Pendiente de pago',
    PAGADO: 'Pagado',
    EN_PREPARACION: 'En preparación',
    ENVIADO: 'Enviado',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
    REEMBOLSADO: 'Reembolsado',
};

class FormatService {
    static dinero(n: number | null | undefined): string {
        return clp.format(Math.round(Number(n ?? 0)));
    }

    /** 2.5 → "2,5" */
    static kg(n: number | null | undefined): string {
        return String(Number(n ?? 0)).replace('.', ',');
    }

    /** "2026-11-07" → "07-11-2026" (sin pasar por Date para no correr el día por zona horaria). */
    static fecha(iso: string | null | undefined): string {
        if (!iso) return '—';
        const [a, m, d] = iso.slice(0, 10).split('-');
        return d && m && a ? `${d}-${m}-${a}` : iso;
    }

    /** Instant ISO → "07-11-2026 18:45" en hora local. */
    static fechaHora(iso: string | null | undefined): string {
        if (!iso) return '—';
        const f = new Date(iso);
        if (Number.isNaN(f.getTime())) return iso;
        const dd = String(f.getDate()).padStart(2, '0');
        const mm = String(f.getMonth() + 1).padStart(2, '0');
        const hh = String(f.getHours()).padStart(2, '0');
        const mi = String(f.getMinutes()).padStart(2, '0');
        return `${dd}-${mm}-${f.getFullYear()} ${hh}:${mi}`;
    }

    /** "08:30:00" → "08:30" */
    static hora(t: string | null | undefined): string {
        return t ? t.slice(0, 5) : '—';
    }

    /** Fecha local "YYYY-MM-DD" desplazada n días desde hoy. */
    static fechaEnDias(n: number): string {
        const d = new Date();
        d.setDate(d.getDate() + n);
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }

    /** Días desde hoy hasta la fecha "YYYY-MM-DD" (negativo si ya pasó). */
    static diasHasta(iso: string): number {
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        return Math.round((new Date(iso + 'T00:00:00').getTime() - hoy.getTime()) / 86400000);
    }

    /** Clase de color del prototipo: ok (verde), pending (ámbar) o bad (rojo). */
    static claseEstado(estado: EstadoPedido): 'ok' | 'pending' | 'bad' {
        if (estado === 'ENTREGADO') return 'ok';
        if (estado === 'CANCELADO' || estado === 'REEMBOLSADO') return 'bad';
        return 'pending';
    }

    static estadoPedido(e: EstadoPedido): string {
        return NOMBRE_ESTADO[e] ?? e;
    }

    /** Texto legible para valores en MAYÚSCULAS_CON_GUION: "FUERA_SERVICIO" → "Fuera servicio". */
    static etiqueta(valor: string | null | undefined): string {
        if (!valor) return '—';
        const t = valor.replace(/_/g, ' ').toLowerCase();
        return t.charAt(0).toUpperCase() + t.slice(1);
    }

    /** Las imágenes del catálogo vienen como "img/prod-salmon.svg" (carpeta public). */
    static imagenProducto(p: Pick<Producto, 'imagen'> | { imagen?: string }): string {
        const img = p.imagen?.trim();
        if (!img) return '/img/logo.svg';
        return /^(https?:|data:|\/)/.test(img) ? img : `/${img}`;
    }

    static porcentaje(n: number | null | undefined): string {
        if (n === null || n === undefined) return '—';
        return `${n > 0 ? '+' : ''}${String(n).replace('.', ',')}%`;
    }
}

export default FormatService;
