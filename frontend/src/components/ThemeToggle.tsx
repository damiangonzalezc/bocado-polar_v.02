// components/ThemeToggle.tsx · Botón de modo claro / oscuro.

import React from 'react';
import { useTheme } from '../hooks/useTheme';

const Luna = () => (
    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 14.5A8.5 8.5 0 0 1 9.5 3a9 9 0 1 0 11.5 11.5z" /></svg>
);

const Sol = () => (
    <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="4.5" />
        <path d="M12 1.5v3M12 19.5v3M1.5 12h3M19.5 12h3M4.6 4.6l2.1 2.1M17.3 17.3l2.1 2.1M4.6 19.4l2.1-2.1M17.3 6.7l2.1-2.1" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
);

const ThemeToggle: React.FC = () => {
    const { oscuro, alternar } = useTheme();
    return (
        <button type="button" className="btn secondary small theme-toggle" aria-pressed={oscuro} onClick={alternar}>
            {oscuro ? <Sol /> : <Luna />}
            <span>{oscuro ? 'Modo claro' : 'Modo oscuro'}</span>
        </button>
    );
};

export default ThemeToggle;
