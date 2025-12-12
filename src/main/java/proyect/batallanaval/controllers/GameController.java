package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

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
    @FXML private Label lblMensajeTurno; // Se mantiene para mostrar el resultado/estado
    @FXML private Button btnCheck;

    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    // MODEL REFERENCES
    private Juego juego;
    private Jugador humano;
    private Maquina maquina;

    // GAME STATE (Simplificado)
    private StackPane celdaSeleccionadaMaquina; // Celda visual seleccionada para el ataque
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
        Tablero tableroMaquina = maquina.getTableroPosicion();
        int[] pos = (int[]) cell.getUserData();
        int fila = pos[0];
        int col = pos[1];

        // Verifica si la celda ya fue atacada (AGUA o ya TOCADA/HUNDIDA)
        EstadoCelda estadoActual = tableroMaquina.getCelda(fila, col).getEstado();
        if (estadoActual == EstadoCelda.TOCADA || estadoActual == EstadoCelda.HUNDIDA) {
            lblMensajeTurno.setText("¡Ya atacaste esa celda! Selecciona otra.");
            return;
        }

        // 1. Desmarcar la celda previamente seleccionada
        if (celdaSeleccionadaMaquina != null) {
            celdaSeleccionadaMaquina.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #b0b0b0;");
        }

        // 2. Marcar la nueva celda seleccionada (amarillo)
        filaAtaque = fila;
        colAtaque = col;
        celdaSeleccionadaMaquina = cell;
        celdaSeleccionadaMaquina.setStyle("-fx-background-color: yellow; -fx-border-color: #b0b0b0;");

        btnAtacar.setDisable(false); // Habilitar el botón Atacar
        lblMensajeTurno.setText("Objetivo seleccionado. ¡Presiona ATACAR!");
    }


    @FXML
    private void handleAttack() {
        if (celdaSeleccionadaMaquina == null) {
            lblMensajeTurno.setText("Error: Selecciona una celda antes de atacar.");
            return;
        }

        System.out.println("Jugador atacando: (" + filaAtaque + ", " + colAtaque + ")");

        Tablero tableroMaquina = maquina.getTableroPosicion();

        // 1. Ejecutar el disparo en el modelo
        ResultadoDisparo resultado = tableroMaquina.disparar(filaAtaque, colAtaque);

        // 2. Actualizar la vista de la celda atacada
        actualizarVistaAtaque(celdaSeleccionadaMaquina, resultado, tableroMaquina, gridMaquina);

        // 3. Revisar condición de victoria
        if (maquina.getFlota().estaFlotaHundida()) {
            mostrarMensajeFinal("¡GANASTE! Has hundido toda la flota enemiga.");
            return;
        }

        // 4. Mostrar resultado y reiniciar estado de selección
        lblMensajeTurno.setText("Resultado: " + resultado.name() + ". ¡Vuelve a seleccionar!");
        celdaSeleccionadaMaquina = null;
        btnAtacar.setDisable(true);
    }

    /* ---------- Lógica de Pintado de Celdas (Vista) ---------- */

    private void actualizarVistaAtaque(StackPane cell, ResultadoDisparo resultado, Tablero tablero, GridPane grid) {
        String color;

        switch (resultado) {
            case AGUA:
                color = "blue";
                break;
            case TOCADO:
                color = "orange";
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
                barcoHundido.getCeldas().forEach(c -> {
                    StackPane cellView = getCell(c.getColumna(), c.getFila(), grid);
                    if (cellView != null) {
                        cellView.setStyle("-fx-background-color: red; -fx-border-color: #b0b0b0;");
                    }
                });
            }
        }
    }

    private void mostrarMensajeFinal(String mensaje) {
        lblMensajeTurno.setText(mensaje);
        btnAtacar.setDisable(true);
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

    /**
     * Loads the machine colocation view and switches the current scene
     * to display the machine's board.
     * <p>
     * Any errors loading the FXML are printed to the standard error output.
     * </p>
     */


    private void irAColocacionMaquina() {
        if (this.juego == null) {
            System.err.println("ERROR: El objeto 'juego' no ha sido inicializado.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/machine-colocation-view.fxml"));
            Parent root = loader.load();

            // Inyección del modelo: se pasa el mismo objeto Juego.
            MachineColocationController machineController = loader.getController();
            machineController.setJuego(this.juego);

            // ********************************************************
            // PASO 2: Crear un NUEVO Stage para abrir una nueva ventana
            // ********************************************************
            Stage nuevaVentana = new Stage();
            nuevaVentana.setTitle("Vista de Colocación de la Máquina");
            nuevaVentana.setScene(new Scene(root));

            // Opcional: Bloquear la interacción con la ventana principal mientras está abierta
            // nuevaVentana.initModality(Modality.APPLICATION_MODAL);

            nuevaVentana.show();

            System.out.println("Nueva ventana de Colocación Máquina abierta exitosamente.");

        } catch (IOException ex) {
            System.err.println("Error al cargar machine-colocation-view.fxml:");
            ex.printStackTrace();
        }
    }
}