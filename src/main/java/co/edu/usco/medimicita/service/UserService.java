package co.edu.usco.medimicita.service;

import co.edu.usco.medimicita.dto.UserRegistrationDto;
import co.edu.usco.medimicita.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import co.edu.usco.medimicita.dto.PatientProfileUpdateDto;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    UserEntity registerNewPatient(UserRegistrationDto registrationDto); // (2) Método para registrar un nuevo paciente

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(Long id);

    UserEntity updatePatientProfile(Long userId, PatientProfileUpdateDto updateDto);

    UserEntity save(UserEntity userEntity); // Método general para guardar/actualizar

    // Métodos para el administrador
    UserEntity createUser(UserRegistrationDto userDto, String roleName); // Crear cualquier tipo de usuario
    UserEntity updateUser(Long userId, UserRegistrationDto userDto, Optional<String> roleName); // Actualizar usuario
    void deleteUser(Long userId); // O marcar como inactivo
    List<UserEntity> getAllUsers();
}