package co.edu.usco.medimicita.exception;

public class IdentificationNumberAlreadyExistsException extends RuntimeException {
    public IdentificationNumberAlreadyExistsException(String message) {
        super(message);
    }
}
