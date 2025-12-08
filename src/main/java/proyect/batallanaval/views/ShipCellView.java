package proyect.batallanaval.views;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import proyect.batallanaval.models.TipoBarco;

public class ShipCellView extends StackPane {

    private final TipoBarco tipo;
    private boolean seleccionado = false;

    public ShipCellView(TipoBarco tipo, double size) {
        this.tipo = tipo;

        Rectangle rect = new Rectangle(size - 4, size - 4);
        rect.setArcWidth(6);
        rect.setArcHeight(6);

        switch (tipo) {
            case PORTAAVIONES -> rect.setFill(Color.DARKBLUE);
            case SUBMARINO   -> rect.setFill(Color.DARKGREEN);
            case DESTRUCTOR  -> rect.setFill(Color.DARKORANGE);
            case FRAGATA     -> rect.setFill(Color.DARKRED);
        }

        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1.5);

        getChildren().add(rect);
        setCursor(Cursor.HAND);

        setOnMouseClicked(this::toggleSeleccion);
        actualizarEstilo();
    }

    private void toggleSeleccion(MouseEvent e) {
        seleccionado = !seleccionado;
        actualizarEstilo();
        e.consume();
    }

    private void actualizarEstilo() {
        setStyle(seleccionado
                ? "-fx-border-color: yellow; -fx-border-width: 2;"
                : "-fx-border-color: transparent;");
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        actualizarEstilo();
    }

    public TipoBarco getTipo() {
        return tipo;
    }
}
