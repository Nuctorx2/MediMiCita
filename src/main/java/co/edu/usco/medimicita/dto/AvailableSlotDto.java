package co.edu.usco.medimicita.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder; // Opcional, para un patrón de construcción más legible

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Añade el patrón Builder, que es conveniente para crear instancias
public class AvailableSlotDto {

    /**
     * Hora de inicio del slot disponible.
     */
    private LocalTime startTime;

    /**
     * Hora de finalización del slot disponible (calculada).
     * Puede ser útil para mostrar al usuario o para validaciones.
     */
    private LocalTime endTime;

    /**
     * ID del médico asignado a este slot.
     * Especialmente útil si la búsqueda inicial de slots no especificó un médico,
     * permitiendo al sistema asignar o mostrar el médico disponible.
     */
    private Long doctorId;

    /**
     * Nombre del médico asignado a este slot, para mostrar en la interfaz de usuario.
     */
    private String doctorName;

    /**
     * (Opcional) ID de la especialidad, si es relevante mantenerlo en el DTO del slot.
     * Podría ser útil si un médico atiende múltiples especialidades con diferentes duraciones.
     */
    // private Integer specialtyId;

    /**
     * Duración del slot en minutos.
     * Podría ser útil si la duración puede variar incluso dentro de una misma especialidad o médico.
     */
     private Integer durationMinutes;
}