package co.edu.usco.medimicita.util; // O el paquete que prefieras

import co.edu.usco.medimicita.entity.*;
import co.edu.usco.medimicita.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
@Order(1)
@Profile("!test") // No ejecutar en perfil de prueba si no se desea
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final EpsRepository epsRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           EpsRepository epsRepository,
                           SpecialtyRepository specialtyRepository,
                           UserRepository userRepository,
                           DoctorProfileRepository doctorProfileRepository,
                           UserAddressRepository userAddressRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.epsRepository = epsRepository;
        this.specialtyRepository = specialtyRepository;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userAddressRepository = userAddressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando carga de datos iniciales...");

        // 1. Crear Roles
        RoleEntity pacienteRole = createRoleIfNotExists("PACIENTE");
        RoleEntity medicoRole = createRoleIfNotExists("MEDICO");
        RoleEntity adminRole = createRoleIfNotExists("ADMINISTRADOR");

        // 2. Crear EPS
        EpsEntity epsSura = createEpsIfNotExists("EPS Sura");
        EpsEntity epsSanitas = createEpsIfNotExists("EPS Sanitas");
        createEpsIfNotExists("Nueva EPS");

        // 3. Crear Especialidades
        SpecialtyEntity medGeneral = createSpecialtyIfNotExists("Medicina General", 30);
        SpecialtyEntity odontologia = createSpecialtyIfNotExists("Odontología", 40);
        createSpecialtyIfNotExists("Ginecología", 45);

        // 4. Crear Usuario Administrador
        if (!userRepository.existsByUserEmail("admin@medimicita.usco.edu.co")) {
            UserEntity adminUser = new UserEntity();
            adminUser.setUserIdentificationNumber("0000000001");
            adminUser.setUserFirstName("Admin");
            adminUser.setUserLastName("Principal");
            adminUser.setUserBirthDate(LocalDate.of(1990, 1, 1));
            adminUser.setUserPhoneNumber("3001234567");
            adminUser.setUserEmail("admin@medimicita.usco.edu.co");
            adminUser.setUserPasswordHash("$2a$10$YDFhZ/GUps0BriYiLDurgegUy/k1xWP.IwqTuCyTvieZKV0M04W9K"); // TU HASH PARA admin123
            adminUser.setRole(adminRole);
            adminUser.setUserIsActive(true);
            adminUser.setUserRequiresPasswordChange(false);
            userRepository.save(adminUser);
            log.info("Usuario Administrador creado: admin@medimicita.usco.edu.co");
        } else {
            log.info("Usuario Administrador ya existe: admin@medimicita.usco.edu.co");
        }

        // 5. Crear Usuario Médico de Prueba
        if (!userRepository.existsByUserEmail("carlos.ramirez@medimicita.usco.edu.co")) {
            UserEntity medicoUser = new UserEntity();
            medicoUser.setUserIdentificationNumber("1075000001");
            medicoUser.setUserFirstName("Carlos");
            medicoUser.setUserLastName("Ramirez");
            medicoUser.setUserBirthDate(LocalDate.of(1985, 5, 15));
            medicoUser.setUserPhoneNumber("3109876543");
            medicoUser.setUserEmail("carlos.ramirez@medimicita.usco.edu.co");
            medicoUser.setUserPasswordHash("$2a$10$jkw6tTTiUgNrzvwprV2ZM.Zl24elFIgc1GSdvrXy8X3dFVQxjKSfa"); // TU HASH PARA medico123
            medicoUser.setRole(medicoRole);
            medicoUser.setUserIsActive(true);
            medicoUser.setUserRequiresPasswordChange(true); // Médico nuevo requiere cambio

            UserEntity savedMedicoUser = userRepository.save(medicoUser); // Guardar primero para obtener ID y asociar perfil

            DoctorProfileEntity medicoProfile = new DoctorProfileEntity();
            medicoProfile.setUser(savedMedicoUser);
            medicoProfile.setSpecialty(medGeneral); // Asignar especialidad creada
            medicoProfile.setDoctorProfileOfficeNumber("Consultorio 101");
            doctorProfileRepository.save(medicoProfile);
            log.info("Usuario Médico creado: carlos.ramirez@medimicita.usco.edu.co con perfil.");
        } else {
            log.info("Usuario Médico ya existe: carlos.ramirez@medimicita.usco.edu.co");
        }

        // 6. Crear Usuario Paciente de Prueba
        if (!userRepository.existsByUserEmail("ana.perez@example.com")) {
            UserEntity pacienteUser = new UserEntity();
            pacienteUser.setUserIdentificationNumber("1075000002");
            pacienteUser.setUserFirstName("Ana");
            pacienteUser.setUserLastName("Perez");
            pacienteUser.setUserBirthDate(LocalDate.of(1992, 8, 20));
            pacienteUser.setUserPhoneNumber("3151112233");
            pacienteUser.setUserEmail("ana.perez@example.com");
            pacienteUser.setUserPasswordHash("$2a$10$v2BEsudrg11uBhm8EEJVOejnA.p4Zddj7E1VUhZENSexiYOh8j35i"); // TU HASH PARA paciente123
            pacienteUser.setRole(pacienteRole);
            pacienteUser.setEps(epsSura); // Asignar EPS creada
            pacienteUser.setUserTermsAcceptedAt(OffsetDateTime.now());
            pacienteUser.setUserIsActive(true);
            pacienteUser.setUserRequiresPasswordChange(false);

            UserEntity savedPacienteUser = userRepository.save(pacienteUser); // Guardar primero

            UserAddressEntity pacienteAddress = new UserAddressEntity();
            pacienteAddress.setUser(savedPacienteUser);
            pacienteAddress.setUserAddressStreetType("Calle");
            pacienteAddress.setUserAddressStreetNumber("10A # 20-30");
            pacienteAddress.setUserAddressAdditionalInfo("Barrio Centro, Casa 2, Frente al parque");
            pacienteAddress.setUserAddressIsCurrent(true);
            userAddressRepository.save(pacienteAddress);
            log.info("Usuario Paciente creado: ana.perez@example.com con dirección.");
        } else {
            log.info("Usuario Paciente ya existe: ana.perez@example.com");
        }

        log.info("Carga de datos iniciales completada.");
    }

    private RoleEntity createRoleIfNotExists(String roleName) {
        return roleRepository.findByRoleName(roleName.toUpperCase())
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setRoleName(roleName.toUpperCase());
                    log.info("Creando rol: {}", roleName.toUpperCase());
                    return roleRepository.save(newRole);
                });
    }

    private EpsEntity createEpsIfNotExists(String epsName) {
        return epsRepository.findByEpsName(epsName)
                .orElseGet(() -> {
                    EpsEntity newEps = new EpsEntity();
                    newEps.setEpsName(epsName);
                    newEps.setEpsIsActive(true);
                    log.info("Creando EPS: {}", epsName);
                    return epsRepository.save(newEps);
                });
    }

    private SpecialtyEntity createSpecialtyIfNotExists(String specialtyName, int duration) {
        return specialtyRepository.findBySpecialtyName(specialtyName)
                .orElseGet(() -> {
                    SpecialtyEntity newSpecialty = new SpecialtyEntity();
                    newSpecialty.setSpecialtyName(specialtyName);
                    newSpecialty.setSpecialtyDefaultDurationMinutes(duration);
                    newSpecialty.setSpecialtyIsActive(true);
                    log.info("Creando Especialidad: {}", specialtyName);
                    return specialtyRepository.save(newSpecialty);
                });
    }
}