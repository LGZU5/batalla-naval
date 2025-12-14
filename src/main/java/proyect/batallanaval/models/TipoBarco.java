package proyect.batallanaval.models;

import java.io.Serializable;

public enum TipoBarco implements Serializable {
    PORTAAVIONES(4),
    SUBMARINO(3),
    DESTRUCTOR(2),
    FRAGATA(1);

    private final int size;

    TipoBarco(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
