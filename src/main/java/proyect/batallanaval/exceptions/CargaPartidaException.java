package proyect.batallanaval.exceptions;

/**
 * Checked exception thrown when there is a failure to load a saved game state
 * due to file corruption, missing data, or I/O errors during deserialization.
 */
public class CargaPartidaException extends Exception {
    public CargaPartidaException(String message) {
        super(message);
    }
    public CargaPartidaException(String message, Throwable cause) {
        super(message, cause);
    }
}