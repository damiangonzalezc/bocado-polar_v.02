// pages/NotFound.tsx
import React from 'react';
import EmptyState from '../components/EmptyState';

const NotFound: React.FC = () => (
    <EmptyState mensaje="No encontramos la página que buscas." accion={{ texto: 'Volver al inicio', a: '/' }} />
);

export default NotFound;
