package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.RoleEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUserEmail(String userEmail);

    Optional<UserEntity> findByUserIdentificationNumber(String userIdentificationNumber);

    Boolean existsByUserEmail(String userEmail);

    Boolean existsByUserIdentificationNumber(String userIdentificationNumber);

    List<UserEntity> findByRole(RoleEntity role); // Encuentra usuarios por rol

    // Ejemplo: Encontrar médicos activos de una especialidad específica
    // (Esta query asume que user.doctorProfile.specialty está bien mapeado)
    @Query("SELECT u FROM UserEntity u JOIN u.doctorProfile dp WHERE u.role.roleName = 'MEDICO' AND u.userIsActive = true AND dp.specialty.specialtyId = :specialtyId")
    List<UserEntity> findActiveDoctorsBySpecialtyId(@Param("specialtyId") Integer specialtyId);

    // Encuentra usuarios que requieren cambio de contraseña
    List<UserEntity> findByUserRequiresPasswordChangeTrue();
}