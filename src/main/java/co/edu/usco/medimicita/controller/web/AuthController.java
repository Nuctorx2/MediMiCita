package co.edu.usco.medimicita.controller.web;

import jakarta.servlet.http.HttpServletRequest; // Para la redirección basada en rol
import org.springframework.security.core.Authentication; // Para obtener detalles del usuario autenticado
import org.springframework.security.core.context.SecurityContextHolder; // Para obtener el contexto de seguridad
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
            return "redirect:/default-success"; // Redirigir si ya está logueado
        }
        return "login"; // Devuelve el nombre de la plantilla Thymeleaf (login.html)
    }

    // (4) Método para la redirección después de un login exitoso
    @GetMapping("/default-success")
    public String defaultSuccessPage(HttpServletRequest request) {
        // HttpServletRequest.isUserInRole espera el nombre del rol SIN el prefijo "ROLE_"
        if (request.isUserInRole("ADMINISTRADOR")) {
            return "redirect:/admin/dashboard"; // Asegúrate que esta ruta exista o la crearás
        } else if (request.isUserInRole("MEDICO")) {
            return "redirect:/doctor/dashboard"; // Asegúrate que esta ruta exista o la crearás
        } else if (request.isUserInRole("PACIENTE")) {
            return "redirect:/patient/dashboard"; // Asegúrate que esta ruta exista o la crearás
        }
        // Fallback si el rol no se reconoce o para usuarios sin un dashboard específico
        // Podría ser la página de inicio o una página de error/perfil genérico.
        return "redirect:/"; // O "redirect:/user/profile" o alguna página por defecto
    }

    // (Opcional) Página de inicio simple
    @GetMapping("/")
    public String homePage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/default-success"; // Si está logueado, redirigir a su dashboard
        }
        return "index"; // Nombre de tu plantilla para la página de inicio (index.html)
        // Esta página puede tener enlaces a /login y /register
    }

    // (Opcional pero recomendado) Página de acceso denegado
    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "error/403"; // Nombre de tu plantilla para error 403 (access_denied.html o 403.html)
    }
}