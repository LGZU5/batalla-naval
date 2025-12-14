package proyect.batallanaval.controllers;

import java.io.*;
import java.util.ArrayList;

/**
 * Provides basic Create, Read, Update, and Delete (CRUD) operations for a simple
 * text file, handling data line by line.
 * <p>
 * This class is designed for persistent storage of simple, line-oriented data
 * (like configuration or score tracking), not complex object serialization.
 * </p>
 */
public class FileCRUD {
    private String filePath;

    /**
     * Constructs a FileCRUD manager instance associated with a specific file path.
     *
     * @param filePath The path to the text file that this manager will operate on.
     */
    public FileCRUD(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Appends a new line of content to the end of the managed file.
     * <p>
     * If the file does not exist, it will be created.
     * </p>
     *
     * @param content The string content to write to the file.
     */
    public void create(String content) {
        try {
            // true parameter enables appending
            FileWriter fileWriter = new FileWriter(this.filePath, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            // Logs error if writing fails
            e.printStackTrace();
        }
    }

    /**
     * Reads all lines from the managed file into an ArrayList.
     *
     * @return An {@code ArrayList<String>} containing every line of the file.
     * Returns an empty list if the file cannot be read or is empty.
     */
    public ArrayList<String> read() {
        ArrayList<String> lines = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(this.filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            // Logs error if reading fails
            e.printStackTrace();
        }
        return lines;
    }

    /**
     * Updates a specific line in the file, replacing its content with a new string.
     * <p>
     * This operation reads the entire file, modifies the line in memory, and then
     * overwrites the entire file with the updated content.
     * </p>
     *
     * @param index The zero-based index of the line to update.
     * @param content The new string content for that line.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size()).
     */
    public void update(int index, String content) {
        ArrayList<String> lines = this.read();

        // The set method handles IndexOutOfBoundsException if index is invalid
        try {
            lines.set(index, content);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Error: Index out of bounds for update operation.");
            e.printStackTrace();
            return; // Stop execution if index is invalid
        }


        try {
            // false parameter ensures the file is overwritten
            FileWriter fileWriter = new FileWriter(this.filePath, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for(String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            // Logs error if writing fails
            e.printStackTrace();
        }
    }

    /**
     * Deletes a specific line from the file.
     * <p>
     * This operation reads the entire file, removes the line in memory, and then
     * overwrites the entire file with the remaining content.
     * </p>
     *
     * @param index The zero-based index of the line to delete.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size()).
     */
    public void delete(int index) {
        ArrayList<String> lines = this.read();

        // The remove method handles IndexOutOfBoundsException if index is invalid
        try {
            lines.remove(index);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Error: Index out of bounds for delete operation.");
            e.printStackTrace();
            return; // Stop execution if index is invalid
        }

        try {
            // false parameter ensures the file is overwritten
            FileWriter fileWriter = new FileWriter(this.filePath, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for(String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            // Logs error if writing fails
            e.printStackTrace();
        }
    }
}