package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.SpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<SpecialtyEntity, Integer> {

    Optional<SpecialtyEntity> findBySpecialtyName(String specialtyName);

    List<SpecialtyEntity> findBySpecialtyIsActiveTrue();
}