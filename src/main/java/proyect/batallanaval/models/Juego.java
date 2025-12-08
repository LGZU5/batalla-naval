package proyect.batallanaval.models;

public class Juego {

    private final Jugador jugador;

    public Juego(String nicknameJugador) {
        this.jugador = new Jugador(nicknameJugador);
    }

    public Jugador getJugador() { return jugador; }
}
