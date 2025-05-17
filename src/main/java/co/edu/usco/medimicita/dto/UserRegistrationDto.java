package co.edu.usco.medimicita.dto;

import jakarta.validation.constraints.*; // Import general para las anotaciones de Jakarta Bean Validation
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

    @NotBlank(message = "El número de identificación no puede estar vacío.") // (1) No nulo y no solo espacios en blanco
    @Size(min = 8, max = 10, message = "El número de identificación debe tener entre 8 y 10 caracteres.")
    @Pattern(regexp = "^[0-9]+$", message = "El número de identificación solo debe contener dígitos.") // (2) Solo números
    private String identificationNumber;

    @NotBlank(message = "El nombre no puede estar vacío.")
    @Size(max = 50, message = "El nombre no debe exceder los 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios.") // (3) Solo letras y espacios
    private String firstName;

    @NotBlank(message = "El apellido no puede estar vacío.")
    @Size(max = 50, message = "El apellido no debe exceder los 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido solo debe contener letras y espacios.")
    private String lastName;

    @NotNull(message = "La fecha de nacimiento no puede estar vacía.")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada.") // (4) Fecha en el pasado
    private LocalDate birthDate; // La validación de "no > 100 años atrás" se haría en el servicio.

    @NotBlank(message = "El número de celular no puede estar vacío.")
    @Size(min = 10, max = 10, message = "El número de celular debe tener exactamente 10 dígitos.")
    @Pattern(regexp = "^[0-9]+$", message = "El número de celular solo debe contener dígitos.")
    private String phoneNumber;

    @NotBlank(message = "El email no puede estar vacío.")
    @Email(message = "El formato del email no es válido.")
    @Size(max = 255, message = "El email no debe exceder los 255 caracteres.")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía.")
    @Size(min = 8, max = 20, message = "La contraseña debe tener entre 8 y 20 caracteres.")
    // (5) Podrías crear una anotación de validación personalizada para complejidad de contraseña
    // o validar en el servicio. Requisitos: mín 1 Mayús, mín 1 Minús, caracteres especiales (@, #, $, %).
    private String password;

    @NotBlank(message = "La confirmación de contraseña no puede estar vacía.")
    private String confirmPassword; // La validación de que coincida con 'password' se hace en el servicio o con una anotación a nivel de clase.

    // Campos específicos para el paciente
    @NotNull(message = "Debe seleccionar una EPS para el paciente.") // (6) Asumiendo que epsId es obligatorio para pacientes
    private Integer epsId;

    @NotNull(message = "Debe aceptar los términos y condiciones.")
    @AssertTrue(message = "Debe aceptar los términos y condiciones para registrarse.") // (7) Para campos booleanos que deben ser true
    private Boolean termsAccepted;

    // Para la creación de usuarios por el admin (estos campos podrían ser opcionales dependiendo del flujo)
    private String roleName; // La validación del rol (que exista, etc.) se haría en el servicio.
    private Integer specialtyId;
    private String officeNumber;

    // (8) Validación a nivel de clase (Ejemplo para confirmar contraseña)
    // Para esto, crearías una interfaz de anotación y un validador.
    // Por simplicidad, la validación de confirmación de contraseña se hace en el servicio por ahora.
}