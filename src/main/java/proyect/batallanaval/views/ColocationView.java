package proyect.batallanaval.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ColocationView extends Stage{
    public ColocationView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/colocation-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        this.getIcons().add(new Image(
                getClass().getResourceAsStream("/proyect/batallanaval/images/favicon.svg")
        ));
        scene.getStylesheets().add(
                getClass().getResource("/proyect/batallanaval/styles/styles.css").toExternalForm()
        );
        this.setTitle("Batalla Naval - Colocación de barcos");
        this.setScene(scene);

    }

    public static ColocationView getInstance() throws IOException {
        if (ColocationView.StartViewHolder.INSTANCE == null) {
            ColocationView.StartViewHolder.INSTANCE = new ColocationView();
        }
        return ColocationView.StartViewHolder.INSTANCE;
    }

    private static class StartViewHolder {
        private static ColocationView INSTANCE = null;
    }
}
