package co.edu.usco.medimicita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek; // Para el día de la semana
import java.time.LocalTime; // Para la hora de inicio y fin
import java.time.OffsetDateTime;

// (1) Constraint UNIQUE a nivel de tabla para (doctor_user_id, day_of_week, start_time, end_time)
// para evitar duplicados exactos de plantillas de horario.
// Se puede definir un índice único compuesto.
@Entity
@Table(name = "doctor_schedule_templates", indexes = {
        @Index(name = "idx_dst_doctor_user_id", columnList = "doctor_user_id"),
        @Index(name = "idx_dst_day_of_week", columnList = "doctor_schedule_template_day_of_week"),
        // Índice para la constraint UNIQUE mencionada en el comentario (1)
        @Index(name = "uidx_dst_doctor_day_times",
                columnList = "doctor_user_id, doctor_schedule_template_day_of_week, doctor_schedule_template_start_time, doctor_schedule_template_end_time",
                unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_schedule_template_id")
    private Long doctorScheduleTemplateId;

    // --- Relación con UserEntity (el Médico) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_dst_doctor_user"))
    private UserEntity doctorUser; // El médico al que pertenece esta plantilla de horario

    // (2) Día de la semana. ISO 8601: 1 (Lunes) a 7 (Domingo).
    @Enumerated(EnumType.ORDINAL) // Almacena el ordinal del enum (0 para LUNES, 1 para MARTES, etc. si DayOfWeek empieza en LUNES)
    // O podrías usar EnumType.STRING si prefieres almacenar el nombre ("MONDAY", "TUESDAY").
    // O simplemente un Integer si no usas enum.
    @Column(name = "doctor_schedule_template_day_of_week", nullable = false)
    private DayOfWeek doctorScheduleTemplateDayOfWeek; // java.time.DayOfWeek (LUNES=1, ..., DOMINGO=7)
    // El ordinal de DayOfWeek va de 0 (MONDAY) a 6 (SUNDAY)
    // Si necesitas 1-7, tendrás que ajustar al persistir/recuperar o usar Integer.

    // OJO: DayOfWeek.getValue() devuelve 1-7, DayOfWeek.ordinal() devuelve 0-6.

    // Para que coincida con ISO 1-7, si usas ORDINAL, DayOfWeek.MONDAY (valor 1) se guardaría como 0.
    // Es más seguro usar un Integer aquí y mapear manualmente si quieres 1-7 estrictamente.
    // Por simplicidad con EnumType.ORDINAL, aceptaremos 0-6.

    @Column(name = "doctor_schedule_template_start_time", nullable = false, columnDefinition = "TIME")
    private LocalTime doctorScheduleTemplateStartTime; // Para tipo TIME

    @Column(name = "doctor_schedule_template_end_time", nullable = false, columnDefinition = "TIME")
    private LocalTime doctorScheduleTemplateEndTime; // Para tipo TIME
    // (3) Constraint CHECK(end_time > start_time) se define en BD.

    @Column(name = "doctor_schedule_template_is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean doctorScheduleTemplateIsActive = true;

    @CreationTimestamp
    @Column(name = "doctor_schedule_template_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime doctorScheduleTemplateCreatedAt;

    @UpdateTimestamp
    @Column(name = "doctor_schedule_template_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime doctorScheduleTemplateUpdatedAt;
}