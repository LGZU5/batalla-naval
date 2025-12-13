package proyect.batallanaval.models;

import javafx.application.Platform;
import proyect.batallanaval.models.Juego;
import proyect.batallanaval.models.ResultadoDisparo;
import proyect.batallanaval.models.Tablero;
import proyect.batallanaval.models.strategy.EstrategiaAtaque;

import java.util.Random;

/**
 * A thread that executes machine turns in a loop.
 * Follows the same pattern as CpuTurnsThread from Cincuentazo.
 *
 * - Sleeps for a random time (1-3 seconds) to simulate "thinking"
 * - All mutations on the Juego are thread-safe using synchronized(juego)
 * - UI updates are posted to JavaFX thread using Platform.runLater()
 */

public class MaquinaThread extends Thread {

    private final Juego juego;
    private final EstrategiaAtaque estrategia;
    private final Runnable uiRefresh;        // Callback to refresh UI
    private final Runnable uiCheckWinner;    // Callback to check winner
    private final Random random = new Random();

    // Volatile flag to safely request the thread to stop
    private volatile boolean stop = false;

    /**
     * Constructs a new machine turn processing thread.
     */
    public MaquinaThread(Juego juego,
                         EstrategiaAtaque estrategia,
                         Runnable uiRefresh,
                         Runnable uiCheckWinner) {
        this.juego = juego;
        this.estrategia = estrategia;
        this.uiRefresh = uiRefresh;
        this.uiCheckWinner = uiCheckWinner;
        setDaemon(true);
    }

    /**
     * Signals the thread to terminate its loop and exit.
     */
    public void requestStop() {
        stop = true;
        interrupt();
    }

    // -------------------------------------------------------------------
    // METODO AUXILIAR PARA GARANTIZAR LA SELECCIÓN DE CELDA
    // -------------------------------------------------------------------

    /**
     * Busca la primera celda que está en estado VACIA o BARCO (es decir, sin atacar).
     * Este es el mecanismo de respaldo si la estrategia aleatoria falla.
     * @param tablero El tablero del jugador.
     * @return Coordenadas [fila, columna] de una celda disponible, o null si el tablero está lleno.
     */
    private int[] buscarCeldaLibre(Tablero tablero) {
        for (int f = 0; f < Tablero.SIZE; f++) {
            for (int c = 0; c < Tablero.SIZE; c++) {
                EstadoCelda estado = tablero.getCelda(f, c).getEstado();
                if (estado == EstadoCelda.VACIA || estado == EstadoCelda.BARCO) {
                    return new int[]{f, c};
                }
            }
        }
        return null; // No hay celdas libres
    }

    // -------------------------------------------------------------------

    /**
     * Main execution loop for the thread.
     * Continuously checks if it's the machine's turn and executes attacks.
     */
    @Override
    public void run() {
        try {
            while (!stop) {

                // 1. Revisar turno de la maquina
                boolean esTurnoMaquina;
                synchronized (juego) {
                    esTurnoMaquina = !juego.esTurnoJugador();
                }

                if (!esTurnoMaquina) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        if (stop) break;
                    }
                    continue;
                }

                // 2. Simular "pensando" (1–3 segundos)
                double delaySeconds = 1.0 + random.nextDouble() * 2.0;
                long delayMillis = (long) (delaySeconds * 1000);
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ie) {
                    if (stop) break;
                    Thread.currentThread().interrupt();
                }
                if (stop) break;

                // 3. Ejecutar el ataque de la máquina
                ResultadoDisparo resultado;
                synchronized (juego) {
                    if (juego.esTurnoJugador()) {
                        continue;
                    }

                    Tablero tableroJugador = juego.getJugador().getTableroPosicion();
                    int fila, col;

                    // --- LÓGICA DE SELECCIÓN Y VALIDACIÓN (CORRECCIÓN CRÍTICA) ---
                    int maxTries = 100; // Límite de intentos aleatorios
                    int tries = 0;
                    EstadoCelda estadoActual;
                    boolean foundValidCoordinates = false;

                    int[] coordenadas;

                    do {
                        coordenadas = estrategia.seleccionarAtaque(tableroJugador);

                        if (coordenadas == null) {
                            System.err.println("ERROR: Machine has no valid moves from strategy!");
                            return;
                        }

                        fila = coordenadas[0];
                        col = coordenadas[1];

                        estadoActual = tableroJugador.getCelda(fila, col).getEstado();

                        if (estadoActual == EstadoCelda.VACIA || estadoActual == EstadoCelda.BARCO) {
                            foundValidCoordinates = true;
                            break; // Se encontraron coordenadas aleatorias válidas.
                        }

                        tries++;
                        if (tries >= maxTries) {

                            // LÓGICA DE EMERGENCIA: Buscar la primera celda libre por iteración.
                            System.err.println("Máquina falló en selección aleatoria. Buscando celda de emergencia...");
                            int[] emergencia = buscarCeldaLibre(tableroJugador);

                            if (emergencia != null) {
                                fila = emergencia[0];
                                col = emergencia[1];
                                foundValidCoordinates = true;
                            } else {
                                System.err.println("ERROR: No quedan celdas libres para atacar.");
                            }
                            break; // Salir del do-while.
                        }

                    } while (!foundValidCoordinates);

                    // Si no se encontraron coordenadas válidas (solo ocurre si no quedan celdas):
                    if (!foundValidCoordinates) {
                        System.out.println("Juego Finalizado - No quedan movimientos para la máquina.");
                        continue;
                    }
                    // --- FIN LÓGICA DE SELECCIÓN Y VALIDACIÓN ---


                    System.out.println("Máquina atacando: (" + fila + ", " + col + ")");

                    // El ataque a tablero.disparar() será válido, evitando la IllegalStateException.
                    resultado = juego.ejecutarAtaqueMaquina(fila, col);

                    // Notificar a la estrategia
                    estrategia.notificarResultado(fila, col, resultado);

                    System.out.println("Resultado: " + resultado);
                }

                // 4. Actualizar UI y verificar ganador SOLO después del ataque
                Platform.runLater(() -> {
                    uiRefresh.run();      // refrescar tableros
                    uiCheckWinner.run();  // llamar a verificarGanador() en el controller
                });
            }
        } catch (Exception ex) {
            System.err.println("Error en MaquinaThread:");
            ex.printStackTrace();
        }
    }
}