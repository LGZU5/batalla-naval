package proyect.batallanaval.models.strategy;

import proyect.batallanaval.models.ResultadoDisparo;
import proyect.batallanaval.models.Tablero;

public interface EstrategiaAtaque {
    /**
     * Selects the next coordinates to attack on the opponent's board.
     *
     * @param tableroObjetivo the board to attack
     * @return an array [row, col] with the selected coordinates, or null if no valid moves
     */
    int[] seleccionarAtaque(Tablero tableroObjetivo);

    /**
     * Notifies the strategy about the result of the last attack.
     * Can be used by smarter strategies to adjust their behavior.
     *
     * @param fila row of the last attack
     * @param col column of the last attack
     * @param resultado result of the attack
     */
    default void notificarResultado(int fila, int col, ResultadoDisparo resultado) {
        // Default implementation does nothing
    }
}
