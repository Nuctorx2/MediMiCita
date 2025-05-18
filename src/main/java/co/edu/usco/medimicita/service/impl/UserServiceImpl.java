package co.edu.usco.medimicita.service.impl;

import co.edu.usco.medimicita.dto.UserRegistrationDto;
import co.edu.usco.medimicita.entity.*;
import co.edu.usco.medimicita.exception.*; // Asume que aquí están tus excepciones personalizadas
import co.edu.usco.medimicita.repository.*;
import co.edu.usco.medimicita.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EpsRepository epsRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorProfileRepository doctorProfileRepository;
    private final SpecialtyRepository specialtyRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           EpsRepository epsRepository,
                           PasswordEncoder passwordEncoder,
                           DoctorProfileRepository doctorProfileRepository,
                           SpecialtyRepository specialtyRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.epsRepository = epsRepository;
        this.passwordEncoder = passwordEncoder;
        this.doctorProfileRepository = doctorProfileRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Override
    @Transactional
    public UserEntity registerNewPatient(UserRegistrationDto registrationDto) {
        validateUserDoesNotExist(registrationDto.getEmail(), registrationDto.getIdentificationNumber());

        if (registrationDto.getPassword() == null || !registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new PasswordsDoNotMatchException("Las contraseñas no coinciden o están vacías.");
        }
        if (!Boolean.TRUE.equals(registrationDto.getTermsAccepted())) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones.");
        }

        UserEntity newUser = mapDtoToEntityForRegistration(registrationDto, "PACIENTE");

        if (registrationDto.getEpsId() != null) {
            EpsEntity eps = epsRepository.findById(registrationDto.getEpsId())
                    .orElseThrow(() -> new ResourceNotFoundException("EPS no encontrada con ID: " + registrationDto.getEpsId()));
            newUser.setEps(eps);
        }
        newUser.setUserTermsAcceptedAt(OffsetDateTime.now());

        return userRepository.save(newUser);
    }

    @Override
    @Transactional
    public UserEntity createUser(UserRegistrationDto userDto, String roleName) {
        String roleNameToFind = roleName.trim().toUpperCase();
        validateUserDoesNotExist(userDto.getEmail(), userDto.getIdentificationNumber());

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es requerida para crear un usuario.");
        }
        // Para creación por admin, no se suele pedir confirmación, pero se puede añadir al DTO si se desea.

        UserEntity newUser = mapDtoToEntityForRegistration(userDto, roleNameToFind);

        if (roleNameToFind.equals("PACIENTE")) {
            if (userDto.getEpsId() != null) {
                EpsEntity eps = epsRepository.findById(userDto.getEpsId())
                        .orElseThrow(() -> new ResourceNotFoundException("EPS no encontrada con ID: " + userDto.getEpsId()));
                newUser.setEps(eps);
            }
            // Asumimos que si un admin crea un paciente, los términos se aceptan implícitamente o hay otro flujo
            if (Boolean.TRUE.equals(userDto.getTermsAccepted())) {
                newUser.setUserTermsAcceptedAt(OffsetDateTime.now());
            }
        }

        UserEntity savedUser = userRepository.save(newUser); // Guardar primero para obtener el ID

        if (roleNameToFind.equals("MEDICO")) {
            if (userDto.getSpecialtyId() == null || userDto.getOfficeNumber() == null || userDto.getOfficeNumber().isBlank()) {
                throw new IllegalArgumentException("Para rol MÉDICO, se requiere especialidad y número de consultorio.");
            }
            SpecialtyEntity specialty = specialtyRepository.findById(userDto.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + userDto.getSpecialtyId()));

            DoctorProfileEntity profile = new DoctorProfileEntity();
            profile.setUser(savedUser);
            profile.setSpecialty(specialty);
            profile.setDoctorProfileOfficeNumber(userDto.getOfficeNumber());
            doctorProfileRepository.save(profile);
            // Opcional: savedUser.setDoctorProfile(profile); si necesitas retornar el UserEntity con el perfil ya en el objeto
        }
        return savedUser;
    }


    @Override
    @Transactional
    public UserEntity updateUser(Long userId, UserRegistrationDto userDto, Optional<String> roleNameOpt) {
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        // Actualizar campos básicos
        if (userDto.getFirstName() != null && !userDto.getFirstName().isBlank()) {
            existingUser.setUserFirstName(userDto.getFirstName());
        }
        if (userDto.getLastName() != null && !userDto.getLastName().isBlank()) {
            existingUser.setUserLastName(userDto.getLastName());
        }
        if (userDto.getBirthDate() != null) {
            existingUser.setUserBirthDate(userDto.getBirthDate());
        }
        if (userDto.getPhoneNumber() != null && !userDto.getPhoneNumber().isBlank()) {
            existingUser.setUserPhoneNumber(userDto.getPhoneNumber());
        }

        // Actualizar email con validación de unicidad (si cambia)
        if (userDto.getEmail() != null && !userDto.getEmail().isBlank() && !existingUser.getUserEmail().equalsIgnoreCase(userDto.getEmail())) {
            if (userRepository.existsByUserEmail(userDto.getEmail())) {
                throw new EmailAlreadyExistsException("El nuevo email ya está registrado: " + userDto.getEmail());
            }
            existingUser.setUserEmail(userDto.getEmail());
        }

        // Actualizar identificación con validación de unicidad (si cambia)
        if (userDto.getIdentificationNumber() != null && !userDto.getIdentificationNumber().isBlank() && !existingUser.getUserIdentificationNumber().equals(userDto.getIdentificationNumber())) {
            if (userRepository.existsByUserIdentificationNumber(userDto.getIdentificationNumber())) {
                throw new IdentificationNumberAlreadyExistsException("El nuevo número de identificación ya está registrado: " + userDto.getIdentificationNumber());
            }
            existingUser.setUserIdentificationNumber(userDto.getIdentificationNumber());
        }

        // Actualizar contraseña si se proporciona
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            if(userDto.getConfirmPassword() == null || !userDto.getPassword().equals(userDto.getConfirmPassword())){
                throw new PasswordsDoNotMatchException("Las contraseñas para la actualización no coinciden.");
            }
            existingUser.setUserPasswordHash(passwordEncoder.encode(userDto.getPassword()));
            existingUser.setUserRequiresPasswordChange(false); // Si cambia la pass, ya no requiere cambio.
        }

        // Actualizar Rol y lógica asociada (DoctorProfile, EPS)
        String originalRoleName = existingUser.getRole().getRoleName();
        final String[] newRoleName = {originalRoleName}; // Para usar dentro del lambda

        roleNameOpt.ifPresent(rn -> {
            String roleNameToFind = rn.trim().toUpperCase();
            if (!originalRoleName.equals(roleNameToFind)) {
                RoleEntity newRoleEntity = roleRepository.findByRoleName(roleNameToFind)
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleNameToFind));
                existingUser.setRole(newRoleEntity);
                newRoleName[0] = roleNameToFind; // Actualizar el nuevo nombre de rol
            }
        });

        // Manejo de EPS
        if (newRoleName[0].equals("PACIENTE")) {
            if (userDto.getEpsId() != null) {
                EpsEntity eps = epsRepository.findById(userDto.getEpsId())
                        .orElseThrow(() -> new ResourceNotFoundException("EPS no encontrada con ID: " + userDto.getEpsId()));
                existingUser.setEps(eps);
            } else if (existingUser.getEps() != null && !originalRoleName.equals("PACIENTE")) {
                // Si era paciente y se le asignó EPS, pero deja de serlo, o si se le quita la EPS.
                existingUser.setEps(null); // Asumiendo que un no-paciente no tiene EPS.
            }
        } else {
            existingUser.setEps(null); // Si no es PACIENTE, no tiene EPS.
        }


        // Manejo de DoctorProfile
        if (newRoleName[0].equals("MEDICO")) {
            DoctorProfileEntity profile = doctorProfileRepository.findByUser(existingUser).orElseGet(() -> {
                DoctorProfileEntity newProfile = new DoctorProfileEntity();
                newProfile.setUser(existingUser); // Asociar con el usuario existente
                return newProfile;
            });

            if (userDto.getSpecialtyId() == null || userDto.getOfficeNumber() == null || userDto.getOfficeNumber().isBlank()) {
                if(profile.getSpecialty() == null || profile.getDoctorProfileOfficeNumber() == null || profile.getDoctorProfileOfficeNumber().isBlank()){
                    throw new IllegalArgumentException("Para rol MÉDICO, se requiere especialidad y número de consultorio.");
                }
            }

            if(userDto.getSpecialtyId() != null) {
                SpecialtyEntity specialty = specialtyRepository.findById(userDto.getSpecialtyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + userDto.getSpecialtyId()));
                profile.setSpecialty(specialty);
            }
            if(userDto.getOfficeNumber() != null && !userDto.getOfficeNumber().isBlank()){
                profile.setDoctorProfileOfficeNumber(userDto.getOfficeNumber());
            }
            doctorProfileRepository.save(profile);
        } else {
            // Si el usuario existía y era médico, pero ahora tiene otro rol, eliminar su perfil de doctor.
            if (originalRoleName.equals("MEDICO")) {
                doctorProfileRepository.findByUser(existingUser).ifPresent(doctorProfileRepository::delete);
                // existingUser.setDoctorProfile(null); // JPA debería manejar la eliminación de la asociación
            }
        }

        return userRepository.save(existingUser);
    }


    @Override
    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        // Borrado lógico:
        user.setUserIsActive(false);
        // Opcional: anonimizar algunos datos si es necesario por GDPR u otras regulaciones
        // user.setUserEmail("deleted_" + userId + "@example.com");
        // user.setUserIdentificationNumber("DELETED_" + userId);
        userRepository.save(user);

        // Considerar si al "eliminar" (inactivar) un médico, sus citas futuras deben ser canceladas/reagendadas.
        // O si al eliminar un paciente, sus citas futuras se cancelan. Esto sería lógica adicional aquí o en un listener.
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll(); // Para una aplicación real, usar paginación aquí.
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByUserEmail(email);
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public UserEntity save(UserEntity userEntity) {
        // Este metodo save genérico podría ser usado internamente o para casos simples.
        // Si se está actualizando una contraseña a través de este metodo, asegurarse que ya esté hasheada.
        return userRepository.save(userEntity);
    }


    // --- Implementación de UserDetailsService ---
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUserEmail(usernameOrEmail.toLowerCase()) // Normalizar a minúsculas
                .or(() -> userRepository.findByUserIdentificationNumber(usernameOrEmail)) // Intentar por identificación
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email o identificación: " + usernameOrEmail));

        if (!user.getUserIsActive()) {
            // Puedes lanzar una excepción más específica de Spring Security si quieres, como DisabledException
            throw new UserAccountNotActiveException("La cuenta del usuario '" + usernameOrEmail + "' está inactiva.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUserEmail(), // Usar el email (o un identificador único) como el "username" para Spring Security
                user.getUserPasswordHash(),
                user.getUserIsActive(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked (el flag requires_password_change se maneja post-autenticación)
                getAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(RoleEntity role) {
        if (role == null || role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol del usuario o el nombre del rol no pueden ser nulos o vacíos");
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.getRoleName().trim().toUpperCase()));
    }

    // --- Métodos Helper ---
    private void validateUserDoesNotExist(String email, String identificationNumber) {
        if (userRepository.existsByUserEmail(email)) {
            throw new EmailAlreadyExistsException("Ya existe una cuenta registrada con el email: " + email);
        }
        if (userRepository.existsByUserIdentificationNumber(identificationNumber)) {
            throw new IdentificationNumberAlreadyExistsException("Ya existe una cuenta registrada con el número de identificación: " + identificationNumber);
        }
    }

    private UserEntity mapDtoToEntityForRegistration(UserRegistrationDto dto, String roleName) {
        UserEntity entity = new UserEntity();
        entity.setUserFirstName(dto.getFirstName());
        entity.setUserLastName(dto.getLastName());
        entity.setUserEmail(dto.getEmail().toLowerCase()); // Guardar email en minúsculas
        entity.setUserPasswordHash(passwordEncoder.encode(dto.getPassword()));
        entity.setUserIdentificationNumber(dto.getIdentificationNumber());
        entity.setUserBirthDate(dto.getBirthDate());
        entity.setUserPhoneNumber(dto.getPhoneNumber());
        entity.setUserIsActive(true);
        entity.setUserRequiresPasswordChange(roleName.equals("MEDICO")); // Solo médicos requieren cambio inicial

        RoleEntity roleEntity = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol " + roleName + " no encontrado."));
        entity.setRole(roleEntity);

        return entity;
    }
}