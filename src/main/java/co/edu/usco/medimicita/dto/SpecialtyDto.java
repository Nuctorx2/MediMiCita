package co.edu.usco.medimicita.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank; // Mejor que NotEmpty para Strings
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyDto {

    private Integer specialtyId; // Se usará para la actualización, será null para la creación

    @NotBlank(message = "El nombre de la especialidad no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre de la especialidad debe tener entre 3 y 100 caracteres.")
    private String specialtyName;

    @NotNull(message = "La duración por defecto no puede estar vacía.")
    @Min(value = 5, message = "La duración por defecto debe ser de al menos 5 minutos.") // Ajustado a 5 min
    private Integer specialtyDefaultDurationMinutes;

    // Por defecto, una nueva especialidad se crea como activa.
    // El cliente podría enviar 'false' si quiere crearla inactiva,
    // o el formulario de edición permitiría cambiar este estado.
    private Boolean specialtyIsActive = true;
}