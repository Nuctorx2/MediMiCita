package co.edu.usco.medimicita.repository;

import co.edu.usco.medimicita.entity.UserAddressEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {

    List<UserAddressEntity> findByUser(UserEntity user);

    Optional<UserAddressEntity> findByUserAndUserAddressIsCurrentTrue(UserEntity user);

    // Query para poner todas las direcciones de un usuario como no actuales (userAddressIsCurrent = false)
    @Modifying // Indica que esta query modifica datos
    @Query("UPDATE UserAddressEntity ua SET ua.userAddressIsCurrent = false WHERE ua.user = :user")
    void setAllAddressesAsNotCurrentForUser(@Param("user") UserEntity user);
}