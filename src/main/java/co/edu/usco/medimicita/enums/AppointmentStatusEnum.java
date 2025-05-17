package co.edu.usco.medimicita.enums; // o co.edu.usco.medimicita.enums

public enum AppointmentStatusEnum {
    SCHEDULED,         // Cita agendada
    COMPLETED,         // Cita completada
    CANCELLED_PATIENT, // Cita cancelada por el paciente
    CANCELLED_ADMIN,   // Cita cancelada por un administrador (o servicio al cliente si existiera)
    CANCELLED_SYSTEM,  // Cita cancelada por el sistema (ej. cambio de horario del médico)
    NO_SHOW            // Paciente no se presentó a la cita
    // Puedes añadir más estados según evolucione la lógica.
}