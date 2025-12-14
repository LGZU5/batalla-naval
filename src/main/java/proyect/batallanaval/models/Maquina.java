package proyect.batallanaval.models;

/**
 * Represents the machine (CPU) within the game.
 * It groups and encapsulates all the elements that belong to the machine.
 * And shows the ships that are allocated randomly.
 */
public class Maquina {
    private final String nickname;
    private final Tablero tableroPosicion;
    private final Flota flota;

    /**
     * The {@code Maquina} class creates a new instance of the machine, initializing its nickname,
     * its empty board, and its undeployed fleet of ships.
     * @param nickname machine identifier name
     * @throws IllegalArgumentException if the nickname is null or empty

     */
    public Maquina(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(
                    "El nickname de la máquina no puede ser nulo ni vacío."
            );
        }

        this.nickname = nickname;
        this.tableroPosicion = new Tablero();
        this.flota = new Flota();
    }

    public Maquina(String nickname, Tablero tablero, Flota flota) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(
                    "El nickname de la máquina no puede ser nulo ni vacío."
            );
        }
        if (tablero == null || flota == null) {
            throw new IllegalArgumentException(
                    "El tablero y la flota no pueden ser nulos."
            );
        }

        this.nickname = nickname;
        this.tableroPosicion = tablero;
        this.flota = flota;
    }

    public Tablero getTableroPosicion() { return tableroPosicion; }
    public Flota getFlota() { return flota; }
}
