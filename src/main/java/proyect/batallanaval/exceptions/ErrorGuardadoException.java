package proyect.batallanaval.exceptions;

import java.io.IOException;

/**
 * Checked exception thrown when the GestorPartida fails to save or load
 * game state due to an underlying I/O error or corruption.
 * Forces the controller to notify the user of a critical persistence failure.
 */
public class ErrorGuardadoException extends Exception {
    public ErrorGuardadoException(String message, Throwable cause) {
        super(message, cause);
    }
}