package co.edu.usco.medimicita.exception;

public class AppointmentCancellationException extends RuntimeException {
    public AppointmentCancellationException(String message) {
        super(message);
    }
}
