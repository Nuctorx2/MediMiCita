package co.edu.usco.medimicita.entity;

import co.edu.usco.medimicita.enums.AppointmentStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

// (1) Constraints a considerar a nivel de BD (además de las FKs):
// - CHECK(appointment_end_datetime > appointment_start_datetime)
// - Constraints de Exclusión (GiST en PostgreSQL) para prevenir solapamientos:
//   - Un médico no puede tener dos citas 'SCHEDULED' solapadas.
//     (doctor_user_id, [appointment_start_datetime, appointment_end_datetime]) WHERE status = 'SCHEDULED'
//   - Un paciente no puede tener dos citas 'SCHEDULED' solapadas.
//     (patient_user_id, [appointment_start_datetime, appointment_end_datetime]) WHERE status = 'SCHEDULED'
//   Estas son complejas de definir solo con JPA y usualmente se hacen en BD o se validan rigurosamente en el servicio.
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_patient_user_id", columnList = "patient_user_id"),
        @Index(name = "idx_appointment_doctor_user_id", columnList = "doctor_user_id"),
        @Index(name = "idx_appointment_specialty_id", columnList = "specialty_id"),
        @Index(name = "idx_appointment_start_datetime", columnList = "appointment_start_datetime"),
        @Index(name = "idx_appointment_status", columnList = "appointment_status"),
        @Index(name = "idx_appointment_cancelled_by_user_id", columnList = "cancelled_by_user_id"),
        // Índices para las constraints de exclusión (ayudan pero no las imponen por sí solos)
        @Index(name = "idx_appointment_doctor_slot", columnList = "doctor_user_id, appointment_start_datetime"),
        @Index(name = "idx_appointment_patient_slot", columnList = "patient_user_id, appointment_start_datetime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O GenerationType.UUID si cambias
    @Column(name = "appointment_id")
    private Long appointmentId;

    // --- Relación con UserEntity (Paciente) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointment_patient_user"))
    // ON DELETE RESTRICT se maneja a nivel de BD. JPA por defecto no restringe,
    // pero si la BD lo tiene, se respetará.
    private UserEntity patientUser;

    // --- Relación con UserEntity (Médico) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointment_doctor_user"))
    private UserEntity doctorUser;

    // --- Relación con SpecialtyEntity ---
    @ManyToOne(fetch = FetchType.LAZY) // EAGER podría considerarse si siempre se muestra la especialidad de la cita.
    @JoinColumn(name = "specialty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointment_specialty"))
    private SpecialtyEntity specialty;

    @Column(name = "appointment_start_datetime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime appointmentStartDatetime;

    @Column(name = "appointment_end_datetime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime appointmentEndDatetime; // Calculado al crear (start + specialty duration)

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status", nullable = false, length = 20)
    private AppointmentStatusEnum appointmentStatus; // (2) Enum a crear

    @Column(name = "appointment_cancellation_reason", columnDefinition = "TEXT") // Nullable por defecto
    private String appointmentCancellationReason;

    @Column(name = "appointment_cancelled_at", columnDefinition = "TIMESTAMP WITH TIME ZONE") // Nullable por defecto
    private OffsetDateTime appointmentCancelledAt;

    // --- Relación con UserEntity (Quién canceló) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id", nullable = true, // Nullable, ya que no todas las citas se cancelan.
            foreignKey = @ForeignKey(name = "fk_appointment_cancelled_by_user"))
    private UserEntity cancelledByUser;

    @CreationTimestamp
    @Column(name = "appointment_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime appointmentCreatedAt;

    @UpdateTimestamp
    @Column(name = "appointment_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime appointmentUpdatedAt;

    // --- Relaciones Inversas (mappedBy) ---
    // (3) Lado inverso de la relación con ClinicalRecordEntity
    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    // cascade y orphanRemoval aquí significa que si se elimina una cita, sus registros clínicos asociados también.
    // Considera si este es el comportamiento deseado. Podría ser mejor que los ClinicalRecord queden
    // y solo se desvincule la cita (appointment_id = NULL en clinical_records).
    // Si es así, elimina cascade y orphanRemoval de aquí.
    private List<ClinicalRecordEntity> clinicalRecords;
}