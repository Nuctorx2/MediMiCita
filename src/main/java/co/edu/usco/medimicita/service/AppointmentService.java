package co.edu.usco.medimicita.service;

import java.util.Optional;
import co.edu.usco.medimicita.entity.AppointmentEntity;
import co.edu.usco.medimicita.exception.AppointmentCancellationException;
import co.edu.usco.medimicita.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentService {

    AppointmentEntity scheduleNewAppointment(Long patientUserId,
                                             Integer specialtyId,
                                             Long doctorId,
                                             OffsetDateTime startDateTime,
                                             Integer durationMinutes);

    /**
     * Encuentra todas las citas para un paciente específico.
     * @param patientUserId El ID del paciente.
     * @return Una lista de citas del paciente.
     */
    List<AppointmentEntity> findAppointmentsForPatient(Long patientUserId);

    /**
     * Encuentra todas las citas futuras para un paciente específico.
     * @param patientUserId El ID del paciente.
     * @return Una lista de citas futuras del paciente.
     */
    List<AppointmentEntity> findFutureAppointmentsForPatient(Long patientUserId);

    /**
     * Encuentra todas las citas pasadas para un paciente específico.
     * @param patientUserId El ID del paciente.
     * @return Una lista de citas pasadas del paciente.
     */
    List<AppointmentEntity> findPastAppointmentsForPatient(Long patientUserId);

    /**
     * Permite a un paciente cancelar una de sus citas.
     * @param appointmentId El ID de la cita a cancelar.
     * @param patientUserId El ID del paciente que solicita la cancelación.
     * @throws ResourceNotFoundException si la cita o el paciente no se encuentran.
     * @throws AppointmentCancellationException si la cita no puede ser cancelada (ej. no pertenece al paciente, estado incorrecto, fuera de plazo).
     */
    void cancelPatientAppointment(Long appointmentId, Long patientUserId);

    Optional<AppointmentEntity> findAppointmentById(Long appointmentId); // Útil para validaciones`
}