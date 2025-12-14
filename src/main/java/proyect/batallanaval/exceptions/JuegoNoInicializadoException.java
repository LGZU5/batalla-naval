package proyect.batallanaval.exceptions;

/**
 * Checked exception thrown when an operation is attempted before the main
 * Juego (Game) object has been properly set and initialized.
 * Extends Exception to force handling/declaration.
 */
public class JuegoNoInicializadoException extends Exception {
    public JuegoNoInicializadoException(String message) {
        super(message);
    }
}