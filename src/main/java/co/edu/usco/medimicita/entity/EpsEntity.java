package co.edu.usco.medimicita.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name = "eps")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class EpsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eps_id")
    private Long epsId;

    @Column(name = "eps_name", unique = true, nullable = false, length = 100)
    private String epsName;

    @Column(name = "eps_is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean epsIsActive = true; //El valor por defecto en Java también

    @CreationTimestamp
    @Column(name = "eps_created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime epsCreatedAt;

    @UpdateTimestamp // (1) Hibernate: Asigna automáticamente la fecha/hora de la última actualización
    @Column(name = "eps_updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime epsUpdatedAt;

    // Relaciones Inversas (si es necesario):
    // @OneToMany(mappedBy = "eps", fetch = FetchType.LAZY)
    // private java.util.List<UserEntity> users;
}
