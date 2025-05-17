package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.DoctorProfileEntity;
import co.edu.usco.medimicita.entity.SpecialtyEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfileEntity, Long> {

    Optional<DoctorProfileEntity> findByUser(UserEntity user);

    List<DoctorProfileEntity> findBySpecialty(SpecialtyEntity specialty);
}