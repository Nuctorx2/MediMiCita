package co.edu.usco.medimicita.controller.web;


import co.edu.usco.medimicita.enums.StreetTypeEnum;
import co.edu.usco.medimicita.enums.ZoneTypeEnum;
import java.util.List;
import java.util.Optional;

import java.time.LocalDate; // (AÑADIR IMPORT)
import java.time.YearMonth; // (AÑADIR IMPORT)
import java.util.stream.Collectors;

import co.edu.usco.medimicita.service.AvailabilityService; // (AÑADIR IMPORT)
import co.edu.usco.medimicita.dto.AvailableSlotDto; // (AÑADIR IMPORT, aunque no se usa directamente en este método GET)
import co.edu.usco.medimicita.service.SpecialtyService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Necesario
import org.springframework.security.core.context.SecurityContextHolder;
import co.edu.usco.medimicita.dto.PatientProfileUpdateDto;
import co.edu.usco.medimicita.exception.EmailAlreadyExistsException;
import co.edu.usco.medimicita.exception.ResourceNotFoundException;
import jakarta.validation.Valid; // Para validación
import org.springframework.validation.BindingResult; // Para resultados de validación
import org.springframework.web.bind.annotation.ModelAttribute; // Para el DTO del formulario
import org.springframework.web.bind.annotation.PostMapping; // Para el POST
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import co.edu.usco.medimicita.entity.UserAddressEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import co.edu.usco.medimicita.entity.SpecialtyEntity;
import co.edu.usco.medimicita.service.UserService; // Asegúrate que el import sea correcto
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

    // (1) DECLARAR EL CAMPO DEL SERVICIO
    private final UserService userService;
    private final SpecialtyService specialtyService;
    private final AvailabilityService availabilityService;

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

        // 1. Obtener médicos disponibles para esta especialidad (opcional)
        //    Por ahora, asumiremos que el filtro de médico se maneja en el frontend
        //    o que se buscan slots para "todos los médicos de la especialidad".
        //    Si quieres listar médicos:
//            List<UserEntity> availableDoctors = userService.findActiveDoctorsBySpecialty(specialtyId);
//            model.addAttribute("availableDoctors", availableDoctors);

        // 2. Obtener días disponibles para el mes actual
        YearMonth currentOrInitialMonth = YearMonth.now(); // O el mes que quieras mostrar por defecto
        List<LocalDate> availableDatesInMonth = availabilityService.getAvailableDates(specialtyId, Optional.empty(), currentOrInitialMonth);

        model.addAttribute("currentYearMonth", currentOrInitialMonth.toString()); // Ej: "2025-05"
        List<String> availableDateStrings = availableDatesInMonth.stream().map(LocalDate::toString).collect(Collectors.toList());
        model.addAttribute("availableDates", availableDateStrings); // Lista de LocalDate
        LOGGER.info("Fechas disponibles para {} ({}): {}", currentOrInitialMonth, specialtyName, availableDateStrings);

        // 3. (Opcional) Pasar una lista inicial de slots si se selecciona una fecha por defecto
        //    o si quieres que el primer día disponible ya muestre sus slots.
        //    Por ahora, dejaremos que el JavaScript solicite los slots al seleccionar un día.

        return "patient/appointment-step2-slot"; // Nueva plantilla que crearemos
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
}