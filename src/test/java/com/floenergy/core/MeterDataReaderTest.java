package com.floenergy.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MeterDataReaderTest {

    @TempDir
    Path tempDir;

    private MeterDataReader meterDataReader;
    private Path inputFile;
    private Path outputFile;

    @BeforeEach
    void setUp() {
        inputFile = tempDir.resolve("input.csv");
        outputFile = tempDir.resolve("output.sql");
        meterDataReader = new MeterDataReader(outputFile);
    }



    @Test
    void shouldReadNem12FileAndGenerateSqlInsertStatements() throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.sql");

        Files.writeString(inputFile, validNem12InputWithOne300Record());

        MeterDataReader meterDataReader = new MeterDataReader(outputFile);
        meterDataReader.read(inputFile);

        assertTrue(Files.exists(outputFile));

        List<String> sqlLines = Files.readAllLines(outputFile);

        assertEquals(48, sqlLines.size());

        assertEquals(
                "INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") " +
                        "VALUES ('NEM1201009', '2005-03-01 00:30:00', 0);",
                sqlLines.get(0)
        );

        assertEquals(
                "INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") " +
                        "VALUES ('NEM1201009', '2005-03-02 00:00:00', 0.231);",
                sqlLines.get(47)
        );
    }

    @Test
    void shouldSupportMultipleNmisInSameFile() throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.sql");

        Files.writeString(inputFile, validNem12InputWithTwoNmis());

        MeterDataReader meterDataReader = new MeterDataReader(outputFile);
        meterDataReader.read(inputFile);

        List<String> sqlLines = Files.readAllLines(outputFile);

        assertEquals(96, sqlLines.size());

        assertTrue(sqlLines.get(0).contains("'NEM1201009'"));
        assertTrue(sqlLines.get(47).contains("'NEM1201009'"));

        assertTrue(sqlLines.get(48).contains("'NEM1201010'"));
        assertTrue(sqlLines.get(95).contains("'NEM1201010'"));

        assertTrue(sqlLines.get(0).contains("'2005-03-01 00:30:00'"));
        assertTrue(sqlLines.get(47).contains("'2005-03-02 00:00:00'"));
        assertTrue(sqlLines.get(48).contains("'2005-03-01 00:30:00'"));
        assertTrue(sqlLines.get(95).contains("'2005-03-02 00:00:00'"));
    }

    @Test
    void shouldThrowExceptionWhenHeaderIsInvalid() throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.sql");

        Files.writeString(inputFile,
                "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610\n" +
                        valid300Record() + "\n" +
                        "900\n"
        );

        MeterDataReader meterDataReader = new MeterDataReader(outputFile);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> meterDataReader.read(inputFile)
        );

        assertTrue(exception.getMessage().contains("Invalid Header"));
    }

    @Test
    void shouldCreateOutputFileEvenIfNo300RecordExists() throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.sql");

        Files.writeString(inputFile,
                "100,NEM12,200506081149,UNITEDDP,NEMMCO\n" +
                        "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610\n" +
                        "900\n"
        );

        MeterDataReader meterDataReader = new MeterDataReader(outputFile);
        meterDataReader.read(inputFile);

        assertTrue(Files.exists(outputFile));
        assertEquals(0, Files.readAllLines(outputFile).size());
    }

    @Test
    void shouldContinueProcessingWhenOne300RecordHasError() throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.sql");

        String invalid300RecordWithInsufficientValues =
                "300,20050301,0,0,0";

        Files.writeString(inputFile,
                "100,NEM12,200506081149,UNITEDDP,NEMMCO\n" +
                        "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610\n" +
                        invalid300RecordWithInsufficientValues + "\n" +
                        valid300Record() + "\n" +
                        "900\n"
        );

        MeterDataReader meterDataReader = new MeterDataReader(outputFile);
        meterDataReader.read(inputFile);

        List<String> sqlLines = Files.readAllLines(outputFile);

        assertEquals(48, sqlLines.size());

        assertEquals(
                "INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") " +
                        "VALUES ('NEM1201009', '2005-03-01 00:30:00', 0);",
                sqlLines.get(0)
        );
    }

    private String validNem12InputWithOne300Record() {
        return "100,NEM12,200506081149,UNITEDDP,NEMMCO\n" +
                "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610\n" +
                valid300Record() + "\n" +
                "900\n";
    }

    private String validNem12InputWithTwoNmis() {
        return "100,NEM12,200506081149,UNITEDDP,NEMMCO\n" +
                "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610\n" +
                valid300Record() + "\n" +
                "200,NEM1201010,E1E2,2,E2,,01009,kWh,30,20050610\n" +
                valid300Record() + "\n" +
                "900\n";
    }

    private String valid300Record() {
        return "300,20050301," +
                "0,0,0,0,0,0,0,0,0,0,0,0," +
                "0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327," +
                "1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760," +
                "0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231," +
                "A,,,20050310121004,20050310182204";
    }
}
