package co.edu.usco.medimicita.controller.web;

import co.edu.usco.medimicita.dto.UserRegistrationDto;
import co.edu.usco.medimicita.entity.EpsEntity;
import co.edu.usco.medimicita.exception.EmailAlreadyExistsException;
import co.edu.usco.medimicita.exception.IdentificationNumberAlreadyExistsException;
import co.edu.usco.medimicita.exception.PasswordsDoNotMatchException;
import co.edu.usco.medimicita.service.EpsService;
import co.edu.usco.medimicita.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private final UserService userService;
    private final EpsService epsService; // Necesario para poblar el desplegable de EPS

    // Método GET para mostrar el formulario de registro
    @GetMapping
    public String showRegistrationForm(Model model) {
        // Si el usuario ya está autenticado, redirigir al dashboard
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            LOGGER.info("Usuario ya autenticado ({}), redirigiendo a /default-success", authentication.getName());
            return "redirect:/default-success";
        }

        if (!model.containsAttribute("userRegistrationDto")) {
            model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        }

        loadCommonRegistrationData(model);
        model.addAttribute("pageTitle", "Registro de Paciente");
        return "register"; // Nombre de la plantilla Thymeleaf (register.html)
    }

    // Método POST para procesar el formulario de registro
    @PostMapping
    public String processRegistration(
            @Valid @ModelAttribute("userRegistrationDto") UserRegistrationDto registrationDto,
            BindingResult bindingResult, // Debe ir INMEDIATAMENTE después del objeto validado
            Model model, // Para añadir atributos si volvemos a mostrar el form
            RedirectAttributes redirectAttributes) {

        LOGGER.info("Procesando intento de registro para email: {}", registrationDto.getEmail());
        if (registrationDto.getPassword() != null &&
                !registrationDto.getPassword().isEmpty() && // Solo validar si se proporcionó contraseña
                !registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "Passwords.mismatch", "Las contraseñas no coinciden.");
        }

        if (registrationDto.getTermsAccepted() == null || !registrationDto.getTermsAccepted()) {
            bindingResult.rejectValue("termsAccepted", "Terms.notAccepted", "Debe aceptar los términos y condiciones para registrarse.");
        }


        if (bindingResult.hasErrors()) {
            LOGGER.warn("Errores de validación en el formulario de registro: {}", bindingResult.getAllErrors());
            loadCommonRegistrationData(model); // Volver a cargar datos para el formulario
            model.addAttribute("pageTitle", "Registro de Paciente - Errores");
            return "register";
        }

        try {
            // El servicio UserService.registerNewPatient ya debería validar la existencia de email/identificación
            userService.registerNewPatient(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage", "¡Registro exitoso! Por favor, inicia sesión con tus credenciales.");
            LOGGER.info("Registro exitoso para: {}", registrationDto.getEmail());
            return "redirect:/login"; // Redirigir a la página de login
        } catch (EmailAlreadyExistsException | IdentificationNumberAlreadyExistsException e) {
            LOGGER.warn("Error de registro (datos duplicados): {}", e.getMessage());
            // Es mejor usar códigos de error para los mensajes para i18n
            if (e instanceof EmailAlreadyExistsException) {
                bindingResult.rejectValue("userEmail", "registration.emailExists", e.getMessage());
            } else {
                bindingResult.rejectValue("identificationNumber", "registration.idExists", e.getMessage());
            }
            loadCommonRegistrationData(model);
            model.addAttribute("pageTitle", "Registro de Paciente - Errores");
            return "register";
        } catch (PasswordsDoNotMatchException e) { // Si tu servicio lanza esta específicamente
            LOGGER.warn("Error de registro (contraseñas no coinciden en servicio): {}", e.getMessage());
            bindingResult.rejectValue("confirmPassword", "Passwords.mismatch", e.getMessage());
            loadCommonRegistrationData(model);
            model.addAttribute("pageTitle", "Registro de Paciente - Errores");
            return "register";
        }
        catch (IllegalArgumentException e) { // Como la de términos no aceptados si el servicio la lanza
            LOGGER.warn("Error de registro (argumento inválido): {}", e.getMessage());
            // Intenta mapear a un campo específico si es posible, o error global
            bindingResult.reject("registration.failed", e.getMessage());
            loadCommonRegistrationData(model);
            model.addAttribute("pageTitle", "Registro de Paciente - Errores");
            return "register";
        }
        catch (Exception e) {
            LOGGER.error("Error inesperado durante el proceso de registro para: {}", registrationDto.getEmail(), e);
            // Usar un mensaje genérico para el usuario
            redirectAttributes.addFlashAttribute("errorMessage", "Ocurrió un error inesperado durante el registro. Por favor, inténtelo más tarde.");
            return "redirect:/register"; // Redirigir de nuevo al formulario de registro
        }
    }

    // Método helper para cargar datos comunes al modelo del formulario de registro
    private void loadCommonRegistrationData(Model model) {
        List<EpsEntity> epsList = epsService.getAllActiveEps(); // Necesitarás este método en EpsService
        model.addAttribute("epsList", epsList);
        // Aquí podrías añadir otros datos necesarios para el formulario, como listas para otros desplegables
    }
}