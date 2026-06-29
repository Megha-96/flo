package com.floenergy.core;


import com.floenergy.model.NMIIntervalRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SqlWriter implements AutoCloseable {

    private final BufferedWriter writer;
    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SqlWriter(Path outputFilePath) {
        try {
            this.writer = Files.newBufferedWriter(outputFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SQL output file: " + outputFilePath, e);
        }
    }

    public void batchWrite(List<NMIIntervalRecord> intervalRecords) {
        if (intervalRecords == null || intervalRecords.isEmpty()) {
            return;
        }

        try {
            writer.write("INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") VALUES");
            writer.newLine();

            for (int i = 0; i < intervalRecords.size(); i++) {
                NMIIntervalRecord record = intervalRecords.get(i);

                writer.write(toValueRow(record));

                if (i == intervalRecords.size() - 1) {
                    writer.write(";");
                } else {
                    writer.write(",");
                }

                writer.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write SQL insert statements", e);
        }
    }

    public void write(List<NMIIntervalRecord> intervalRecords) {
        if (intervalRecords == null || intervalRecords.isEmpty()) {
            return;
        }

        try {
            for (NMIIntervalRecord record : intervalRecords) {
                writer.write(toInsertStatement(record));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write SQL insert statements", e);
        }
    }

    private String toInsertStatement(NMIIntervalRecord record) {
        return String.format(
                "INSERT INTO meter_readings (\"nmi\", \"timestamp\", \"consumption\") " +
                        "VALUES ('%s', '%s', %s);",
                record.getNMI(),
                record.getTimestamp().toLocalDateTime().format(SQL_TIMESTAMP_FORMATTER),
                record.getConsumption()
        );
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String toValueRow(NMIIntervalRecord record) {
        String formattedTimestamp = record.getTimestamp()
                .toLocalDateTime()
                .format(SQL_TIMESTAMP_FORMATTER);

        return String.format(
                "('%s', '%s', %s)",
                record.getNMI(),
                formattedTimestamp,
                record.getConsumption()
        );
    }

    @Override
    public void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close SQL writer", e);
        }
    }
}