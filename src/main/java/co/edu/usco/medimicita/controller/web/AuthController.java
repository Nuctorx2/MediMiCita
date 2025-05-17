package co.edu.usco.medimicita.controller.web; // O donde tengas tus controladores web

import co.edu.usco.medimicita.dto.UserRegistrationDto;
import co.edu.usco.medimicita.service.UserService;
import jakarta.validation.Valid; // Importante
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // Para capturar errores de validación
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register") // Asumiendo una ruta base para el registro
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto()); // Objeto para el formulario
        // model.addAttribute("epsList", epsService.findAllActive()); // Para el desplegable de EPS
        return "auth/register"; // Nombre de tu plantilla Thymeleaf
    }

    @PostMapping
    public String processRegistration(
            @Valid @ModelAttribute("userDto") UserRegistrationDto userDto, // (1) @Valid aquí
            BindingResult bindingResult, // (2) Captura los resultados de la validación
            Model model) {

        // (3) Validar que las contraseñas coincidan (cross-field validation, si no usas anotación a nivel de clase)
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            // Añade un error específico al campo 'confirmPassword' o un error global
            bindingResult.rejectValue("confirmPassword", "error.userDto", "Las contraseñas no coinciden.");
            // O para un error global:
            // bindingResult.reject("passwords.mismatch", "Las contraseñas no coinciden.");
        }

        // (4) Validar complejidad de contraseña si no se hizo con anotación personalizada
        // if (!isPasswordComplexEnough(userDto.getPassword())) {
        //    bindingResult.rejectValue("password", "error.userDto", "La contraseña no cumple los requisitos de complejidad.");
        // }


        if (bindingResult.hasErrors()) {
            // model.addAttribute("epsList", epsService.findAllActive()); // Volver a cargar datos necesarios para el form
            return "auth/register"; // Vuelve al formulario si hay errores
        }

        try {
            userService.registerNewPatient(userDto);
            // return "redirect:/register?success"; // Redirige a una página de éxito o al login
            return "redirect:/login?registrationSuccess";
        } catch (RuntimeException ex) { // Captura excepciones del servicio (ej. email ya existe)
            // model.addAttribute("epsList", epsService.findAllActive());
            model.addAttribute("registrationError", ex.getMessage());
            return "auth/register";
        }
    }

    // private boolean isPasswordComplexEnough(String password) {
    //     if (password == null || password.length() < 8 || password.length() > 20) return false;
    //     boolean hasUpper = false, hasLower = false, hasSpecial = false;
    //     String specialChars = "@#$%%"; // Tu lista de caracteres especiales
    //     for (char c : password.toCharArray()) {
    //         if (Character.isUpperCase(c)) hasUpper = true;
    //         else if (Character.isLowerCase(c)) hasLower = true;
    //         else if (specialChars.indexOf(c) != -1) hasSpecial = true;
    //     }
    //     return hasUpper && hasLower && hasSpecial;
    // }
}