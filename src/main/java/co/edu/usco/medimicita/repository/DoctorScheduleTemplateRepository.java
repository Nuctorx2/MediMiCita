package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.DoctorScheduleTemplateEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorScheduleTemplateRepository extends JpaRepository<DoctorScheduleTemplateEntity, Long> {

    List<DoctorScheduleTemplateEntity> findByDoctorUser(UserEntity doctorUser);

    List<DoctorScheduleTemplateEntity> findByDoctorUserAndDoctorScheduleTemplateIsActiveTrue(UserEntity doctorUser);

    List<DoctorScheduleTemplateEntity> findByDoctorUserAndDoctorScheduleTemplateDayOfWeekAndDoctorScheduleTemplateIsActiveTrue(
            UserEntity doctorUser, Integer dayOfWeek);
}