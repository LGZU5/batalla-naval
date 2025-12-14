package proyect.batallanaval.models;

/**
 * The class {@code Juego} initializes and keeps the references of the two types of
 * participants on the game: player (human) and machine. It also facilitates the access
 * to both participants during the game, as it works as a bridge the controllers and the models.
 */
public class Juego {

    private final Jugador jugador;
    private final Maquina maquina;
    private boolean esTurnoJugador;

    /**
     * Creates a new instance of the game, initializing the human player with
     * the specified nickname and the machine with a fixed name.
     *
     * @param nicknameJugador name that will identify the human player
     * @throws IllegalArgumentException if the nickname is null or empty
     */
    public Juego(String nicknameJugador) {
        this.jugador = new Jugador(nicknameJugador);
        this.maquina = new Maquina("CPU");
        this.esTurnoJugador = true; // El jugador siempre empieza

        GeneradorFlotaAleatoria generador = new GeneradorFlotaAleatoria();
        generador.generarFlotaAleatoria(
                maquina.getFlota(),
                maquina.getTableroPosicion()
        );
        System.out.println("Flota de la máquina generada desde Juego. Barcos: "
                + maquina.getFlota().getBarcos().size());
    }

    public Juego(Jugador jugador, Maquina maquina) {
        if (jugador == null || maquina == null) {
            throw new IllegalArgumentException(
                    "El jugador y la máquina no pueden ser nulos."
            );
        }

        this.jugador = jugador;
        this.maquina = maquina;
        this.esTurnoJugador = true; // Por defecto, empieza el jugador

        System.out.println("Juego reconstruido desde partida guardada");
        System.out.println("Jugador: " + jugador.getNickname());
        System.out.println("Barcos jugador: " + jugador.getFlota().getBarcos().size());
        System.out.println("Barcos máquina: " + maquina.getFlota().getBarcos().size());
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Maquina getMaquina() {
        return maquina;
    }

    /**
     * Verifies if it's the player's turn.
     *
     * @return true if it's the player's turn, false if it's the machine's turn
     */
    public boolean esTurnoJugador() {
        return esTurnoJugador;
    }

    /**
     * Executes an attack from the player to the machine's board.
     * Manages the turn logic based on the attack result.
     *
     * @param fila row coordinate of the attack
     * @param col column coordinate of the attack
     * @return the result of the attack (AGUA, TOCADO, HUNDIDO)
     * @throws IllegalStateException if it's not the player's turn
     */
    public ResultadoDisparo ejecutarAtaqueJugador(int fila, int col) {
        if (!esTurnoJugador) {
            throw new IllegalStateException("No es el turno del jugador");
        }

        Tablero tableroMaquina = maquina.getTableroPosicion();
        ResultadoDisparo resultado = tableroMaquina.disparar(fila, col);

        // Si es AGUA, cambia el turno a la máquina
        if (resultado == ResultadoDisparo.AGUA) {
            cambiarTurno();
        }
        // Si es TOCADO o HUNDIDO, el jugador sigue jugando (no se cambia el turno)

        return resultado;
    }

    /**
     * Executes an attack from the machine to the player's board.
     * Manages the turn logic based on the attack result.
     *
     * @param fila row coordinate of the attack
     * @param col column coordinate of the attack
     * @return the result of the attack (AGUA, TOCADO, HUNDIDO)
     * @throws IllegalStateException if it's not the machine's turn
     */
    public ResultadoDisparo ejecutarAtaqueMaquina(int fila, int col) {
        if (esTurnoJugador) {
            throw new IllegalStateException("No es el turno de la máquina");
        }

        Tablero tableroJugador = jugador.getTableroPosicion();
        ResultadoDisparo resultado = tableroJugador.disparar(fila, col);

        // Si es AGUA, cambia el turno al jugador
        if (resultado == ResultadoDisparo.AGUA) {
            cambiarTurno();
        }
        // Si es TOCADO o HUNDIDO, la máquina sigue jugando (no se cambia el turno)

        return resultado;
    }

    /**
     * Changes the turn from one player to the other.
     */
    private void cambiarTurno() {
        esTurnoJugador = !esTurnoJugador;
        System.out.println("Turno cambiado. Ahora es turno de: " +
                (esTurnoJugador ? "JUGADOR" : "MÁQUINA"));
    }

    /**
     * Verifies if the player has won the game.
     *
     * @return true if the machine's fleet is completely destroyed
     */
    public boolean haGanadoJugador() {
        return maquina.getFlota().estaFlotaHundida();
    }

    /**
     * Verifies if the machine has won the game.
     *
     * @return true if the player's fleet is completely destroyed
     */
    public boolean haGanadoMaquina() {
        return jugador.getFlota().estaFlotaHundida();
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if either player or machine has won
     */
    public boolean juegoTerminado() {
        return haGanadoJugador() || haGanadoMaquina();
    }
}
