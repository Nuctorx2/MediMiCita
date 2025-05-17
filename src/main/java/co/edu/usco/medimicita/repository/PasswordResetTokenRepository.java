package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.PasswordResetTokenEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByPasswordResetTokenTokenHash(String tokenHash);

    Optional<PasswordResetTokenEntity> findByUserAndPasswordResetTokenExpiresAtAfterAndPasswordResetTokenUsedAtIsNull(
            UserEntity user, OffsetDateTime currentTime);

    void deleteAllByPasswordResetTokenExpiresAtBefore(OffsetDateTime expiryTime); // Para limpiar tokens expirados
}