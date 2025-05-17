package co.edu.usco.medimicita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_user_id", columnList = "user_id"),
        @Index(name = "idx_prt_token_hash", columnList = "password_reset_token_token_hash", unique = true), // (1) El token hasheado debe ser único
        @Index(name = "idx_prt_expires_at", columnList = "password_reset_token_expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_reset_token_id")
    private Long passwordResetTokenId;

    // --- Relación con UserEntity ---
    @ManyToOne(fetch = FetchType.LAZY) // EAGER podría considerarse si al cargar un token siempre necesitas el usuario,
    // pero LAZY es más seguro para evitar cargas innecesarias.
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_prt_user"))
    private UserEntity user;

    @Column(name = "password_reset_token_token_hash", unique = true, nullable = false, length = 255) // (2) Almacena el HASH del token
    private String passwordResetTokenTokenHash; // El token real se envía al usuario, aquí se guarda su hash.

    @Column(name = "password_reset_token_expires_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime passwordResetTokenExpiresAt; // Fecha y hora de expiración del token

    @Column(name = "password_reset_token_used_at", columnDefinition = "TIMESTAMP WITH TIME ZONE") // (3) Nullable, se actualiza cuando el token es usado.
    private OffsetDateTime passwordResetTokenUsedAt; // Fecha y hora en que se usó el token

    @CreationTimestamp
    @Column(name = "password_reset_token_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime passwordResetTokenCreatedAt;

    // (4) Constructor conveniente para crear un token nuevo
    public PasswordResetTokenEntity(UserEntity user, String tokenHash, OffsetDateTime expiresAt) {
        this.user = user;
        this.passwordResetTokenTokenHash = tokenHash;
        this.passwordResetTokenExpiresAt = expiresAt;
    }
}