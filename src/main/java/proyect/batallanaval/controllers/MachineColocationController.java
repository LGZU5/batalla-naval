package proyect.batallanaval.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller responsible for displaying the machine's ship placement board
 * before the game begins. This view is strictly observational: the user cannot
 * modify or interact with the grid.
 *
 * <p>
 * When the shared {@link Juego} instance is injected via {@code setJuego}:
 * <ul>
 * <li>It ensures the machine's fleet is generated if not already present.</li>
 * <li>Builds a 10×10 grid of non-interactive cells.</li>
 * <li>Renders the machine’s fleet visually on the board.</li>
 * </ul>
 * </p>
 */
public class MachineColocationController implements Initializable {

    @FXML
    private GridPane gridTableroMaquina;

    @FXML
    private Button btnContinuar;

    /** Size (in pixels) of each cell */
    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    private Juego juego;
    private Maquina maquina;
    private Tablero tablero;
    private Flota flota;

    /**
     * Initializes the controller after the FXML view has been loaded.
     * <p>
     * This method is called automatically and is primarily used for setting up
     * the initial state of the components before the {@code Juego} instance
     * is available via {@code setJuego()}.
     * </p>
     *
     * @param url unused URL parameter (FXML requirement)
     * @param resourceBundle unused ResourceBundle parameter (FXML requirement)
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialization logic is deferred to setJuego, where model dependencies are met.
    }

    /**
     * Closes the current stage (the Machine Colocation View) when the "Continuar" button is pressed.
     *
     * @param event The action event triggered by the button click.
     */
    @FXML
    public void Continuar(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Injects the shared {@link Juego} instance used across scenes.
     * <p>
     * This method must be called by the controller that loads this view
     * to ensure both controllers operate on the same game state. It initializes
     * the machine's fleet if needed and builds the visual board.
     * </p>
     *
     * @param juego the game instance to associate with this controller
     * @throws NullPointerException if the provided {@code juego} or its internal machine/board objects are null.
     */
    public void setJuego(Juego juego) {
        // Use a try-catch block to handle potential NullPointerExceptions if setup is incomplete
        try {
            this.juego = juego;
            this.maquina = juego.getMaquina();
            this.tablero = maquina.getTableroPosicion();
            this.flota = maquina.getFlota();

            System.out.println("MC - juego: " + System.identityHashCode(juego));
            System.out.println("MC - flota máquina size AFTER generate: " + flota.getBarcos().size());

            // 1. Ensure the machine's fleet is complete
            if (!flota.estaCompleta()) {
                GeneradorFlotaAleatoria generador = new GeneradorFlotaAleatoria();
                generador.generarFlotaAleatoria(flota, tablero);
                System.out.println("Flota de la Máquina GENERADA aleatoriamente.");
            } else {
                System.out.println("Flota de la Máquina ya existía, usando flota previa.");
            }

            // 2. Initialize and paint the view with the final model
            if (gridTableroMaquina != null) {
                inicializarGrid();
                pintarFlotaEnTablero();
                System.out.println("Barcos en flota máquina: " + flota.getBarcos().size());
            } else {
                System.err.println("ERROR: gridTableroMaquina es null.");
            }
        } catch (NullPointerException e) {
            System.err.println("ERROR: setJuego recibió un objeto nulo o la inicialización del juego falló: " + e.getMessage());
            // Re-throw the standard unchecked exception for caller to address the setup failure
            throw new NullPointerException("Juego o sus componentes (Maquina/Tablero/Flota) son nulos en setJuego.");
        }
    }

    /* ---------- Grid 10x10 (10×10 read-only board) ---------- */

    /**
     * Builds a 10×10 grid of non-interactive cells representing
     * the machine's territory.
     * <p>
     * Each cell is a {@link StackPane} with fixed dimensions.
     * No event handlers are registered because this board is for visualization only.
     * </p>
     */
    private void inicializarGrid() {
        if (gridTableroMaquina == null) {
            System.err.println("Cannot initialize grid: gridTableroMaquina is null.");
            return;
        }

        gridTableroMaquina.getChildren().clear();
        gridTableroMaquina.getColumnConstraints().clear();
        gridTableroMaquina.getRowConstraints().clear();

        for (int i = 0; i < Tablero.SIZE; i++) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE);
            col.setMinWidth(CELL_SIZE);
            col.setMaxWidth(CELL_SIZE);
            col.setHgrow(Priority.NEVER);
            gridTableroMaquina.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints(CELL_SIZE);
            row.setMinHeight(CELL_SIZE);
            row.setMaxHeight(CELL_SIZE);
            row.setVgrow(Priority.NEVER);
            gridTableroMaquina.getRowConstraints().add(row);
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

                gridTableroMaquina.add(cell, col, fila);
            }
        }

        int total = Tablero.SIZE * CELL_SIZE + 2 * Tablero.SIZE;
        gridTableroMaquina.setPrefSize(total, total);
        gridTableroMaquina.setMinSize(total, total);
        gridTableroMaquina.setMaxSize(total, total);
    }

    /* ---------- Paint random fleet of the machine ---------- */
    /**
     * Iterates through the machine's fleet and renders each ship visually
     * on the corresponding cells of the grid.
     *
     * @throws NullPointerException if the {@code flota} object is null when trying to access its ships.
     */
    private void pintarFlotaEnTablero() {
        if (flota == null) {
            System.err.println("ERROR: Cannot paint fleet, Flota object is null.");
            // Re-throw the standard unchecked exception
            throw new NullPointerException("Flota object is null during rendering.");
        }

        for (Barco barco : flota.getBarcos()) {
            pintarBarco(barco);
        }
    }

    /**
     * Paints a single ship on the grid by rendering a {@link ShipCellView}
     * in each cell occupied by the ship.
     *
     * @param barco the ship to be visually rendered
     */
    private void pintarBarco(Barco barco) {
        barco.getCeldas().forEach(c -> {
            StackPane cell = getCell(c.getFila(), c.getColumna());
            if (cell == null) return;

            cell.getChildren().removeIf(n -> n instanceof ShipCellView);

            ShipCellView view = new ShipCellView(barco.getTipo(), CELL_SIZE);

            cell.getChildren().add(view);
        });
    }


    /**
     * Retrieves the {@link StackPane} that corresponds to the given row and column.
     *
     * @param fila the row index
     * @param col the column index
     * @return the matching StackPane, or {@code null} if not found
     */
    private StackPane getCell(int fila, int col) {
        if (gridTableroMaquina == null) return null;

        for (Node node : gridTableroMaquina.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && c != null && r == fila && c == col) {
                return (StackPane) node;
            }
        }
        return null;
    }
}