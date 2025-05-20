package co.edu.usco.medimicita.controller.web;

import co.edu.usco.medimicita.entity.UserAddressEntity;
import co.edu.usco.medimicita.entity.UserEntity;
import co.edu.usco.medimicita.service.UserService; // Asegúrate que el import sea correcto
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Principal no es necesario si usas Authentication
// import java.security.Principal;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    // (1) DECLARAR EL CAMPO DEL SERVICIO
    private final UserService userService;

    // (2) INYECTAR EL SERVICIO VÍA CONSTRUCTOR (ya lo tenías, pero asegúrate que el campo de arriba exista)
    @Autowired
    public PatientController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String patientDashboard(Model model, Authentication authentication) {
        log.info("Accediendo al método patientDashboard.");
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            log.info("Usuario autenticado en dashboard: {}", userDetails.getUsername());
            model.addAttribute("username", userDetails.getUsername());
        } else {
            log.warn("Authentication es null o no autenticado en patientDashboard.");
            model.addAttribute("username", "Usuario Desconocido");
        }
        model.addAttribute("pageTitle", "Dashboard del Paciente");
        return "patient/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        log.info("Accediendo al método viewProfile.");
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            log.warn("Intento de acceso a perfil sin autenticación válida o UserDetails no disponible.");
            return "redirect:/login";
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.info("Buscando perfil para usuario: {}", userDetails.getUsername());

        UserEntity currentUser = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado en la base de datos para el email: {}", userDetails.getUsername());
                    return new RuntimeException("Usuario no encontrado: " + userDetails.getUsername()); // Considera una excepción personalizada
                });

        log.info("Perfil encontrado para: {}", currentUser.getUserEmail());
        model.addAttribute("user", currentUser);
        model.addAttribute("pageTitle", "Mi Perfil");

        // --- NUEVA LÓGICA PARA OBTENER LA DIRECCIÓN ACTUAL ---
        UserAddressEntity currentAddress = null;
        if (currentUser.getUserAddresses() != null && !currentUser.getUserAddresses().isEmpty()) {
            currentAddress = currentUser.getUserAddresses().stream()
                    .filter(address -> address.getUserAddressIsCurrent() != null && address.getUserAddressIsCurrent())
                    .findFirst()
                    .orElse(null); // Si no hay ninguna marcada como actual, currentAddress será null
        }
        model.addAttribute("currentAddress", currentAddress); // Pasar la dirección actual (o null) al modelo

        // Opcional: Pasar todas las direcciones si quieres listarlas todas
        // model.addAttribute("allAddresses", currentUser.getUserAddresses());

        return "patient/profile";
    }

    // Aquí añadirás más métodos para el perfil, citas, etc.
}