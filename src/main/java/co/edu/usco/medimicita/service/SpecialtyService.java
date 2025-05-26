package co.edu.usco.medimicita.service;

import co.edu.usco.medimicita.dto.SpecialtyDto;
import co.edu.usco.medimicita.entity.SpecialtyEntity;
import co.edu.usco.medimicita.exception.ResourceNotFoundException; // Si la usas
import co.edu.usco.medimicita.exception.SpecialtyAlreadyExistsException; // Si la usas

import java.util.List;
import java.util.Optional;

public interface SpecialtyService {

    List<SpecialtyEntity> getAllActiveSpecialties();
    Optional<SpecialtyEntity> findById(Integer id);
    Optional<SpecialtyEntity> findByName(String name); // Útil para validaciones de unicidad

    /**
     * Crea una nueva especialidad.
     * @param specialtyDto DTO con los datos de la nueva especialidad.
     * @return la SpecialtyEntity creada.
     * @throws SpecialtyAlreadyExistsException si ya existe una especialidad con el mismo nombre.
     */
    SpecialtyEntity create(SpecialtyDto specialtyDto);

    /**
     * Actualiza una especialidad existente.
     * @param id el ID de la especialidad a actualizar.
     * @param specialtyDto DTO con los nuevos datos para la especialidad.
     * @return la SpecialtyEntity actualizada.
     * @throws ResourceNotFoundException si la especialidad con el ID dado no se encuentra.
     * @throws SpecialtyAlreadyExistsException si el nuevo nombre ya está en uso por otra especialidad.
     */
    SpecialtyEntity update(Integer id, SpecialtyDto specialtyDto);

    /**
     * Desactiva una especialidad por su ID (borrado lógico).
     * @param id el ID de la especialidad a desactivar.
     * @throws ResourceNotFoundException si la especialidad no se encuentra.
     */
    void deactivateById(Integer id);

    /**
     * Activa una especialidad previamente desactivada por su ID.
     * @param id el ID de la especialidad a activar.
     * @throws ResourceNotFoundException si la especialidad no se encuentra.
     */
    void activateById(Integer id);


    List<SpecialtyEntity> findAll(); // Para la gestión del admin (activas e inactivas)
}