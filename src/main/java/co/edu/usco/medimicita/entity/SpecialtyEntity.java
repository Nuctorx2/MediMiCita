package co.edu.usco.medimicita.entity;

import jakarta.persistence.*; // Para JPA (Jakarta Persistence API)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Específico de Hibernate para timestamps automáticos
import java.time.OffsetDateTime;


@Entity
@Table(name = "specialty")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class SpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "specialty_id")
    private Integer specialtyId;

    @Column(name = "specialty_name", nullable = false, length = 50)
    private String specialtyName;

    @CreationTimestamp
    @Column(name = "specialty_created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime specialtyCreatedAt;
}
