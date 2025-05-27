package co.edu.usco.medimicita.service;

import co.edu.usco.medimicita.dto.AvailableSlotDto;
// No necesitamos UserEntity aquí directamente, usaremos IDs.


import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime; // Para isSlotAvailable que trabaja con fecha y hora completas
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AvailabilityService {


    /**
     * Obtiene una lista de fechas dentro de un mes específico que tienen al menos un slot
     * disponible para una especialidad dada y, opcionalmente, para un médico específico.
     *
     * @param specialtyId El ID de la especialidad.
     * @param doctorId    Optional con el ID del médico. Si está vacío, se consideran todos los médicos
     *                    de esa especialidad.
     * @param month       El mes (YearMonth) para el cual se busca disponibilidad.
     * @return Una lista de objetos LocalDate que representan los días con disponibilidad.
     *         La lista estará vacía si no hay días disponibles.
     */
    List<LocalDate> getAvailableDates(Integer specialtyId, Optional<Long> doctorId, YearMonth month);

    /**
     * Obtiene una lista de slots de tiempo (DTOs) disponibles para una fecha específica,
     * una especialidad y, opcionalmente, un médico específico.
     *
     * @param specialtyId El ID de la especialidad (necesario para determinar la duración de la cita).
     * @param doctorId    Optional con el ID del médico. Si está vacío, se buscan slots disponibles
     *                    con cualquier médico de esa especialidad, y el DTO resultante debe
     *                    incluir la información del médico para cada slot.
     * @param date        La fecha específica para la cual se buscan los slots.
     * @return Una lista de {@link AvailableSlotDto} que representan los horarios de inicio disponibles.
     *         La lista estará vacía si no hay slots disponibles.
     */
    List<AvailableSlotDto> getAvailableTimeSlots(Integer specialtyId, Optional<Long> doctorId, LocalDate date);

    /**
     * Verifica si un slot de cita específico (identificado por médico y fecha/hora de inicio)
     * está realmente disponible en el momento de la consulta. Este método es crucial para
     * realizar una doble verificación justo antes de confirmar una cita, para evitar
     * problemas de concurrencia donde dos usuarios podrían intentar reservar el mismo slot
     * simultáneamente.
     *
     * @param doctorId      El ID del médico para el cual se verifica el slot.
     * @param startDateTime La fecha y hora exactas de inicio del slot a verificar.
     * @param durationInMinutes La duración de la cita en minutos (generalmente obtenida de la especialidad).
     * @return {@code true} si el slot está disponible, {@code false} en caso contrario.
     */
    boolean isSlotActuallyAvailable(Long doctorId, OffsetDateTime startDateTime, int durationInMinutes);

}