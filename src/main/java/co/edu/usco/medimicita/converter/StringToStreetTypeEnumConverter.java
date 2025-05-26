package co.edu.usco.medimicita.converter;

import co.edu.usco.medimicita.enums.StreetTypeEnum; // Asegúrate que tu enum esté aquí
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component // Para que Spring lo detecte y registre automáticamente
public class StringToStreetTypeEnumConverter implements Converter<String, StreetTypeEnum> {

    @Override
    public StreetTypeEnum convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null; // O lanzar IllegalArgumentException si no debe ser nulo
        }
        try {
            // Intenta convertir por nombre (insensible a mayúsculas/minúsculas)
            return StreetTypeEnum.valueOf(source.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Puedes añadir lógica de fallback o lanzar una excepción más descriptiva
            // Por ejemplo, si "Avenida Principal" debe mapear a AVENIDA
            // O simplemente retornar null o lanzar la excepción si no hay un match exacto.
            // Para este ejemplo, relanzamos si no es un valor válido del enum.
            throw new IllegalArgumentException("Valor inválido para StreetTypeEnum: " + source, e);
        }
    }
}