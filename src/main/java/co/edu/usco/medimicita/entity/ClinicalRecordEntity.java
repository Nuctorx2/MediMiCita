package co.edu.usco.medimicita.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clinical_records", indexes =  {@Index(name = "idx_cr_appointment_id", columnList = "appointment_id"),
        @Index(name = "idx_cr_patient_user_id", columnList = "patient_user_id"),
        @Index(name = "idx_cr_doctor_user_id", columnList = "doctor_user_id"), // Quién escribió la nota
        @Index(name = "idx_cr_record_datetime", columnList = "clinical_record_record_datetime")})
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ClinicalRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clinical_record_id")
    private Long clinicalRecordId;

    // --- Relación con AppointmentEntity (Opcional) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = true, // (1) Nullable: Un registro clínico podría no estar atado a una cita formal del sistema.
            foreignKey = @ForeignKey(name = "fk_cr_appointment"))
    private AppointmentEntity appointment;

    // --- Relación con UserEntity (Paciente) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cr_patient_user"))
    private UserEntity patientUser;

    // --- Relación con UserEntity (Médico que escribe la nota) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cr_doctor_user"))
    private UserEntity doctorUser; // El médico que crea/escribe este registro clínico

    @Column(name = "clinical_record_record_datetime", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP") // (2) Fecha/Hora de la nota/registro
    private OffsetDateTime clinicalRecordRecordDatetime;

    @Column(name = "clinical_record_notes", nullable = false, columnDefinition = "TEXT")
    private String clinicalRecordNotes;

    @CreationTimestamp
    @Column(name = "clinical_record_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime clinicalRecordCreatedAt; // Cuándo se creó la entrada en la BD

    @UpdateTimestamp
    @Column(name = "clinical_record_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime clinicalRecordUpdatedAt; // Cuándo se actualizó la entrada en la BD

}
