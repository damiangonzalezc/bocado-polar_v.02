// services/navigationService.ts
// Reglas de navegación según el rol (funciones puras, fáciles de probar).

import type { Rol } from '../types';

class NavigationService {
    /** Página de inicio según el rol después de iniciar sesión. */
    static paginaPorRol(rol: Rol): string {
        if (rol === 'REPARTIDOR') return '/repartidor';
        if (rol === 'CLIENTE') return '/catalogo';
        return '/admin';
    }

    /** Solo se aceptan destinos internos del tipo "/seguimiento" (evita redirecciones a otros sitios). */
    static destinoSeguro(volver: string | null): string | null {
        return volver && /^\/[a-z-/]*$/.test(volver) && !volver.startsWith('//') ? volver : null;
    }
}

export default NavigationService;
