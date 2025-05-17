package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {

    // Encuentra un rol por su nombre (ej. "PACIENTE", "MEDICO")
    Optional<RoleEntity> findByRoleName(String roleName);
}