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
     *
     * @param juego the shared Juego instance
     * @param estrategia the attack strategy to use
     * @param uiRefresh callback to refresh the UI on FX thread
     * @param uiCheckWinner callback to check for winner on FX thread
     */
    public MaquinaThread(Juego juego,
                         EstrategiaAtaque estrategia,
                         Runnable uiRefresh,
                         Runnable uiCheckWinner) {
        this.juego = juego;
        this.estrategia = estrategia;
        this.uiRefresh = uiRefresh;
        this.uiCheckWinner = uiCheckWinner;
        // Set as daemon so it doesn't prevent app from exiting
        setDaemon(true);
    }

    /**
     * Signals the thread to terminate its loop and exit.
     */
    public void requestStop() {
        stop = true;
        interrupt();
    }

    /**
     * Main execution loop for the thread.
     * Continuously checks if it's the machine's turn and executes attacks.
     */
    @Override
    public void run() {
        try {
            while (!stop) {

                // 1. revisar turno de la maquina
                boolean esTurnoMaquina;
                synchronized (juego) {
                    esTurnoMaquina = !juego.esTurnoJugador();
                }

                // Si es turno del jugador, dormimos un poquito y seguimos
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
                    int[] coordenadas = estrategia.seleccionarAtaque(tableroJugador);

                    if (coordenadas == null) {
                        System.err.println("ERROR: Machine has no valid moves!");
                        break;
                    }

                    int fila = coordenadas[0];
                    int col = coordenadas[1];

                    System.out.println("Máquina atacando: (" + fila + ", " + col + ")");

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
