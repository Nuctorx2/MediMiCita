package co.edu.usco.medimicita.entity;

import co.edu.usco.medimicita.enums.StreetTypeEnum; // Importar el Enum
import co.edu.usco.medimicita.enums.ZoneTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_address_user_id", columnList = "user_id"),
        @Index(name = "idx_user_address_is_current", columnList = "user_address_is_current")
        // El índice para la constraint UNIQUE condicional se definiría manualmente en la BD.
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_address_id")
    @EqualsAndHashCode.Include
    private Long userAddressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_address_user"))
    private UserEntity user;


//    Inicio de Nuevos campos
    @Enumerated(EnumType.STRING)
    @Column(name = "user_address_street_type", length = 30) // Tipo de vía (Calle, Carrera, etc.)
    private StreetTypeEnum userAddressStreetType;

    @Column(name = "user_address_main_way_number", length = 50) // Número o nombre de vía principal (e.g., "87B", "QUINTA")
    private String userAddressMainWayNumber;

    // Para la placa (vía generadora)
    @Column(name = "user_address_secondary_way_number", length = 50) // Número/letra de vía generadora (e.g., "19A", "26C SUR")
    private String userAddressSecondaryWayNumber;

    @Column(name = "user_address_house_or_building_number", length = 50) // Número de casa/edificio (e.g., "21", "LOTE 5")
    private String userAddressHouseOrBuildingNumber;

    @Column(name = "user_address_complement", length = 255) // Interior, Apto, Piso, Bloque, etc.
    private String userAddressComplement;

    @Column(name = "user_address_neighborhood", length = 100) // Barrio o Vereda
    private String userAddressNeighborhood;

    @Column(name = "user_address_municipality", length = 100) // Municipio
    private String userAddressMunicipality;

    @Column(name = "user_address_department", length = 100) // Departamento
    private String userAddressDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_address_zone", length = 10) // Urbana / Rural
    private ZoneTypeEnum userAddressZone;
    //Fin de nuevos campos

    @Column(name = "user_address_is_current", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean userAddressIsCurrent = true;

    @CreationTimestamp
    @Column(name = "user_address_created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userAddressCreatedAt;

    @UpdateTimestamp
    @Column(name = "user_address_updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime userAddressUpdatedAt;

}


//  (1) Constraint UNIQUE Condicional (Comentario Importante):
//    Tu esquema original menciona: (Constraint: UNIQUE(user_id, is_current) where is_current = TRUE -- Depende del SGBD).
//    Esta es una constraint de unicidad parcial o filtrada. JPA estándar no tiene una forma directa de definir esto mediante anotaciones que funcione en todos los SGBD.
//    PostgreSQL: Puedes crear esto usando un índice único parcial:

//    CREATE UNIQUE INDEX uidx_user_address_current ON user_addresses (user_id, user_address_is_current)
//    WHERE user_address_is_current = TRUE;

//    Hibernate no generará esta DDL automáticamente con ddl-auto. Deberás crearla manualmente en tu script de migración de base de datos o directamente.
//La lógica para asegurar que solo una dirección sea is_current = true por usuario debe implementarse robustamente en tu capa de servicio (por ejemplo, antes de guardar una nueva dirección como actual, poner todas las demás de ese usuario como no actuales).