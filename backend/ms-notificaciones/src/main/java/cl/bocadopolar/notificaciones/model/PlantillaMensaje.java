package cl.bocadopolar.notificaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Plantilla por motivo. Usa variables con doble llave: "Hola {{cliente}}, tu pedido {{codigo}}..." */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plantillas_mensaje")
public class PlantillaMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private MotivoNotificacion motivo;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(nullable = false)
    private Boolean activa;
}
