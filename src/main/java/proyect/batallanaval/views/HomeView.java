package proyect.batallanaval.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeView extends Stage {
    public HomeView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/proyect/batallanaval/home-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        this.getIcons().add(new Image(
                getClass().getResourceAsStream("/proyect/batallanaval/images/favicon.svg")
        ));
        scene.getStylesheets().add(
                getClass().getResource("/proyect/batallanaval/styles/styles.css").toExternalForm()
        );
        this.setTitle("Batalla Naval - Home");
        this.setScene(scene);
    }
}
