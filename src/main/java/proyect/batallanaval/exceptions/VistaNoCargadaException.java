package proyect.batallanaval.exceptions;

import java.io.IOException;

/**
 * Checked exception thrown when an FXML view file cannot be loaded,
 * typically due to an underlying IOException (file not found, corrupted).
 */
public class VistaNoCargadaException extends Exception {
    public VistaNoCargadaException(String message, Throwable cause) {
        super(message, cause);
    }
}