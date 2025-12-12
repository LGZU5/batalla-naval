package proyect.batallanaval;

import javafx.application.Application;
import javafx.stage.Stage;
import proyect.batallanaval.views.HomeView;

import java.io.IOException;

public class    HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        HomeView homeView = new HomeView();
        homeView.show();
    }
}
