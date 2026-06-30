package com.floenergy;

import com.floenergy.core.MeterDataReader;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FloDataProcessor {
    private static final Logger LOGGER =
           Logger.getLogger(FloDataProcessor.class.getName());

    private  static MeterDataReader meterDataReader;

    public static void main(String[] args) {
        LOGGER.info("Flo Energy NEM12 parser");
        String inputFilePath = args.length >=1 ? args[0] : null;
        String outputFilePath =args.length >=2 ? args[1] : null;
        meterDataReader = new MeterDataReader();
        try {

            Path inputFile  = inputFilePath != null ? Path.of(inputFilePath) :Path.of("input.csv");
            Path outputFile = outputFilePath != null ? Path.of(outputFilePath) : Path.of("output.sql");
            meterDataReader.read(inputFile,outputFile);
           LOGGER.log(Level.INFO,"Successfully generated SQL insert statements at: " + outputFile.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,"Failed to process NEM12 file " + e.getMessage());
        }
    }

    private static Path getResourceFilePath(String fileName) {
        URL resource = FloDataProcessor.class.getClassLoader().getResource(fileName);
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
