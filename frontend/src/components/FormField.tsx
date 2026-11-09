// components/FormField.tsx
// Campo con etiqueta, ayuda (.hint) y mensaje de error (.error-message), igual que el prototipo.

import React from 'react';
import type { ReactNode } from 'react';

interface FormFieldProps {
    id: string;
    label: string;
    hint?: string;
    error?: string;
    children: ReactNode;
    /** Para casillas: la etiqueta envuelve el input. */
    checkbox?: boolean;
    labelOculto?: boolean;
}

const FormField: React.FC<FormFieldProps> = ({ id, label, hint, error, children, checkbox = false, labelOculto = false }) => (
    <div className="field">
        {checkbox ? (
            <label className="checkbox">
                {children} {label}
            </label>
        ) : (
            <>
                <label htmlFor={id} className={labelOculto ? 'visually-hidden' : undefined}>
                    {label}
                </label>
                {children}
            </>
        )}
        {hint && !checkbox && (
            <small className="hint" id={`${id}-hint`}>
                {hint}
            </small>
        )}
        <span className="error-message" id={`${id}-error`} aria-live="polite">
            {error ?? ''}
        </span>
    </div>
);

export default FormField;
