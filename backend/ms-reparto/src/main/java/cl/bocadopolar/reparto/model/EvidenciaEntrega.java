package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Foto y/o firma. Se aceptan URLs o imágenes en base64 (data:image/png;base64,...). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evidencias_entrega")
public class EvidenciaEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fotoUrl;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String firmaUrl;
}
