package proyect.batallanaval.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;
import proyect.batallanaval.views.HomeView;

public class InstructionsController {
    @FXML
    private void onBack(javafx.event.ActionEvent e) {
        try {
            // Open menu view (StartView)
            HomeView homeView = new HomeView();
            homeView.show();

            // Close this window
            Stage current = (Stage) ((Node) e.getSource()).getScene().getWindow();
            current.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
