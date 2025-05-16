package co.edu.usco.medimicita.entity;

import jakarta.persistence.*; // Para JPA (Jakarta Persistence API)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Específico de Hibernate para timestamps automáticos
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.List;


@Entity
@Table(name = "specialty", indexes = {@Index(name = "idx_specialty_name", columnList = "specialty_name", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor

public class SpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "specialty_id")
    private Long specialtyId;

    @Column(name = "specialty_name", unique = true,nullable = false, length = 100)
    private String specialtyName; // Ej: 'Medicina General', 'Odontología', 'Ginecología'

    @Column(name = "specialty_default_duration_minutes", nullable = false)
    private Integer specialtyDefaultDurationMinutes;

    @Column(name = "specialty_is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean specialtyIsActive = true;

    @CreationTimestamp
    @Column(name = "specialty_created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime specialtyCreatedAt;

    @UpdateTimestamp
    @Column(name = "specialty_updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime specialtyUpdatedAt;

    // --- Relaciones Inversas (mappedBy) ---
    // (1) Lado inverso de la relación con DoctorProfileEntity
    @OneToMany(mappedBy = "specialty", fetch = FetchType.LAZY)
    private List<DoctorProfileEntity> doctorProfiles;

    // (2) Lado inverso de la relación con AppointmentEntity
    @OneToMany(mappedBy = "specialty", fetch = FetchType.LAZY)
    private List<AppointmentEntity> appointments;
}
