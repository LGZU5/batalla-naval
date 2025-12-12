package proyect.batallanaval.models.strategy;

import proyect.batallanaval.models.EstadoCelda;
import proyect.batallanaval.models.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class EstrategiaAleatoria implements EstrategiaAtaque {

    private final Random random;

    public EstrategiaAleatoria() {
        this.random = new Random();
    }

    /**
     * Selects a random cell that hasn't been attacked yet.
     *
     * @param tableroObjetivo the board to attack
     * @return coordinates [row, col] of a valid unattacked cell, or null if none exist
     */
    @Override
    public int[] seleccionarAtaque(Tablero tableroObjetivo) {
        List<int[]> celdasDisponibles = new ArrayList<>();

        // Collect all cells that haven't been attacked
        for (int fila = 0; fila < Tablero.SIZE; fila++) {
            for (int col = 0; col < Tablero.SIZE; col++) {
                EstadoCelda estado = tableroObjetivo.getCelda(fila, col).getEstado();
                if (estado != EstadoCelda.TOCADA && estado != EstadoCelda.HUNDIDA) {
                    celdasDisponibles.add(new int[]{fila, col});
                }
            }
        }

        // If no cells available, return null
        if (celdasDisponibles.isEmpty()) {
            return null;
        }

        // Select a random cell from available ones
        int indice = random.nextInt(celdasDisponibles.size());
        return celdasDisponibles.get(indice);
    }
}
