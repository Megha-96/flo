package com.floenergy.core;

import com.floenergy.model.NMIIntervalRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteSingleBatchedInsertForMultipleRecords() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                ),
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 1, 0)),
                        "0.461"
                ),
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 1, 30)),
                        "0.810"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(records);
        }

        String actualSql = Files.readString(outputFile);

        String expectedSql = """
                INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES
                ('NEM1201009', '2005-03-01 00:30:00', 0),
                ('NEM1201009', '2005-03-01 01:00:00', 0.461),
                ('NEM1201009', '2005-03-01 01:30:00', 0.810);
                """;

        assertEquals(expectedSql, actualSql);
    }

    @Test
    void shouldWriteSingleBatchedInsertForOneRecord() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(records);
        }

        String actualSql = Files.readString(outputFile);

        String expectedSql = """
                INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES
                ('NEM1201009', '2005-03-01 00:30:00', 0);
                """;

        assertEquals(expectedSql, actualSql);
    }

    @Test
    void shouldEndBatchedInsertWithSemicolon() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(records);
        }

        String actualSql = Files.readString(outputFile);

        assertTrue(actualSql.trim().endsWith(";"));
    }

    @Test
    void shouldSeparateBatchedRowsWithCommaExceptLastRow() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                ),
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 1, 0)),
                        "0.461"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(records);
        }

        List<String> lines = Files.readAllLines(outputFile);

        assertEquals(
                "INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") VALUES",
                lines.get(0)
        );
        assertEquals(
                "('NEM1201009', '2005-03-01 00:30:00', 0),",
                lines.get(1)
        );
        assertEquals(
                "('NEM1201009', '2005-03-01 01:00:00', 0.461);",
                lines.get(2)
        );
    }

    @Test
    void shouldNotWriteAnythingForEmptyBatch() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(List.of());
        }

        String actualSql = Files.readString(outputFile);

        assertEquals("", actualSql);
    }

    @Test
    void shouldNotWriteAnythingForNullBatch() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(null);
        }

        String actualSql = Files.readString(outputFile);

        assertEquals("", actualSql);
    }

    @Test
    void shouldWriteSeparateInsertStatementsForWriteMethod() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                ),
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 1, 0)),
                        "0.461"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.write(records);
        }

        String actualSql = Files.readString(outputFile);

        String expectedSql = """
                INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 00:30:00', 0);
                INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 01:00:00', 0.461);
                """;

        assertEquals(expectedSql, actualSql);
    }

    @Test
    void shouldNotWriteAnythingForEmptyListInWriteMethod() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.write(List.of());
        }

        String actualSql = Files.readString(outputFile);

        assertEquals("", actualSql);
    }

    @Test
    void shouldNotWriteAnythingForNullListInWriteMethod() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.write(null);
        }

        String actualSql = Files.readString(outputFile);

        assertEquals("", actualSql);
    }

    @Test
    void shouldOverwriteExistingOutputFile() throws Exception {
        Path outputFile = tempDir.resolve("output.sql");

        Files.writeString(outputFile, "old content");

        List<NMIIntervalRecord> records = List.of(
                new NMIIntervalRecord(
                        "NEM1201009",
                        Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30)),
                        "0"
                )
        );

        try (SqlWriter sqlWriter = new SqlWriter(outputFile)) {
            sqlWriter.batchWrite(records);
        }

        String actualSql = Files.readString(outputFile);

        assertTrue(actualSql.contains("INSERT INTO meter_readings"));
        assertTrue(!actualSql.contains("old content"));
    }
}