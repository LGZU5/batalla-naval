package proyect.batallanaval.controllers;

import proyect.batallanaval.models.*;
import proyect.batallanaval.controllers.FileCRUD;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GestorPartida {
    private static final String TABLERO_JUGADOR_FILE = "tablero_jugador.ser";
    private static final String TABLERO_MAQUINA_FILE = "tablero_maquina.ser";
    private static final String FLOTA_JUGADOR_FILE = "flota_jugador.ser";
    private static final String FLOTA_MAQUINA_FILE = "flota_maquina.ser";
    private static final String ESTADO_FILE = "estado_partida.txt";

    private FileCRUD fileCRUD;

    public GestorPartida() {
        this.fileCRUD = new FileCRUD(ESTADO_FILE);
    }

    /**
     * Guarda el estado completo de la partida.
     * Cumple con los criterios:
     * - Archivos serializables para tableros y flotas
     * - Archivo plano para nickname y barcos hundidos
     */
    public void guardarPartida(Jugador jugador, Maquina maquina) {
        System.out.println("=== GUARDANDO PARTIDA ===");

        serializarObjeto(jugador.getTableroPosicion(), TABLERO_JUGADOR_FILE);
        serializarObjeto(maquina.getTableroPosicion(), TABLERO_MAQUINA_FILE);

        serializarObjeto(jugador.getFlota(), FLOTA_JUGADOR_FILE);
        serializarObjeto(maquina.getFlota(), FLOTA_MAQUINA_FILE);

        int barcosHundidosJugador = contarBarcosHundidos(maquina.getFlota());
        int barcosHundidosMaquina = contarBarcosHundidos(jugador.getFlota());

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
     * Cuenta cuántos barcos están hundidos en una flota
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
     * Serializa un objeto en archivo binario
     */
    private void serializarObjeto(Object objeto, String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(fileName))) {
            oos.writeObject(objeto);
        } catch (IOException e) {
            System.err.println("Error al serializar " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Guarda estado simple usando FileCRUD (como el ejemplo Person del profesor)
     */
    private void guardarEstadoSimple(String nickname, int barcosJugador, int barcosMaquina) {
        File file = new File(ESTADO_FILE);
        if (file.exists()) {
            file.delete();
        }

        this.fileCRUD = new FileCRUD(ESTADO_FILE);

        String[] datos = new String[]{
                nickname,
                String.valueOf(barcosJugador),
                String.valueOf(barcosMaquina)
        };

        String content = String.join(",", datos);
        fileCRUD.create(content);
    }

    /**
     * Carga el estado completo de la partida
     */
    public PartidaGuardada cargarPartida() {
        System.out.println("=== CARGANDO PARTIDA ===");
        PartidaGuardada partida = new PartidaGuardada();

        partida.tableroJugador = (Tablero) deserializarObjeto(TABLERO_JUGADOR_FILE);
        partida.tableroMaquina = (Tablero) deserializarObjeto(TABLERO_MAQUINA_FILE);

        if (partida.tableroJugador == null || partida.tableroMaquina == null) {
            System.err.println("Error: No se pudieron cargar los tableros");
            return null;
        }

        partida.flotaJugador = (Flota) deserializarObjeto(FLOTA_JUGADOR_FILE);
        partida.flotaMaquina = (Flota) deserializarObjeto(FLOTA_MAQUINA_FILE);

        if (partida.flotaJugador == null || partida.flotaMaquina == null) {
            System.err.println("Error: No se pudieron cargar las flotas");
            return null;
        }

        reVincularFlotaDesdeTablero(partida.tableroJugador, partida.flotaJugador);
        reVincularFlotaDesdeTablero(partida.tableroMaquina, partida.flotaMaquina);


        ArrayList<String> lines = fileCRUD.read();

        if (lines.isEmpty()) {
            System.err.println("Error: Archivo de estado vacío");
            return null;
        }

        // Parsear datos (como PersonController del profesor)
        String[] datos = lines.get(0).split(",");
        partida.nickname = datos[0];
        partida.barcosHundidosJugador = Integer.parseInt(datos[1]);
        partida.barcosHundidosMaquina = Integer.parseInt(datos[2]);

        System.out.println("✅ Partida cargada: " + partida.nickname);
        System.out.println("   Barcos hundidos por jugador: " + partida.barcosHundidosJugador);
        System.out.println("   Barcos hundidos por máquina: " + partida.barcosHundidosMaquina);

        return partida;
    }

    /**
     * Deserializa un objeto desde archivo binario
     */
    private Object deserializarObjeto(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(fileName))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al deserializar " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private void reVincularFlotaDesdeTablero(Tablero tablero, Flota flota) {
        System.out.println("=== RE-VINCULANDO FLOTA ===");

        // 1. Usar un mapa basado en la identidad del objeto Barco
        Map<Barco, Barco> barcosUnicos = new HashMap<>();

        // 2. Recorrer el tablero y encontrar todos los barcos únicos
        for (int f = 0; f < Tablero.SIZE; f++) {
            for (int c = 0; c < Tablero.SIZE; c++) {
                Celda celda = tablero.getCelda(f, c);

                if (celda.tieneBarco()) {
                    Barco barcoDelTablero = celda.getBarco();

                    // Si es la primera vez que vemos este barco (por identidad de objeto)
                    if (!barcosUnicos.containsKey(barcoDelTablero)) {
                        barcosUnicos.put(barcoDelTablero, barcoDelTablero);
                        // Limpiar sus celdas para reconstruirlas
                        barcoDelTablero.getCeldas().clear();
                    }

                    // Agregar esta celda al barco
                    barcoDelTablero.agregarCelda(celda);
                }
            }
        }

        // 3. Actualizar la flota con los barcos únicos encontrados
        flota.getBarcos().clear();
        flota.getBarcos().addAll(barcosUnicos.values());

        System.out.println("✅ Flota reconstruida con " + flota.getBarcos().size() + " barcos");

        // 4. Diagnóstico: Verificar que los barcos tienen celdas
        for (Barco b : flota.getBarcos()) {
            System.out.println("   - " + b.getTipo() + ": " + b.getCeldas().size() + " celdas, Hundido: " + b.estaHundido());
        }
    }

    /**
     * Verifica si existe una partida guardada
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
     * Elimina los archivos de partida guardada
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
     * Clase interna para retornar todos los datos de la partida cargada
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
