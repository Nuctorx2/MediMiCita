package co.edu.usco.medimicita.util; // O el paquete que prefieras

import co.edu.usco.medimicita.entity.*;
import co.edu.usco.medimicita.enums.StreetTypeEnum; // Asegúrate que estos enums existan
import co.edu.usco.medimicita.enums.ZoneTypeEnum;   // en el paquete 'enums'
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
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Component
@Order(1)
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final EpsRepository epsRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final DoctorScheduleTemplateRepository scheduleTemplateRepository; // (AÑADIDO)
    private final PasswordEncoder passwordEncoder;

    // Constructor actualizado para incluir DoctorScheduleTemplateRepository
    public DataInitializer(RoleRepository roleRepository,
                           EpsRepository epsRepository,
                           SpecialtyRepository specialtyRepository,
                           UserRepository userRepository,
                           DoctorProfileRepository doctorProfileRepository,
                           UserAddressRepository userAddressRepository,
                           DoctorScheduleTemplateRepository scheduleTemplateRepository, // (AÑADIDO)
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.epsRepository = epsRepository;
        this.specialtyRepository = specialtyRepository;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userAddressRepository = userAddressRepository;
        this.scheduleTemplateRepository = scheduleTemplateRepository; // (AÑADIDO)
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
        createEpsIfNotExists("EPS Sanitas"); // No necesitamos la variable si no la usamos después

        // 3. Crear Especialidades
        SpecialtyEntity medGeneral = createSpecialtyIfNotExists("Medicina General", 30);
        SpecialtyEntity odontologia = createSpecialtyIfNotExists("Odontología", 40);
        SpecialtyEntity ginecologia = createSpecialtyIfNotExists("Ginecología", 45);

        // 4. Crear Usuario Administrador
        if (!userRepository.existsByUserEmail("admin@medimicita.usco.edu.co")) {
            UserEntity adminUser = new UserEntity();
            adminUser.setUserIdentificationNumber("0000000001");
            adminUser.setUserFirstName("Admin");
            adminUser.setUserLastName("Principal");
            adminUser.setUserBirthDate(LocalDate.of(1990, 1, 1));
            adminUser.setUserPhoneNumber("3001234567");
            adminUser.setUserEmail("admin@medimicita.usco.edu.co");
            adminUser.setUserPasswordHash("$2a$10$YDFhZ/GUps0BriYiLDurgegUy/k1xWP.IwqTuCyTvieZKV0M04W9K"); // TU HASH
            adminUser.setRole(adminRole);
            adminUser.setUserIsActive(true);
            adminUser.setUserRequiresPasswordChange(false);
            userRepository.save(adminUser);
            log.info("Usuario Administrador creado: admin@medimicita.usco.edu.co");
        } else {
            log.info("Usuario Administrador ya existe: admin@medimicita.usco.edu.co");
        }

        // 5. Crear Usuario Médico (Medicina General) de Prueba
        UserEntity savedMedicoUser = userRepository.findByUserEmail("carlos@medimicita.co")
                .orElseGet(() -> {
                    UserEntity medicoUser = new UserEntity();
                    medicoUser.setUserIdentificationNumber("1075000001");
                    medicoUser.setUserFirstName("Carlos");
                    medicoUser.setUserLastName("Ramirez");
                    medicoUser.setUserBirthDate(LocalDate.of(1985, 5, 15));
                    medicoUser.setUserPhoneNumber("3109876543");
                    medicoUser.setUserEmail("carlos@medimicita.co");
                    medicoUser.setUserPasswordHash("$2a$10$jkw6tTTiUgNrzvwprV2ZM.Zl24elFIgc1GSdvrXy8X3dFVQxjKSfa"); // TU HASH
                    medicoUser.setRole(medicoRole);
                    medicoUser.setUserIsActive(true);
                    medicoUser.setUserRequiresPasswordChange(true);
                    UserEntity savedUser = userRepository.save(medicoUser); // Guardar primero

                    DoctorProfileEntity medicoProfile = new DoctorProfileEntity();
                    medicoProfile.setUser(savedUser);
                    medicoProfile.setSpecialty(medGeneral);
                    medicoProfile.setDoctorProfileOfficeNumber("101");
                    doctorProfileRepository.save(medicoProfile);
                    log.info("Usuario Médico creado: carlos@medimicita.co con perfil.");
                    return savedUser;
                });
        if (userRepository.existsByUserEmail("carlos@medimicita.co") && savedMedicoUser.getDoctorProfile() == null) {
            // Este bloque es por si el usuario ya existía pero el perfil no,
            // aunque la lógica orElseGet debería manejar la creación completa si no existe.
            // Podría ser redundante o necesitar ajuste si quieres que se cree el perfil si el usuario ya existe.
            // Por ahora, asumimos que si el usuario se crea, el perfil también.
        }
        // 6. Crear Horarios para el Médico de Prueba (si existe)
        if (savedMedicoUser != null && savedMedicoUser.getUserId() != null && scheduleTemplateRepository != null) {
            createScheduleTemplateIfNotExists(savedMedicoUser, 1, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Lunes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUser, 1, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Lunes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUser, 2, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Martes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUser, 2, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Martes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUser, 3, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Miércoles Mañana
            createScheduleTemplateIfNotExists(savedMedicoUser, 3, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Miércoles Tarde
            createScheduleTemplateIfNotExists(savedMedicoUser, 4, LocalTime.of(8,0),LocalTime.of(12,0)); // Jueves Mañana
            createScheduleTemplateIfNotExists(savedMedicoUser, 4, LocalTime.of(14,0),LocalTime.of(18,0)); // Jueves Tarde
            createScheduleTemplateIfNotExists(savedMedicoUser, 5, LocalTime.of(8,0),LocalTime.of(12,0)); // Viernes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUser, 5, LocalTime.of(14,0),LocalTime.of(18,0)); // Viernes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUser, 6, LocalTime.of(6,0),LocalTime.of(12,0)); // Sábado Mañana
            // Puedes añadir más plantillas de horario aquí para otros días/horas
            // Ejemplo:
            // createScheduleTemplateIfNotExists(savedMedicoUser, 5, LocalTime.of(10, 0), LocalTime.of(16, 0)); // Viernes
        } else if (savedMedicoUser == null || savedMedicoUser.getUserId() == null) {
            log.warn("No se pudo crear horarios para el médico de prueba porque el médico no se encontró o no se guardó correctamente.");
        } else if (scheduleTemplateRepository == null) {
            log.error("DoctorScheduleTemplateRepository no fue inyectado, no se pueden crear horarios.");
        }

        // 7. Crear Usuario Médico (Odontología) de Prueba
        UserEntity savedMedicoUserOdontolgia = userRepository.findByUserEmail("karla@medimicita.co")
                .orElseGet(() -> {
                    UserEntity medicoUser = new UserEntity();
                    medicoUser.setUserIdentificationNumber("2075000001");
                    medicoUser.setUserFirstName("Karla");
                    medicoUser.setUserLastName("Jimenez");
                    medicoUser.setUserBirthDate(LocalDate.of(1995, 4, 20));
                    medicoUser.setUserPhoneNumber("3154226985");
                    medicoUser.setUserEmail("karla@medimicita.co");
                    medicoUser.setUserPasswordHash("$2a$10$jkw6tTTiUgNrzvwprV2ZM.Zl24elFIgc1GSdvrXy8X3dFVQxjKSfa"); // TU HASH
                    medicoUser.setRole(medicoRole);
                    medicoUser.setUserIsActive(true);
                    medicoUser.setUserRequiresPasswordChange(true);
                    UserEntity savedUserO = userRepository.save(medicoUser); // Guardar primero

                    DoctorProfileEntity medicoProfile = new DoctorProfileEntity();
                    medicoProfile.setUser(savedUserO);
                    medicoProfile.setSpecialty(odontologia);
                    medicoProfile.setDoctorProfileOfficeNumber("102");
                    doctorProfileRepository.save(medicoProfile);
                    log.info("Usuario Médico creado: karla@medimicita.co con perfil.");
                    return savedUserO;
                });
        if (userRepository.existsByUserEmail("karla@medimicita.co") && savedMedicoUserOdontolgia.getDoctorProfile() == null) {
            // Este bloque es por si el usuario ya existía pero el perfil no,
            // aunque la lógica orElseGet debería manejar la creación completa si no existe.
            // Podría ser redundante o necesitar ajuste si quieres que se cree el perfil si el usuario ya existe.
            // Por ahora, asumimos que si el usuario se crea, el perfil también.
        }


        // 8. Crear Horarios para el Médico (Odontología) de Prueba (si existe)
        if (savedMedicoUserOdontolgia != null && savedMedicoUserOdontolgia.getUserId() != null && scheduleTemplateRepository != null) {
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 1, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Lunes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 1, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Lunes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 2, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Martes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 2, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Martes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 3, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Miércoles Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 3, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Miércoles Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 4, LocalTime.of(8,0),LocalTime.of(12,0)); // Jueves Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 4, LocalTime.of(14,0),LocalTime.of(18,0)); // Jueves Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 5, LocalTime.of(8,0),LocalTime.of(12,0)); // Viernes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 5, LocalTime.of(14,0),LocalTime.of(18,0)); // Viernes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserOdontolgia, 6, LocalTime.of(6,0),LocalTime.of(12,0)); // Sábado Mañana
        } else if (savedMedicoUserOdontolgia == null || savedMedicoUserOdontolgia.getUserId() == null) {
            log.warn("No se pudo crear horarios para el médico (Odontología) de prueba porque el médico no se encontró o no se guardó correctamente.");
        } else if (scheduleTemplateRepository == null) {
            log.error("DoctorScheduleTemplateRepository Odontología no fue inyectado, no se pueden crear horarios.");
        }


        // 9. Crear Usuario Médico (Odontología) de Prueba
        UserEntity savedMedicoUserGine = userRepository.findByUserEmail("Juana@medimicita.co")
                .orElseGet(() -> {
                    UserEntity medicoUser = new UserEntity();
                    medicoUser.setUserIdentificationNumber("3088000001");
                    medicoUser.setUserFirstName("Juana");
                    medicoUser.setUserLastName("Roa");
                    medicoUser.setUserBirthDate(LocalDate.of(1998, 9, 25));
                    medicoUser.setUserPhoneNumber("3214568476");
                    medicoUser.setUserEmail("Juana@medimicita.co");
                    medicoUser.setUserPasswordHash("$2a$10$jkw6tTTiUgNrzvwprV2ZM.Zl24elFIgc1GSdvrXy8X3dFVQxjKSfa"); // TU HASH
                    medicoUser.setRole(medicoRole);
                    medicoUser.setUserIsActive(true);
                    medicoUser.setUserRequiresPasswordChange(true);
                    UserEntity savedUserG = userRepository.save(medicoUser); // Guardar primero

                    DoctorProfileEntity medicoProfile = new DoctorProfileEntity();
                    medicoProfile.setUser(savedUserG);
                    medicoProfile.setSpecialty(ginecologia);
                    medicoProfile.setDoctorProfileOfficeNumber("102");
                    doctorProfileRepository.save(medicoProfile);
                    log.info("Usuario Médico creado: Juana@medimicita.co con perfil.");
                    return savedUserG;
                });
        if (userRepository.existsByUserEmail("Juana@medimicita.co") && savedMedicoUserGine.getDoctorProfile() == null) {
            // Este bloque es por si el usuario ya existía pero el perfil no,
            // aunque la lógica orElseGet debería manejar la creación completa si no existe.
            // Podría ser redundante o necesitar ajuste si quieres que se cree el perfil si el usuario ya existe.
            // Por ahora, asumimos que si el usuario se crea, el perfil también.
        }


        // 10. Crear Horarios para el Médico (Odontología) de Prueba (si existe)
        if (savedMedicoUserGine != null && savedMedicoUserGine.getUserId() != null && scheduleTemplateRepository != null) {
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 1, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Lunes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 1, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Lunes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 2, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Martes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 2, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Martes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 3, LocalTime.of(8, 0), LocalTime.of(12, 0)); // Miércoles Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 3, LocalTime.of(14, 0), LocalTime.of(18, 0)); // Miércoles Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 4, LocalTime.of(8,0),LocalTime.of(12,0)); // Jueves Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 4, LocalTime.of(14,0),LocalTime.of(18,0)); // Jueves Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 5, LocalTime.of(8,0),LocalTime.of(12,0)); // Viernes Mañana
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 5, LocalTime.of(14,0),LocalTime.of(18,0)); // Viernes Tarde
            createScheduleTemplateIfNotExists(savedMedicoUserGine, 6, LocalTime.of(6,0),LocalTime.of(12,0)); // Sábado Mañana
        } else if (savedMedicoUserGine == null || savedMedicoUserGine.getUserId() == null) {
            log.warn("No se pudo crear horarios para el médico (Ginecologia) de prueba porque el médico no se encontró o no se guardó correctamente.");
        } else if (scheduleTemplateRepository == null) {
            log.error("DoctorScheduleTemplateRepository Ginecologia no fue inyectado, no se pueden crear horarios.");
        }


        // 11. Crear Usuario Paciente de Prueba
        if (!userRepository.existsByUserEmail("ana.perez@example.com")) {
            UserEntity pacienteUser = new UserEntity();
            pacienteUser.setUserIdentificationNumber("1075123002");
            pacienteUser.setUserFirstName("Ana Juliana");
            pacienteUser.setUserLastName("Perez Martinez");
            pacienteUser.setUserBirthDate(LocalDate.of(1992, 8, 20));
            pacienteUser.setUserPhoneNumber("3151112233");
            pacienteUser.setUserEmail("ana@example.com");
            pacienteUser.setUserPasswordHash("$2a$10$v2BEsudrg11uBhm8EEJVOejnA.p4Zddj7E1VUhZENSexiYOh8j35i"); // TU HASH
            pacienteUser.setRole(pacienteRole);
            pacienteUser.setEps(epsSura);
            pacienteUser.setUserTermsAcceptedAt(OffsetDateTime.now());
            pacienteUser.setUserIsActive(true);
            pacienteUser.setUserRequiresPasswordChange(false);
            UserEntity savedPacienteUser = userRepository.save(pacienteUser);

            UserAddressEntity pacienteAddress = new UserAddressEntity();
            pacienteAddress.setUser(savedPacienteUser);
            pacienteAddress.setUserAddressStreetType(StreetTypeEnum.CALLE); // Usar el enum
            pacienteAddress.setUserAddressMainWayNumber("10A");
            pacienteAddress.setUserAddressSecondaryWayNumber("20");
            pacienteAddress.setUserAddressHouseOrBuildingNumber("30");
            pacienteAddress.setUserAddressComplement("Apto 101, Interior 2, Frente al parque");
            pacienteAddress.setUserAddressNeighborhood("Centro Histórico");
            pacienteAddress.setUserAddressMunicipality("Neiva");
            pacienteAddress.setUserAddressDepartment("Huila");
            pacienteAddress.setUserAddressZone(ZoneTypeEnum.URBANA); // Usar el enum
            pacienteAddress.setUserAddressIsCurrent(true);
            userAddressRepository.save(pacienteAddress);
            log.info("Usuario Paciente creado: ana@example.com con dirección.");
        } else {
            log.info("Usuario Paciente ya existe: ana@example.com");
        }

        log.info("Carga de datos iniciales completada.");
    }

    // --- Métodos Helper ---
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

    // Método helper para crear plantillas de horario
    private void createScheduleTemplateIfNotExists(UserEntity doctor, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // Lógica para verificar si ya existe una plantilla idéntica para este doctor, día y horas
        // Esto es opcional, ya que tienes una constraint UNIQUE en la tabla que prevendría duplicados exactos.
        // Si la constraint está, simplemente intentar guardar y dejar que la BD maneje el conflicto es una opción.
        // O puedes hacer una búsqueda previa:
        /*
        Optional<DoctorScheduleTemplateEntity> existing = scheduleTemplateRepository
            .findByDoctorUserAndDoctorScheduleTemplateDayOfWeekAndDoctorScheduleTemplateStartTimeAndDoctorScheduleTemplateEndTime(
                doctor, dayOfWeek, startTime, endTime);
        if (existing.isPresent()) {
            log.info("Plantilla de horario ya existe para Dr. {}, Día: {}, Hora: {}-{}", doctor.getUserId(), dayOfWeek, startTime, endTime);
            return;
        }
        */

        DoctorScheduleTemplateEntity template = new DoctorScheduleTemplateEntity();
        template.setDoctorUser(doctor);
        template.setDoctorScheduleTemplateDayOfWeek(dayOfWeek); // INTEGER (1=Lunes, ..., 7=Domingo)
        template.setDoctorScheduleTemplateStartTime(startTime);
        template.setDoctorScheduleTemplateEndTime(endTime);
        template.setDoctorScheduleTemplateIsActive(true);
        scheduleTemplateRepository.save(template); // Usar el repositorio inyectado
        log.info("Creada plantilla de horario para Dr. {}, Día: {}, Hora: {}-{}", doctor.getUserId(), dayOfWeek, startTime, endTime);
    }
}