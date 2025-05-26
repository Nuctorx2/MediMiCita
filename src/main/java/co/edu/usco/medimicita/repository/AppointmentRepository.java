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

    // ... (otros métodos que ya tenías: findByPatientUserOrderBy..., findByDoctorUserOrderBy..., etc.) ...
    List<AppointmentEntity> findByPatientUserOrderByAppointmentStartDatetimeDesc(UserEntity patientUser);
    List<AppointmentEntity> findByDoctorUserOrderByAppointmentStartDatetimeAsc(UserEntity doctorUser);

    List<AppointmentEntity> findByPatientUserAndAppointmentStartDatetimeAfterOrderByAppointmentStartDatetimeAsc(
            UserEntity patientUser, OffsetDateTime currentTime);

    List<AppointmentEntity> findByPatientUserAndAppointmentStartDatetimeBeforeOrderByAppointmentStartDatetimeDesc(
            UserEntity patientUser, OffsetDateTime currentTime);

    // Este método es útil para cargar las citas de un día para generateSlotsForDoctorOnDate
    List<AppointmentEntity> findByDoctorUserAndAppointmentStartDatetimeBetweenOrderByAppointmentStartDatetimeAsc(
            UserEntity doctorUser, OffsetDateTime startRange, OffsetDateTime endRange);


    /**
     * Cuenta el número de citas AGENDADAS ('SCHEDULED') para un médico específico
     * que se solapan con el slot de tiempo propuesto [slotStartDateTime, slotEndDateTime).
     * No cuenta la cita que se está intentando reprogramar (si se proporciona excludeAppointmentId).
     *
     * @param doctor             El médico.
     * @param slotStartDateTime  Inicio del slot propuesto.
     * @param slotEndDateTime    Fin del slot propuesto.
     * @param excludeAppointmentId ID de la cita a excluir de la cuenta (útil al reprogramar).
     *                           Pasar un valor no existente (ej. -1L o null si el tipo fuera Long objeto) si es una nueva cita.
     * @return El número de citas agendadas que se solapan.
     */
    @Query("SELECT COUNT(a) FROM AppointmentEntity a " +
            "WHERE a.doctorUser = :doctor " +
            "AND a.appointmentStatus = co.edu.usco.medimicita.enums.AppointmentStatusEnum.SCHEDULED " + // Referencia completa al Enum
            "AND (:excludeAppointmentId IS NULL OR a.appointmentId <> :excludeAppointmentId) " +
            "AND a.appointmentStartDatetime < :slotEndDateTime " +
            "AND a.appointmentEndDatetime > :slotStartDateTime")
    long countScheduledAppointmentsForDoctorInSlotExcludingId(
            @Param("doctor") UserEntity doctor,
            @Param("slotStartDateTime") OffsetDateTime slotStartDateTime,
            @Param("slotEndDateTime") OffsetDateTime slotEndDateTime,
            @Param("excludeAppointmentId") Long excludeAppointmentId);

    // Método de conveniencia para nuevas citas (sin excluir ninguna)
    default long countScheduledAppointmentsForDoctorInSlot(UserEntity doctor, OffsetDateTime slotStartDateTime, OffsetDateTime slotEndDateTime) {
        return countScheduledAppointmentsForDoctorInSlotExcludingId(doctor, slotStartDateTime, slotEndDateTime, null); // Pasar null para que la condición :excludeAppointmentId IS NULL sea true
    }


    @Query("SELECT COUNT(a) FROM AppointmentEntity a " +
            "WHERE a.patientUser = :patientUser " +
            "AND a.specialty.specialtyId = :specialtyId " +
            "AND a.appointmentStatus = co.edu.usco.medimicita.enums.AppointmentStatusEnum.SCHEDULED " +
            "AND a.appointmentStartDatetime > :currentTime")
    long countActiveFutureAppointmentsByPatientAndSpecialty(
            @Param("patientUser") UserEntity patientUser,
            @Param("specialtyId") Integer specialtyId,
            @Param("currentTime") OffsetDateTime currentTime);


    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.patientUser = :patientUser " +
            "AND a.appointmentStatus = co.edu.usco.medimicita.enums.AppointmentStatusEnum.SCHEDULED " +
            "AND a.appointmentStartDatetime = :exactStartDateTime")
    long countScheduledAppointmentsForPatientAtExactTime(
            @Param("patientUser") UserEntity patientUser,
            @Param("exactStartDateTime") OffsetDateTime exactStartDateTime);
}