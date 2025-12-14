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
import proyect.batallanaval.controllers.GestorPartida.PartidaGuardada;
import proyect.batallanaval.views.ColocationView;
import proyect.batallanaval.views.InstructionsView;

import java.io.IOException;

public class HomeController {

    private GestorPartida gestorPartida;

    public HomeController() {
        this.gestorPartida = new GestorPartida();
    }

    @FXML
    private void onQuit() {
        System.exit(0);
    }

    @FXML
    public void onGuia(ActionEvent event) {
        try {
            InstructionsView instructionsView = InstructionsView.getInstance();
            instructionsView.show();

            // Cerrar la ventana de inicio
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onPlay(ActionEvent event) {
        if (gestorPartida.existePartidaGuardada()) {
            boolean continuar = mostrarDialogoContinuar();

            if (continuar) {
                cargarYContinuarPartida(event);
                return;
            } else {
                gestorPartida.eliminarPartidaGuardada();
            }
        }

        iniciarNuevaPartida(event);
    }

    private boolean mostrarDialogoContinuar() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Partida guardada");
        alert.setHeaderText("Se encontró una partida guardada");
        alert.setContentText("¿Deseas continuar donde lo dejaste?");

        ButtonType btnContinuar = new ButtonType("Continuar");
        ButtonType btnNueva = new ButtonType("Nueva Partida");

        alert.getButtonTypes().setAll(btnContinuar, btnNueva);

        return alert.showAndWait().orElse(btnNueva) == btnContinuar;
    }

    private void cargarYContinuarPartida(ActionEvent event) {
        PartidaGuardada partida = gestorPartida.cargarPartida();

        if (partida == null) {
            mostrarError("No se pudo cargar la partida guardada. Iniciando nueva partida.");
            iniciarNuevaPartida(event);
            return;
        }

        try {
            Jugador jugadorRecuperado = new Jugador(
                    partida.nickname,
                    partida.tableroJugador,
                    partida.flotaJugador
            );

            Maquina maquinaRecuperada = new Maquina(
                    "CPU",
                    partida.tableroMaquina,
                    partida.flotaMaquina
            );

            Juego juegoRecuperado = new Juego(jugadorRecuperado, maquinaRecuperada);

            abrirGameView(juegoRecuperado, event);

            System.out.println("✅ Partida cargada correctamente - Saltando a GameView");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar la partida: " + e.getMessage());
            iniciarNuevaPartida(event);
        }
    }

    private void iniciarNuevaPartida(ActionEvent event) {
        try {
            ColocationView colocationView = ColocationView.getInstance();
            colocationView.show();

            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirGameView(Juego juego, ActionEvent event) {
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
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
