package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.ScheduleExceptionEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleExceptionEntity, Long> {

    List<ScheduleExceptionEntity> findByDoctorUser(UserEntity doctorUser);

    /**
     * Encuentra todas las excepciones de horario para un médico específico que se solapan
     * con el rango de tiempo [rangeStart, rangeEnd).
     * Un solapamiento ocurre si la excepción comienza antes de que el rango termine Y
     * la excepción termina después de que el rango comience.
     *
     * @param doctorUser El médico para el cual buscar excepciones.
     * @param rangeStart El inicio del rango de tiempo a verificar (exclusivo en el límite superior de la excepción).
     * @param rangeEnd   El fin del rango de tiempo a verificar (exclusivo en el límite inferior de la excepción).
     * @return Una lista de ScheduleExceptionEntity que se solapan.
     */
    @Query("SELECT se FROM ScheduleExceptionEntity se " +
            "WHERE se.doctorUser = :doctorUser " +
            "AND se.scheduleExceptionStartDatetime < :rangeEnd " + // La excepción empieza antes de que el rango termine
            "AND se.scheduleExceptionEndDatetime > :rangeStart")   // Y la excepción termina después de que el rango empiece
    List<ScheduleExceptionEntity> findOverlappingExceptionsForDoctor(
            @Param("doctorUser") UserEntity doctorUser,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd);
}