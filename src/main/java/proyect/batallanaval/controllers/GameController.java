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
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controller responsible for managing the main game view, displaying both
 * the human player's board and the machine's board for attacks.
 * Funcionalidad enfocada únicamente en el ataque del jugador.
 */
public class GameController implements Initializable {

    private GestorPartida gestorPartida;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialize llamado - esperando setJuego()");

        this.gestorPartida = new GestorPartida();

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

    private void inicializarVistaConEstados(GridPane grid, Tablero tablero, Flota flota, boolean isPlayerBoard) {
        inicializarGridBase(grid);

        if (isPlayerBoard && flota != null && !flota.getBarcos().isEmpty()) {
            pintarFlotaEnTablero(flota, grid);
        }

        aplicarEstadosDeAtaque(grid, tablero, isPlayerBoard);
    }

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

        // Verificar barcos hundidos sin estado HUNDIDA
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

    private void assignReferences() {
        System.out.println("Asignando referencias de juego...");
        this.humano = juego.getJugador();
        this.maquina = juego.getMaquina();
        System.out.println("Referencias asignadas.");
    }

    private void actualizarMensajeTurno() {
        if (juego.juegoTerminado()) {
            lblMensajeTurno.setText("¡Juego Terminado!");
            return;
        }

        if (juego.esTurnoJugador()) {
            // Asumiendo que el Jugador Humano siempre debe seleccionar una celda
            lblMensajeTurno.setText("Tu turno: Selecciona una casilla para atacar.");
        } else {
            lblMensajeTurno.setText("Turno de la Máquina: Esperando ataque...");
        }
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
                this::actualizarVistaConGuardado,
                this::verificarGanadorConLimpieza          // Winner check callback
        );

        maquinaThread.start();
        System.out.println("Thread de la máquina iniciado.");
    }

    private void actualizarVistaConGuardado() {
        // Guardar automáticamente después del disparo de la máquina
        gestorPartida.guardarPartida(humano, maquina);

        // Actualizar vista normal
        actualizarVistaCompleta();
    }

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

    private void verificarGanadorConLimpieza() {
        System.out.println("=== VERIFICANDO GANADOR ===");
        System.out.println("Flota Máquina hundida: " + maquina.getFlota().estaFlotaHundida());
        System.out.println("Flota Jugador hundida: " + humano.getFlota().estaFlotaHundida());

        if (juego.haGanadoJugador()) {
            System.out.println("¡JUGADOR GANÓ!");
            detenerThreadMaquina();
            // ✅ AGREGAR: Eliminar partida al ganar
            gestorPartida.eliminarPartidaGuardada();
            mostrarAlerta("¡VICTORIA!", "¡GANASTE! Has hundido toda la flota enemiga.", Alert.AlertType.INFORMATION);
        } else if (juego.haGanadoMaquina()) {
            System.out.println("¡MÁQUINA GANÓ!");
            detenerThreadMaquina();
            // ✅ AGREGAR: Eliminar partida al perder
            gestorPartida.eliminarPartidaGuardada();
            mostrarAlerta("DERROTA", "La máquina ha hundido toda tu flota. ¡Mejor suerte la próxima vez!", Alert.AlertType.INFORMATION);
        } else {
            System.out.println("Juego continúa...");
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

                // 1. Condición principal: Solo actualizamos si la celda ha sido atacada.
                if (estado == EstadoCelda.TOCADA ||
                        estado == EstadoCelda.HUNDIDA ||
                        estado == EstadoCelda.AGUA_TOCADA) {

                    // 2. Lógica de repintado de IMPACTOS (Máquina al Jugador)

                    if (estado == EstadoCelda.AGUA_TOCADA) {
                        // La Máquina ha disparado al agua del jugador.
                        cell.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;"); // AZUL
                    } else if (estado == EstadoCelda.HUNDIDA) {
                        // La Máquina ha hundido un barco (ROJO)
                        // Si el barco está hundido, pintamos esa celda de rojo.
                        pintarBarcoHundido(cell, tableroJugador, playerGrid);                    } else if (estado == EstadoCelda.TOCADA) {
                        // La Máquina ha tocado un barco (NARANJA)
                        cell.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
                    }
                }
                // Si el estado es VACIA o BARCO, se omite y mantiene el color inicial (gris o el ShipCellView).
            }
        }
    }

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

        EstadoCelda estadoActual = tableroMaquina.getCelda(fila, col).getEstado();

        // 1. Verifica si la celda ya fue atacada (INCLUYENDO AGUA_TOCADA)
        if (estadoActual == EstadoCelda.TOCADA ||
                estadoActual == EstadoCelda.HUNDIDA ||
                estadoActual == EstadoCelda.AGUA_TOCADA) { // <--- Inclusión de AGUA_TOCADA

            mostrarAlerta("Celda ya atacada", "¡Ya atacaste esa celda! Selecciona otra.", Alert.AlertType.WARNING);

            // ¡IMPORTANTE! Si ya fue atacada y no se va a seleccionar,
            // no debemos continuar con la lógica de selección/deselección que sigue.
            return;
        }

        // 1. Desmarcar la celda previamente seleccionada
        if (celdaSeleccionadaMaquina != null) {
            int[] prevPos = (int[]) celdaSeleccionadaMaquina.getUserData();
            EstadoCelda prevEstado = tableroMaquina.getCelda(prevPos[0], prevPos[1]).getEstado();

            // RESTAURACIÓN CORREGIDA: Restauramos el color basado en el estado persistente.

            // Si el estado previo era AGUA_TOCADA, TOCADA o HUNDIDA,
            // restaurar al color que le corresponde.
            if (prevEstado == EstadoCelda.AGUA_TOCADA) {
                // Si la previa era AGUA_TOCADA, restaurar a AZUL
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: #4444ff; -fx-border-color: #b0b0b0;");
            }
            else if (prevEstado == EstadoCelda.TOCADA) {
                // Si la previa era TOCADA, restaurar a NARANJA
                celdaSeleccionadaMaquina.setStyle("-fx-background-color: orange; -fx-border-color: #b0b0b0;");
            }
            // Nota: HUNDIDA se maneja con pintarBarcoHundido y suele ser rojo.
            // Aquí no necesitamos una condición HUNDIDA porque el hundimiento es permanente.

            else {
                // Si el estado previo era VACIA o BARCO (es decir, NO ATACADA), restaurar al GRIS base.
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
            gestorPartida.guardarPartida(humano, maquina);

            // 2. Actualizar la vista de la celda atacada
            actualizarVistaAtaque(celdaSeleccionadaMaquina, resultado, maquina.getTableroPosicion(), gridMaquina);

            // 3. Revisar condición de victoria
            if (juego.haGanadoJugador()) {
                gestorPartida.eliminarPartidaGuardada();
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