// services/themeService.ts
// Modo claro / oscuro (como theme.js del prototipo V3): se aplica con <html data-theme="dark">,
// recuerda la elección en localStorage y, si no hay, usa la preferencia del sistema.

export type Tema = 'light' | 'dark';

const CLAVE = 'bocadoTheme';

class ThemeService {
    static guardado(): Tema | null {
        try {
            const v = localStorage.getItem(CLAVE);
            return v === 'dark' || v === 'light' ? v : null;
        } catch {
            return null;
        }
    }

    static delSistema(): Tema {
        return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    /** El tema guardado o, si no hay, el del sistema operativo. */
    static inicial(): Tema {
        return this.guardado() ?? this.delSistema();
    }

    static aplicar(tema: Tema): void {
        document.documentElement.setAttribute('data-theme', tema);
    }

    static guardar(tema: Tema): void {
        try {
            localStorage.setItem(CLAVE, tema);
        } catch {
            /* almacenamiento no disponible: solo dura esta visita */
        }
    }

    static opuesto(tema: Tema): Tema {
        return tema === 'dark' ? 'light' : 'dark';
    }
}

export default ThemeService;
