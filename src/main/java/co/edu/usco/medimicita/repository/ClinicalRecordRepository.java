package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.AppointmentEntity;
import co.edu.usco.medimicita.entity.ClinicalRecordEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecordEntity, Long> {

    // Encuentra todos los registros clínicos para un paciente, ordenados por fecha del registro descendente
    List<ClinicalRecordEntity> findByPatientUserOrderByClinicalRecordRecordDatetimeDesc(UserEntity patientUser);

    // Encuentra todos los registros clínicos escritos por un médico específico
    List<ClinicalRecordEntity> findByDoctorUserOrderByClinicalRecordRecordDatetimeDesc(UserEntity doctorUser);

    // Encuentra registros clínicos asociados a una cita específica
    List<ClinicalRecordEntity> findByAppointmentOrderByClinicalRecordRecordDatetimeAsc(AppointmentEntity appointment);

    // Encuentra registros clínicos para un paciente dentro de un rango de fechas del registro
    List<ClinicalRecordEntity> findByPatientUserAndClinicalRecordRecordDatetimeBetweenOrderByClinicalRecordRecordDatetimeDesc(
            UserEntity patientUser, OffsetDateTime startRange, OffsetDateTime endRange);
}