package co.edu.usco.medimicita.enums;

public enum StreetTypeEnum {
    AVENIDA("Avenida"),
    AVENIDA_CALLE("Avenida Calle"),
    AVENIDA_CARRERA("Avenida Carrera"),
    CALLE("Calle"),
    CARRERA("Carrera"),
    CIRCULAR("Circular"),
    CIRCUNVALAR("Circunvalar"),
    DIAGONAL("Diagonal"),
    PEATONAL("Peatonal"),
    TRANSVERSAL("Transversal"),
    VIA("Vía"), // Genérico
    AUTOPISTA("Autopista"),
    KILOMETRO("Kilómetro"), // Común en zonas rurales/carreteras
    OTRO("Otro"); // Para casos no estándar

    private final String displayName;

    StreetTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}