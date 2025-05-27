package co.edu.usco.medimicita.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class ClinicConstants {

    private ClinicConstants() {
        // Constructor privado para prevenir instanciación de clase de utilidad
    }

    // Hacerla pública y estática
    public static final ZoneId CLINIC_ZONE_ID = ZoneId.of("America/Bogota");

    public static ZoneOffset getClinicZoneOffset() {
        return CLINIC_ZONE_ID.getRules().getOffset(Instant.now());
    }
    // Asegúrate que "America/Bogota" sea la zona correcta o hazla configurable
}