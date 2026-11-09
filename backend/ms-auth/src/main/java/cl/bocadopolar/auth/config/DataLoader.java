package cl.bocadopolar.auth.config;

import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import cl.bocadopolar.auth.model.EstadoUsuario;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.model.Usuario;
import cl.bocadopolar.auth.repository.PermisoRolRepository;
import cl.bocadopolar.auth.repository.UsuarioRepository;
import cl.bocadopolar.auth.service.PermisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cuentas de prueba del prototipo (IDs 1 a 7 en una base nueva):
 * 1 admin, 2 ventas, 3 bodega, 4 soporte, 5 reparto, 6 reparto2, 7 cliente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PermisoRolRepository permisoRepository;
    private final PermisoService permisoService;
    private final PasswordEncoder encoder;

    /**
     * Cuentas de prueba: {nombre, apellido, correo, contraseña nueva, contraseña antigua, teléfono, rut}.
     * Las contraseñas antiguas (Admin123, Cliente123…) aparecen en listas de filtraciones y Chrome muestra
     * «Cambia tu contraseña»; por eso se usan otras únicas.
     */
    private static final String[][] DEMO = {
            { "Administrador", "BOCADO", "admin@bocado.cl", "PolarAdmin#26", "Admin123", "+56 9 1111 1111", null },
            { "Equipo", "Ventas", "ventas@bocado.cl", "PolarVentas#26", "Ventas123", "+56 9 2222 2222", null },
            { "Bruno", "Bodega", "bodega@bocado.cl", "PolarBodega#26", "Bodega123", "+56 9 2323 2323", null },
            { "Sofía", "Soporte", "soporte@bocado.cl", "PolarSoporte#26", "Soporte123", "+56 9 2424 2424", null },
            { "Repartidor", "Demo", "reparto@bocado.cl", "PolarReparto#26", "Reparto123", "+56 9 3333 3333", "12345678-5" },
            { "Rodrigo", "Norte", "reparto2@bocado.cl", "PolarReparto#26", "Reparto123", "+56 9 3434 3434", "13456789-9" },
            { "Cliente", "Demo", "cliente@bocado.cl", "PolarCliente#26", "Cliente123", "+56 9 4444 4444", "11111111-1" } };

    private static final Rol[] ROLES = { Rol.ADMIN, Rol.VENDEDOR, Rol.BODEGUERO, Rol.SOPORTE, Rol.REPARTIDOR,
            Rol.REPARTIDOR, Rol.CLIENTE };

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            for (int i = 0; i < DEMO.length; i++) {
                String[] d = DEMO[i];
                crear(d[0], d[1], d[2], d[3], ROLES[i], d[5], d[6], i == 0);
            }
            log.info("ms-auth: cuentas de prueba creadas");
        } else {
            actualizarContrasenasAntiguas();
        }
        if (permisoRepository.count() == 0) {
            permisoService.sembrar(Rol.VENDEDOR, "resumen", "pedidos", "clientes", "marketing");
            permisoService.sembrar(Rol.BODEGUERO, "resumen", "inventario", "categorias", "frio", "rutas");
            permisoService.sembrar(Rol.SOPORTE, "resumen", "pedidos", "clientes");
        }
    }

    /** Bases creadas antes del cambio: si una cuenta demo sigue con la contraseña antigua, se cambia a la nueva. */
    private void actualizarContrasenasAntiguas() {
        int cambiadas = 0;
        for (String[] d : DEMO) {
            Usuario u = usuarioRepository.findByEmailIgnoreCase(d[2]).orElse(null);
            if (u != null && encoder.matches(d[4], u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode(d[3]));
                usuarioRepository.save(u);
                cambiadas++;
            }
        }
        if (cambiadas > 0) {
            log.info("ms-auth: {} cuentas de prueba actualizadas a las contraseñas nuevas", cambiadas);
        }
    }

    private void crear(String nombre, String apellido, String email, String pass, Rol rol, String tel, String rut,
            boolean protegido) {
        usuarioRepository.save(new Usuario(null, nombre, apellido, email, encoder.encode(pass), rut, tel, rol,
                EstadoUsuario.ACTIVO, protegido, Instant.now()));
    }
}
