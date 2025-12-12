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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller responsible for managing the main game view, displaying both
 * the human player's board and the machine's board for attacks.
 * Funcionalidad enfocada únicamente en el ataque del jugador.
 */
public class GameController implements Initializable {

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

    // MACHINE THREAD
    private MaquinaThread maquinaThread;
    private EstrategiaAtaque estrategiaMaquina;


    // GAME STATE (Simplificado - solo para la vista)
    private StackPane celdaSeleccionadaMaquina;
    private int filaAtaque = -1;
    private int colAtaque = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialize llamado - esperando setJuego()");
        btnAtacar.setDisable(true);
        if (btnCheck != null) {
            btnCheck.setOnAction(event -> irAColocacionMaquina());
        }
    }

    /**
     * Injects the shared {@link Juego} instance and initializes both boards.
     */
    public void setJuego(Juego juego) {
        System.out.println("=== setJuego llamado ===");
        this.juego = juego;
        assignReferences();

        if (playerGrid != null && gridMaquina != null) {
            System.out.println("Grids existen, inicializando vistas...");

            // 1. Tablero Jugador: Barcos visibles
            inicializarVista(playerGrid, humano.getTableroPosicion(), humano.getFlota(), true);

            // 2. Tablero Máquina: Celdas vacías, clickeables
            inicializarVista(gridMaquina, maquina.getTableroPosicion(), maquina.getFlota(), false);

            configurarManejadoresAtaque();

            // 3. Incializar y comenzar la MachineThread
            iniciarThreadMaquina();
        } else {
            System.out.println("ERROR: Uno o ambos grids son nulos!");
        }
    }

    private void assignReferences() {
        System.out.println("Asignando referencias de juego...");
        this.humano = juego.getJugador();
        this.maquina = juego.getMaquina();
        System.out.println("Referencias asignadas.");
    }

    private void iniciarThreadMaquina() {
        if (humano.getFlota() == null || humano.getFlota().getBarcos().isEmpty()) {
            System.err.println("ERROR: Flota del jugador no está inicializada!");
            return;
        }

        System.out.println("GC - juego: " + System.identityHashCode(juego));
        System.out.println("GC - flota máquina size: " +
                (maquina.getFlota() == null ? "null" : maquina.getFlota().getBarcos().size()));

        if (maquina.getFlota() == null || maquina.getFlota().getBarcos().isEmpty()) {
            System.err.println("ERROR: Flota de la máquina no está inicializada!");
            return;
        }

        System.out.println("Iniciando thread - Barcos Jugador: " + humano.getFlota().getBarcos().size());
        System.out.println("Iniciando thread - Barcos Máquina: " + maquina.getFlota().getBarcos().size());

        // Create attack strategy (can be changed to other implementations)
        estrategiaMaquina = new EstrategiaAleatoria();

        // Create and start machine thread
        maquinaThread = new MaquinaThread(
                juego,
                estrategiaMaquina,
                this::actualizarVistaCompleta,  // UI refresh callback
                this::verificarGanador           // Winner check callback
        );

        maquinaThread.start();
        System.out.println("Thread de la máquina iniciado.");
    }

    private void actualizarVistaCompleta() {
        // Update player board (shows hits on player's ships)
        actualizarTableroJugador();

        // Update machine board (shows player's attacks)
        actualizarTableroMaquina();

        // Update turn message
        if (juego.esTurnoJugador()) {
            lblMensajeTurno.setText("Tu turno - Selecciona una casilla para atacar");
        } else {
            lblMensajeTurno.setText("Turno de la máquina - Pensando...");
        }
    }

    private void actualizarTableroJugador() {
        Tablero tableroJugador = humano.getTableroPosicion();

        for (Node node : playerGrid.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                int fila = pos[0];
                int col = pos[1];

                EstadoCelda estado = tableroJugador.getCelda(fila, col).getEstado();

                // Only update attacked cells
                if (estado == EstadoCelda.TOCADA || estado == EstadoCelda.HUNDIDA) {
                    Celda celda = tableroJugador.getCelda(fila, col);

                    if (celda.tieneBarco() && celda.getBarco().estaHundido()) {
                        // Sunk ship - paint red
                        cell.setStyle("-fx-background-color: red; -fx-border-color: #b0b0b0;");
                    } else if (estado == EstadoCelda.TOCADA) {
                        // Hit - paint orange
                        cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                    }
                }
            }
        }
    }

    private void actualizarTableroMaquina() {
        Tablero tableroMaquina = maquina.getTableroPosicion();

        for (Node node : gridMaquina.getChildren()) {
            if (node instanceof StackPane cell) {
                int[] pos = (int[]) cell.getUserData();
                int fila = pos[0];
                int col = pos[1];

                EstadoCelda estado = tableroMaquina.getCelda(fila, col).getEstado();

                if (estado == EstadoCelda.TOCADA || estado == EstadoCelda.HUNDIDA) {
                    Celda celda = tableroMaquina.getCelda(fila, col);

                    if (celda.tieneBarco() && celda.getBarco().estaHundido()) {
                        // Sunk ship - paint all cells red
                        pintarBarcoHundido(cell, tableroMaquina, gridMaquina);
                    } else if (estado == EstadoCelda.TOCADA) {
                        // Hit - paint orange
                        cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                    }
                }
            }
        }
    }

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

    private void detenerThreadMaquina() {
        if (maquinaThread != null) {
            maquinaThread.requestStop();
            System.out.println("Thread de la máquina detenido.");
        }
    }

    /* ---------- Lógica de Ataque (Selección y Ejecución) ---------- */

    private void configurarManejadoresAtaque() {
        for (Node node : gridMaquina.getChildren()) {
            if (node instanceof StackPane cell) {
                cell.setOnMouseClicked(e -> seleccionarCeldaAtaque(cell));
            }
        }
        btnAtacar.setOnAction(e -> handleAttack());
    }


    private void seleccionarCeldaAtaque(StackPane cell) {
        // Verificar si es el turno del jugador (AHORA desde Juego)
        if (!juego.esTurnoJugador()) {
            mostrarAlerta("Turno bloqueado", "¡Espera! Es el turno de la máquina.", Alert.AlertType.WARNING);
            return;
        }

        Tablero tableroMaquina = maquina.getTableroPosicion();
        int[] pos = (int[]) cell.getUserData();
        int fila = pos[0];
        int col = pos[1];

        // Verifica si la celda ya fue atacada
        EstadoCelda estadoActual = tableroMaquina.getCelda(fila, col).getEstado();
        if (estadoActual == EstadoCelda.TOCADA || estadoActual == EstadoCelda.HUNDIDA) {
            mostrarAlerta("Celda ya atacada", "¡Ya atacaste esa celda! Selecciona otra.", Alert.AlertType.WARNING);
            return;
        }

        // 1. Desmarcar la celda previamente seleccionada
        if (celdaSeleccionadaMaquina != null) {
            int[] prevPos = (int[]) celdaSeleccionadaMaquina.getUserData();
            EstadoCelda prevEstado = tableroMaquina.getCelda(prevPos[0], prevPos[1]).getEstado();

            // Solo restaurar color si NO fue atacada
            if (prevEstado != EstadoCelda.TOCADA && prevEstado != EstadoCelda.HUNDIDA) {
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #b0b0b0;");
            }
        }

        // 2. Marcar la nueva celda seleccionada (amarillo)
        filaAtaque = fila;
        colAtaque = col;
        celdaSeleccionadaMaquina = cell;
        celdaSeleccionadaMaquina.setStyle("-fx-background-color: yellow; -fx-border-color: #b0b0b0;");

        btnAtacar.setDisable(false);
    }

    @FXML
    private void handleAttack() {
        if (celdaSeleccionadaMaquina == null) {
            mostrarAlerta("Celda ya atacada", "¡Ya atacaste esa celda! Selecciona otra.", Alert.AlertType.WARNING);
            return;
        }

        System.out.println("Jugador atacando: (" + filaAtaque + ", " + colAtaque + ")");

        try {
            // 1. Ejecutar el ataque
            ResultadoDisparo resultado = juego.ejecutarAtaqueJugador(filaAtaque, colAtaque);

            // 2. Actualizar la vista de la celda atacada
            actualizarVistaAtaque(celdaSeleccionadaMaquina, resultado, maquina.getTableroPosicion(), gridMaquina);

            // 3. Revisar condición de victoria
            if (juego.haGanadoJugador()) {
                mostrarAlerta("¡VICTORIA!", "¡GANASTE! Has hundido toda la flota enemiga.", Alert.AlertType.INFORMATION);
                return;
            }

            // 4. Lógica de mensajes según el resultado
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
        }
    }

    /* ---------- Lógica de Pintado de Celdas (Vista) ---------- */

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
                // Si es hundido, pintamos todas las celdas del barco en rojo
                pintarBarcoHundido(cell, tablero, grid);
                return;
            default:
                color = "#e0e0e0";
        }

        cell.setStyle("-fx-background-color: " + color + "; -fx-border-color: #b0b0b0;");
    }

    private void pintarBarcoHundido(StackPane celdaAtaque, Tablero tablero, GridPane grid) {
        int[] pos = (int[]) celdaAtaque.getUserData();
        Celda celdaModelo = tablero.getCelda(pos[0], pos[1]);

        if (celdaModelo.tieneBarco()) {
            Barco barcoHundido = celdaModelo.getBarco();

            if (barcoHundido.estaHundido()) {
                System.out.println("Pintando barco hundido: " + barcoHundido.getTipo());
                System.out.println("Celdas del barco: " + barcoHundido.getCeldas().size());

                // Pintar TODAS las celdas del barco en rojo
                barcoHundido.getCeldas().forEach(c -> {
                    StackPane cellView = getCell(c.getColumna(), c.getFila(), grid);
                    if (cellView != null) {
                        cellView.setStyle("-fx-background-color: red; -fx-border-color: #b0b0b0;");
                        System.out.println("  Pintando celda: (" + c.getFila() + ", " + c.getColumna() + ") en ROJO");
                    } else {
                        System.out.println("  ERROR: No se encontró la celda visual para (" + c.getFila() + ", " + c.getColumna() + ")");
                    }
                });
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /* ---------- Métodos Existentes y Auxiliares ---------- */

    public void inicializarVista(GridPane grid, Tablero tablero, Flota flota, boolean isPlayerBoard) {
        inicializarGridBase(grid);

        if (isPlayerBoard) {
            pintarFlotaEnTablero(flota, grid);
        }
    }

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

    private void irAColocacionMaquina() {
        if (this.juego == null) {
            System.err.println("ERROR: El objeto 'juego' no ha sido inicializado.");
            return;
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
        }
    }

    public void cleanup() {
        detenerThreadMaquina();
    }
}