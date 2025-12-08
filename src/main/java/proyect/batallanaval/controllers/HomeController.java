package proyect.batallanaval.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;
import proyect.batallanaval.views.InstructionsView;

import java.io.IOException;

public class HomeController {
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
}
