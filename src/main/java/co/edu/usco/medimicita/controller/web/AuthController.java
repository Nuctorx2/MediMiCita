package co.edu.usco.medimicita.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // (1) Anota la clase como un controlador de Spring MVC
public class AuthController {

    // (2) Método para mostrar la página de login
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Usuario o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Has cerrado sesión exitosamente.");
        }
        // (3) Verificar si el usuario ya está autenticado para no mostrar el login de nuevo
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/default-success";
        }
        return "login";
    }

    // (4) Método para la redirección después de un login exitoso
    @GetMapping("/default-success")
    public String defaultSuccessPage(HttpServletRequest request) {
        // HttpServletRequest.isUserInRole espera el nombre del rol SIN el prefijo "ROLE_"
        if (request.isUserInRole("ADMINISTRADOR")) {
            return "redirect:/admin/dashboard";
        } else if (request.isUserInRole("MEDICO")) {
            return "redirect:/doctor/dashboard";
        } else if (request.isUserInRole("PACIENTE")) {
            return "redirect:/patient/dashboard";
        }

        return "redirect:/";
    }


    @GetMapping("/")
    public String homePage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            model.addAttribute("isAuthenticated", true);
        } else {
            model.addAttribute("isAuthenticated", false);
        }
        model.addAttribute("pageTitle", "Bienvenido a MediMiCita");
        return "index"; // Renderiza index.html
    }


    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "error/403";
    }


}