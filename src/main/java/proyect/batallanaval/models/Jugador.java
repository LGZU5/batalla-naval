package proyect.batallanaval.models;

public class Jugador {

    private final String nickname;
    private final Tablero tableroPosicion;
    private final Flota flota;

    public Jugador(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(
                    "El nickname del jugador no puede ser nulo ni vacío."
            );
        }

        this.nickname = nickname;
        this.tableroPosicion = new Tablero();
        this.flota = new Flota();
    }

    public Jugador(String nickname, Tablero tablero, Flota flota) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(
                    "El nickname del jugador no puede ser nulo ni vacío."
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

    public String getNickname() { return nickname; }

    public Tablero getTableroPosicion() { return tableroPosicion; }

    public Flota getFlota() { return flota; }
}

