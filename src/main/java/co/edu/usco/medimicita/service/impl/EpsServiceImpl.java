package co.edu.usco.medimicita.service.impl;

import co.edu.usco.medimicita.entity.EpsEntity;
import co.edu.usco.medimicita.repository.EpsRepository;
import co.edu.usco.medimicita.service.EpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service // <--- ANOTACIÓN CLAVE
public class EpsServiceImpl implements EpsService {

    private final EpsRepository epsRepository;

    @Autowired
    public EpsServiceImpl(EpsRepository epsRepository) {
        this.epsRepository = epsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EpsEntity> getAllActiveEps() {
        // Implementa la lógica para obtener EPS activas, por ejemplo:
        return epsRepository.findByEpsIsActiveTrue();
        // Asegúrate que findByEpsIsActiveTrue() exista en EpsRepository
    }

    // Implementa otros métodos de EpsService si los tienes definidos
    @Transactional(readOnly = true)
    @Override
    public Optional<EpsEntity> findById(Integer id) {
        return epsRepository.findById(id);
    }

    // ... más métodos ...
}