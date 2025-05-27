package co.edu.usco.medimicita.service;

import co.edu.usco.medimicita.entity.EpsEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EpsService {
    List<EpsEntity> getAllActiveEps();

    // Implementa otros métodos de EpsService si los tienes definidos
    @Transactional(readOnly = true)
    Optional<EpsEntity> findById(Integer id);
}
