package proyect.batallanaval.models;

/**
 * The class {@code Juego} initializes and keeps the references of the two types of
 * participants on the game: player (human) and machine. It also facilitates the access
 * to both participants during the game, as it works as a bridge the controllers and the models.
 */
public class Juego {

    private final Jugador jugador;
    private final Maquina maquina;

    /**Creates a new instance of the game, initializing the human player with
     the specified nickname and the machine with a fixed name.
     *
     @param nicknameJugador name that will identify the human player
     @throws IllegalArgumentException if the nickname is null or empty
     */
    public Juego(String nicknameJugador) {

        this.jugador = new Jugador(nicknameJugador);
        this.maquina = new Maquina("CPU");
    }

    public Jugador getJugador() { return jugador; }
    public Maquina getMaquina() { return maquina; }
}
