package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.AppointmentEntity;
import co.edu.usco.medimicita.enums.AppointmentStatusEnum;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByPatientUserOrderByAppointmentStartDatetimeDesc(UserEntity patientUser);
    List<AppointmentEntity> findByDoctorUserOrderByAppointmentStartDatetimeAsc(UserEntity doctorUser);

    // Citas futuras de un paciente
    List<AppointmentEntity> findByPatientUserAndAppointmentStartDatetimeAfterOrderByAppointmentStartDatetimeAsc(
            UserEntity patientUser, OffsetDateTime currentTime);

    // Citas pasadas de un paciente
    List<AppointmentEntity> findByPatientUserAndAppointmentStartDatetimeBeforeOrderByAppointmentStartDatetimeDesc(
            UserEntity patientUser, OffsetDateTime currentTime);

    // Citas de un médico en un rango de fechas
    List<AppointmentEntity> findByDoctorUserAndAppointmentStartDatetimeBetweenOrderByAppointmentStartDatetimeAsc(
            UserEntity doctorUser, OffsetDateTime startRange, OffsetDateTime endRange);

    // Verificar si un médico tiene una cita en un slot específico (excluyendo una cita existente si se está actualizando)
    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.doctorUser = :doctorUser " +
            "AND a.appointmentStatus = 'SCHEDULED' " +
            "AND a.appointmentId <> :excludeAppointmentId " + // Para excluir la cita actual al reprogramar
            "AND a.appointmentStartDatetime < :endDateTime " +
            "AND a.appointmentEndDatetime > :startDateTime")
    long countScheduledAppointmentsForDoctorInSlot(
            @Param("doctorUser") UserEntity doctorUser,
            @Param("startDateTime") OffsetDateTime startDateTime,
            @Param("endDateTime") OffsetDateTime endDateTime,
            @Param("excludeAppointmentId") Long excludeAppointmentId); // Pasar -1L o null si es una nueva cita

    // Por defecto para nueva cita (sin excluir ninguna)
    default long countScheduledAppointmentsForDoctorInSlot(UserEntity doctorUser, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        return countScheduledAppointmentsForDoctorInSlot(doctorUser, startDateTime, endDateTime, -1L);
    }


    // Contar citas futuras activas de un paciente para una especialidad
    @Query("SELECT COUNT(a) FROM AppointmentEntity a " +
            "WHERE a.patientUser = :patientUser " +
            "AND a.specialty.specialtyId = :specialtyId " +
            "AND a.appointmentStatus = 'SCHEDULED' " +
            "AND a.appointmentStartDatetime > :currentTime")
    long countActiveFutureAppointmentsByPatientAndSpecialty(
            @Param("patientUser") UserEntity patientUser,
            @Param("specialtyId") Integer specialtyId,
            @Param("currentTime") OffsetDateTime currentTime);


    // Verificar si un paciente tiene una cita en la misma fecha y hora exacta
    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.patientUser = :patientUser " +
            "AND a.appointmentStatus = 'SCHEDULED' " +
            "AND a.appointmentStartDatetime = :exactStartDateTime")
    long countScheduledAppointmentsForPatientAtExactTime(
            @Param("patientUser") UserEntity patientUser,
            @Param("exactStartDateTime") OffsetDateTime exactStartDateTime);

}