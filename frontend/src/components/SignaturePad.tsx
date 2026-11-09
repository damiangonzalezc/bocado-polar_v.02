// components/SignaturePad.tsx
// Firma del cliente con el dedo o el mouse. Entrega la imagen como data URL (PNG).

import React, { useEffect, useRef } from 'react';
import type { PointerEvent } from 'react';

interface SignaturePadProps {
    id: string;
    onCambio: (dataUrl: string) => void;
}

const SignaturePad: React.FC<SignaturePadProps> = ({ id, onCambio }) => {
    const canvas = useRef<HTMLCanvasElement>(null);
    const dibujando = useRef(false);
    const trazo = useRef(false);

    useEffect(() => {
        const c = canvas.current;
        if (!c) return;
        c.width = c.offsetWidth || 300;
        c.height = c.offsetHeight || 150;
        const ctx = c.getContext('2d');
        if (ctx) {
            ctx.lineWidth = 2.2;
            ctx.lineCap = 'round';
            ctx.strokeStyle = '#173042';
        }
    }, []);

    const punto = (e: PointerEvent<HTMLCanvasElement>) => {
        const r = e.currentTarget.getBoundingClientRect();
        return { x: e.clientX - r.left, y: e.clientY - r.top };
    };

    const empezar = (e: PointerEvent<HTMLCanvasElement>) => {
        const ctx = canvas.current?.getContext('2d');
        if (!ctx) return;
        dibujando.current = true;
        const { x, y } = punto(e);
        ctx.beginPath();
        ctx.moveTo(x, y);
    };

    const mover = (e: PointerEvent<HTMLCanvasElement>) => {
        if (!dibujando.current) return;
        const ctx = canvas.current?.getContext('2d');
        if (!ctx) return;
        const { x, y } = punto(e);
        ctx.lineTo(x, y);
        ctx.stroke();
        trazo.current = true;
    };

    const terminar = () => {
        if (!dibujando.current) return;
        dibujando.current = false;
        if (trazo.current && canvas.current) onCambio(canvas.current.toDataURL('image/png'));
    };

    const limpiar = () => {
        const c = canvas.current;
        c?.getContext('2d')?.clearRect(0, 0, c.width, c.height);
        trazo.current = false;
        onCambio('');
    };

    return (
        <div>
            <canvas
                id={id}
                ref={canvas}
                className="signature"
                aria-label="Recuadro para la firma del cliente"
                onPointerDown={empezar}
                onPointerMove={mover}
                onPointerUp={terminar}
                onPointerLeave={terminar}
            />
            <button type="button" className="btn secondary small" onClick={limpiar}>
                Borrar firma
            </button>
        </div>
    );
};

export default SignaturePad;
