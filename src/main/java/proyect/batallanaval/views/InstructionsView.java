package proyect.batallanaval.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class InstructionsView extends Stage {
    public InstructionsView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/instructions.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        this.getIcons().add(new Image(
                getClass().getResourceAsStream("/proyect/batallanaval/images/favicon.svg")
        ));
        scene.getStylesheets().add(
                getClass().getResource("/proyect/batallanaval/styles/styles.css").toExternalForm()
        );
        this.setTitle("Batalla Naval - Instructions");
        this.setScene(scene);
    }

    public static InstructionsView getInstance() throws IOException {
        if (InstructionsView.StartViewHolder.INSTANCE == null) {
            InstructionsView.StartViewHolder.INSTANCE = new InstructionsView();
        }
        return InstructionsView.StartViewHolder.INSTANCE;
    }

    private static class StartViewHolder {
        private static InstructionsView INSTANCE = null;
    }
}
