package proyect.batallanaval.models;

import java.io.Serializable;

public class Celda implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int fila;
    private final int columna;
    private EstadoCelda estado;
    private Barco barco; // null si no hay barco

    public Celda(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
        this.estado = EstadoCelda.VACIA;
    }

    public int getFila() { return fila; }

    public int getColumna() { return columna; }

    public EstadoCelda getEstado() { return estado; }

    public void setEstado(EstadoCelda estado) { this.estado = estado; }

    public Barco getBarco() { return barco; }

    public void setBarco(Barco barco) { this.barco = barco; }

    public boolean tieneBarco() { return barco != null; }
}
