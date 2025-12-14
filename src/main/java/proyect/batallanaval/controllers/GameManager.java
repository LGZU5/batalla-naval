package proyect.batallanaval.controllers;

import proyect.batallanaval.models.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the persistence (saving and loading) of the complete game state,
 * including player/machine boards, fleets, and basic game status.
 *
 * Uses Java's standard Serialization for complex objects (Tablero, Flota)
 * and a simple text file/FileCRUD for scalar data (nickname, scores).
 */
public class GameManager {
    private static final String TABLERO_JUGADOR_FILE = "tablero_jugador.ser";
    private static final String TABLERO_MAQUINA_FILE = "tablero_maquina.ser";
    private static final String FLOTA_JUGADOR_FILE = "flota_jugador.ser";
    private static final String FLOTA_MAQUINA_FILE = "flota_maquina.ser";
    private static final String ESTADO_FILE = "estado_partida.txt";

    private FileCRUD fileCRUD;

    /**
     * Constructs the GameManager and initializes the persistence mechanism.
     */
    public GameManager() {
        // Initializes FileCRUD for the simple state file
        this.fileCRUD = new FileCRUD(ESTADO_FILE);
    }

    /**
     * Saves the complete game state.
     * <p>
     * Criteria met:
     * <ul>
     * <li>Serializable files for boards and fleets (binary).</li>
     * <li>Plain text file for nickname and sunk ship counts.</li>
     * </ul>
     * </p>
     *
     * @param jugador The human player instance.
     * @param maquina The machine player instance.
     * @throws IOException If any file operation (serialization or text writing) fails.
     */
    public void guardarPartida(Jugador jugador, Maquina maquina) throws IOException {
        System.out.println("=== GUARDANDO PARTIDA ===");

        // 1. Serialize complex objects
        serializarObjeto(jugador.getTableroPosicion(), TABLERO_JUGADOR_FILE);
        serializarObjeto(maquina.getTableroPosicion(), TABLERO_MAQUINA_FILE);

        serializarObjeto(jugador.getFlota(), FLOTA_JUGADOR_FILE);
        serializarObjeto(maquina.getFlota(), FLOTA_MAQUINA_FILE);

        // 2. Prepare and save simple state
        int barcosHundidosJugador = contarBarcosHundidos(maquina.getFlota()); // Ships sunk by player
        int barcosHundidosMaquina = contarBarcosHundidos(jugador.getFlota()); // Ships sunk by machine

        guardarEstadoSimple(
                jugador.getNickname(),
                barcosHundidosJugador,
                barcosHundidosMaquina
        );

        System.out.println("✅ Partida guardada: " + jugador.getNickname());
        System.out.println("   Barcos hundidos por jugador: " + barcosHundidosJugador);
        System.out.println("   Barcos hundidos por máquina: " + barcosHundidosMaquina);
    }

    /**
     * Counts how many ships in a fleet are sunk.
     *
     * @param flota The fleet to check.
     * @return The number of sunk ships.
     */
    private int contarBarcosHundidos(Flota flota) {
        if (flota == null || flota.getBarcos().isEmpty()) {
            return 0;
        }
        return (int) flota.getBarcos().stream()
                .filter(Barco::estaHundido)
                .count();
    }

    /**
     * Serializes an object to a binary file using ObjectOutputStream.
     *
     * @param objeto The object to serialize (must implement {@code Serializable}).
     * @param fileName The name of the file to save to.
     * @throws IOException If an I/O error occurs during serialization.
     */
    private void serializarObjeto(Object objeto, String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(fileName))) {
            oos.writeObject(objeto);
        } catch (IOException e) {
            // Re-throwing the exception to be handled by the caller (guardarPartida)
            System.err.println("Error al serializar " + fileName + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Saves simple game state (nickname, scores) using FileCRUD.
     *
     * @param nickname Player's nickname.
     * @param barcosJugador Sunk ships count by the player.
     * @param barcosMaquina Sunk ships count by the machine.
     */
    private void guardarEstadoSimple(String nickname, int barcosJugador, int barcosMaquina) {
        File file = new File(ESTADO_FILE);
        if (file.exists()) {
            // Ensure the file is fresh by deleting it, though FileCRUD.create often overwrites.
            file.delete();
        }

        this.fileCRUD = new FileCRUD(ESTADO_FILE);

        String[] datos = new String[]{
                nickname,
                String.valueOf(barcosJugador),
                String.valueOf(barcosMaquina)
        };

        String content = String.join(",", datos);
        fileCRUD.create(content); // FileCRUD should handle internal IOException if it fails
    }

    /**
     * Loads the complete game state from saved files.
     *
     * @return A {@code PartidaGuardada} object containing all loaded game components, or {@code null} if loading fails.
     * @throws IOException If any file access fails during deserialization or reading the state file.
     * @throws ClassNotFoundException If a serialized class file is missing or corrupted.
     * @throws NumberFormatException If the score data in the state file is not a valid number.
     */
    public PartidaGuardada cargarPartida() throws IOException, ClassNotFoundException, NumberFormatException {
        System.out.println("=== CARGANDO PARTIDA ===");
        PartidaGuardada partida = new PartidaGuardada();

        // 1. Deserialize complex objects
        partida.tableroJugador = (Tablero) deserializarObjeto(TABLERO_JUGADOR_FILE);
        partida.tableroMaquina = (Tablero) deserializarObjeto(TABLERO_MAQUINA_FILE);

        if (partida.tableroJugador == null || partida.tableroMaquina == null) {
            System.err.println("Error: No se pudieron cargar los tableros");
            // Throwing IOException to signal a critical file access failure
            throw new IOException("Failed to deserialize one or both Tablero objects.");
        }

        partida.flotaJugador = (Flota) deserializarObjeto(FLOTA_JUGADOR_FILE);
        partida.flotaMaquina = (Flota) deserializarObjeto(FLOTA_MAQUINA_FILE);

        if (partida.flotaJugador == null || partida.flotaMaquina == null) {
            System.err.println("Error: No se pudieron cargar las flotas");
            throw new IOException("Failed to deserialize one or both Flota objects.");
        }

        // 2. Re-link the ships' cells (critical step after deserialization)
        reVincularFlotaDesdeTablero(partida.tableroJugador, partida.flotaJugador);
        reVincularFlotaDesdeTablero(partida.tableroMaquina, partida.flotaMaquina);


        // 3. Read simple state file
        ArrayList<String> lines = fileCRUD.read(); // Assuming fileCRUD.read() throws IOException or returns empty on failure

        if (lines.isEmpty()) {
            System.err.println("Error: Archivo de estado vacío o no se pudo leer");
            throw new IOException("State file is empty or unreadable: " + ESTADO_FILE);
        }

        // 4. Parse data
        String[] datos = lines.get(0).split(",");

        if (datos.length < 3) {
            System.err.println("Error: Formato de archivo de estado incorrecto");
            throw new IOException("State file has incorrect format (expected 3 fields).");
        }

        // Potential for NumberFormatException here
        partida.nickname = datos[0];
        partida.barcosHundidosJugador = Integer.parseInt(datos[1]);
        partida.barcosHundidosMaquina = Integer.parseInt(datos[2]);

        System.out.println("✅ Partida cargada: " + partida.nickname);
        System.out.println("   Barcos hundidos por jugador: " + partida.barcosHundidosJugador);
        System.out.println("   Barcos hundidos por máquina: " + partida.barcosHundidosMaquina);

        return partida;
    }

    /**
     * Deserializes an object from a binary file using ObjectInputStream.
     *
     * @param fileName The name of the file to load from.
     * @return The deserialized object, or {@code null} if loading fails.
     * @throws IOException If an I/O error occurs (e.g., file not found, corrupt stream).
     * @throws ClassNotFoundException If the serialized class cannot be found.
     */
    private Object deserializarObjeto(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(fileName))) {
            return ois.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("Error: Archivo no encontrado " + fileName);
            throw e;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al deserializar " + fileName + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Re-establishes the connection (binding) between the {@code Celda} objects in the
     * {@code Tablero} and their corresponding parent {@code Barco} object in the {@code Flota}.
     * This is necessary because Java's default serialization often breaks object relationships
     * when objects are serialized and deserialized separately.
     *
     * @param tablero The board model (source of cells).
     * @param flota The fleet model (destination for reconstructed ships).
     */
    private void reVincularFlotaDesdeTablero(Tablero tablero, Flota flota) {
        System.out.println("=== RE-VINCULANDO FLOTA ===");

        // 1. Use a map to track unique Barco objects by identity (since they are deserialized copies)
        Map<Barco, Barco> barcosUnicos = new HashMap<>();

        // 2. Iterate through the board and find all unique ships
        for (int f = 0; f < Tablero.SIZE; f++) {
            for (int c = 0; c < Tablero.SIZE; c++) {
                Celda celda = tablero.getCelda(f, c);

                if (celda.tieneBarco()) {
                    Barco barcoDelTablero = celda.getBarco();

                    // If it's the first time we see this ship (by object identity)
                    if (!barcosUnicos.containsKey(barcoDelTablero)) {
                        barcosUnicos.put(barcoDelTablero, barcoDelTablero);
                        // Clear its cells to rebuild the correct list from the board
                        barcoDelTablero.getCeldas().clear();
                    }

                    // Add this cell to the ship's cell list
                    barcoDelTablero.agregarCelda(celda);
                }
            }
        }

        // 3. Update the fleet with the unique ships found
        flota.getBarcos().clear();
        flota.getBarcos().addAll(barcosUnicos.values());

        System.out.println("✅ Flota reconstruida con " + flota.getBarcos().size() + " barcos");

        // 4. Diagnosis: Verify that the ships have cells
        for (Barco b : flota.getBarcos()) {
            System.out.println("   - " + b.getTipo() + ": " + b.getCeldas().size() + " celdas, Hundido: " + b.estaHundido());
        }
    }

    /**
     * Checks if a complete saved game state exists (all necessary files are present).
     *
     * @return {@code true} if all persistence files exist, {@code false} otherwise.
     */
    public boolean existePartidaGuardada() {
        File f1 = new File(TABLERO_JUGADOR_FILE);
        File f2 = new File(TABLERO_MAQUINA_FILE);
        File f3 = new File(FLOTA_JUGADOR_FILE);
        File f4 = new File(FLOTA_MAQUINA_FILE);
        File f5 = new File(ESTADO_FILE);

        return f1.exists() && f2.exists() && f3.exists() && f4.exists() && f5.exists();
    }

    /**
     * Deletes all saved game state files.
     *
     * @throws SecurityException If a security manager exists and its {@code checkDelete} method denies delete access.
     */
    public void eliminarPartidaGuardada() {
        new File(TABLERO_JUGADOR_FILE).delete();
        new File(TABLERO_MAQUINA_FILE).delete();
        new File(FLOTA_JUGADOR_FILE).delete();
        new File(FLOTA_MAQUINA_FILE).delete();
        new File(ESTADO_FILE).delete();
        System.out.println("✅ Archivos de partida eliminados");
    }

    /**
     * Internal static class used to return all loaded data from the saved game files
     * in a single container object.
     */
    public static class PartidaGuardada {
        public Tablero tableroJugador;
        public Tablero tableroMaquina;
        public Flota flotaJugador;
        public Flota flotaMaquina;
        public String nickname;
        public int barcosHundidosJugador;
        public int barcosHundidosMaquina;
    }
}