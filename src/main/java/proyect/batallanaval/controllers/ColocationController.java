package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller responsible for managing the ship placement phase (HU-1)
 * for the human player on their position board.
 * <p>
 * This controller allows the user to:
 * <ul>
 *     <li>View an initial predefined arrangement of all ships on the board.</li>
 *     <li>Select ships, drag them to new valid positions and rotate them.</li>
 *     <li>Validate that all ships are placed before continuing to the machine
 *         placement view.</li>
 * </ul>
 * It uses a 10×10 {@link GridPane} as the visual representation of the
 * player’s {@link Tablero}, and keeps the model and the view synchronized
 * while the player rearranges the fleet.
 * </p>
 */
public class ColocationController implements Initializable {

    @FXML
    private GridPane gridTablero;

    @FXML
    private Button btnRotar;

    @FXML
    private Button btnJugar;

    private Juego juego;
    private Jugador jugador;
    private Tablero tablero;
    private Flota flota;

    /** Size in pixels of each board cell*/
    private static final int CELL_SIZE = Tablero.CELL_SIZE;

    /** Currently selected ship (for rotation and movement). */
    private Barco barcoSeleccionado;

    /** Drag & drop*/
    private Barco barcoEnDrag;
    private int dragStartFila;
    private int dragStartCol;

    /**
     * Initializes the controller after the FXML has been loaded.
     * <p>
     * A new {@link Juego} instance is created for the human player.
     * The player, board and fleet are obtained from the model, the
     * grid is built, an initial placement is applied to the fleet,
     * and button handlers are configured.
     * </p>
     *
     * @param url            unused URL parameter required by {@link Initializable}
     * @param resourceBundle unused ResourceBundle parameter required by {@link Initializable}
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        juego = new Juego("Jugador");
        jugador = juego.getJugador();
        tablero = jugador.getTableroPosicion();
        flota = jugador.getFlota();

        inicializarGrid();
        inicializarFlotaEnTablero();
        configurarBotones();
    }

    /* ---------- Grid 10x10 ---------- */
    /**
     * Initializes the 10×10 grid that represents the player's board.
     * <p>
     * This method:
     * <ul>
     *     <li>Clears any previously defined content and constraints.</li>
     *     <li>Creates 10 columns and 10 rows with fixed size so the grid
     *         does not resize unexpectedly.</li>
     *     <li>Creates a {@link StackPane} cell for each position and assigns
     *         drag-and-drop handlers to support ship movement.</li>
     *     <li>Sets a fixed preferred, minimum and maximum size for the grid.</li>
     * </ul>
     * </p>
     */
    private void inicializarGrid() {
        gridTablero.getChildren().clear();
        gridTablero.getColumnConstraints().clear();
        gridTablero.getRowConstraints().clear();

        // The columns and rows are fixed
        for (int i = 0; i < Tablero.SIZE; i++) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE);
            col.setMinWidth(CELL_SIZE);
            col.setMaxWidth(CELL_SIZE);
            col.setHgrow(Priority.NEVER);
            gridTablero.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints(CELL_SIZE);
            row.setMinHeight(CELL_SIZE);
            row.setMaxHeight(CELL_SIZE);
            row.setVgrow(Priority.NEVER);
            gridTablero.getRowConstraints().add(row);
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

                cell.setOnDragOver(e -> onDragOver(e, cell));
                cell.setOnDragDropped(e -> onDragDropped(e, cell));

                gridTablero.add(cell, col, fila);
            }
        }

        // Total size of the grid is fixed (approximation including margins)
        int total = Tablero.SIZE * CELL_SIZE + 2 * Tablero.SIZE;
        gridTablero.setPrefSize(total, total);
        gridTablero.setMinSize(total, total);
        gridTablero.setMaxSize(total, total);
    }

    /* ---------- Initial fleet already deployed ---------- */
    /**
     * Places the initial fleet arrangement on the board.
     * <p>
     * Ships are placed in predefined positions and orientations, ensuring
     * that a complete fleet is available for the player to start adjusting
     * positions using drag-and-drop and rotation.
     * </p>
     */
    private void inicializarFlotaEnTablero() {
        colocarInicial(TipoBarco.PORTAAVIONES, 0, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.SUBMARINO,    1, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.SUBMARINO,    2, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.DESTRUCTOR,   3, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.DESTRUCTOR,   4, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.DESTRUCTOR,   5, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.FRAGATA,      6, 0, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.FRAGATA,      6, 2, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.FRAGATA,      6, 4, Orientacion.HORIZONTAL);
        colocarInicial(TipoBarco.FRAGATA,      6, 6, Orientacion.HORIZONTAL);
    }

    /**
     * Places a single ship of the given type at the specified starting position
     * and orientation, if the placement is valid.
     * <p>
     * If the board allows the placement, the ship is created in the model,
     * added to the fleet, and then rendered on the grid.
     * </p>
     *
     * @param tipo        type of ship to place
     * @param fila        starting row
     * @param col         starting column
     * @param orientacion orientation of the ship (horizontal or vertical)
     */
    private void colocarInicial(TipoBarco tipo, int fila, int col, Orientacion orientacion) {
        if (!tablero.puedeColocarBarco(fila, col, orientacion, tipo)) {
            return;
        }
        Barco barco = tablero.colocarBarco(fila, col, orientacion, tipo);
        flota.agregarBarco(barco);
        pintarBarco(barco);
    }

    /* ---------- Painted and selection ---------- */

    /**
     * Renders a ship on the grid according to its current cells in the model.
     * <p>
     * Existing ship views on those cells are removed, a new {@link ShipCellView}
     * is created for each cell, selection state is applied if necessary, and
     * mouse handlers for selection and drag-detection are registered.
     * </p>
     *
     * @param barco the ship to paint on the grid
     */
    private void pintarBarco(Barco barco) {
        boolean esSeleccionado = (barcoSeleccionado == barco);

        barco.getCeldas().forEach(c -> {
            StackPane cell = getCell(c.getFila(), c.getColumna());
            if (cell == null) return;

            // Remove only previous ship views
            cell.getChildren().removeIf(n -> n instanceof ShipCellView);

            ShipCellView view = new ShipCellView(barco.getTipo(), CELL_SIZE);

            if (esSeleccionado) {
                view.setSeleccionado(true);
            }

            view.setOnMouseClicked(e -> seleccionarBarco(barco, view));
            view.setOnDragDetected(e -> iniciarDragBarco(e, barco, view));

            cell.getChildren().add(view);
        });
    }

    /**
     * Returns the visual cell corresponding to the given row and column.
     *
     * @param fila row index
     * @param col  column index
     * @return the {@link StackPane} representing the cell, or {@code null} if not found
     */
    private StackPane getCell(int fila, int col) {
        for (Node node : gridTablero.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && c != null && r == fila && c == col) {
                return (StackPane) node;
            }
        }
        return null;
    }

    /**
     * Selects the given ship and updates the visual highlighting of all
     * ship cells on the grid.
     *
     * @param barco       the ship to select
     * @param clickedView the view that was clicked (not strictly required,
     *                    but useful if extended later)
     */
    private void seleccionarBarco(Barco barco, ShipCellView clickedView) {
        barcoSeleccionado = barco;

        // Clear current visual selection
        for (Node node : gridTablero.getChildren()) {
            StackPane cell = (StackPane) node;
            if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof ShipCellView view) {
                view.setSeleccionado(false);
            }
        }

        // Mark all cells of the selected ship
        barco.getCeldas().forEach(c -> {
            StackPane cell = getCell(c.getFila(), c.getColumna());
            if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof ShipCellView view) {
                view.setSeleccionado(true);
            }
        });
    }

    /* ---------- Drag & Drop ---------- */
    /**
     * Starts the drag operation for a ship when the user drags a ship view.
     * <p>
     * Drag is allowed only if the dragged ship is currently selected.
     * The dragboard is prepared with a simple string marker, and the
     * starting cell coordinates are stored to compute movement deltas.
     * </p>
     *
     * @param event the mouse event that triggered the drag detection
     * @param barco the ship being dragged
     * @param view  the visual representation of the ship's cell
     */
    private void iniciarDragBarco(MouseEvent event, Barco barco, ShipCellView view) {
        // Only the selected ship can be dragged
        if (barcoSeleccionado == null || barcoSeleccionado != barco) {
            return;
        }

        StackPane cell = (StackPane) view.getParent();
        int[] pos = (int[]) cell.getUserData();
        dragStartFila = pos[0];
        dragStartCol = pos[1];

        Dragboard db = view.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString("barco");
        db.setContent(content);

        barcoEnDrag = barcoSeleccionado;
        event.consume();
    }

    /**
     * Handles the drag-over event for a grid cell.
     * <p>
     * If the dragboard contains the expected data, this method accepts
     * the move transfer mode so that the drop can take place.
     * </p>
     *
     * @param event the drag event
     * @param cell  the cell over which the drag is occurring
     */
    private void onDragOver(DragEvent event, StackPane cell) {
        if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
    }

    /**
     * Handles the drop event on a grid cell.
     * <p>
     * Computes the movement delta from the drag start position to the
     * target cell, attempts to move the ship accordingly, and updates
     * the drag result.
     * </p>
     *
     * @param event the drag event containing the drop information
     * @param cell  the target cell where the drop occurred
     */
    private void onDragDropped(DragEvent event, StackPane cell) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasString() && barcoEnDrag != null) {
            int[] targetPos = (int[]) cell.getUserData();
            int targetFila = targetPos[0];
            int targetCol = targetPos[1];

            int deltaFila = targetFila - dragStartFila;
            int deltaCol  = targetCol - dragStartCol;

            if (moverBarcoConDelta(barcoEnDrag, deltaFila, deltaCol)) {
                success = true;
            }
            barcoEnDrag = null;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Moves a ship on the board by applying the given row and column deltas.
     * <p>
     * The method:
     * <ol>
     *     <li>Copies the current cells occupied by the ship.</li>
     *     <li>Computes the destination cells by adding the deltas.</li>
     *     <li>Validates board boundaries and checks for collisions with other ships.</li>
     *     <li>If the move is valid, clears the old cells in both model and view,</li>
     *     <li>Assigns the ship to the new cells and repaints it on the grid.</li>
     * </ol>
     * </p>
     *
     * @param barco     the ship to move
     * @param deltaFila the vertical offset to apply
     * @param deltaCol  the horizontal offset to apply
     * @return {@code true} if the move was valid and applied; {@code false} otherwise
     */
    private boolean moverBarcoConDelta(Barco barco, int deltaFila, int deltaCol) {
        // Copy of current cells; also validates that the ship has cells and that movement is non-zero
        List<Celda> actuales = new ArrayList<>(barco.getCeldas());
        if (actuales.isEmpty() || (deltaFila == 0 && deltaCol == 0)) {
            return false;
        }

        // List for new positions of the ship
        List<Celda> nuevasCeldas = new ArrayList<>();

        for (Celda c : actuales) {
            // Does the math for the new position of the cell by adding the deltas
            int nuevaFila = c.getFila() + deltaFila;
            int nuevaCol  = c.getColumna() + deltaCol;

            // Verifies that the new position is between the limits
            if (nuevaFila < 0 || nuevaFila >= Tablero.SIZE
                    || nuevaCol < 0 || nuevaCol >= Tablero.SIZE) {
                return false;
            }

            // Obtains new destination
            Celda destino = tablero.getCelda(nuevaFila, nuevaCol);

            // Detects collisions
            if (destino.tieneBarco() && !actuales.contains(destino)) {
                return false;
            }

            // If the cell is valid it gets added to the list of new cells
            nuevasCeldas.add(destino);
        }

        // Cleans the model and the old view
        for (Celda c : actuales) {
            StackPane cellView = getCell(c.getFila(), c.getColumna());
            if (cellView != null) {
                cellView.getChildren().removeIf(n -> n instanceof ShipCellView);
            }
            c.setBarco(null);
            c.setEstado(EstadoCelda.VACIA);
        }
        barco.getCeldas().clear();

        // Puts the ship in the new position
        for (Celda destino : nuevasCeldas) {
            destino.setBarco(barco);
            destino.setEstado(EstadoCelda.BARCO);
            barco.getCeldas().add(destino);
        }

        pintarBarco(barco);

        return true;
    }


    /**
     * Loads the game view and switches the current scene
     * to display the machine's board.
     * <p>
     * Any errors loading the FXML are printed to the standard error output.
     * </p>
     */
    @FXML
    private void irAJuego() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/proyect/batallanaval/game-view.fxml")
            );
            Parent root = loader.load();

            GameController gameController = loader.getController();
            gameController.setJuego(this.juego);

            Stage stage = (Stage) btnJugar.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            System.out.println("Vista de juego cargada correctamente");
        } catch (IOException ex) {
            System.err.println("Error al cargar game-view.fxml:");
            ex.printStackTrace();
        }
    }

    /* ---------- Buttons ---------- */
    /**
     * Configures the behavior of the rotation and play buttons.
     * <ul>
     *     <li>{@code btnRotar}: rotates the currently selected ship if the
     *     rotation is valid (no collisions and within board boundaries).</li>
     *     <li>{@code btnJugar}: checks that all ships are placed and, if so,
     *     switches to the machine colocation view.</li>
     * </ul>
     */
    private void configurarBotones() {
        btnRotar.setOnAction(e -> {
            if (barcoSeleccionado == null) {
                return;
            }

            // Current cells of the ship
            List<Celda> actuales = new ArrayList<>(barcoSeleccionado.getCeldas());
            if (actuales.isEmpty()) {
                return;
            }

            // Origin: first cell used as pivot
            Celda primera = actuales.get(0);
            int filaOrigen = primera.getFila();
            int colOrigen  = primera.getColumna();

            Orientacion orientacionActual = barcoSeleccionado.getOrientacion();
            Orientacion nuevaOrientacion =
                    (orientacionActual == Orientacion.HORIZONTAL)
                            ? Orientacion.VERTICAL
                            : Orientacion.HORIZONTAL;

            // Validate new orientation ignoring the ship's own current cells
            if (!puedeReubicarBarcoIgnorandoPropiasCeldasRotacion(
                    barcoSeleccionado, filaOrigen, colOrigen, nuevaOrientacion)) {
                return; // cannot rotate without collision or going out of bounds
            }

            // Clear old model + view
            for (Celda c : actuales) {
                StackPane cell = getCell(c.getFila(), c.getColumna());
                if (cell != null) {
                    cell.getChildren().removeIf(n -> n instanceof ShipCellView); //Now it only removes the ship
                }
                c.setBarco(null);
                c.setEstado(EstadoCelda.VACIA);
            }
            barcoSeleccionado.getCeldas().clear();

            // Re-place with new orientation
            barcoSeleccionado.setOrientacion(nuevaOrientacion);
            Barco nuevoBarco = tablero.colocarBarco(filaOrigen, colOrigen,
                    nuevaOrientacion, barcoSeleccionado.getTipo());
            barcoSeleccionado.getCeldas().addAll(nuevoBarco.getCeldas());

            pintarBarco(barcoSeleccionado);  // keep it highlighted
        });

        btnJugar.setOnAction(e -> {
            // Validate that all ships have been placed
            if (flota.getBarcos().size() < 10) { // 1+2+3+4 = 10 ships
                System.out.println("Debes colocar todos los barcos");
                return;
            }

            // Go to machine colocation view
            irAJuego();
        });
    }

    /**
     * Loads the machine colocation view and switches the current scene
     * to display the machine's board.
     * <p>
     * Any errors loading the FXML are printed to the standard error output.
     * </p>
     */
    private void irAColocacionMaquina() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/machine-colocation-view.fxml"));
            Parent root = loader.load();

            MachineColocationController machineController = loader.getController();
            machineController.setJuego(this.juego); // Pass the 'juego' object WITH the player's fleet

            Stage stage = (Stage) btnJugar.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show(); // You must call show() to update the scene

            System.out.println("Transición a Colocación Máquina exitosa.");
        } catch (IOException ex) {
            System.err.println("Error al cargar machine-colocation-view.fxml:");
            ex.printStackTrace();
        }
    }

    /**
     * Validates if the ship can be rotated to a new orientation at the given origin
     * position, ignoring its own current cells.
     *
     * @param barco            the ship to rotate
     * @param filaOrigen       origin row used as pivot
     * @param colOrigen        origin column used as pivot
     * @param nuevaOrientacion new orientation to be tested
     * @return {@code true} if the rotation is valid (no collisions and within bounds);
     *         {@code false} otherwise
     */
    private boolean puedeReubicarBarcoIgnorandoPropiasCeldasRotacion(
            Barco barco, int filaOrigen, int colOrigen, Orientacion nuevaOrientacion) {

        int longitud = barco.getTipo().getSize();
        List<Celda> celdasActuales = new ArrayList<>(barco.getCeldas());

        for (int i = 0; i < longitud; i++) {
            // If horizontal increases columns, if vertical increases rows
            int f = (nuevaOrientacion == Orientacion.HORIZONTAL) ? filaOrigen : filaOrigen + i;
            int c = (nuevaOrientacion == Orientacion.HORIZONTAL) ? colOrigen + i : colOrigen;

            // Out of board boundaries
            if (f < 0 || f >= Tablero.SIZE || c < 0 || c >= Tablero.SIZE) {
                return false;
            }

            Celda destino = tablero.getCelda(f, c);

            boolean esMismaCeldaDelBarco = celdasActuales.contains(destino);
            if (destino.tieneBarco() && !esMismaCeldaDelBarco) {
                return false; // Would collide with another ship
            }
        }
        return true;
    }


}
