package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller responsible for managing the main game board where
 * the human player shoots at the machine's territory (HU-2).
 * <p>
 * This controller:
 * <ul>
 *     <li>Receives a shared {@link Juego} instance via {@link #setJuego(Juego)}.</li>
 *     <li>Obtains references to the human player, the machine, and
 *         the machine's board and fleet.</li>
 *     <li>Initializes a 10×10 grid representing the machine's board.</li>
 *     <li>Provides utility methods to paint the machine's fleet on the grid.</li>
 * </ul>
 * The shooting logic and turn management can be built on top of the
 * grid structure created here.
 * </p>
 */
public class GameController {

    @FXML
    private GridPane gridMaquina;

    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    private Juego juego;
    private Jugador humano;
    private Maquina maquina;

    private Tablero tableroMaquina;
    private Flota flotaMaquina;

    /**
     * Initialization hook called by the JavaFX framework.
     * <p>
     * At this point, the {@link #setJuego(Juego)} method has not
     * been called yet. RIGHT NOW IT DOES NOTHING, ONLY PRINTS, WAITING FOR THE ATTACKS
     * </p>
     *
     * @param url            unused URL parameter
     * @param resourceBundle unused ResourceBundle parameter
     */
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialize llamado - esperando setJuego()");
    }

    /**
     * Injects the shared {@link Juego} instance used by the application.
     * <p>
     * Once the game is set, this method assigns local references to the
     * human player, the machine, the machine's board and fleet, and
     * initializes the visual grid if it is already available.
     * </p>
     *
     * @param juego the game instance to associate with this controller
     */
    public void setJuego(Juego juego) {
        System.out.println("=== setJuego llamado ===");
        this.juego = juego;
        asignarReferencias();

        if (gridMaquina != null) {
            System.out.println("gridMaquina existe, inicializando vista...");
            inicializarVista();
        } else {
            System.out.println("ERROR: gridMaquina es null!");
        }
    }

    /**
     * Assigns references to the human player, the machine,
     * the machine's board and its fleet from the {@link Juego} model.
     */
    private void asignarReferencias() {
        System.out.println("Asignando referencias del juego...");
        this.humano = juego.getJugador();
        this.maquina = juego.getMaquina();
        this.tableroMaquina = maquina.getTableroPosicion();
        this.flotaMaquina = maquina.getFlota();
        System.out.println("Referencias asignadas correctamente");
    }

    /**
     * Initializes the visual representation of the machine's board.
     * <p>
     * This method:
     * <ul>
     *     <li>Builds an empty 10×10 grid with fixed cell sizes.</li>
     *     <li>Logs diagnostic information to the console.</li>
     * </ul>
     * The fleet can later be painted on this grid using
     * {@link #pintarFlotaEnTablero(Flota, GridPane)}.
     * </p>
     */
    public void inicializarVista() {
        System.out.println("=== Inicializando tablero de juego ===");
        System.out.println("gridMaquina: " + gridMaquina);
        System.out.println("Barcos máquina: " + flotaMaquina.getBarcos().size());

        inicializarGrid(gridMaquina);

        System.out.println("Grid inicializado - Children count: " + gridMaquina.getChildren().size());
        System.out.println("Grid size: " + gridMaquina.getPrefWidth() + "x" + gridMaquina.getPrefHeight());

        System.out.println("=== Tablero inicializado correctamente ===");
    }

    /* ---------- Grid 10x10 vacío ---------- */

    /**
     * Builds an empty 10×10 grid with fixed-size cells.
     * <p>
     * This method:
     * <ul>
     *     <li>Clears any existing children and constraints.</li>
     *     <li>Creates 10 columns and 10 rows with fixed widths and heights.</li>
     *     <li>Creates a {@link StackPane} for each cell, with background and border styles.</li>
     *     <li>Sets a fixed preferred, minimum and maximum size for the grid.</li>
     * </ul>
     * </p>
     *
     * @param grid the {@link GridPane} to initialize
     */
    private void inicializarGrid(GridPane grid) {
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

}
