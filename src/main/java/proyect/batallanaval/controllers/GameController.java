package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;
import proyect.batallanaval.models.strategy.EstrategiaAleatoria;
import proyect.batallanaval.models.strategy.EstrategiaAtaque;
// Importaciones de excepciones
import proyect.batallanaval.exceptions.AtaqueInvalidoException;
import proyect.batallanaval.exceptions.JuegoNoInicializadoException;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controller responsible for managing the main game view, displaying both
 * the human player's board and the machine's board for attacks.
 * Functionality focused solely on the player's attack and overall game flow.
 */
public class GameController implements Initializable {

    private GameManager gameManager;

    // FXML ELEMENTS
    @FXML private GridPane playerGrid;
    @FXML private GridPane gridMaquina;
    @FXML private Button btnAtacar;
    @FXML private Label lblMensajeTurno;
    @FXML private Button btnCheck;

    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    // MODEL REFERENCES
    private Juego juego;
    private Jugador humano;
    private Maquina maquina;

    private MaquinaThread maquinaThread;
    private EstrategiaAtaque estrategiaMaquina;

    private StackPane celdaSeleccionadaMaquina;
    private int filaAtaque = -1;
    private int colAtaque = -1;

    /**
     * Called to initialize a controller after its root element has been completely processed.
     * Initializes component states and sets up the event handler for the check button.
     *
     * @param url The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resourceBundle The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialize llamado - esperando setJuego()");

        this.gameManager = new GameManager();

        btnAtacar.setDisable(true);
        if (btnCheck != null) {
            btnCheck.setOnAction(event -> irAColocacionMaquina());
        }
    }

    /**
     * Injects the shared {@link Juego} instance and initializes both boards.
     * Assigns model references and starts the machine's turn thread.
     *
     * @param juego The shared game instance.
     * @throws JuegoNoInicializadoException if the provided game object or its components are null.
     */
    public void setJuego(Juego juego) {
        System.out.println("=== setJuego llamado ===");

        if (juego == null || juego.getJugador() == null || juego.getMaquina() == null) {
            // We throw an unchecked error internally if the essential setup failed
            throw new RuntimeException("El objeto Juego o sus componentes son nulos.");
        }

        this.juego = juego;
        assignReferences();

        int conBarco = 0;
        for (int f = 0; f < Tablero.SIZE; f++) {
            for (int c = 0; c < Tablero.SIZE; c++) {
                if (humano.getTableroPosicion().getCelda(f, c).tieneBarco()) {
                    conBarco++;
                }
            }
        }
        System.out.println("Celdas con barco en tablero jugador: " + conBarco);

        if (playerGrid != null && gridMaquina != null) {
            System.out.println("Grids existen, inicializando vistas...");

            inicializarVistaConEstados(playerGrid, humano.getTableroPosicion(), humano.getFlota(), true);
            inicializarVistaConEstados(gridMaquina, maquina.getTableroPosicion(), maquina.getFlota(), false);

            configurarManejadoresAtaque();
            iniciarThreadMaquina();
            actualizarMensajeTurno();
        } else {
            System.out.println("ERROR: Uno o ambos grids son nulos!");
        }
    }

    /**
     * Initializes the basic grid view and applies the current state of attacks
     * and ship placement to the visual board.
     *
     * @param grid The GridPane to initialize.
     * @param tablero The corresponding board model.
     * @param flota The player's fleet (only painted if isPlayerBoard is true).
     * @param isPlayerBoard True if this is the player's positioning board (ships are visible).
     */
    private void inicializarVistaConEstados(GridPane grid, Tablero tablero, Flota flota, boolean isPlayerBoard) {
        inicializarGridBase(grid);

        if (isPlayerBoard && flota != null && !flota.getBarcos().isEmpty()) {
            pintarFlotaEnTablero(flota, grid);
        }

        aplicarEstadosDeAtaque(grid, tablero, isPlayerBoard);
    }

    /**
     * Applies visual styles based on the cell's current attack state (Hit, Miss, Sunk).
     *
     * @param grid The target GridPane.
     * @param tablero The corresponding Tablero model.
     * @param isPlayerBoard Flag indicating if it's the player's board (affects which visual elements are relevant).
     */
    private void aplicarEstadosDeAtaque(GridPane grid, Tablero tablero, boolean isPlayerBoard) {
        Set<Barco> barcosHundidosPintados = new HashSet<>();

        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                int fila = pos[0];
                int col = pos[1];

                Celda celda = tablero.getCelda(fila, col);
                EstadoCelda estado = celda.getEstado();

                switch (estado) {
                    case AGUA_TOCADA:
                        cell.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;");
                        System.out.println("Aplicando AGUA_TOCADA en (" + fila + ", " + col + ")");
                        break;

                    case TOCADA:
                        if (celda.tieneBarco() && !celda.getBarco().estaHundido()) {
                            cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                            System.out.println("Aplicando TOCADA en (" + fila + ", " + col + ")");
                        }
                        break;

                    case HUNDIDA:
                        if (celda.tieneBarco()) {
                            Barco barco = celda.getBarco();
                            if (barco.estaHundido() && !barcosHundidosPintados.contains(barco)) {
                                pintarBarcoHundido(cell, tablero, grid);
                                barcosHundidosPintados.add(barco);
                                System.out.println(" Aplicando HUNDIDA para barco " + barco.getTipo());
                            }
                        }
                        break;

                    case BARCO:
                    case VACIA:
                        break;
                }
            }
        }

        // Check sunk ships without HUNDIDA state
        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                Celda celda = tablero.getCelda(pos[0], pos[1]);

                if (celda.tieneBarco()) {
                    Barco barco = celda.getBarco();
                    if (barco.estaHundido() && !barcosHundidosPintados.contains(barco)) {
                        System.out.println(" Barco hundido sin estado HUNDIDA: " + barco.getTipo());
                        pintarBarcoHundido(cell, tablero, grid);
                        barcosHundidosPintados.add(barco);
                    }
                }
            }
        }
    }

    /**
     * Assigns the references to the player and machine models from the game instance.
     * @throws NullPointerException if the Juego object was not properly initialized.
     */
    private void assignReferences() {
        System.out.println("Asignando referencias de juego...");
        if (juego == null) {
            // This is a programmatic error if setJuego was called without checking.
            throw new NullPointerException("Juego no puede ser nulo al asignar referencias.");
        }
        this.humano = juego.getJugador();
        this.maquina = juego.getMaquina();
        System.out.println("Referencias asignadas.");
    }

    /**
     * Updates the turn message Label based on the current game state (whose turn it is).
     */
    private void actualizarMensajeTurno() {
        if (juego.juegoTerminado()) {
            lblMensajeTurno.setText("¡Juego Terminado!");
            return;
        }

        if (juego.esTurnoJugador()) {
            // Assuming the Human Player must always select a cell
            lblMensajeTurno.setText("Tu turno: Selecciona una casilla para atacar.");
        } else {
            lblMensajeTurno.setText("Turno de la Máquina: Esperando ataque...");
        }
    }

    /**
     * Initializes and starts the MaquinaThread to manage the machine's turns.
     * @throws JuegoNoInicializadoException if the player or machine fleet is missing, indicating
     * an incomplete setup.
     */
    private void iniciarThreadMaquina() {
        if (humano.getFlota() == null || humano.getFlota().getBarcos().isEmpty()) {
            System.err.println("ERROR: Flota del jugador no está inicializada!");
            // Although checked exceptions are better for this, we use Runtime here
            // as this is an internal setup consistency error.
            throw new RuntimeException("ERROR: Flota del jugador no está inicializada!");
        }

        System.out.println("GC - juego: " + System.identityHashCode(juego));
        System.out.println("GC - flota máquina size: " +
                (maquina.getFlota() == null ? "null" : maquina.getFlota().getBarcos().size()));

        if (maquina.getFlota() == null || maquina.getFlota().getBarcos().isEmpty()) {
            System.err.println("ERROR: Flota de la máquina no está inicializada!");
            throw new RuntimeException("ERROR: Flota de la máquina no está inicializada!");
        }

        System.out.println("Iniciando thread - Barcos Jugador: " + humano.getFlota().getBarcos().size());
        System.out.println("Iniciando thread - Barcos Máquina: " + maquina.getFlota().getBarcos().size());

        // Create attack strategy (can be changed to other implementations)
        estrategiaMaquina = new EstrategiaAleatoria();

        // Create and start machine thread
        maquinaThread = new MaquinaThread(
                juego,
                estrategiaMaquina,
                this::actualizarVistaConGuardado,
                this::verificarGanadorConLimpieza          // Winner check callback
        );

        maquinaThread.start();
        System.out.println("Thread de la máquina iniciado.");
    }

    /**
     * Updates the full view and automatically saves the game state after the machine's shot.
     */
    private void actualizarVistaConGuardado() {
        try {
            // Automatically save after the machine's shot
            // SIMULATING CHECKED EXCEPTION:
            // if (Math.random() < 0.1) throw new IOException("Disk failure during save.");
            gameManager.guardarPartida(humano, maquina);
        } catch (Exception e) {
            // Here we would catch a real IOException from GestorPartida and wrap it
            mostrarAlerta("Error de Guardado", "No se pudo guardar la partida.", Alert.AlertType.ERROR);
            // In a real application, you might throw an ErrorGuardadoException here
            // throw new ErrorGuardadoException("Failed to save game state.", e);
        }


        // Update the normal view
        actualizarVistaCompleta();
    }

    /**
     * Refreshes the state of both the player's and the machine's boards.
     */
    private void actualizarVistaCompleta() {
        // Update player board (shows hits on player's ships)
        actualizarTableroJugador();

        // Update machine board (shows player's attacks)
        actualizarTableroMaquina();

        actualizarMensajeTurno();

        // Update turn message
        if (juego.esTurnoJugador()) {
            lblMensajeTurno.setText("Tu turno - Selecciona una casilla para atacar");
        } else {
            lblMensajeTurno.setText("Turno de la máquina - Pensando...");
        }
    }

    /**
     * Checks for a winner and performs cleanup (game saving/deletion).
     */
    private void verificarGanadorConLimpieza() {
        System.out.println("=== VERIFICANDO GANADOR ===");
        System.out.println("Flota Máquina hundida: " + maquina.getFlota().estaFlotaHundida());
        System.out.println("Flota Jugador hundida: " + humano.getFlota().estaFlotaHundida());

        if (juego.haGanadoJugador()) {
            System.out.println("¡JUGADOR GANÓ!");
            detenerThreadMaquina();
            try {
                // ✅ AGREGAR: Eliminar partida al ganar
                gameManager.eliminarPartidaGuardada();
            } catch (Exception e) {
                mostrarAlerta("Error de Guardado", "No se pudo eliminar el archivo de partida guardada.", Alert.AlertType.WARNING);
            }
            mostrarAlerta("¡VICTORIA!", "¡GANASTE! Has hundido toda la flota enemiga.", Alert.AlertType.INFORMATION);
        } else if (juego.haGanadoMaquina()) {
            System.out.println("¡MÁQUINA GANÓ!");
            detenerThreadMaquina();
            try {
                // ✅ AGREGAR: Eliminar partida al perder
                gameManager.eliminarPartidaGuardada();
            } catch (Exception e) {
                mostrarAlerta("Error de Guardado", "No se pudo eliminar el archivo de partida guardada.", Alert.AlertType.WARNING);
            }
            mostrarAlerta("DERROTA", "La máquina ha hundido toda tu flota. ¡Mejor suerte la próxima vez!", Alert.AlertType.INFORMATION);
        } else {
            System.out.println("Juego continúa...");
        }
    }

    /**
     * Updates the player's board view to reflect the machine's attacks.
     */
    private void actualizarTableroJugador() {
        Tablero tableroJugador = humano.getTableroPosicion();

        for (Node node : playerGrid.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                int fila = pos[0];
                int col = pos[1];

                EstadoCelda estado = tableroJugador.getCelda(fila, col).getEstado();

                // 1. Main condition: Only update if the cell has been attacked.
                if (estado == EstadoCelda.TOCADA ||
                        estado == EstadoCelda.HUNDIDA ||
                        estado == EstadoCelda.AGUA_TOCADA) {

                    // 2. Repainting logic for HITS (Machine to Player)

                    if (estado == EstadoCelda.AGUA_TOCADA) {
                        // The Machine shot water on the player's board.
                        cell.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;"); // AZUL
                    } else if (estado == EstadoCelda.HUNDIDA) {
                        // The Machine has sunk a ship (ROJO)
                        // If the ship is sunk, paint that cell red.
                        pintarBarcoHundido(cell, tableroJugador, playerGrid);                    } else if (estado == EstadoCelda.TOCADA) {
                        // The Machine has hit a ship (NARANJA)
                        cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                    }
                }
                // If the state is VACIA or BARCO, it is skipped, maintaining the initial color (gris or the ShipCellView).
            }
        }
    }

    /**
     * Updates the machine's attack board view to reflect the player's attacks.
     */
    private void actualizarTableroMaquina() {
        Tablero tableroMaquina = maquina.getTableroPosicion();

        for (Node node : gridMaquina.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                int fila = pos[0], col = pos[1];

                EstadoCelda estado = tableroMaquina.getCelda(fila, col).getEstado();

                if (estado == EstadoCelda.AGUA_TOCADA) {
                    cell.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;");
                } else if (estado == EstadoCelda.TOCADA) {
                    cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                } else if (estado == EstadoCelda.HUNDIDA) {
                    pintarBarcoHundido(cell, tableroMaquina, gridMaquina);
                }
            }
        }
    }

    /**
     * Checks for the end of the game and displays a winner/loser alert.
     */
    private void verificarGanador() {
        System.out.println("=== VERIFICANDO GANADOR ===");
        System.out.println("Flota Máquina hundida: " + maquina.getFlota().estaFlotaHundida());
        System.out.println("Flota Jugador hundida: " + humano.getFlota().estaFlotaHundida());
        System.out.println("Barcos Máquina: " + maquina.getFlota().getBarcos().size());
        System.out.println("Barcos Jugador: " + humano.getFlota().getBarcos().size());

        if (juego.haGanadoJugador()) {
            System.out.println("¡JUGADOR GANÓ!");
            detenerThreadMaquina();
            mostrarAlerta("¡VICTORIA!",
                    "¡GANASTE! Has hundido toda la flota enemiga.",
                    Alert.AlertType.INFORMATION);
        } else if (juego.haGanadoMaquina()) {
            System.out.println("¡MÁQUINA GANÓ!");
            detenerThreadMaquina();
            mostrarAlerta("DERROTA",
                    "La máquina ha hundido toda tu flota. ¡Mejor suerte la próxima vez!",
                    Alert.AlertType.INFORMATION);
        } else {
            System.out.println("Juego continúa...");
        }
    }

    /**
     * Stops the machine's turn processing thread safely.
     */
    private void detenerThreadMaquina() {
        if (maquinaThread != null) {
            maquinaThread.requestStop();
            System.out.println("Thread de la máquina detenido.");
        }
    }


    /* ---------- Attack Logic (Selection and Execution) ---------- */

    /**
     * Configures the click handlers for the machine's board cells and the attack button.
     */
    private void configurarManejadoresAtaque() {
        for (Node node : gridMaquina.getChildren()) {
            if (node instanceof StackPane cell) {
                cell.setOnMouseClicked(e -> seleccionarCeldaAtaque(cell));
            }
        }
        btnAtacar.setOnAction(e -> handleAttack());
    }


    /**
     * Handles the selection logic when a player clicks a cell on the machine's board.
     * Manages highlighting, validation, and updating the attack coordinates.
     *
     * @param cell The StackPane cell selected by the player.
     * @throws AtaqueInvalidoException if the selected cell has already been attacked.
     */
    private void seleccionarCeldaAtaque(StackPane cell) {
        // Check if it's the player's turn
        if (!juego.esTurnoJugador()) {
            mostrarAlerta("Turno bloqueado", "¡Espera! Es el turno de la máquina.", Alert.AlertType.WARNING);
            return;
        }

        Tablero tableroMaquina = maquina.getTableroPosicion();
        int[] pos = (int[]) cell.getUserData();
        int fila = pos[0];
        int col = pos[1];

        EstadoCelda estadoActual = tableroMaquina.getCelda(fila, col).getEstado();

        // 1. Check if the cell has already been attacked (INCLUDING AGUA_TOCADA)
        if (estadoActual == EstadoCelda.TOCADA ||
                estadoActual == EstadoCelda.HUNDIDA ||
                estadoActual == EstadoCelda.AGUA_TOCADA) { // <--- Inclusión de AGUA_TOCADA

            mostrarAlerta("Celda ya atacada", "¡Ya atacaste esa celda! Selecciona otra.", Alert.AlertType.WARNING);

            // Throwing custom unchecked exception for a logical game flow error
            throw new AtaqueInvalidoException("El jugador intentó atacar una celda ya atacada.");

            // IMPORTANT! If already attacked and not going to be selected,
            // we must not continue with the selection/deselection logic that follows.
            // return;
        }

        // 1. Deselect the previously selected cell
        if (celdaSeleccionadaMaquina != null) {
            int[] prevPos = (int[]) celdaSeleccionadaMaquina.getUserData();
            EstadoCelda prevEstado = tableroMaquina.getCelda(prevPos[0], prevPos[1]).getEstado();

            // If the previous state was AGUA_TOCADA, TOCADA, or HUNDIDA,
            // restore to the color that corresponds to it.
            if (prevEstado == EstadoCelda.AGUA_TOCADA) {
                // If the previous was AGUA_TOCADA, restore to BLUE
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;");
            }
            else if (prevEstado == EstadoCelda.TOCADA) {
                // If the previous was TOCADA, restore to ORANGE
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
            }
            // Note: HUNDIDA is handled with pintarBarcoHundido and is usually red.
            // We don't need a HUNDIDA condition here because sinking is permanent.

            else {
                // If the previous state was VACIA or BARCO (i.e., NOT ATTACKED), restore to the base GREY.
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #b0b0b0;");
            }
        }

        // 2. Mark the new selected cell (yellow)
        filaAtaque = fila;
        colAtaque = col;
        celdaSeleccionadaMaquina = cell;
        celdaSeleccionadaMaquina.setStyle("-fx-background-color: yellow; -fx-border-color: #b0b0b0;");

        btnAtacar.setDisable(false);
    }

    /**
     * Executes the attack when the "ATACAR" button is pressed.
     * Manages game logic, UI update, victory check, and turn changes.
     * * @throws IllegalStateException if the game is in an invalid state for an attack (e.g., not player's turn, invalid cell).
     */
    @FXML
    private void handleAttack() {
        if (celdaSeleccionadaMaquina == null) {
            mostrarAlerta("Celda ya atacada", "¡Ya atacaste esa celda! Selecciona otra.", Alert.AlertType.WARNING);
            return;
        }

        // This coordinates check should prevent ArrayIndexOutOfBoundsException
        if (filaAtaque < 0 || filaAtaque >= Tablero.SIZE || colAtaque < 0 || colAtaque >= Tablero.SIZE) {
            throw new ArrayIndexOutOfBoundsException("Coordenadas de ataque fuera de límites.");
        }

        System.out.println("Jugador atacando: (" + filaAtaque + ", " + colAtaque + ")");

        try {
            // 1. Execute the attack
            ResultadoDisparo resultado = juego.ejecutarAtaqueJugador(filaAtaque, colAtaque);

            // Save after successful attack
            try {
                gameManager.guardarPartida(humano, maquina);
            } catch (Exception e) {
                // Catching a general exception and wrapping it as our custom checked exception
                // to manage persistence errors more specifically.
                mostrarAlerta("Error de Guardado", "No se pudo guardar la partida.", Alert.AlertType.ERROR);
                // throw new ErrorGuardadoException("Fallo al guardar después del ataque.", e);
            }

            // 2. Update the view of the attacked cell
            actualizarVistaAtaque(celdaSeleccionadaMaquina, resultado, maquina.getTableroPosicion(), gridMaquina);

            // 3. Check victory condition
            if (juego.haGanadoJugador()) {
                gameManager.eliminarPartidaGuardada();
                mostrarAlerta("¡VICTORIA!", "¡GANASTE! Has hundido toda la flota enemiga.", Alert.AlertType.INFORMATION);
                return;
            }

            // 4. Message logic based on result
            celdaSeleccionadaMaquina = null;
            btnAtacar.setDisable(true);

            switch (resultado) {
                case AGUA:
                    lblMensajeTurno.setText("¡AGUA! Turno de la máquina...");
                    // El turno cambia automaticamente en Juego
                    // turnoMaquina();
                    break;

                case TOCADO:
                    lblMensajeTurno.setText("¡IMPACTO! Sigue disparando...");
                    // El jugador mantiene el turno (Juego no lo cambió)
                    break;

                case HUNDIDO:
                    lblMensajeTurno.setText("¡BARCO HUNDIDO! Sigue disparando...");
                    // El jugador mantiene el turno (Juego no lo cambió)
                    break;
            }

        } catch (IllegalStateException e) {
            lblMensajeTurno.setText("Error: " + e.getMessage());
            System.err.println(e.getMessage());
        } catch (RuntimeException e) {
            lblMensajeTurno.setText("Error en el ataque: " + e.getMessage());
            System.err.println(e.getMessage());
        }
    }

    /* ---------- Cell Painting Logic (View) ---------- */

    /**
     * Updates the view of a single attacked cell based on the attack result.
     *
     * @param cell The visual cell (StackPane) being updated.
     * @param resultado The result of the attack.
     * @param tablero The board model.
     * @param grid The entire visual grid.
     */
    private void actualizarVistaAtaque(StackPane cell, ResultadoDisparo resultado, Tablero tablero, GridPane grid) {
        String color;

        switch (resultado) {
            case AGUA:
                color = "#4444ff"; // Azul
                break;
            case TOCADO:
                color = "orange"; // Naranja
                break;
            case HUNDIDO:
                // If sunk, paint all cells of the ship red
                pintarBarcoHundido(cell, tablero, grid);
                return;
            default:
                color = "#e0e0e0";
        }

        cell.setStyle("-fx-background-color: " + color + "; -fx-border-color: #b0b0b0;");
    }

    /**
     * Paints all cells belonging to a sunk ship in red.
     *
     * @param celdaAtaque The cell where the sinking hit occurred.
     * @param tablero The board model.
     * @param grid The visual grid.
     * @throws NullPointerException if the sunk ship or its cells are null.
     */
    private void pintarBarcoHundido(StackPane celdaAtaque, Tablero tablero, GridPane grid) {
        int[] pos = (int[]) celdaAtaque.getUserData();
        Celda celdaModelo = tablero.getCelda(pos[0], pos[1]);

        if (celdaModelo.tieneBarco()) {
            Barco barcoHundido = celdaModelo.getBarco();

            if (barcoHundido.estaHundido()) {
                System.out.println(" Pintando barco hundido: " + barcoHundido.getTipo());
                System.out.println("   Celdas del barco: " + barcoHundido.getCeldas().size());

                barcoHundido.getCeldas().forEach(c -> {
                    StackPane cellView = getCell(c.getColumna(), c.getFila(), grid);
                    if (cellView != null) {
                        cellView.setStyle("-fx-background-color: red; -fx-border-color: #b0b0b0;");
                        System.out.println("   Celda (" + c.getFila() + ", " + c.getColumna() + ") → ROJO");
                    } else {
                        System.out.println("   No se encontró celda (" + c.getFila() + ", " + c.getColumna() + ")");
                    }
                });
            }
        }
    }


    /**
     * Displays a JavaFX Alert dialog.
     *
     * @param titulo The title of the alert.
     * @param mensaje The message content.
     * @param tipo The type of the alert (e.g., INFORMATION, WARNING).
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /* ---------- Existing and Auxiliary Methods ---------- */

    /**
     * Initializes the basic grid structure and, optionally, paints the fleet.
     *
     * @param grid The GridPane to initialize.
     * @param tablero The corresponding board model.
     * @param flota The fleet to paint (if applicable).
     * @param isPlayerBoard True if this is the player's positioning board (ships are visible).
     */
    public void inicializarVista(GridPane grid, Tablero tablero, Flota flota, boolean isPlayerBoard) {
        inicializarGridBase(grid);

        if (isPlayerBoard) {
            pintarFlotaEnTablero(flota, grid);
        }
    }

    /**
     * Renders the ships visually on the player's board.
     *
     * @param flota The fleet containing the ships.
     * @param grid The GridPane of the player.
     * @throws NullPointerException if the fleet or its contents are unexpectedly null during rendering.
     */
    private void pintarFlotaEnTablero(Flota flota, GridPane grid) {
        if (flota == null || flota.getBarcos().isEmpty()) {
            System.err.println("DIAGNOSTIC: Flota vacía. No hay barcos para pintar.");
            return;
        }

        flota.getBarcos().forEach(barco -> {
            barco.getCeldas().forEach(celda -> {
                StackPane cellView = getCell(
                        celda.getColumna(),
                        celda.getFila(),
                        grid
                );

                if (cellView != null) {
                    cellView.getChildren().clear();
                    ShipCellView view = new ShipCellView(barco.getTipo(), CELL_SIZE);
                    cellView.getChildren().add(view);
                }
            });
        });
    }

    /**
     * Retrieves the {@link StackPane} that corresponds to the given column and row.
     *
     * @param col The column index.
     * @param fila The row index.
     * @param grid The GridPane to search within.
     * @return The StackPane cell or null if not found.
     */
    private StackPane getCell(int col, int fila, GridPane grid) {
        for (Node node : grid.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeFila = GridPane.getRowIndex(node);

            if (nodeCol == null) nodeCol = 0;
            if (nodeFila == null) nodeFila = 0;

            if (nodeCol == col && nodeFila == fila) {
                return (StackPane) node;
            }
        }
        return null;
    }

    /**
     * Initializes the basic visual structure of the GridPane (cells and constraints).
     *
     * @param grid The GridPane to set up.
     */
    private void inicializarGridBase(GridPane grid) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        for (int i = 0; i < Tablero.SIZE; i++) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE);
            col.setMinWidth(CELL_SIZE);
            col.setMaxWidth(CELL_SIZE);
            col.setHgrow(Priority.NEVER);
            grid.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints(CELL_SIZE);
            row.setMinHeight(CELL_SIZE);
            row.setMaxHeight(CELL_SIZE);
            row.setVgrow(Priority.NEVER);
            grid.getRowConstraints().add(row);
        }

        for (int fila = 0; fila < Tablero.SIZE; fila++) {
            for (int col = 0; col < Tablero.SIZE; col++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);

                cell.setUserData(new int[]{fila, col});
                cell.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #b0b0b0;");
                GridPane.setMargin(cell, new Insets(1));

                grid.add(cell, col, fila);
            }
        }

        int total = Tablero.SIZE * CELL_SIZE + 2 * Tablero.SIZE;
        grid.setPrefSize(total, total);
        grid.setMinSize(total, total);
        grid.setMaxSize(total, total);
    }

    /**
     * Navigates to the Machine Colocation View (for debugging/testing purposes).
     * @throws JuegoNoInicializadoException if the main game object is null.
     */
    private void irAColocacionMaquina() {
        if (this.juego == null) {
            System.err.println("ERROR: El objeto 'juego' no ha sido inicializado.");
            // Throwing a checked exception would force a try-catch here.
            // We use RuntimeException since this is an internal flow error often related to initialization order.
            throw new RuntimeException("ERROR: El objeto 'juego' no ha sido inicializado.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/machine-colocation-view.fxml"));
            Parent root = loader.load();

            MachineColocationController machineController = loader.getController();
            machineController.setJuego(this.juego);

            Stage nuevaVentana = new Stage();
            nuevaVentana.setTitle("Vista de Colocación de la Máquina");
            nuevaVentana.setScene(new Scene(root));

            nuevaVentana.show();

            System.out.println("Nueva ventana de Colocación Máquina abierta exitosamente.");

        } catch (IOException ex) {
            System.err.println("Error al cargar machine-colocation-view.fxml:");
            ex.printStackTrace();
            // This is an external/checked exception related to file access
            mostrarAlerta("Error de Vista", "No se pudo cargar la vista de colocación.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Performs clean-up operations, such as stopping the background thread.
     */
    public void cleanup() {
        detenerThreadMaquina();
    }
}