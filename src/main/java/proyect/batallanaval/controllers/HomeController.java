package proyect.batallanaval.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import proyect.batallanaval.models.*;
import proyect.batallanaval.controllers.GameManager.PartidaGuardada;
import proyect.batallanaval.views.ColocationView;
import proyect.batallanaval.views.InstructionsView;

import proyect.batallanaval.exceptions.CargaPartidaException;
import proyect.batallanaval.exceptions.VistaNoCargadaException;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller for the main application home screen (Home View).
 * Manages actions related to quitting, viewing instructions, starting a new game,
 * and loading a saved game.
 */
public class HomeController {

    private GameManager gameManager;

    /**
     * Initializes the controller and creates a new {@code GestorPartida} instance
     * for persistence management.
     */
    public HomeController() {
        this.gameManager = new GameManager();
    }

    /**
     * Handles the application exit when the Quit button is pressed.
     */
    @FXML
    private void onQuit() {
        System.exit(0);
    }

    /**
     * Loads and displays the Instructions View.
     *
     * @param event The action event triggered by the user.
     * @throws VistaNoCargadaException If the instructions FXML file cannot be loaded.
     */
    @FXML
    public void onGuia(ActionEvent event) throws VistaNoCargadaException {
        try {
            InstructionsView instructionsView = InstructionsView.getInstance();
            instructionsView.show();

            // Close the home window
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            // Wrapping IOException into a custom exception
            mostrarError("Error al cargar la vista de instrucciones.");
            throw new VistaNoCargadaException("Failed to load InstructionsView.", e);
        }
    }

    /**
     * Initiates the game flow. Checks for a saved game and prompts the user
     * to continue or start a new game.
     *
     * @param event The action event triggered by the user.
     */
    @FXML
    public void onPlay(ActionEvent event) throws VistaNoCargadaException {
        if (gameManager.existePartidaGuardada()) {
            boolean continuar = mostrarDialogoContinuar();

            if (continuar) {
                try {
                    cargarYContinuarPartida(event);
                } catch (CargaPartidaException | IOException | ClassNotFoundException e) {
                    mostrarError(e.getMessage());
                    // If loading fails, start a new game as a fallback
                    iniciarNuevaPartida(event);
                }
                return;
            } else {
                // User chose New Game, so delete the old save file
                gameManager.eliminarPartidaGuardada();
            }
        }

        iniciarNuevaPartida(event);
    }

    /**
     * Displays a confirmation dialog asking the user whether they want to continue
     * the saved game or start a new one.
     *
     * @return {@code true} if the user selects 'Continuar', {@code false} otherwise.
     */
    private boolean mostrarDialogoContinuar() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Partida guardada");
        alert.setHeaderText("Se encontró una partida guardada");
        alert.setContentText("¿Deseas continuar donde lo dejaste?");

        ButtonType btnContinuar = new ButtonType("Continuar");
        ButtonType btnNueva = new ButtonType("Nueva Partida");

        alert.getButtonTypes().setAll(btnContinuar, btnNueva);

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(btnNueva) == btnContinuar;
    }

    /**
     * Loads the saved game state, reconstructs the {@code Juego} model, and
     * navigates to the {@code GameController}.
     *
     * @param event The action event to extract the current stage.
     * @throws CargaPartidaException If the saved data cannot be loaded or is corrupted.
     */
    private void cargarYContinuarPartida(ActionEvent event) throws CargaPartidaException, IOException, ClassNotFoundException {
        PartidaGuardada partida = gameManager.cargarPartida();

        if (partida == null) {
            String msg = "No se pudo cargar la partida guardada. Iniciando nueva partida.";
            mostrarError(msg);
            throw new CargaPartidaException(msg);
        }

        try {
            // Reconstruct the Player model
            Jugador jugadorRecuperado = new Jugador(
                    partida.nickname,
                    partida.tableroJugador,
                    partida.flotaJugador
            );

            // Reconstruct the Machine model
            Maquina maquinaRecuperada = new Maquina(
                    "CPU",
                    partida.tableroMaquina,
                    partida.flotaMaquina
            );

            // Reconstruct the Game model
            Juego juegoRecuperado = new Juego(jugadorRecuperado, maquinaRecuperada);

            abrirGameView(juegoRecuperado, event);

            System.out.println("✅ Partida cargada correctamente - Saltando a GameView");

        } catch (VistaNoCargadaException e) {
            // Propagate the view loading error
            throw new CargaPartidaException("Error al abrir la vista de juego después de cargar la partida.", e);
        } catch (Exception e) {
            // Catch any unexpected data errors during model reconstruction and wrap them
            e.printStackTrace();
            throw new CargaPartidaException("Error al cargar la partida: " + e.getMessage(), e);
        }
    }

    /**
     * Starts a new game by navigating to the ship Colocation View.
     *
     * @param event The action event to extract the current stage.
     * @throws VistaNoCargadaException If the colocation FXML file cannot be loaded.
     */
    private void iniciarNuevaPartida(ActionEvent event) throws VistaNoCargadaException {
        try {
            ColocationView colocationView = ColocationView.getInstance();
            colocationView.show();

            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            // Wrapping IOException into a custom exception
            mostrarError("Error al cargar la vista de colocación.");
            throw new VistaNoCargadaException("Failed to load ColocationView.", e);
        }
    }

    /**
     * Opens the main Game View using the provided {@code Juego} instance.
     *
     * @param juego The initialized or recovered game model.
     * @param event The action event to extract the current stage.
     * @throws VistaNoCargadaException If the game FXML file cannot be loaded.
     */
    private void abrirGameView(Juego juego, ActionEvent event) throws VistaNoCargadaException {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/proyect/batallanaval/game-view.fxml")
            );
            Parent root = loader.load();

            GameController gameController = loader.getController();
            gameController.setJuego(juego);

            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            System.out.println("Vista de juego cargada con partida guardada");

        } catch (IOException ex) {
            System.err.println("Error al cargar game-view.fxml:");
            ex.printStackTrace();
            mostrarError("Error al abrir la vista del juego");
            // Throwing custom checked exception for FXML loading failure
            throw new VistaNoCargadaException("Failed to load game-view.fxml.", ex);
        }
    }

    /**
     * Displays a standard JavaFX error alert to the user.
     *
     * @param mensaje The error message to display.
     */
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}