package co.edu.usco.medimicita.dto;

import co.edu.usco.medimicita.enums.StreetTypeEnum;
import co.edu.usco.medimicita.enums.ZoneTypeEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileUpdateDto {

    // Campos del UserEntity que el paciente puede editar
    @NotBlank(message = "El número de celular no puede estar vacío")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de celular debe tener 10 dígitos numéricos")
    private String userPhoneNumber;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    @Size(max = 255, message = "El email no debe exceder los 255 caracteres")
    private String userEmail;

    // --- NUEVOS CAMPOS DE DIRECCIÓN DEL DTO ---
    @NotNull(message = "El tipo de vía es obligatorio")
    private StreetTypeEnum userAddressStreetType;

    @NotBlank(message = "El número o nombre de la vía principal es obligatorio")
    @Size(max = 50, message = "El número de vía principal no debe exceder los 50 caracteres")
    private String userAddressMainWayNumber;

    @NotBlank(message = "El número de vía secundaria (placa) es obligatorio")
    @Size(max = 50, message = "El número de vía secundaria no debe exceder los 50 caracteres")
    private String userAddressSecondaryWayNumber;

    @NotBlank(message = "El número de casa o edificio (placa) es obligatorio")
    @Size(max = 50, message = "El número de casa/edificio no debe exceder los 50 caracteres")
    private String userAddressHouseOrBuildingNumber;

    @Size(max = 255, message = "El complemento no debe exceder los 255 caracteres")
    private String userAddressComplement; // Opcional

    @NotBlank(message = "El barrio o vereda es obligatorio")
    @Size(max = 100, message = "El barrio/vereda no debe exceder los 100 caracteres")
    private String userAddressNeighborhood;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no debe exceder los 100 caracteres")
    private String userAddressMunicipality;

    @NotBlank(message = "El departamento es obligatorio")
    @Size(max = 100, message = "El departamento no debe exceder los 100 caracteres")
    private String userAddressDepartment;

    @NotNull(message = "La zona es obligatoria")
    private ZoneTypeEnum userAddressZone;

    private Long currentAddressId; // Para saber si estamos actualizando una dirección existente o creando una nueva
}