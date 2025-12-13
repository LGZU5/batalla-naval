package proyect.batallanaval.models;

public class Tablero {

    public static final int SIZE = 10;
    public static final int CELL_SIZE = 30;

    private final Celda[][] celdas;

    public Tablero() {
        celdas = new Celda[SIZE][SIZE];
        for (int f = 0; f < SIZE; f++) {
            for (int c = 0; c < SIZE; c++) {
                celdas[f][c] = new Celda(f, c);
            }
        }
    }

    public Celda getCelda(int fila, int columna) {
        return celdas[fila][columna];
    }

    public boolean dentroDeLimites(int fila, int columna) {
        return fila >= 0 && fila < SIZE && columna >= 0 && columna < SIZE;
    }

    public boolean puedeColocarBarco(int fila, int columna, Orientacion orientacion, TipoBarco tipo) {
        int longitud = tipo.getSize();

        for (int i = 0; i < longitud; i++) {
            int f = orientacion == Orientacion.HORIZONTAL ? fila : fila + i;
            int c = orientacion == Orientacion.HORIZONTAL ? columna + i : columna;

            if (!dentroDeLimites(f, c)) {
                return false;
            }
            if (celdas[f][c].tieneBarco()) {
                return false;
            }
        }
        return true;
    }

    public Barco colocarBarco(int fila, int columna, Orientacion orientacion, TipoBarco tipo) {
        Barco barco = new Barco(tipo, orientacion);
        int longitud = tipo.getSize();

        for (int i = 0; i < longitud; i++) {
            int f = orientacion == Orientacion.HORIZONTAL ? fila : fila + i;
            int c = orientacion == Orientacion.HORIZONTAL ? columna + i : columna;

            Celda celda = celdas[f][c];
            celda.setBarco(barco);
            celda.setEstado(EstadoCelda.BARCO);
            barco.agregarCelda(celda);
        }
        return barco;
    }

    public ResultadoDisparo disparar(int fila, int columna) {
        Celda celda = celdas[fila][columna];

        // --- Caso 1: La celda ya fue atacada (importante para prevenir re-ataques) ---
        if (celda.getEstado() == EstadoCelda.TOCADA ||
                celda.getEstado() == EstadoCelda.HUNDIDA ||
                celda.getEstado() == EstadoCelda.AGUA_TOCADA) {
            // Lanza una excepción si el atacante (la máquina) intenta atacar dos veces la misma celda
            throw new IllegalStateException("La celda ya fue atacada.");
        }

        // --- Caso 2: Disparo al agua (no hay barco) ---
        if (!celda.tieneBarco()) {
            // Si no tiene barco, el resultado es AGUA
            celda.setEstado(EstadoCelda.AGUA_TOCADA); // <--- AHORA SE USA CORRECTAMENTE
            return ResultadoDisparo.AGUA;
        }

        // --- Caso 3: Disparo a un barco ---
        // Si llega aquí, es un barco que no ha sido atacado (estado BARCO)
        celda.setEstado(EstadoCelda.TOCADA);
        Barco barco = celda.getBarco();

        // (Aquí falta la lógica para que el barco sepa que fue tocado,
        // pero eso debería estar dentro de Barco o Celda).
        // Asumiendo que el modelo ya marca el barco como tocado:

        if (barco.estaHundido()) {
            // Si se hunde, marca todas las celdas del barco como HUNDIDA
            for (Celda c : barco.getCeldas()) {
                c.setEstado(EstadoCelda.HUNDIDA);
            }
            return ResultadoDisparo.HUNDIDO;
        }

        return ResultadoDisparo.TOCADO;
    }
}