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
 * before the game begins.This view is strictly observational: the user cannot
 * modify or interact with the grid.
 *
 * <p>
 * When initialized, the controller:
 * <ul>
 * <li>Retrieves or creates the shared {@link Juego} instance.</li>
 * <li>Accesses the machine's {@link Tablero} and {@link Flota}.</li>
 * <li>Generates a random fleet if one has not yet been created.</li>
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
     * If the {@code Juego} instance has not been injected externally,
     * a fallback game is created for testing purposes.
     * </p>
     * This method prepares the machine's board, ensures its fleet is generated,
     * builds the graphical grid, and paints the ships on it.
     *
     * @param url unused URL parameter (FXML requirement)
     * @param resourceBundle unused ResourceBundle parameter (FXML requirement)
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
// If the game passes from another scene, it is assigned as setJuego(juego)
        if (juego == null) {
// fallback for testing
            juego = new Juego("Jugador");
        }
    }

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
     * to ensure both controllers operate on the same game state.
     * </p>
     *
     * @param juego the game instance to associate with this controller
     */
    public void setJuego(Juego juego) {
        this.juego = juego;
        this.maquina = juego.getMaquina();
        this.tablero = maquina.getTableroPosicion();
        this.flota = maquina.getFlota();

        if (!flota.estaCompleta()) {
            GeneradorFlotaAleatoria generador = new GeneradorFlotaAleatoria();
            generador.generarFlotaAleatoria(flota, tablero);
            System.out.println("Flota de la Máquina GENERADA aleatoriamente.");
        } else {
            System.out.println("Flota de la Máquina ya existía, usando flota previa.");
        }

        // Inicializar y pintar la vista con el modelo final
        if (gridTableroMaquina != null) {
            inicializarGrid();
            pintarFlotaEnTablero();
            System.out.println("Barcos en flota máquina: " + flota.getBarcos().size());
        } else {
            System.err.println("ERROR: gridTableroMaquina es null.");
        }
    }

    /* ---------- Grid 10x10 (10×10 read-only board) ---------- */

    /**
     * Builds a 10×10 grid of non-interactive cells representing
     * the machine's territory.
     * <p>
     * Each cell is a {@link StackPane} with fixed dimensions.
     * No event handlers (click, drag, etc.) are registered because
     * this board is intended for visualization only.
     * </p>
     */
    private void inicializarGrid() {
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

    /* ---------- Pintar flota aleatoria de la máquina ---------- */
    /**
     * Iterates through the machine's fleet and renders each ship visually
     * on the corresponding cells of the grid.
     */
    private void pintarFlotaEnTablero() {
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