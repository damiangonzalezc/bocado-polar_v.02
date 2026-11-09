// components/StarRating.tsx
// Estrellas de 0,5 a 5. Modo lectura (promedio) o selector para que el cliente vote.

import React from 'react';

interface StarRatingProps {
    valor: number;
    votos?: number;
    /** Si viene, se muestra el selector de 0,5 a 5. */
    onElegir?: (estrellas: number) => void;
    elegida?: number;
    deshabilitado?: boolean;
}

const OPCIONES = [0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5];

const StarRating: React.FC<StarRatingProps> = ({ valor, votos, onElegir, elegida = 0, deshabilitado = false }) => {
    const llenas = Math.round(valor);
    return (
        <div>
            <span className="stars" role="img" aria-label={`${String(valor).replace('.', ',')} de 5 estrellas`}>
                {[1, 2, 3, 4, 5].map((i) => (
                    <span key={i} className={i <= llenas ? 'on' : 'off'} aria-hidden="true">
                        ★
                    </span>
                ))}
            </span>{' '}
            <small className="muted">
                {String(valor).replace('.', ',')}
                {votos !== undefined && ` · ${votos} voto${votos === 1 ? '' : 's'}`}
            </small>
            {onElegir && (
                <div className="star-picker" role="group" aria-label="Tu valoración">
                    {OPCIONES.map((o) => (
                        <button
                            key={o}
                            type="button"
                            aria-pressed={elegida === o}
                            disabled={deshabilitado}
                            onClick={() => onElegir(o)}
                        >
                            {String(o).replace('.', ',')}★
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};

export default StarRating;
