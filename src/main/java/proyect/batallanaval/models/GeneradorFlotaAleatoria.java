package proyect.batallanaval.models;

import java.util.Random;

/**
 * Class responsible for generating a complete fleet of ships and
 * placing them randomly on a given board following the rules of the game.
 * .
 * The {@code GeneradorFlotaAleatoria} class is used by the machine opponent
 * to automatically deploy its fleet before the match starts, ensuring
 * that ships do not overlap and remain within the board limits.
 */
public class GeneradorFlotaAleatoria {

    private final Random random = new Random();

    /**
     * Generates and places an entire fleet on the specified board.
     * <p>
     * Ships are placed according to the standard fleet composition:
     * <ul>
     *     <li>1 Aircraft carrier (size 4)</li>
     *     <li>2 Submarines (size 3)</li>
     *     <li>3 Destroyers (size 2)</li>
     *     <li>4 Frigates (size 1)</li>
     * </ul>
     * The method ensures that all ships are placed in valid, non-overlapping
     * positions.
     * </p>
     *
     * @param flota   the fleet object that will store the newly placed ships
     * @param tablero the board where the ships will be placed
     * @throws IllegalStateException if ships cannot be placed after a large
     *                               number of attempts
     */
    public void generarFlotaAleatoria(Flota flota, Tablero tablero) {

        colocarTipo(flota, tablero, TipoBarco.PORTAAVIONES, 1);
        colocarTipo(flota, tablero, TipoBarco.SUBMARINO, 2);
        colocarTipo(flota, tablero, TipoBarco.DESTRUCTOR, 3);
        colocarTipo(flota, tablero, TipoBarco.FRAGATA, 4);
    }

    /**
     * Attempts to place a given number of ships of the same type on the board.
     * <p>
     * For each ship, the method repeatedly selects a random orientation and a
     * random starting coordinate until a valid placement is found. If valid,
     * the ship is placed on the board and added to the fleet.
     * </p>
     *
     * @param flota     the fleet to which the ship will be added
     * @param tablero   the board where the ship will be placed
     * @param tipo      the type of ship to place
     * @param cantidad  number of ships of this type to place
     * @throws IllegalStateException if a ship cannot be placed after 1000 attempts
     */
    private void colocarTipo(Flota flota, Tablero tablero, TipoBarco tipo, int cantidad) {

        for (int n = 0; n < cantidad; n++) {
            boolean colocado = false;
            int intentos = 0;

            while (!colocado) {
                Orientacion orientacion =
                        random.nextBoolean() ? Orientacion.HORIZONTAL : Orientacion.VERTICAL;

                int maxFila = orientacion == Orientacion.HORIZONTAL
                        ? Tablero.SIZE
                        : Tablero.SIZE - tipo.getSize() + 1;

                int maxCol = orientacion == Orientacion.HORIZONTAL
                        ? Tablero.SIZE - tipo.getSize() + 1
                        : Tablero.SIZE;

                int fila = random.nextInt(maxFila);
                int col  = random.nextInt(maxCol);

                if (tablero.puedeColocarBarco(fila, col, orientacion, tipo)) {
                    Barco barco = tablero.colocarBarco(fila, col, orientacion, tipo);
                    flota.agregarBarco(barco);      // 👈 IMPORTANTE
                    colocado = true;
                }

                if (++intentos > 1000) {
                    throw new IllegalStateException(
                            "No se pudo colocar el barco tipo " + tipo + " de forma aleatoria");
                }
            }
        }
    }
}
