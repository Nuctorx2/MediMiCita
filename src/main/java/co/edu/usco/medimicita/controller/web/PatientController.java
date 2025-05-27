package co.edu.usco.medimicita.controller.web;


import co.edu.usco.medimicita.entity.AppointmentEntity;
import co.edu.usco.medimicita.enums.StreetTypeEnum;
import co.edu.usco.medimicita.enums.ZoneTypeEnum;
import java.util.List;
import java.util.Optional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import co.edu.usco.medimicita.exception.AppointmentCancellationException;
import co.edu.usco.medimicita.service.AppointmentService;
import java.time.YearMonth;
import java.util.stream.Collectors;
import co.edu.usco.medimicita.exception.SlotUnavailableException;
import co.edu.usco.medimicita.util.ClinicConstants;
import co.edu.usco.medimicita.service.AvailabilityService;
import co.edu.usco.medimicita.dto.AvailableSlotDto;
import co.edu.usco.medimicita.service.SpecialtyService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Necesario
import org.springframework.security.core.context.SecurityContextHolder;
import co.edu.usco.medimicita.dto.PatientProfileUpdateDto;
import co.edu.usco.medimicita.exception.EmailAlreadyExistsException;
import co.edu.usco.medimicita.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import co.edu.usco.medimicita.entity.UserAddressEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import co.edu.usco.medimicita.entity.SpecialtyEntity;
import co.edu.usco.medimicita.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientController.class);

    private final UserService userService;
    private final SpecialtyService specialtyService;
    private final AvailabilityService availabilityService;
    private final AppointmentService appointmentService;

    @GetMapping("/dashboard")
    public String patientDashboard(Model model, Authentication authentication) {
        LOGGER.info("Accediendo al método patientDashboard.");
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            LOGGER.info("Usuario autenticado en dashboard: {}", userDetails.getUsername());
            model.addAttribute("username", userDetails.getUsername());
        } else {
            LOGGER.warn("Authentication es null o no autenticado en patientDashboard.");
            model.addAttribute("username", "Usuario Desconocido");
        }
        model.addAttribute("pageTitle", "Dashboard del Paciente");
        return "patient/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        LOGGER.info("Accediendo al método viewProfile.");
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            LOGGER.warn("Intento de acceso a perfil sin autenticación válida o UserDetails no disponible.");
            return "redirect:/login";
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        LOGGER.info("Buscando perfil para usuario: {}", userDetails.getUsername());

        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    LOGGER.error("Usuario no encontrado en la base de datos para el email: {}", userDetails.getUsername());
                    return new ResourceNotFoundException("Usuario no encontrado: " + userDetails.getUsername()); // Considera una excepción personalizada
                });

        LOGGER.info("Perfil encontrado para: {}", currentUser.getUserEmail());
        model.addAttribute("user", currentUser);
        model.addAttribute("pageTitle", "Mi Perfil");

        // --- NUEVA LÓGICA PARA OBTENER LA DIRECCIÓN ACTUAL ---
        UserAddressEntity currentAddress = null;
        if (currentUser.getUserAddresses() != null && !currentUser.getUserAddresses().isEmpty()) {
            currentAddress = currentUser.getUserAddresses().stream()
                    .filter(address -> address.getUserAddressIsCurrent() != null && address.getUserAddressIsCurrent())
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("currentAddress", currentAddress);


        return "patient/profile";
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model, Authentication authentication) {
        LOGGER.info("Accediendo a showEditProfileForm");
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            return "redirect:/login";
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para edición: " + userDetails.getUsername()));

        PatientProfileUpdateDto profileDto = new PatientProfileUpdateDto();
        profileDto.setUserPhoneNumber(currentUser.getUserPhoneNumber());
        profileDto.setUserEmail(currentUser.getUserEmail());

        UserAddressEntity currentAddress = currentUser.getUserAddresses().stream()
                .filter(addr -> addr.getUserAddressIsCurrent() != null && addr.getUserAddressIsCurrent())
                .findFirst().orElse(null);

        if (currentAddress != null) {
            profileDto.setCurrentAddressId(currentAddress.getUserAddressId());
            profileDto.setUserAddressStreetType(currentAddress.getUserAddressStreetType());
            profileDto.setUserAddressMainWayNumber(currentAddress.getUserAddressMainWayNumber());
            profileDto.setUserAddressSecondaryWayNumber(currentAddress.getUserAddressSecondaryWayNumber());
            profileDto.setUserAddressHouseOrBuildingNumber(currentAddress.getUserAddressHouseOrBuildingNumber());
            profileDto.setUserAddressComplement(currentAddress.getUserAddressComplement());
            profileDto.setUserAddressNeighborhood(currentAddress.getUserAddressNeighborhood());
            profileDto.setUserAddressMunicipality(currentAddress.getUserAddressMunicipality());
            profileDto.setUserAddressDepartment(currentAddress.getUserAddressDepartment());
            profileDto.setUserAddressZone(currentAddress.getUserAddressZone());
        }

        model.addAttribute("profileUpdateDto", profileDto);
        model.addAttribute("streetTypes", StreetTypeEnum.values());
        model.addAttribute("zoneTypes", ZoneTypeEnum.values());
        model.addAttribute("pageTitle", "Editar Mi Perfil");
        return "patient/edit-profile";
    }

    // --- AGENDAR CITA - PASO 1: SELECCIÓN DE ESPECIALIDAD ---
    @GetMapping("/appointments/new/select-specialty")
    public String showSelectSpecialtyForm(Model model) {
        LOGGER.info("Mostrando formulario de selección de especialidad.");
        // (6) AHORA PUEDES USAR specialtyService
        List<SpecialtyEntity> specialties = specialtyService.getAllActiveSpecialties();
        model.addAttribute("specialties", specialties);
        model.addAttribute("pageTitle", "Agendar Cita - Seleccionar Especialidad");
        model.addAttribute("progressStep", 1);
        return "patient/appointment-step1-specialty";
    }

    @PostMapping("/appointments/new/select-specialty")
    public String processSelectSpecialty(@RequestParam("specialtyId") Integer specialtyId,
                                         RedirectAttributes redirectAttributes) {
        LOGGER.info("Procesando selección de especialidad con ID: {}", specialtyId);
        // (6) AHORA PUEDES USAR specialtyService
        Optional<SpecialtyEntity> specialtyOpt = specialtyService.findById(specialtyId);

        if (specialtyOpt.isEmpty() || !specialtyOpt.get().getSpecialtyIsActive()) {
            LOGGER.warn("Intento de seleccionar especialidad no válida o inactiva. ID: {}", specialtyId);
            redirectAttributes.addFlashAttribute("errorMessage", "Especialidad no válida o no disponible seleccionada.");
            return "redirect:/patient/appointments/new/select-specialty";
        }

        SpecialtyEntity selectedSpecialty = specialtyOpt.get();
        LOGGER.info("Especialidad seleccionada: {}", selectedSpecialty.getSpecialtyName());

        redirectAttributes.addFlashAttribute("selectedSpecialtyId", selectedSpecialty.getSpecialtyId());
        redirectAttributes.addFlashAttribute("selectedSpecialtyName", selectedSpecialty.getSpecialtyName());
        redirectAttributes.addFlashAttribute("selectedSpecialtyDuration", selectedSpecialty.getSpecialtyDefaultDurationMinutes());

        return "redirect:/patient/appointments/new/select-slot"; // Redirige al Paso 2
    }

    // --- AGENDAR CITA - PASO 2: SELECCIÓN DE FECHA/HORA/MÉDICO ---
    // (Este método lo crearemos después)
    @GetMapping("/appointments/new/select-slot")
    public String showSelectSlotForm(Model model,
                                     @ModelAttribute("selectedSpecialtyId") Integer specialtyId,
                                     @ModelAttribute("selectedSpecialtyName") String specialtyName,
                                     @ModelAttribute("selectedSpecialtyDuration") Integer specialtyDuration) {
        LOGGER.info("Mostrando formulario de selección de slot para especialidad ID: {}, Nombre: {}, Duración: {} min",
                specialtyId, specialtyName, specialtyDuration);

        if (specialtyId == null || specialtyName == null || specialtyDuration == null) {
            LOGGER.warn("Información de especialidad incompleta para el paso 2. Redirigiendo a paso 1.");
            // Podrías añadir un mensaje flash aquí también si lo deseas.
            return "redirect:/patient/appointments/new/select-specialty";
        }

        model.addAttribute("pageTitle", "Agendar Cita - Seleccionar Horario para " + specialtyName);
        model.addAttribute("progressStep", 2);
        model.addAttribute("specialtyId", specialtyId);
        model.addAttribute("specialtyName", specialtyName);
        model.addAttribute("specialtyDuration", specialtyDuration);

        // 2. Obtener días disponibles para el mes actual
        YearMonth currentOrInitialMonth = YearMonth.now();
        List<LocalDate> availableDatesInMonth = availabilityService.getAvailableDates(specialtyId, Optional.empty(), currentOrInitialMonth);

        model.addAttribute("currentYearMonth", currentOrInitialMonth.toString()); // Ej: "2025-05"
        List<String> availableDateStrings = availableDatesInMonth.stream().map(LocalDate::toString).collect(Collectors.toList());
        model.addAttribute("availableDates", availableDateStrings); // Lista de LocalDate
        LOGGER.info("Fechas disponibles para {} ({}): {}", currentOrInitialMonth, specialtyName, availableDateStrings);

        return "patient/appointment-step2-slot";
    }

    @GetMapping("/appointments/new/confirm")
    public String showAppointmentConfirmationForm(Model model,
                                                  // @ModelAttribute para recoger flash attributes
                                                  @ModelAttribute("confirmSpecialtyId") Integer specialtyId,
                                                  @ModelAttribute("confirmSpecialtyName") String specialtyName,
                                                  @ModelAttribute("confirmDoctorId") Long doctorId,
                                                  @ModelAttribute("confirmDoctorName") String doctorName,
                                                  @ModelAttribute("confirmDate") String dateStr, // YYYY-MM-DD
                                                  @ModelAttribute("confirmTime") String timeStr, // HH:MM o HH:MM:SS
                                                  @ModelAttribute("confirmDurationMinutes") Integer durationMinutes,
                                                  @ModelAttribute("confirmOfficeNumber") String officeNumber,
                                                  RedirectAttributes redirectAttributes) {

        LOGGER.info("Mostrando página de confirmación para: SpecID={}, DrID={}, Date={}, Time={}",
                specialtyId, doctorId, dateStr, timeStr);

        // Validar que los atributos flash clave estén presentes
        if (specialtyId == null || doctorId == null || dateStr == null || timeStr == null || durationMinutes == null) {
            LOGGER.warn("Datos incompletos para la página de confirmación. Redirigiendo al paso 1.");
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo cargar el resumen de la cita. Por favor, intente de nuevo desde el inicio.");
            return "redirect:/patient/appointments/new/select-specialty";
        }

        model.addAttribute("pageTitle", "Confirmar Cita para " + specialtyName);
        model.addAttribute("progressStep", 3);

        // Pasar todos los datos recibidos al modelo para que la vista los use
        model.addAttribute("specialtyId", specialtyId);
        model.addAttribute("specialtyName", specialtyName);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("doctorName", doctorName);
        model.addAttribute("selectedDate", dateStr); // Para los inputs ocultos del form de confirmación
        model.addAttribute("selectedTime", timeStr); // Para los inputs ocultos del form de confirmación
        model.addAttribute("durationMinutes", durationMinutes);
        model.addAttribute("officeNumber", officeNumber);

        // Formatear fecha y hora para visualización amigable
        try {
            LocalDate parsedDate = LocalDate.parse(dateStr);
            LocalTime parsedTime = LocalTime.parse(timeStr);
            model.addAttribute("displayDate", parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new java.util.Locale("es", "ES"))));
            model.addAttribute("displayTime", parsedTime.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
        } catch (DateTimeParseException e) {
            LOGGER.error("Error formateando fecha/hora para confirmación: {} {}", dateStr, timeStr, e);
            model.addAttribute("displayDate", dateStr); // Fallback
            model.addAttribute("displayTime", timeStr); // Fallback
        }

        return "patient/appointment-step3-confirm";
    }
    // --- PÁGINA "MIS CITAS" ---
    @GetMapping("/appointments")
    public String viewMyApointments(Model model, Authentication authentication,
                                    @ModelAttribute("successMessage") String successMessage,
                                    @ModelAttribute("cancelSuccessMessage") String cancelSuccessMessage,
                                    @ModelAttribute("cancelErrorMessage") String cancelErrorMessage) {
        LOGGER.info("Accediendo a la página Mis Citas.");
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            return "redirect:/login";
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userDetails.getUsername()));

        List<AppointmentEntity> futureAppointments = appointmentService.findFutureAppointmentsForPatient(currentUser.getUserId());
        List<AppointmentEntity> pastAppointments = appointmentService.findPastAppointmentsForPatient(currentUser.getUserId());

        model.addAttribute("futureAppointments", futureAppointments);
        model.addAttribute("pastAppointments", pastAppointments);
        model.addAttribute("pageTitle", "Mis Citas");

        // Pasar mensajes flash si existen
        if (successMessage != null && !successMessage.isEmpty()) {
            model.addAttribute("successMessage", successMessage);
        }
        if (cancelSuccessMessage != null && !cancelSuccessMessage.isEmpty()) {
            model.addAttribute("successMessage", cancelSuccessMessage); // Usar el mismo atributo o uno diferente
        }
        if (cancelErrorMessage != null && !cancelErrorMessage.isEmpty()) {
            model.addAttribute("errorMessage", cancelErrorMessage);
        }


        return "patient/my-appointments"; // Nueva plantilla Thymeleaf
    }

    @PostMapping("/profile/edit")
    public String processEditProfileForm(
            @Valid @ModelAttribute("profileUpdateDto") PatientProfileUpdateDto profileUpdateDto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        UserEntity originalUser = userService.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario original no encontrado durante la actualización"));

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            return "redirect:/login";
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userDetails.getUsername()));

        // Re-validar el email para unicidad si ha cambiado y no es el del usuario actual
        if (!originalUser.getUserEmail().equalsIgnoreCase(profileUpdateDto.getUserEmail()) &&
                userService.findByEmail(profileUpdateDto.getUserEmail()).filter(u -> !u.getUserId().equals(originalUser.getUserId())).isPresent()) {
            bindingResult.rejectValue("userEmail", "email.exists", "Este email ya está registrado por otro usuario.");
        }


        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar Mi Perfil");
            return "patient/edit-profile";
        }

        try {
            // (1) Llama al servicio para actualizar. El servicio devuelve el UserEntity actualizado.
            UserEntity updatedUser = userService.updatePatientProfile(originalUser.getUserId(), profileUpdateDto);

            // (2) Actualizar el Principal en el SecurityContext si el email (username) cambió
            // O si otros detalles relevantes para UserDetails cambiaron.
            // Obtenemos el UserDetails actual del objeto Authentication original.
            UserDetails originalUserDetails = (UserDetails) authentication.getPrincipal();

            // Si el email (que usamos como username en UserDetails) ha cambiado
            if (!originalUserDetails.getUsername().equalsIgnoreCase(updatedUser.getUserEmail())) {
                // Crear un nuevo UserDetails basado en el usuario actualizado
                // Esto es similar a lo que haces en UserServiceImpl.loadUserByUsername()
                // pero sin la carga de contraseña, ya que el usuario ya está autenticado.
                // Los roles/authorities generalmente no cambian en una actualización de perfil de paciente.
                UserDetails newPrincipal = new org.springframework.security.core.userdetails.User(
                        updatedUser.getUserEmail(),
                        updatedUser.getUserPasswordHash(), // Aunque no se usa para re-autenticar aquí, es parte del contrato
                        updatedUser.getUserIsActive(),
                        true, // accountNonExpired
                        true, // credentialsNonExpired
                        true, // accountNonLocked
                        originalUserDetails.getAuthorities() // Reutilizar las autoridades existentes
                );

                // Crear un nuevo token de autenticación con el nuevo principal y las mismas autoridades
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        newPrincipal,
                        authentication.getCredentials(), // Usualmente null o la contraseña si es relevante
                        newPrincipal.getAuthorities()
                );

                // Establecer la nueva autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                LOGGER.info("Principal de Spring Security actualizado para el nuevo email: {}", updatedUser.getUserEmail());
            }


            redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado exitosamente.");
            return "redirect:/patient/profile";
        } catch (EmailAlreadyExistsException e) {
            bindingResult.rejectValue("userEmail", "email.exists", e.getMessage());
            model.addAttribute("pageTitle", "Editar Mi Perfil");
            return "patient/edit-profile";
        } catch (Exception e) {
            LOGGER.error("Error al actualizar perfil para usuario: " + originalUser.getUserId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar el perfil. Intente de nuevo.");
            return "redirect:/patient/profile/edit";
        }

    }

    @PostMapping("/appointments/new/select-slot")
    public String processSelectSlot(
            @RequestParam("specialtyId") Integer specialtyId,
            @RequestParam("selectedDate") String selectedDateStr, // Viene como "YYYY-MM-DD"
            @RequestParam("selectedTime") String selectedTimeStr, // Viene como "HH:MM" o "HH:MM:SS"
            @RequestParam("selectedDoctorId") Long selectedDoctorId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        LOGGER.info("Procesando slot seleccionado: SpecialtyID={}, Date={}, Time={}, DoctorID={}",
                specialtyId, selectedDateStr, selectedTimeStr, selectedDoctorId);

        // Validar que los datos necesarios estén presentes
        if (specialtyId == null || selectedDateStr == null || selectedDateStr.isEmpty() ||
                selectedTimeStr == null || selectedTimeStr.isEmpty() || selectedDoctorId == null) {
            LOGGER.warn("Datos incompletos recibidos para procesar el slot.");
            redirectAttributes.addFlashAttribute("errorMessage", "Información incompleta. Por favor, intente de nuevo.");
            // Redirigir de nuevo al paso 1 o 2 con los datos de especialidad si es posible
            // Para redirigir al paso 2, necesitaríamos el nombre y duración de la especialidad.
            // Por simplicidad, redirigimos al paso 1.
            return "redirect:/patient/appointments/new/select-specialty";
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity patientUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + userDetails.getUsername()));

        SpecialtyEntity specialty = specialtyService.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + specialtyId));

        UserEntity doctor = userService.findById(selectedDoctorId) // Asumiendo que userService puede encontrar cualquier usuario por ID
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado con ID: " + selectedDoctorId));


        LocalDate selectedDate;
        LocalTime selectedTime;
        OffsetDateTime slotStartDateTime;
        int durationMinutes = specialty.getSpecialtyDefaultDurationMinutes();

        try {
            selectedDate = LocalDate.parse(selectedDateStr); // Parsea "YYYY-MM-DD"
            selectedTime = LocalTime.parse(selectedTimeStr); // Parsea "HH:MM" o "HH:MM:SS"
            slotStartDateTime = OffsetDateTime.of(selectedDate, selectedTime, ClinicConstants.getClinicZoneOffset());
        } catch (DateTimeParseException e) {
            LOGGER.error("Error al parsear fecha u hora: {} , {}", selectedDateStr, selectedTimeStr, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Formato de fecha u hora inválido.");
            // Redirigir de nuevo al paso 2, pasando datos de especialidad
            redirectAttributes.addFlashAttribute("selectedSpecialtyId", specialtyId);
            redirectAttributes.addFlashAttribute("selectedSpecialtyName", specialty.getSpecialtyName());
            redirectAttributes.addFlashAttribute("selectedSpecialtyDuration", durationMinutes);
            return "redirect:/patient/appointments/new/select-slot";
        }

        // CRUCIAL: Doble verificación de disponibilidad en el backend
        if (!availabilityService.isSlotActuallyAvailable(selectedDoctorId, slotStartDateTime, durationMinutes)) {
            LOGGER.warn("CONFIRMACIÓN FALLIDA: Slot ya no disponible para DrID={}, Inicio={}", selectedDoctorId, slotStartDateTime);
            redirectAttributes.addFlashAttribute("errorMessage", "El horario seleccionado ya no está disponible. Por favor, elija otro.");
            // Redirigir de nuevo al paso 2, pasando datos de especialidad
            redirectAttributes.addFlashAttribute("selectedSpecialtyId", specialtyId);
            redirectAttributes.addFlashAttribute("selectedSpecialtyName", specialty.getSpecialtyName());
            redirectAttributes.addFlashAttribute("selectedSpecialtyDuration", durationMinutes);
            return "redirect:/patient/appointments/new/select-slot";
        }

        // Si el slot está disponible, pasar todos los datos al Paso 3 (Confirmación)
        redirectAttributes.addFlashAttribute("confirmSpecialtyId", specialtyId);
        redirectAttributes.addFlashAttribute("confirmSpecialtyName", specialty.getSpecialtyName());
        redirectAttributes.addFlashAttribute("confirmDoctorId", selectedDoctorId);
        redirectAttributes.addFlashAttribute("confirmDoctorName", doctor.getUserFirstName() + " " + doctor.getUserLastName());
        redirectAttributes.addFlashAttribute("confirmDate", selectedDate.toString()); // "YYYY-MM-DD"
        redirectAttributes.addFlashAttribute("confirmTime", selectedTime.toString()); // "HH:MM" o "HH:MM:SS"
        redirectAttributes.addFlashAttribute("confirmDurationMinutes", durationMinutes);
        // El consultorio del médico se podría obtener aquí si es necesario: doctor.getDoctorProfile().getDoctorProfileOfficeNumber()
        if (doctor.getDoctorProfile() != null && doctor.getDoctorProfile().getDoctorProfileOfficeNumber() != null) {
            redirectAttributes.addFlashAttribute("confirmOfficeNumber", doctor.getDoctorProfile().getDoctorProfileOfficeNumber());
        } else {
            redirectAttributes.addFlashAttribute("confirmOfficeNumber", "No especificado");
        }


        LOGGER.info("Slot validado. Redirigiendo a la página de confirmación.");
        return "redirect:/patient/appointments/new/confirm";
    }

    // --- AGENDAR CITA - PASO 3: PROCESAR CONFIRMACIÓN Y GUARDAR CITA ---
    @PostMapping("/appointments/new/confirm")
    public String processAppointmentConfirmation(
            // Los parámetros deben coincidir con los names de los inputs ocultos del formulario
            @RequestParam("specialtyId") Integer specialtyId,
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("selectedDate") String dateStr,    // "YYYY-MM-DD"
            @RequestParam("selectedTime") String timeStr,    // "HH:MM" o "HH:MM:SS"
            @RequestParam("durationMinutes") Integer durationMinutes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        LOGGER.info("Procesando confirmación de cita: SpecID={}, DrID={}, Date={}, Time={}, Duration={}",
                specialtyId, doctorId, dateStr, timeStr, durationMinutes);

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            LOGGER.warn("Intento de confirmar cita sin autenticación válida.");
            return "redirect:/login";
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity patientUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado para confirmar cita: " + userDetails.getUsername()));

        // Validar datos básicos de nuevo (aunque ya deberían venir bien)
        if (specialtyId == null || doctorId == null || dateStr == null || dateStr.isEmpty() ||
                timeStr == null || timeStr.isEmpty() || durationMinutes == null) {
            LOGGER.error("Datos incompletos recibidos al confirmar la cita.");
            redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error con los datos de la cita. Por favor, intente de nuevo.");
            return "redirect:/patient/appointments/new/select-specialty"; // Volver al inicio del flujo
        }

        OffsetDateTime startDateTime;
        try {
            LocalDate parsedDate = LocalDate.parse(dateStr);
            LocalTime parsedTime = LocalTime.parse(timeStr);
            startDateTime = OffsetDateTime.of(parsedDate, parsedTime, ClinicConstants.getClinicZoneOffset());
        } catch (DateTimeParseException e) {
            LOGGER.error("Error al parsear fecha/hora en la confirmación final: {} , {}", dateStr, timeStr, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Formato de fecha u hora inválido al confirmar.");
            // Podríamos intentar pasar datos de especialidad de nuevo al paso 2, pero es más complejo
            // si no los tenemos todos aquí. Por ahora, al paso 1.
            return "redirect:/patient/appointments/new/select-specialty";
        }

        try {
            appointmentService.scheduleNewAppointment(
                    patientUser.getUserId(),
                    specialtyId,
                    doctorId,
                    startDateTime,
                    durationMinutes
            );
            redirectAttributes.addFlashAttribute("successMessage", "¡Tu cita ha sido agendada exitosamente!");
            return "redirect:/patient/appointments"; // Redirigir a la página "Mis Citas"
        } catch (SlotUnavailableException e) {
            LOGGER.warn("Error al agendar cita (Slot no disponible): {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Redirigir de nuevo al paso 2, pasando los datos de la especialidad
            // Para esto, necesitamos el nombre y duración de la especialidad, que no vienen del form.
            // Podríamos recuperarlos o simplificar y redirigir al paso 1.
            // Para una mejor UX, sería ideal volver al paso 2 con la especialidad ya seleccionada.
            // Vamos a intentar recuperar la especialidad para la redirección:
            SpecialtyEntity specialty = specialtyService.findById(specialtyId)
                    .orElse(null); // Manejar si no se encuentra
            if (specialty != null) {
                redirectAttributes.addFlashAttribute("selectedSpecialtyId", specialty.getSpecialtyId());
                redirectAttributes.addFlashAttribute("selectedSpecialtyName", specialty.getSpecialtyName());
                redirectAttributes.addFlashAttribute("selectedSpecialtyDuration", specialty.getSpecialtyDefaultDurationMinutes());
            }
            return "redirect:/patient/appointments/new/select-slot";
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            LOGGER.error("Error de datos al agendar cita: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error en los datos para agendar la cita: " + e.getMessage());
            return "redirect:/patient/appointments/new/select-specialty";
        } catch (Exception e) {
            LOGGER.error("Error inesperado al agendar la cita:", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error inesperado al agendar tu cita. Por favor, intenta más tarde.");
            return "redirect:/patient/appointments/new/select-specialty";
        }
    }

    // --- CANCELAR CITA ---
    @PostMapping("/appointments/cancel/{appointmentId}")
    public String cancelAppointment(@PathVariable("appointmentId") Long appointmentId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        LOGGER.info("Solicitud para cancelar cita ID: {}", appointmentId);

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            LOGGER.warn("Intento de cancelar cita sin autenticación válida.");
            return "redirect:/login";
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userDetails.getUsername()));

        try {
            appointmentService.cancelPatientAppointment(appointmentId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("cancelSuccessMessage", "Tu cita ha sido cancelada exitosamente.");
        } catch (ResourceNotFoundException | AppointmentCancellationException e) {
            LOGGER.warn("Error al cancelar cita ID {}: {}", appointmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("cancelErrorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error inesperado al cancelar cita ID {}:", appointmentId, e);
            redirectAttributes.addFlashAttribute("cancelErrorMessage", "Ocurrió un error inesperado al intentar cancelar tu cita.");
        }

        return "redirect:/patient/appointments";
    }
}