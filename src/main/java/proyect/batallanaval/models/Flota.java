package proyect.batallanaval.models;

import java.util.ArrayList;
import java.util.List;

public class Flota {

    private final List<Barco> barcos = new ArrayList<>();

    public List<Barco> getBarcos() { return barcos; }

    public void agregarBarco(Barco barco) { barcos.add(barco); }

    public boolean estaCompleta() {
        long porta = barcos.stream().filter(b -> b.getTipo() == TipoBarco.PORTAAVIONES).count();
        long submarinos = barcos.stream().filter(b -> b.getTipo() == TipoBarco.SUBMARINO).count();
        long destructores = barcos.stream().filter(b -> b.getTipo() == TipoBarco.DESTRUCTOR).count();
        long fragatas = barcos.stream().filter(b -> b.getTipo() == TipoBarco.FRAGATA).count();

        return porta == 1 && submarinos == 2 && destructores == 3 && fragatas == 4;
    }
}
