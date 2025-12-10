package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import proyect.batallanaval.models.*;
import proyect.batallanaval.views.ShipCellView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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

    private static final int CELL_SIZE = 30;

    // barco seleccionado (para mover/rotar)
    private Barco barcoSeleccionado;

    // drag & drop
    private Barco barcoEnDrag;
    private int dragStartFila;
    private int dragStartCol;

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
    /** Initializes the 10x10 grid that represents the board for the game
     * Clears any previously defined content, columns and row constraints.
     * Creates 10 columns and rows with a fixed size so that the grid doesn't move
     * Assigns the events to each cell for the drag-and-drop system
     * The grid keeps a fixed size so that it doesn't resize unnecessarily
     * **/
    private void inicializarGrid() {
        gridTablero.getChildren().clear();
        gridTablero.getColumnConstraints().clear();
        gridTablero.getRowConstraints().clear();

        // The clumns and rows are fixed
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

        // Total size of the grid is fixed
        int total = Tablero.SIZE * CELL_SIZE + 2 * Tablero.SIZE; // an aprox adding the margins
        gridTablero.setPrefSize(total, total);
        gridTablero.setMinSize(total, total);
        gridTablero.setMaxSize(total, total);
    }

    /* ---------- Flota inicial ya colocada ---------- */

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

    private void colocarInicial(TipoBarco tipo, int fila, int col, Orientacion orientacion) {
        if (!tablero.puedeColocarBarco(fila, col, orientacion, tipo)) {
            return;
        }
        Barco barco = tablero.colocarBarco(fila, col, orientacion, tipo);
        flota.agregarBarco(barco);
        pintarBarco(barco);
    }

    /* ---------- Pintado y selección ---------- */

    private void pintarBarco(Barco barco) {
        boolean esSeleccionado = (barcoSeleccionado == barco);

        barco.getCeldas().forEach(c -> {
            StackPane cell = getCell(c.getFila(), c.getColumna());
            if (cell == null) return;

            // Quitar solo la vista del barco, no las demás fichas
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

    private void seleccionarBarco(Barco barco, ShipCellView clickedView) {
        barcoSeleccionado = barco;

        // limpiar selección visual
        for (Node node : gridTablero.getChildren()) {
            StackPane cell = (StackPane) node;
            if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof ShipCellView view) {
                view.setSeleccionado(false);
            }
        }

        // marcar todas las celdas del barco seleccionado
        barco.getCeldas().forEach(c -> {
            StackPane cell = getCell(c.getFila(), c.getColumna());
            if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof ShipCellView view) {
                view.setSeleccionado(true);
            }
        });
    }

    /* ---------- Drag & Drop ---------- */

    private void iniciarDragBarco(MouseEvent event, Barco barco, ShipCellView view) {
        // solo se puede arrastrar el barco seleccionado
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

    private void onDragOver(DragEvent event, StackPane cell) {
        if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
    }

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

    private boolean puedeReubicarBarcoIgnorandoPropiasCeldas(Barco barco,
                                                             int filaOrigen,
                                                             int colOrigen) {
        int longitud = barco.getTipo().getSize();
        Orientacion orientacion = barco.getOrientacion();

        // conjunto de celdas actuales del barco para poder ignorarlas
        List<Celda> celdasActuales = new ArrayList<>(barco.getCeldas());

        for (int i = 0; i < longitud; i++) {
            int f = (orientacion == Orientacion.HORIZONTAL) ? filaOrigen : filaOrigen + i;
            int c = (orientacion == Orientacion.HORIZONTAL) ? colOrigen : colOrigen + i;

            // fuera del tablero
            if (f < 0 || f >= Tablero.SIZE || c < 0 || c >= Tablero.SIZE) {
                return false;
            }

            Celda celdaDestino = tablero.getCelda(f, c);

            // si la celda destino ya pertenece a este mismo barco, la ignoramos
            boolean esParteDelMismoBarco = celdasActuales.contains(celdaDestino);
            if (celdaDestino.tieneBarco() && !esParteDelMismoBarco) {
                return false; // colisión con otro barco
            }
        }
        return true;
    }

    /*** Private method to move ship:
     * Calculates the ship's current cells
     * Calculates the destination cells
     * Calculates the new cell position
     * Checks for boundaries and collisions with other ships cell by cell
     * If everything is valid, clears old cells and assigns the ship***/
    private boolean moverBarcoConDelta(Barco barco, int deltaFila, int deltaCol) {
        //Copy of cells that the ship is o, validates that the ship can move and has cells
        List<Celda> actuales = new ArrayList<>(barco.getCeldas());
        if (actuales.isEmpty() || (deltaFila == 0 && deltaCol == 0)) {
            return false;
        }

        //List for new positions of the ship
        List<Celda> nuevasCeldas = new ArrayList<>();

        for (Celda c : actuales) {
            //Does the math for the new position of the cell by adding the deltas
            int nuevaFila = c.getFila() + deltaFila;
            int nuevaCol  = c.getColumna() + deltaCol;

            //Verifies that the new position is between the limits
            if (nuevaFila < 0 || nuevaFila >= Tablero.SIZE
                    || nuevaCol < 0 || nuevaCol >= Tablero.SIZE) {
                return false;
            }

            //Obtains new destination
            Celda destino = tablero.getCelda(nuevaFila, nuevaCol);

            //Detects collisions
            if (destino.tieneBarco() && !actuales.contains(destino)) {
                return false;
            }

            //If the cell is valid it gets added to the list of new cells
            nuevasCeldas.add(destino);
        }

        //Cleans the model and the old view
        for (Celda c : actuales) {
            StackPane cellView = getCell(c.getFila(), c.getColumna());
            if (cellView != null) {
                cellView.getChildren().removeIf(n -> n instanceof ShipCellView);
            }
            c.setBarco(null);
            c.setEstado(EstadoCelda.VACIA);
        }
        barco.getCeldas().clear();

        //Puts the ship in the new position
        for (Celda destino : nuevasCeldas) {
            destino.setBarco(barco);
            destino.setEstado(EstadoCelda.BARCO);
            barco.getCeldas().add(destino);
        }

        pintarBarco(barco);

        return true;
    }


    /* ---------- Botones ---------- */

    private void configurarBotones() {
        btnRotar.setOnAction(e -> {
            if (barcoSeleccionado == null) {
                return;
            }

            // celdas actuales del barco
            List<Celda> actuales = new ArrayList<>(barcoSeleccionado.getCeldas());
            if (actuales.isEmpty()) {
                return;
            }

            // origen: tomamos la primera celda como pivote
            Celda primera = actuales.get(0);
            int filaOrigen = primera.getFila();
            int colOrigen  = primera.getColumna();

            Orientacion orientacionActual = barcoSeleccionado.getOrientacion();
            Orientacion nuevaOrientacion =
                    (orientacionActual == Orientacion.HORIZONTAL)
                            ? Orientacion.VERTICAL
                            : Orientacion.HORIZONTAL;

            // validar nueva orientación ignorando las propias celdas
            if (!puedeReubicarBarcoIgnorandoPropiasCeldasRotacion(
                    barcoSeleccionado, filaOrigen, colOrigen, nuevaOrientacion)) {
                return; // no se puede rotar sin chocar o salirse
            }

            // limpiar modelo + vista antiguas
            for (Celda c : actuales) {
                StackPane cell = getCell(c.getFila(), c.getColumna());
                if (cell != null) {
                    cell.getChildren().removeIf(n -> n instanceof ShipCellView); //Now it only removes the ship
                }
                c.setBarco(null);
                c.setEstado(EstadoCelda.VACIA);
            }
            barcoSeleccionado.getCeldas().clear();

            // recolocar con nueva orientación
            barcoSeleccionado.setOrientacion(nuevaOrientacion);
            Barco nuevoBarco = tablero.colocarBarco(filaOrigen, colOrigen,
                    nuevaOrientacion, barcoSeleccionado.getTipo());
            barcoSeleccionado.getCeldas().addAll(nuevoBarco.getCeldas());

            pintarBarco(barcoSeleccionado); // mantiene resaltado
        });

        btnJugar.setOnAction(e -> {
            System.out.println("¡Juego iniciado!");
            // aquí cambias a la vista de juego
        });
    }

    /**
     * Valida si el barco puede rotar en (filaOrigen,colOrigen) a nuevaOrientacion,
     * ignorando sus propias celdas actuales.
     */
    private boolean puedeReubicarBarcoIgnorandoPropiasCeldasRotacion(
            Barco barco, int filaOrigen, int colOrigen, Orientacion nuevaOrientacion) {

        int longitud = barco.getTipo().getSize();
        List<Celda> celdasActuales = new ArrayList<>(barco.getCeldas());

        for (int i = 0; i < longitud; i++) {
            //If horizontal increases columns, if vertical increases rows
            int f = (nuevaOrientacion == Orientacion.HORIZONTAL) ? filaOrigen : filaOrigen + i;
            int c = (nuevaOrientacion == Orientacion.HORIZONTAL) ? colOrigen + i : colOrigen;

            // fuera de tablero
            if (f < 0 || f >= Tablero.SIZE || c < 0 || c >= Tablero.SIZE) {
                return false;
            }

            Celda destino = tablero.getCelda(f, c);

            boolean esMismaCeldaDelBarco = celdasActuales.contains(destino);
            if (destino.tieneBarco() && !esMismaCeldaDelBarco) {
                return false; // chocaría con otro barco
            }
        }
        return true;
    }


}
