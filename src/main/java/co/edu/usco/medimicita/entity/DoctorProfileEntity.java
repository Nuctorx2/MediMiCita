package co.edu.usco.medimicita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "doctor_profiles", indexes = {
        // (1) El índice UNIQUE para user_id se crea automáticamente por la constraint UNIQUE.
        // Pero podemos definir un índice explícito para la FK specialty_id para mejor rendimiento en búsquedas.
        @Index(name = "idx_doctor_profile_user_id", columnList = "user_id", unique = true), // Asegura One-to-One
        @Index(name = "idx_doctor_profile_specialty_id", columnList = "specialty_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_profile_id")
    private Long doctorProfileId;

    // --- Relación One-to-One con UserEntity ---
    // (2) Esta es la entidad propietaria de la relación One-to-One si `mappedBy` está en UserEntity.
    // Sin embargo, es más común que la tabla con la FK sea la propietaria.
    // Aquí, `doctor_profiles` tiene `user_id` como FK, por lo que es la propietaria.
    @OneToOne(fetch = FetchType.LAZY) // LAZY es bueno, podrías no necesitar el User siempre.
    @JoinColumn(name = "user_id", nullable = false, unique = true, // (3) unique = true asegura la relación One-to-One a nivel de BD.
            foreignKey = @ForeignKey(name = "fk_doctor_profile_user"))
    private UserEntity user;

    // --- Relación con SpecialtyEntity ---
    @ManyToOne(fetch = FetchType.EAGER) // (4) EAGER puede ser apropiado si siempre necesitas la especialidad al cargar el perfil.
    @JoinColumn(name = "specialty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_doctor_profile_specialty"))
    private SpecialtyEntity specialty;

    @Column(name = "doctor_profile_office_number", nullable = false, length = 20)
    private String doctorProfileOfficeNumber;

    @CreationTimestamp
    @Column(name = "doctor_profile_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime doctorProfileCreatedAt;

    @UpdateTimestamp
    @Column(name = "doctor_profile_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime doctorProfileUpdatedAt;
}