// components/FormSummary.tsx
// Resumen de errores al enviar un formulario (role="alert").

import React from 'react';

const FormSummary: React.FC<{ mensaje: string }> = ({ mensaje }) =>
    mensaje ? (
        <p className="notice error form-summary" role="alert">
            {mensaje}
        </p>
    ) : null;

export default FormSummary;
