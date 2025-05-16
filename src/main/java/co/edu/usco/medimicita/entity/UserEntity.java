package co.edu.usco.medimicita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List; // Para las relaciones @OneToMany

@Entity
@Table(name = "users", indexes = { // (1) Definición de índices a nivel de tabla
        @Index(name = "idx_user_identification_number", columnList = "user_identification_number", unique = true),
        @Index(name = "idx_user_email", columnList = "user_email", unique = true),
        @Index(name = "idx_user_phone_number", columnList = "user_phone_number", unique = true),
        @Index(name = "idx_user_role_id", columnList = "role_id"),
        @Index(name = "idx_user_eps_id", columnList = "eps_id"),
        @Index(name = "idx_user_is_active", columnList = "user_is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O GenerationType.UUID si cambias a UUID
    @Column(name = "user_id")
    private Long userId; // Usamos Long para user_id, podría ser BIGSERIAL en BD o si planeas muchos usuarios.

    @Column(name = "user_identification_number", nullable = false, length = 15) // El índice unique ya está definido arriba
    private String userIdentificationNumber;

    @Column(name = "user_first_name", nullable = false, length = 50)
    private String userFirstName;

    @Column(name = "user_last_name", nullable = false, length = 50)
    private String userLastName;

    @Column(name = "user_birth_date", nullable = false)
    private LocalDate userBirthDate; // Para tipo DATE

    @Column(name = "user_phone_number", nullable = false, length = 15) // El índice unique ya está definido arriba
    private String userPhoneNumber;

    @Column(name = "user_email", nullable = false, length = 255) // El índice unique ya está definido arriba
    private String userEmail;

    @Column(name = "user_password_hash", nullable = false, length = 255)
    private String userPasswordHash; // Almacena el hash de la contraseña

    // --- Relaciones ---
//    FetchType.EAGER significa que cada vez que se carga un UserEntity, su RoleEntity asociado también se cargará inmediatamente. Esto es a menudo deseable para el rol, ya que se utiliza con frecuencia para la autorización. Ten cuidado con EAGER en relaciones que podrían llevar a cargar demasiados datos (problema N+1).
    @ManyToOne(fetch = FetchType.EAGER) // (2) EAGER es común para el rol, ya que a menudo se necesita.
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_role")) // (3) Nombre explícito para la FK
    private RoleEntity role; // Mapea al campo roleId de RoleEntity

    @ManyToOne(fetch = FetchType.LAZY) // LAZY para EPS, puede que no siempre se necesite
    @JoinColumn(name = "eps_id", nullable = true, // Nullable para usuarios que no son pacientes (Médicos, Admins)
            foreignKey = @ForeignKey(name = "fk_user_eps"))
    private EpsEntity eps; // Mapea al campo epsId de EpsEntity

    // --- Campos de Auditoría y Estado ---
    @Column(name = "user_terms_accepted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE") // Nullable por defecto
    private OffsetDateTime userTermsAcceptedAt; // Para pacientes

    @Column(name = "user_is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean userIsActive = true;

    @Column(name = "user_requires_password_change", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean userRequiresPasswordChange = false; // Para el primer login del médico

    @Column(name = "user_last_login_at", columnDefinition = "TIMESTAMP WITH TIME ZONE") // Nullable por defecto
    private OffsetDateTime userLastLoginAt;

    @CreationTimestamp
    @Column(name = "user_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userCreatedAt;

    @UpdateTimestamp
    @Column(name = "user_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userUpdatedAt;

    // --- Relaciones Inversas (mappedBy) ---
    // (4) Lado inverso de la relación con UserAddressEntity
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserAddressEntity> userAddresses;

    // (5) Lado inverso de la relación con DoctorProfileEntity
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DoctorProfileEntity doctorProfile;

    // (6) Lado inverso de la relación con DoctorScheduleTemplateEntity
    @OneToMany(mappedBy = "doctorUser", fetch = FetchType.LAZY)
    private List<DoctorScheduleTemplateEntity> doctorScheduleTemplates;

    // (7) Lado inverso de la relación con ScheduleExceptionEntity (excepciones creadas por este doctor)
//    @OneToMany(mappedBy = "doctorUser", fetch = FetchType.LAZY)
//    private List<ScheduleExceptionEntity> doctorScheduleExceptions;

    // (8) Lado inverso de la relación con ScheduleExceptionEntity (excepciones creadas por este admin)
    // Nota: Si un usuario puede ser tanto doctor como admin que crea excepciones, necesitarías dos listas
    // o una lógica más compleja. Dado el esquema, un usuario tiene un solo rol.
    // Esta es para el 'created_by_user_id' en schedule_exceptions.
//    @OneToMany(mappedBy = "createdByUser", fetch = FetchType.LAZY)
//    private List<ScheduleExceptionEntity> adminCreatedScheduleExceptions;

    // (9) Citas donde este usuario es el PACIENTE
//    @OneToMany(mappedBy = "patientUser", fetch = FetchType.LAZY)
//    private List<AppointmentEntity> patientAppointments;

    // (10) Citas donde este usuario es el MÉDICO
//    @OneToMany(mappedBy = "doctorUser", fetch = FetchType.LAZY)
//    private List<AppointmentEntity> doctorAppointments;

    // (11) Historiales clínicos donde este usuario es el PACIENTE
//    @OneToMany(mappedBy = "patientUser", fetch = FetchType.LAZY)
//    private List<ClinicalRecordEntity> patientClinicalRecords;

    // (12) Historiales clínicos donde este usuario es el MÉDICO que escribió la nota
//    @OneToMany(mappedBy = "doctorUser", fetch = FetchType.LAZY)
//    private List<ClinicalRecordEntity> doctorAuthoredClinicalRecords;

    // (13) Tokens de reseteo de contraseña para este usuario
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    private List<PasswordResetTokenEntity> passwordResetTokens;

    // (14) Citas canceladas por este usuario
//    @OneToMany(mappedBy = "cancelledByUser", fetch = FetchType.LAZY)
//    private List<AppointmentEntity> cancelledAppointments;

}