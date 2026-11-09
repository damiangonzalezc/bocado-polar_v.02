// components/ErrorNotice.tsx
// Aviso de error con botón para reintentar (cuando un microservicio no responde).

import React from 'react';

const ErrorNotice: React.FC<{ mensaje: string; onReintentar?: () => void }> = ({ mensaje, onReintentar }) =>
    mensaje ? (
        <div className="notice error" role="alert">
            {mensaje}{' '}
            {onReintentar && (
                <button type="button" className="link" onClick={onReintentar}>
                    Reintentar
                </button>
            )}
        </div>
    ) : null;

export default ErrorNotice;
