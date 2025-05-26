package co.edu.usco.medimicita.controller.advice;

import co.edu.usco.medimicita.entity.UserEntity;
import co.edu.usco.medimicita.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalControllerAdvice.class);
    private final UserService userService;

    @Autowired // (2) Inyectar UserService
    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    // (3) Este método se ejecutará antes de los métodos del controlador y añadirá 'userFullName' al Model
    @ModelAttribute("userFullName")
    public String addUserFullNameToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            // El principal puede ser UserDetails o a veces solo el nombre de usuario (String)
            Object principal = authentication.getPrincipal();
            String username;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }

            try {
                Optional<UserEntity> userOpt = userService.findByEmail(username); // Asumiendo que el username es el email
                if (userOpt.isPresent()) {
                    UserEntity currentUser = userOpt.get();
                    return currentUser.getUserFirstName() + " " + currentUser.getUserLastName();
                } else {
                    LOGGER.warn("No se encontró UserEntity para el username: {}", username);
                    return username; // Fallback al username si no se encuentra el UserEntity completo
                }
            } catch (Exception e) {
                LOGGER.error("Error al obtener UserEntity para el username: {}", username, e);
                return username; // Fallback en caso de error
            }
        }
        return null; // O un valor por defecto como "Invitado" si prefieres
    }

    // (4) Este método se ejecutará y añadirá 'userRoleName' al Model
    @ModelAttribute("userRoleName")
    public String addUserRoleNameToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities != null && !authorities.isEmpty()) {
                // Tomar la primera autoridad y quitar el prefijo "ROLE_"
                String authority = authorities.iterator().next().getAuthority();
                if (authority.startsWith("ROLE_")) {
                    return authority.substring(5); // Quita "ROLE_"
                }
                return authority; // Si no tiene el prefijo por alguna razón
            } else {
                LOGGER.warn("El usuario autenticado {} no tiene autoridades (roles).", authentication.getName());
            }
        }
        return null; // O un rol por defecto como "Visitante"
    }
}