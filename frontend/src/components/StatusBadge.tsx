// components/StatusBadge.tsx
// Estado del pedido con el color del prototipo (.status ok / pending / bad).

import React from 'react';
import FormatService from '../services/formatService';
import type { EstadoPedido } from '../types';

const StatusBadge: React.FC<{ estado: EstadoPedido }> = ({ estado }) => (
    <strong className={`status ${FormatService.claseEstado(estado)}`}>{FormatService.estadoPedido(estado)}</strong>
);

export default StatusBadge;
