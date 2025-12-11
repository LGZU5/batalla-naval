package proyect.batallanaval.models;

public class Juego {

    private final Jugador jugador;
    private final Jugador maquina;

    public Juego(String nicknameJugador) {

        this.jugador = new Jugador(nicknameJugador);
        this.maquina = new Jugador("CPU");
    }

    public Jugador getJugador() { return jugador; }
    public Jugador getMaquina() { return maquina; }
}
