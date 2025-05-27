package co.edu.usco.medimicita.service.impl;

import java.util.List;

import co.edu.usco.medimicita.exception.AppointmentCancellationException;
import co.edu.usco.medimicita.util.ClinicConstants;
import co.edu.usco.medimicita.entity.*;
import co.edu.usco.medimicita.enums.AppointmentStatusEnum;
import co.edu.usco.medimicita.exception.ResourceNotFoundException;
import co.edu.usco.medimicita.exception.SlotUnavailableException;
import co.edu.usco.medimicita.repository.AppointmentRepository;
import co.edu.usco.medimicita.repository.SpecialtyRepository;
import co.edu.usco.medimicita.repository.UserRepository;
import co.edu.usco.medimicita.service.AppointmentService;
import co.edu.usco.medimicita.service.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AvailabilityService availabilityService;
    private static final long CANCELLATION_HOURS_LIMIT = 24; // Regla de negocio: 24 horas

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  UserRepository userRepository,
                                  SpecialtyRepository specialtyRepository,
                                  AvailabilityService availabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.specialtyRepository = specialtyRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    @Transactional // Esta operación debe ser transaccional
    public AppointmentEntity scheduleNewAppointment(Long patientUserId, Integer specialtyId, Long doctorId,
                                                    OffsetDateTime startDateTime, Integer durationMinutes) {
        log.info("Intentando agendar nueva cita: PacienteID={}, EspecialidadID={}, DoctorID={}, Inicio={}, Duración={}",
                patientUserId, specialtyId, doctorId, startDateTime, durationMinutes);

        // 1. Doble Verificación de Disponibilidad (CRUCIAL)
        if (!availabilityService.isSlotActuallyAvailable(doctorId, startDateTime, durationMinutes)) {
            log.warn("Slot no disponible al intentar confirmar: DrID={}, Inicio={}", doctorId, startDateTime);
            throw new SlotUnavailableException("El horario seleccionado ya no está disponible. Por favor, elija otro.");
        }

        // 2. Obtener Entidades
        UserEntity patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + patientUserId));
        UserEntity doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado con ID: " + doctorId));
        SpecialtyEntity specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + specialtyId));

        // Validar que el doctor pertenezca a la especialidad (si es una regla de negocio)
        log.info("Datos para validación de especialidad del médico:");
        log.info("Médico ID: {}", doctor.getUserId());
        if (doctor.getDoctorProfile() == null) {
            log.error("ERROR: El DoctorProfile para el médico ID {} ES NULL.", doctor.getUserId());
        } else {
            log.info("DoctorProfile ID: {}", doctor.getDoctorProfile().getDoctorProfileId());
            if (doctor.getDoctorProfile().getSpecialty() == null) {
                log.error("ERROR: La Especialidad DENTRO del DoctorProfile para el médico ID {} ES NULL.", doctor.getUserId());
            } else {
                log.info("DoctorProfile -> Specialty ID: {}", doctor.getDoctorProfile().getSpecialty().getSpecialtyId());
                log.info("DoctorProfile -> Specialty Name: {}", doctor.getDoctorProfile().getSpecialty().getSpecialtyName());
            }
        }
        log.info("Especialidad seleccionada para la cita (specialtyId): {}", specialtyId);
        log.info("Especialidad seleccionada para la cita (specialty.getSpecialtyName()): {}", specialty.getSpecialtyName());


        // Validar si el paciente ya tiene una cita activa para esta especialidad (REQ IV.4.1)
        long activeAppointments = appointmentRepository.countActiveFutureAppointmentsByPatientAndSpecialty(
                patient, specialtyId, OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID) // Necesitas la zona horaria
        );
        if (activeAppointments > 0) {
            log.warn("Paciente {} ya tiene una cita activa para la especialidad {}", patientUserId, specialtyId);
            throw new SlotUnavailableException("Ya tienes una cita futura activa para esta especialidad.");
        }

        // Validar si el paciente ya tiene una cita a la misma hora exacta (REQ IV.4.2)
        long appointmentsAtSameTime = appointmentRepository.countScheduledAppointmentsForPatientAtExactTime(
                patient, startDateTime
        );
        if (appointmentsAtSameTime > 0) {
            log.warn("Paciente {} ya tiene una cita agendada a las {}", patientUserId, startDateTime);
            throw new SlotUnavailableException("Ya tienes otra cita agendada exactamente a esta misma hora.");
        }


        // 3. Crear y Configurar la Nueva Cita
        AppointmentEntity newAppointment = new AppointmentEntity();
        newAppointment.setPatientUser(patient);
        newAppointment.setDoctorUser(doctor);
        newAppointment.setSpecialty(specialty);
        newAppointment.setAppointmentStartDatetime(startDateTime);
        newAppointment.setAppointmentEndDatetime(startDateTime.plusMinutes(durationMinutes));
        newAppointment.setAppointmentStatus(AppointmentStatusEnum.SCHEDULED);
        // createdAt y updatedAt serán manejados por @CreationTimestamp y @UpdateTimestamp

        AppointmentEntity savedAppointment = appointmentRepository.save(newAppointment);
        log.info("Cita agendada exitosamente con ID: {}", savedAppointment.getAppointmentId());

        return savedAppointment;
    }
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentEntity> findAppointmentsForPatient(Long patientUserId) {
        UserEntity patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + patientUserId));
        // Ordenar por fecha, las más recientes primero o como prefieras
        return appointmentRepository.findByPatientUserOrderByAppointmentStartDatetimeDesc(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentEntity> findFutureAppointmentsForPatient(Long patientUserId) {
        UserEntity patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + patientUserId));
        return appointmentRepository.findByPatientUserAndAppointmentStartDatetimeAfterOrderByAppointmentStartDatetimeAsc(
                patient, OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentEntity> findPastAppointmentsForPatient(Long patientUserId) {
        UserEntity patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + patientUserId));
        return appointmentRepository.findByPatientUserAndAppointmentStartDatetimeBeforeOrderByAppointmentStartDatetimeDesc(
                patient, OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID));
    }

    @Override
    @Transactional
    public void cancelPatientAppointment(Long appointmentId, Long patientUserId) {
        log.info("Paciente ID {} intentando cancelar cita ID {}", patientUserId, appointmentId);

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.warn("Intento de cancelar cita no existente. ID: {}", appointmentId);
                    return new ResourceNotFoundException("Cita no encontrada con ID: " + appointmentId);
                });

        // 1. Verificar que la cita pertenezca al paciente
        if (!appointment.getPatientUser().getUserId().equals(patientUserId)) {
            log.warn("Intento de cancelar cita ajena. Paciente ID: {}, Cita ID: {}, Dueño real: {}",
                    patientUserId, appointmentId, appointment.getPatientUser().getUserId());
            throw new AppointmentCancellationException("No tienes permiso para cancelar esta cita.");
        }

        // 2. Verificar que la cita esté en estado 'SCHEDULED'
        if (appointment.getAppointmentStatus() != AppointmentStatusEnum.SCHEDULED) {
            log.warn("Intento de cancelar cita con estado no cancelable: {}. Cita ID: {}",
                    appointment.getAppointmentStatus(), appointmentId);
            throw new AppointmentCancellationException("Esta cita no puede ser cancelada (estado actual: " + appointment.getAppointmentStatus() + ").");
        }

        // 3. Verificar regla de negocio (ej. 24 horas de antelación)
        OffsetDateTime nowInClinicTime = OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID);
        Duration timeUntilAppointment = Duration.between(nowInClinicTime, appointment.getAppointmentStartDatetime());

        if (timeUntilAppointment.toHours() < CANCELLATION_HOURS_LIMIT) {
            log.warn("Intento de cancelar cita fuera del plazo permitido. Cita ID: {}, Horas restantes: {}",
                    appointmentId, timeUntilAppointment.toHours());
            throw new AppointmentCancellationException("No puedes cancelar la cita con menos de " + CANCELLATION_HOURS_LIMIT + " horas de antelación.");
        }

        // Si todas las validaciones pasan, cancelar la cita
        appointment.setAppointmentStatus(AppointmentStatusEnum.CANCELLED_PATIENT);
        appointment.setAppointmentCancelledAt(OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID));

        // Obtener el usuario que cancela (el paciente en este caso)
        UserEntity cancellingUser = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente que cancela no encontrado con ID: " + patientUserId)); // Poco probable si llegó hasta aquí
        appointment.setCancelledByUser(cancellingUser);
        // appointment.setAppointmentCancellationReason("Cancelada por el paciente."); // Opcional

        appointmentRepository.save(appointment);
        log.info("Cita ID {} cancelada exitosamente por el Paciente ID {}", appointmentId, patientUserId);

        // Opcional: Enviar notificación al médico
        // emailService.sendAppointmentCancellationNotificationToDoctor(appointment.getDoctorUser(), appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppointmentEntity> findAppointmentById(Long appointmentId) {
        log.debug("Buscando cita por ID: {}", appointmentId);
        return appointmentRepository.findById(appointmentId);
    }


}