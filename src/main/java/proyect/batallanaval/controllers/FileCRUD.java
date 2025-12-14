package proyect.batallanaval.controllers;

import java.io.*;
import java.util.ArrayList;

public class FileCRUD {
    private String filePath;

    public FileCRUD(String filePath) {
        this.filePath = filePath;
    }

    public void create(String content) {
        try {
            FileWriter fileWriter = new FileWriter(this.filePath, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            e.printStackTrace();
        }
        return lines;
    }

    public void update(int index, String content) {
        ArrayList<String> lines = this.read();
        lines.set(index, content);

        try {
            FileWriter fileWriter = new FileWriter(this.filePath, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for(String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(int index) {
        ArrayList<String> lines = this.read();
        lines.remove(index);

        try {
            FileWriter fileWriter = new FileWriter(this.filePath, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for(String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
