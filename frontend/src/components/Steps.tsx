// components/Steps.tsx
// Barra de pasos (.steps del prototipo). Marca activos los pasos hasta "actual".

import React from 'react';

interface StepsProps {
    pasos: string[];
    /** Índice (0..n-1) del último paso activo. -1 = ninguno. */
    actual: number;
    etiqueta: string;
}

const Steps: React.FC<StepsProps> = ({ pasos, actual, etiqueta }) => (
    <ol className="steps" aria-label={etiqueta}>
        {pasos.map((p, i) => (
            <li key={p} className={`step${i <= actual ? ' active' : ''}`} aria-current={i === actual ? 'step' : undefined}>
                {p}
            </li>
        ))}
    </ol>
);

export default Steps;
