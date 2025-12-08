package proyect.batallanaval.models;
import java.util.ArrayList;
import java.util.List;

public class Barco {

    private final TipoBarco tipo;
    private Orientacion orientacion;
    private final List<Celda> celdas = new ArrayList<>();

    public Barco(TipoBarco tipo, Orientacion orientacion) {
        this.tipo = tipo;
        this.orientacion = orientacion;
    }

    public TipoBarco getTipo() { return tipo; }

    public int getSize() { return tipo.getSize(); }

    public Orientacion getOrientacion() { return orientacion; }

    public void setOrientacion(Orientacion orientacion) { this.orientacion = orientacion; }

    public List<Celda> getCeldas() { return celdas; }

    public void agregarCelda(Celda celda) { celdas.add(celda); }

    public boolean estaHundido() {
        for (Celda celda : celdas) {
            if (celda.getEstado() != EstadoCelda.TOCADA &&
                    celda.getEstado() != EstadoCelda.HUNDIDA) {
                return false;
            }
        }
        return true;
    }
}
