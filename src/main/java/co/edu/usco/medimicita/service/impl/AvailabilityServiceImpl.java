package co.edu.usco.medimicita.service.impl;

import co.edu.usco.medimicita.util.ClinicConstants;
import co.edu.usco.medimicita.dto.AvailableSlotDto;
import co.edu.usco.medimicita.entity.*;
import co.edu.usco.medimicita.enums.AppointmentStatusEnum;
import co.edu.usco.medimicita.exception.ResourceNotFoundException; // Asegúrate de tener esta clase
import co.edu.usco.medimicita.repository.*;
import co.edu.usco.medimicita.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static co.edu.usco.medimicita.util.ClinicConstants.CLINIC_ZONE_ID;

@RequiredArgsConstructor
@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    // Se define la zona horaria de la clínica
//    private static final ZoneId CLINIC_ZONE_ID = ZoneId.of("America/Bogota");

    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorScheduleTemplateRepository scheduleTemplateRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Integer specialtyId, Optional<Long> doctorIdOpt, YearMonth month) {
        log.debug("Buscando fechas disponibles para especialidad ID: {}, médico ID: {}, mes: {}",
                specialtyId, doctorIdOpt.map(String::valueOf).orElse("TODOS"), month);

        SpecialtyEntity specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + specialtyId));

        List<UserEntity> doctorsToConsider = getDoctorsToConsider(specialtyId, doctorIdOpt);
        if (doctorsToConsider.isEmpty()) {
            log.warn("No se encontraron médicos para la especialidad ID: {} y filtro de médico: {}", specialtyId, doctorIdOpt);
            return Collections.emptyList();
        }

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        LocalDate todayInClinicZone = LocalDate.now(CLINIC_ZONE_ID);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.isBefore(todayInClinicZone)) { // No mostrar días pasados
                continue;
            }
            // Aquí iría la lógica para verificar si es festivo (isHoliday(date))

            for (UserEntity doctor : doctorsToConsider) {
                // Optimizacion: Si un doctor tiene slots, el día es disponible.
                if (!generateSlotsForDoctorOnDate(doctor, date, specialty, true).isEmpty()) {
                    availableDates.add(date);
                    break;
                }
            }
        }
        log.info("Fechas disponibles encontradas para {}/{}/{}: {}", month, specialtyId, doctorIdOpt.map(String::valueOf).orElse("TODOS"), availableDates.size());
        return availableDates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableTimeSlots(Integer specialtyId, Optional<Long> doctorIdOpt, LocalDate date) {
        log.debug("Buscando slots de tiempo para especialidad ID: {}, médico ID: {}, fecha: {}",
                specialtyId, doctorIdOpt.map(String::valueOf).orElse("TODOS"), date);

        SpecialtyEntity specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + specialtyId));

        List<UserEntity> doctorsToConsider = getDoctorsToConsider(specialtyId, doctorIdOpt);
        if (doctorsToConsider.isEmpty()) {
            log.warn("No se encontraron médicos para la especialidad ID: {} y filtro de médico: {}", specialtyId, doctorIdOpt);
            return Collections.emptyList();
        }

        LocalDate todayInClinicZone = LocalDate.now(CLINIC_ZONE_ID);
        if (date.isBefore(todayInClinicZone)) {
            log.info("Intento de obtener slots para una fecha pasada: {}", date);
            return Collections.emptyList();
        }


        List<AvailableSlotDto> allAvailableSlots = new ArrayList<>();
        for (UserEntity doctor : doctorsToConsider) {
            allAvailableSlots.addAll(generateSlotsForDoctorOnDate(doctor, date, specialty, false));
        }

        allAvailableSlots.sort((s1, s2) -> { // Ordenar por hora y luego por nombre del doctor
            int timeComparison = s1.getStartTime().compareTo(s2.getStartTime());
            if (timeComparison != 0) {
                return timeComparison;
            }
            return s1.getDoctorName().compareTo(s2.getDoctorName());
        });
        log.info("Slots disponibles encontrados para fecha {}, especialidad {}, médico {}: {}", date, specialtyId, doctorIdOpt.map(String::valueOf).orElse("TODOS"), allAvailableSlots.size());
        return allAvailableSlots;
    }

    /**
     * Genera slots para un médico en una fecha, considerando su horario base, excepciones y citas existentes.
     * @param justCheckExistence Si es true, retorna tan pronto encuentra el primer slot (optimización para getAvailableDates).
     */
    private List<AvailableSlotDto> generateSlotsForDoctorOnDate(UserEntity doctor, LocalDate date, SpecialtyEntity specialty, boolean justCheckExistence) {
        List<AvailableSlotDto> availableSlotsForDoctor = new ArrayList<>();
        int appointmentDurationInMinutes = specialty.getSpecialtyDefaultDurationMinutes();
        int dayOfWeekIso = date.getDayOfWeek().getValue(); // 1 (Lunes) a 7 (Domingo)

        List<DoctorScheduleTemplateEntity> templates = scheduleTemplateRepository
                .findByDoctorUserAndDoctorScheduleTemplateDayOfWeekAndDoctorScheduleTemplateIsActiveTrue(doctor, dayOfWeekIso);

        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // Obtener todas las excepciones y citas para este doctor en esta fecha para optimizar

        OffsetDateTime dayStart = OffsetDateTime.of(date, LocalTime.MIN, CLINIC_ZONE_ID.getRules().getOffset(Instant.now()));
        OffsetDateTime dayEnd = OffsetDateTime.of(date, LocalTime.MAX, CLINIC_ZONE_ID.getRules().getOffset(Instant.now()));

        List<ScheduleExceptionEntity> exceptionsForDay = scheduleExceptionRepository
                .findOverlappingExceptionsForDoctor(doctor, dayStart, dayEnd); // Ajusta el método del repo si es necesario

        List<AppointmentEntity> appointmentsForDay = appointmentRepository
                .findByDoctorUserAndAppointmentStartDatetimeBetweenOrderByAppointmentStartDatetimeAsc(doctor, dayStart, dayEnd);


        for (DoctorScheduleTemplateEntity template : templates) {
            LocalTime currentSlotStartTime = template.getDoctorScheduleTemplateStartTime();
            LocalTime templateEndTime = template.getDoctorScheduleTemplateEndTime();

            while (currentSlotStartTime.plusMinutes(appointmentDurationInMinutes).compareTo(templateEndTime) <= 0) {
                LocalTime currentSlotEndTime = currentSlotStartTime.plusMinutes(appointmentDurationInMinutes);



                // Convertir a OffsetDateTime usando la zona horaria de la clínica para comparaciones
                OffsetDateTime slotStartDateTime = OffsetDateTime.of(date, currentSlotStartTime, ClinicConstants.getClinicZoneOffset());
                OffsetDateTime slotEndDateTime = OffsetDateTime.of(date, currentSlotEndTime, ClinicConstants.getClinicZoneOffset());

                // No generar slots en el pasado (considerando la hora actual)
                if (slotStartDateTime.isBefore(OffsetDateTime.now(ClinicConstants.CLINIC_ZONE_ID))) {
                    currentSlotStartTime = currentSlotStartTime.plusMinutes(appointmentDurationInMinutes); // O un incremento menor para más precisión
                    continue;
                }

                // Verificar contra Excepciones
                boolean inException = false;
                for (ScheduleExceptionEntity ex : exceptionsForDay) {
                    if (rangesOverlap(slotStartDateTime, slotEndDateTime, ex.getScheduleExceptionStartDatetime(), ex.getScheduleExceptionEndDatetime())) {
                        inException = true;
                        // Optimización: avanzar currentSlotStartTime hasta el final de esta excepción
                        // currentSlotStartTime = ex.getScheduleExceptionEndDatetime().atZoneSameInstant(CLINIC_ZONE_ID).toLocalTime();
                        break;
                    }
                }
                if (inException) {
                    // Si estaba en excepción, avanzamos y continuamos el bucle while
                    // La optimización de avance comentada arriba es compleja, por ahora solo avanzamos el slot
                    currentSlotStartTime = currentSlotStartTime.plusMinutes(appointmentDurationInMinutes);
                    continue;
                }

                // Verificar contra Citas Existentes
                boolean hasConflict = false;
                for (AppointmentEntity app : appointmentsForDay) {
                    if (app.getAppointmentStatus() == AppointmentStatusEnum.SCHEDULED && // Solo citas agendadas bloquean
                            rangesOverlap(slotStartDateTime, slotEndDateTime, app.getAppointmentStartDatetime(), app.getAppointmentEndDatetime())) {
                        hasConflict = true;
                        break;
                    }
                }
                if (hasConflict) {
                    currentSlotStartTime = currentSlotStartTime.plusMinutes(appointmentDurationInMinutes);
                    continue;
                }

                // Si pasa todas las validaciones, el slot está disponible
                availableSlotsForDoctor.add(AvailableSlotDto.builder()
                        .startTime(currentSlotStartTime)
                        .endTime(currentSlotEndTime)
                        .doctorId(doctor.getUserId())
                        .doctorName(doctor.getUserFirstName() + " " + doctor.getUserLastName())
                        .durationMinutes(appointmentDurationInMinutes)
                        .build());

                if (justCheckExistence) return availableSlotsForDoctor; // Encontrado uno, suficiente

                currentSlotStartTime = currentSlotStartTime.plusMinutes(appointmentDurationInMinutes);
            }
        }
        return availableSlotsForDoctor;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isSlotActuallyAvailable(Long doctorId, OffsetDateTime startDateTime, int durationInMinutes) {
        log.debug("Verificando disponibilidad real del slot para médico ID: {}, inicio: {}, duración: {} min",
                doctorId, startDateTime, durationInMinutes);

        UserEntity doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado con ID: " + doctorId));

        OffsetDateTime endDateTime = startDateTime.plusMinutes(durationInMinutes);

        // 1. Verificar si el slot cae dentro de alguna excepción de horario del médico
        List<ScheduleExceptionEntity> exceptions = scheduleExceptionRepository.findOverlappingExceptionsForDoctor(
                doctor, startDateTime, endDateTime); // Asegúrate que este método exista y funcione con OffsetDateTime
        if (!exceptions.isEmpty()) {
            log.info("Slot no disponible debido a una excepción de horario para el médico ID: {}", doctorId);
            return false;
        }

        // 2. Verificar si ya hay una cita agendada que se solape con este slot
        long overlappingAppointments = appointmentRepository.countScheduledAppointmentsForDoctorInSlot(
                doctor,
                startDateTime,
                endDateTime
        ); // Asegúrate que este método en el repo maneje OffsetDateTime o convierte

        if (overlappingAppointments > 0) {
            log.info("Slot no disponible debido a una cita existente para el médico ID: {}", doctorId);
            return false;
        }

        log.info("Slot confirmado como disponible para médico ID: {}", doctorId);
        return true;
    }

    private List<UserEntity> getDoctorsToConsider(Integer specialtyId, Optional<Long> doctorIdOpt) {
        if (doctorIdOpt.isPresent()) {
            return userRepository.findById(doctorIdOpt.get())
                    .filter(UserEntity::getUserIsActive)
                    .filter(doc -> doc.getDoctorProfile() != null &&
                            doc.getDoctorProfile().getSpecialty() != null &&
                            doc.getDoctorProfile().getSpecialty().getSpecialtyId().equals(specialtyId))
                    .map(List::of)
                    .orElseGet(() -> {
                        log.warn("Médico ID {} no encontrado, inactivo o no pertenece a la especialidad ID {}", doctorIdOpt.get(), specialtyId);
                        return Collections.emptyList();
                    });
        } else {
            return userRepository.findActiveDoctorsBySpecialtyId(specialtyId);
        }
    }

    /**
     * Verifica si dos rangos de tiempo [start1, end1) y [start2, end2) se solapan.
     * Asume que los rangos son semi-abiertos a la derecha: [inicio, fin)
     */
    private boolean rangesOverlap(OffsetDateTime start1, OffsetDateTime end1, OffsetDateTime start2, OffsetDateTime end2) {
        // Se solapan si el inicio de uno es antes del fin del otro Y el fin de uno es después del inicio del otro.
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}