package co.edu.usco.medimicita.enums;

public enum ZoneTypeEnum {
    URBANA("Urbana"),
    RURAL("Rural");

    private final String displayName;

    ZoneTypeEnum(String displayName) {
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