package com.floenergy;

import com.floenergy.core.MeterDataReader;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class Main {

    private  static MeterDataReader meterDataReader;

    public static void main(String[] args) {
        meterDataReader = new MeterDataReader();
        System.out.println("Flo Energy NEM12 parser");
        String inputFileName = "input.csv";
        Path outputFile = Path.of("output.sql");

        try {
            Path inputFile = getResourceFilePath(inputFileName);
            meterDataReader.read(inputFile);

            System.out.println("Successfully generated SQL insert statements at: " + outputFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to process NEM12 file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path getResourceFilePath(String fileName) {
        URL resource = Main.class.getClassLoader().getResource(fileName);

        if (resource == null) {
            throw new RuntimeException("Resource file not found: " + fileName);
        }

        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid resource file path: " + fileName, e);
        }
    }
}
