package co.edu.usco.medimicita.converter;

import co.edu.usco.medimicita.enums.ZoneTypeEnum; // Asegúrate que tu enum esté aquí
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToZoneTypeEnumConverter implements Converter<String, ZoneTypeEnum> {

    @Override
    public ZoneTypeEnum convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        try {
            return ZoneTypeEnum.valueOf(source.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para ZoneTypeEnum: " + source, e);
        }
    }
}