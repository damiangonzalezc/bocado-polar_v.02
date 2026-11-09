// components/Loader.tsx
import React from 'react';

const Loader: React.FC<{ texto?: string }> = ({ texto = 'Cargando…' }) => (
    <div className="loader" role="status">
        {texto}
    </div>
);

export default Loader;
