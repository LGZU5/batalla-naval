module proyect.batallanaval {
    requires javafx.controls;
    requires javafx.fxml;


    opens proyect.batallanaval to javafx.fxml;
    exports proyect.batallanaval;
    opens proyect.batallanaval.controllers to javafx.fxml;
    exports proyect.batallanaval.controllers;
}