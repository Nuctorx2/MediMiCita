package co.edu.usco.medimicita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

// (1) Constraint UNIQUE a nivel de tabla para (user_id, is_current) donde is_current = TRUE
// Esto es más complejo de definir directamente con anotaciones JPA de forma portable.
// Generalmente, se crea esta constraint directamente en la BD o se maneja en la lógica de servicio.
// Por ejemplo, en PostgreSQL:
// CREATE UNIQUE INDEX uidx_user_address_current ON user_addresses (user_id, user_address_is_current)
// WHERE user_address_is_current = TRUE;
// O con una constraint de exclusión si es más complejo.
// Si `ddl-auto` está en `update`, Hibernate no creará esta constraint condicional.
@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_address_user_id", columnList = "user_id"),
        @Index(name = "idx_user_address_is_current", columnList = "user_address_is_current")
        // El índice para la constraint UNIQUE condicional se definiría manualmente en la BD.
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_address_id")
    private Long userAddressId; // Usamos Long por consistencia si otras IDs son Long

    // --- Relación con UserEntity ---
    @ManyToOne(fetch = FetchType.LAZY) // (2) LAZY es bueno aquí. No siempre necesitas el usuario al cargar una dirección.
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_address_user"))
    private UserEntity user;

    @Column(name = "user_address_street_type", nullable = false, length = 20) // Ej: 'Calle', 'Carrera', 'Avenida'
    private String userAddressStreetType; // (3) Considera usar un Enum aquí si tienes un conjunto fijo de tipos.
    // @Enumerated(EnumType.STRING)

    @Column(name = "user_address_street_number", nullable = false, length = 50)
    private String userAddressStreetNumber; // Ej: "123A", "45-67"

    @Column(name = "user_address_cross_street_number", length = 50) // Nullable por defecto
    private String userAddressCrossStreetNumber; // Ej: "con Calle 10", "Bis"

    @Column(name = "user_address_meters", length = 50) // Nullable por defecto
    private String userAddressMeters; // Ej: "50 metros al norte"

    @Column(name = "user_address_additional_info", nullable = false, length = 255) // Ej: "Barrio Los Pinos, Apto 301, Torre B"
    private String userAddressAdditionalInfo;

    @Column(name = "user_address_is_current", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean userAddressIsCurrent = true; // (4) Indica si es la dirección principal/actual del usuario.
    // La lógica para asegurar solo una TRUE por user_id
    // se manejaría en la capa de servicio.

    @CreationTimestamp
    @Column(name = "user_address_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userAddressCreatedAt;

    @UpdateTimestamp
    @Column(name = "user_address_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userAddressUpdatedAt;

//  (1) Constraint UNIQUE Condicional (Comentario Importante):
//    Tu esquema original menciona: (Constraint: UNIQUE(user_id, is_current) where is_current = TRUE -- Depende del SGBD).
//    Esta es una constraint de unicidad parcial o filtrada. JPA estándar no tiene una forma directa de definir esto mediante anotaciones que funcione en todos los SGBD.
//    PostgreSQL: Puedes crear esto usando un índice único parcial:

//    CREATE UNIQUE INDEX uidx_user_address_current ON user_addresses (user_id, user_address_is_current)
//    WHERE user_address_is_current = TRUE;

//    Hibernate no generará esta DDL automáticamente con ddl-auto. Deberás crearla manualmente en tu script de migración de base de datos o directamente.
//La lógica para asegurar que solo una dirección sea is_current = true por usuario debe implementarse robustamente en tu capa de servicio (por ejemplo, antes de guardar una nueva dirección como actual, poner todas las demás de ese usuario como no actuales).
}