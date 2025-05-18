package co.edu.usco.medimicita.exception;

public class UserAccountNotActiveException extends RuntimeException {
    public UserAccountNotActiveException(String message) {
        super(message);
    }
}
