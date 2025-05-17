package co.edu.usco.medimicita.entity;

import co.edu.usco.medimicita.enums.ScheduleExceptionTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
// No necesitamos UpdateTimestamp aquí si las excepciones una vez creadas no se modifican (solo se crean o eliminan).
// Si se pudieran modificar, entonces sí añadiríamos @UpdateTimestamp y la columna correspondiente.

import java.time.OffsetDateTime;

// (1) Considerar constraints CHECK a nivel de BD:
// - CHECK(schedule_exception_end_datetime > schedule_exception_start_datetime)
// - Podría haber lógicas más complejas para evitar solapamientos de excepciones,
//   aunque esto a menudo se maneja en la lógica de servicio o con constraints de exclusión (GiST en PostgreSQL).
@Entity
@Table(name = "schedule_exceptions", indexes = {
        @Index(name = "idx_se_doctor_user_id", columnList = "doctor_user_id"),
        @Index(name = "idx_se_start_datetime", columnList = "schedule_exception_start_datetime"),
        @Index(name = "idx_se_end_datetime", columnList = "schedule_exception_end_datetime"),
        @Index(name = "idx_se_created_by_user_id", columnList = "created_by_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_exception_id")
    private Long scheduleExceptionId;

    // --- Relación con UserEntity (el Médico afectado por la excepción) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_se_doctor_user"))
    private UserEntity doctorUser;

    @Column(name = "schedule_exception_start_datetime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime scheduleExceptionStartDatetime;

    @Column(name = "schedule_exception_end_datetime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime scheduleExceptionEndDatetime;

    @Enumerated(EnumType.STRING) // (2) Almacena el nombre del enum (BLOCK, VACATION, UNAVAILABILITY)
    @Column(name = "schedule_exception_type", nullable = false, length = 20)
    private ScheduleExceptionTypeEnum scheduleExceptionType; // (3) Enum a crear

    @Column(name = "schedule_exception_reason", columnDefinition = "TEXT") // Nullable por defecto
    private String scheduleExceptionReason;

    // --- Relación con UserEntity (el Admin que creó la excepción) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = true, // (4) Nullable si la excepción la crea el sistema o no se rastrea el creador.
            // Sin embargo, el esquema dice "(Admin)", así que podría ser no nullable
            // si siempre es un admin. Lo dejo nullable por flexibilidad.
            foreignKey = @ForeignKey(name = "fk_se_created_by_user"))
    private UserEntity createdByUser; // El usuario (Admin) que creó esta excepción.

    @CreationTimestamp
    @Column(name = "schedule_exception_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime scheduleExceptionCreatedAt;

    // No hay 'updated_at' en tu esquema original para esta tabla,
    // lo cual sugiere que las excepciones se crean y, si es necesario, se eliminan y se crean nuevas
    // en lugar de modificarse. Si se pudieran modificar, se añadiría.
}