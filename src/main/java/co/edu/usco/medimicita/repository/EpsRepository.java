package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.EpsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpsRepository extends JpaRepository<EpsEntity, Integer> {

    // Encuentra una EPS por su nombre
    Optional<EpsEntity> findByEpsName(String epsName);

    // Encuentra todas las EPS activas
    List<EpsEntity> findByEpsIsActiveTrue();
}