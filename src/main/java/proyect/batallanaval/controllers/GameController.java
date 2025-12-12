package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller responsible for managing the main game view, displaying both
 * the human player's board and the machine's board for attacks.
 */
public class GameController implements Initializable {

    @FXML private GridPane playerGrid;
    @FXML private GridPane gridMaquina;
    @FXML private Button btnAtacar;

    /** Assumed to exist and be an integer */
    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    private Juego juego;
    private Jugador humano;
    private Maquina maquina; // Type changed to Maquina in previous step

    /**
     * Initialization hook called by the JavaFX framework.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialize called - waiting for setJuego()");
    }

    /**
     * Injects the shared {@link Juego} instance and initializes both boards.
     * This is the entry point from the MachineColocationController.
     */
    public void setJuego(Juego juego) {
        System.out.println("=== setJuego called ===");
        this.juego = juego;
        assignReferences();

        if (playerGrid != null && gridMaquina != null) {
            System.out.println("Grids exist, initializing views...");

            // 1. Player Board: Ships visible (isPlayerBoard = true)
            inicializarVista(playerGrid, humano.getTableroPosicion(), humano.getFlota(), true);

            // 2. Machine Board: Empty cells only (isPlayerBoard = false)
            inicializarVista(gridMaquina, maquina.getTableroPosicion(), maquina.getFlota(), false);
        } else {
            System.out.println("ERROR: One or both grids are null!");
        }
    }

    private void assignReferences() {
        System.out.println("Assigning game references...");
        this.humano = juego.getJugador();
        this.maquina = juego.getMaquina(); // Fixed type compatibility
        System.out.println("References successfully assigned.");
    }

    /**
     * Initializes the view for a specific grid.
     */
    public void inicializarVista(GridPane grid, Tablero tablero, Flota flota, boolean isPlayerBoard) {
        inicializarGridBase(grid);

        if (isPlayerBoard) {
            // Only draw ships if it's the human player's board
            pintarFlotaEnTablero(flota, grid);
        }
    }

    /* ---------- Ship Painting Logic (FINAL CORRECTION) ---------- */

    private void pintarFlotaEnTablero(Flota flota, GridPane grid) {
        if (flota == null || flota.getBarcos().isEmpty()) {
            System.err.println("DIAGNOSTIC: Player fleet is empty. No ships to paint.");
            return;
        }

        flota.getBarcos().forEach(barco -> {
            barco.getCeldas().forEach(celda -> {

                // CRITICAL FIX: The search order MUST be (Column, Row) to match
                // how nodes were added to the GridPane (grid.add(cell, col, fila))
                StackPane cellView = getCell(
                        celda.getColumna(), // Column index
                        celda.getFila(),    // Row index
                        grid
                );

                if (cellView != null) {
                    // Final drawing of the ship view
                    cellView.getChildren().clear();
                    ShipCellView view = new ShipCellView(barco.getTipo(), CELL_SIZE);
                    cellView.getChildren().add(view);
                } else {
                    System.err.println("PAINTING FAILURE: Cell NOT found at Row=" + celda.getFila() + ", Col=" + celda.getColumna());
                }
            });
        });
    }

    /**
     * Retrieves the {@link StackPane} that corresponds to the given column and row.
     * Note: The search order is (columnIndex, rowIndex).
     */
    private StackPane getCell(int col, int fila, GridPane grid) {
        for (Node node : grid.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeFila = GridPane.getRowIndex(node);

            // Handle cases where index might be null (often defaults to 0 if not set)
            if (nodeCol == null) nodeCol = 0;
            if (nodeFila == null) nodeFila = 0;

            if (nodeCol == col && nodeFila == fila) {
                return (StackPane) node;
            }
        }
        return null;
    }

    /* ---------- Empty 10x10 Grid ---------- */

    /**
     * Builds an empty 10×10 grid with fixed-size cells.
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

                // IMPORTANT: Nodes are added as (columnIndex, rowIndex)
                grid.add(cell, col, fila);
            }
        }

        // Define the total grid size
        int total = Tablero.SIZE * CELL_SIZE + 2 * Tablero.SIZE;
        grid.setPrefSize(total, total);
        grid.setMinSize(total, total);
        grid.setMaxSize(total, total);
    }

    // [NEW] Attack button handler
    @FXML
    private void handleAttack() {
        System.out.println("Attack button pressed.");
        // Attack and turn change logic here.
    }
}