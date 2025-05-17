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

    // Encuentra excepciones para un doctor que se solapan con un rango de tiempo dado
    @Query("SELECT se FROM ScheduleExceptionEntity se WHERE se.doctorUser = :doctorUser " +
            "AND se.scheduleExceptionStartDatetime < :endDateTime " +
            "AND se.scheduleExceptionEndDatetime > :startDateTime")
    List<ScheduleExceptionEntity> findOverlappingExceptionsForDoctor(
            @Param("doctorUser") UserEntity doctorUser,
            @Param("startDateTime") OffsetDateTime startDateTime,
            @Param("endDateTime") OffsetDateTime endDateTime);
}