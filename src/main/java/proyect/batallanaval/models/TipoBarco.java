package proyect.batallanaval.models;

public enum TipoBarco {
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
