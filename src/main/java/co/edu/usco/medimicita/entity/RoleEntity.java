package co.edu.usco.medimicita.entity;

import jakarta.persistence.*; // Para JPA (Jakarta Persistence API)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Específico de Hibernate para timestamps automáticos
import java.time.OffsetDateTime; // Recomendado para timestamps con zona horaria

@Entity // (1) Marca esta clase como una entidad JPA.
@Table(name = "roles") // (2) Especifica la tabla de la base de datos a la que se mapea.
@Data // (3) Lombok: Genera getters, setters, toString(), equals(), y hashCode().
@NoArgsConstructor // (4) Lombok: Genera un constructor sin argumentos (requerido por JPA).
@AllArgsConstructor // (5) Lombok: Genera un constructor con todos los argumentos (útil para crear instancias).
public class RoleEntity {

    @Id // (6) Marca este campo como la clave primaria (PK).
    @GeneratedValue(strategy = GenerationType.IDENTITY) // (7) Configura la generación automática de la PK.
    // IDENTITY es común para PostgreSQL SERIAL/BIGSERIAL.
    @Column(name = "role_id") // (8) Mapea este campo a la columna 'role_id' en la tabla.
    private Integer roleId; // (9) El tipo de dato Java para la PK (Integer para SERIAL).

    @Column(name = "role_name", unique = true, nullable = false, length = 50) // (10) Mapeo de columna con constraints:
    // unique = true -> Constraint UNIQUE
    // nullable = false -> Constraint NOT NULL
    // length = 50 -> Para VARCHAR(50)
    private String roleName;

    @CreationTimestamp // (11) Hibernate: Asigna automáticamente la fecha/hora de creación del registro.
    // Se ejecuta cuando la entidad es persistida por primera vez.
    @Column(name = "role_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP") // (12) Mapeo de columna:
    // nullable = false -> NOT NULL
    // updatable = false -> Esta columna no se actualiza una vez creada.
    // columnDefinition -> Específico para la BD. Asegura que si se inserta
    // directamente en BD, también tenga un valor por defecto.
    private OffsetDateTime roleCreatedAt; // (13) java.time.OffsetDateTime para TIMESTAMP WITH TIME ZONE.
    // Alternativamente, podrías usar LocalDateTime si no manejas zonas horarias explícitamente.

    // (14) Relaciones:
    // Si necesitas navegar desde RoleEntity a los UserEntity que tienen este rol (relación inversa):
    // @OneToMany(mappedBy = "role", fetch = FetchType.LAZY) // 'role' es el nombre del campo en UserEntity que mapea a RoleEntity.
    // private java.util.List<UserEntity> users;
    // Por ahora, la dejaremos comentada para mantener la entidad simple.
    // La agregarías si realmente necesitas esa navegación bidireccional.
}
