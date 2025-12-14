package proyect.batallanaval.models;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Barco implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TipoBarco tipo;
    private Orientacion orientacion;

    private transient List<Celda> celdas;

    public Barco(TipoBarco tipo, Orientacion orientacion) {
        this.tipo = tipo;
        this.orientacion = orientacion;
        this.celdas = new ArrayList<>();
    }

    public TipoBarco getTipo() { return tipo; }

    public int getSize() { return tipo.getSize(); }

    public Orientacion getOrientacion() { return orientacion; }

    public void setOrientacion(Orientacion orientacion) { this.orientacion = orientacion; }

    public List<Celda> getCeldas() { return celdas; }

    public void agregarCelda(Celda celda) {
        if (celdas == null) {
            celdas = new ArrayList<>();
        }
        celdas.add(celda);
    }

    public boolean estaHundido() {
        if (celdas == null || celdas.isEmpty()) return false;

        for (Celda celda : celdas) {
            if (celda.getEstado() != EstadoCelda.TOCADA &&
                    celda.getEstado() != EstadoCelda.HUNDIDA) {
                return false;
            }
        }
        return true;
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Lee los campos normales
        this.celdas = new ArrayList<>(); // Reinicializa la lista transient
    }
}
