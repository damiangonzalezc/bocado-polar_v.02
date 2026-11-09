package cl.bocadopolar.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sección del panel admin que un rol puede ver (resumen, pedidos, inventario, ...). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permisos_rol", uniqueConstraints = @UniqueConstraint(columnNames = { "rol", "seccion" }))
public class PermisoRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;

    @Column(nullable = false, length = 30)
    private String seccion;

    @Column(nullable = false)
    private Boolean concedido;
}
