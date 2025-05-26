package co.edu.usco.medimicita.service.impl;

import co.edu.usco.medimicita.dto.SpecialtyDto;
import co.edu.usco.medimicita.entity.SpecialtyEntity;
import co.edu.usco.medimicita.exception.ResourceNotFoundException; // Asume que tienes esta excepción
import co.edu.usco.medimicita.exception.SpecialtyAlreadyExistsException; // Asume que tienes esta excepción
import co.edu.usco.medimicita.repository.SpecialtyRepository;
import co.edu.usco.medimicita.service.SpecialtyService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SpecialtyServiceImpl implements SpecialtyService {

    private static final Logger log = LoggerFactory.getLogger(SpecialtyServiceImpl.class);

    private final SpecialtyRepository specialtyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyEntity> getAllActiveSpecialties() {
        log.debug("Solicitando todas las especialidades activas");
        return specialtyRepository.findBySpecialtyIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpecialtyEntity> findById(Integer id) {
        log.debug("Buscando especialidad con ID: {}", id);
        return specialtyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpecialtyEntity> findByName(String name) {
        log.debug("Buscando especialidad con nombre: {}", name);
        return specialtyRepository.findBySpecialtyName(name);
    }

    @Override
    @Transactional
    public SpecialtyEntity create(SpecialtyDto specialtyDto) {
        log.info("Intentando crear especialidad: {}", specialtyDto.getSpecialtyName());
        // Validar si ya existe una especialidad con el mismo nombre
        specialtyRepository.findBySpecialtyName(specialtyDto.getSpecialtyName().trim())
                .ifPresent(existingSpecialty -> {
                    log.warn("Intento de crear especialidad con nombre duplicado: {}", specialtyDto.getSpecialtyName());
                    throw new SpecialtyAlreadyExistsException("Ya existe una especialidad con el nombre: " + specialtyDto.getSpecialtyName());
                });

        SpecialtyEntity newSpecialty = new SpecialtyEntity();
        mapDtoToEntity(specialtyDto, newSpecialty);
        // Los timestamps (createdAt, updatedAt) y el ID se manejan automáticamente

        SpecialtyEntity savedSpecialty = specialtyRepository.save(newSpecialty);
        log.info("Especialidad creada con ID: {}", savedSpecialty.getSpecialtyId());
        return savedSpecialty;
    }

    @Override
    @Transactional
    public SpecialtyEntity update(Integer id, SpecialtyDto specialtyDto) {
        log.info("Intentando actualizar especialidad con ID: {}", id);
        SpecialtyEntity existingSpecialty = specialtyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Especialidad no encontrada para actualizar con ID: {}", id);
                    return new ResourceNotFoundException("Especialidad no encontrada con ID: " + id);
                });

        // Validar si el nuevo nombre ya está en uso por OTRA especialidad
        if (specialtyDto.getSpecialtyName() != null &&
                !existingSpecialty.getSpecialtyName().equalsIgnoreCase(specialtyDto.getSpecialtyName().trim())) {
            specialtyRepository.findBySpecialtyName(specialtyDto.getSpecialtyName().trim())
                    .ifPresent(s -> {
                        if (!s.getSpecialtyId().equals(id)) { // Si es otra especialidad diferente a la que estamos actualizando
                            log.warn("Intento de actualizar especialidad a un nombre duplicado: {}", specialtyDto.getSpecialtyName());
                            throw new SpecialtyAlreadyExistsException("Ya existe otra especialidad con el nombre: " + specialtyDto.getSpecialtyName());
                        }
                    });
        }

        mapDtoToEntity(specialtyDto, existingSpecialty);
        // El timestamp updatedAt se actualizará automáticamente por @UpdateTimestamp

        SpecialtyEntity updatedSpecialty = specialtyRepository.save(existingSpecialty);
        log.info("Especialidad actualizada con ID: {}", updatedSpecialty.getSpecialtyId());
        return updatedSpecialty;
    }

    @Override
    @Transactional
    public void deactivateById(Integer id) {
        log.info("Intentando desactivar especialidad con ID: {}", id);
        SpecialtyEntity specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Especialidad no encontrada para desactivar con ID: {}", id);
                    return new ResourceNotFoundException("Especialidad no encontrada con ID: " + id);
                });
        specialty.setSpecialtyIsActive(false);
        specialtyRepository.save(specialty);
        log.info("Especialidad desactivada con ID: {}", id);
    }

    @Override
    @Transactional
    public void activateById(Integer id) {
        log.info("Intentando activar especialidad con ID: {}", id);
        SpecialtyEntity specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Especialidad no encontrada para activar con ID: {}", id);
                    return new ResourceNotFoundException("Especialidad no encontrada con ID: " + id);
                });
        specialty.setSpecialtyIsActive(true);
        specialtyRepository.save(specialty);
        log.info("Especialidad activada con ID: {}", id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyEntity> findAll() {
        log.debug("Solicitando todas las especialidades (activas e inactivas)");
        return specialtyRepository.findAll(); // Podrías querer ordenarlas, ej. por nombre
    }

    // --- Método Helper para Mapeo DTO -> Entidad ---
    private void mapDtoToEntity(SpecialtyDto dto, SpecialtyEntity entity) {
        if (dto.getSpecialtyName() != null) {
            entity.setSpecialtyName(dto.getSpecialtyName().trim());
        }
        if (dto.getSpecialtyDefaultDurationMinutes() != null) {
            entity.setSpecialtyDefaultDurationMinutes(dto.getSpecialtyDefaultDurationMinutes());
        }
        if (dto.getSpecialtyIsActive() != null) {
            entity.setSpecialtyIsActive(dto.getSpecialtyIsActive());
        }
        // El ID solo se usa para buscar la entidad en 'update', no se setea desde el DTO aquí.
        // Los timestamps son manejados por Hibernate.
    }
}