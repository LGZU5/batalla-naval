package proyect.batallanaval.exceptions;

/**
 * Unchecked exception thrown when a player attempts an attack on a cell
 * that has already been hit, or if the coordinates are out of bounds.
 * Extends RuntimeException as this often indicates a logical error in input
 * or selection state, but is handled locally in the controller.
 */
public class AtaqueInvalidoException extends RuntimeException {
    public AtaqueInvalidoException(String message) {
        super(message);
    }
}