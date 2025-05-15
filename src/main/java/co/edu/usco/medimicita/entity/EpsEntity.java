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
    private Integer epsId;

    @Column(name = "eps_name", nullable = false, length = 50)
    private String epsName;

    @Column(name = "eps_is_active", nullable = false)
    private boolean epsIsActive;

    @CreationTimestamp
    @Column(name = "eps_created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime epsCreatedAt;

    @UpdateTimestamp
    @Column(name = "eps_updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime epsUpdatedAt;
}
