package com.floenergy.core;

import com.floenergy.model.NMIDataDetail;
import com.floenergy.model.NMIIntervalRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Nem12ParserTest {

    private Nem12Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Nem12Parser();
    }

    @Test
    void shouldExtractRecordIndicator() {
        String[] fields = "300,20050301,0.1".split(",", -1);

        Integer recordIndicator = parser.extractRecordIndicator(fields);

        assertEquals(300, recordIndicator);
    }

    @Test
    void shouldParseValidNmiDataDetailRecord() {
        String[] fields = "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610"
                .split(",", -1);

        NMIDataDetail result = parser.parseNMIDataDetail(fields);

        assertEquals("NEM1201009", result.getCurrentNMI());
        assertEquals(30, result.getIntervalLength());
    }

    @Test
    void shouldThrowExceptionWhenNmiIsBlank() {
        String[] fields = "200,,E1E2,1,E1,N1,01009,kWh,30,20050610"
                .split(",", -1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> parser.parseNMIDataDetail(fields)
        );

        assertTrue(exception.getMessage().contains("NMI is null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenIntervalLengthIsNotAllowed() {
        String[] fields = "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,10,20050610"
                .split(",", -1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> parser.parseNMIDataDetail(fields)
        );

        assertTrue(exception.getMessage().contains("Interval length not allowed"));
    }

    @Test
    void shouldParse300RecordAndCreate48IntervalRecordsFor30MinuteInterval() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        assertEquals(48, result.size());
    }

    @Test
    void shouldSetCorrectNmiForAllIntervalRecords() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        assertTrue(result.stream()
                .allMatch(record -> "NEM1201009".equals(record.getNMI())));
    }

    @Test
    void shouldCalculateFirstIntervalTimestampFor30MinuteInterval() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        Timestamp expectedTimestamp =
                Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 0, 30));

        assertEquals(expectedTimestamp, result.get(0).getTimestamp());
    }

    @Test
    void shouldCalculateLastIntervalTimestampFor30MinuteIntervalAsNextDayMidnight() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        Timestamp expectedTimestamp =
                Timestamp.valueOf(LocalDateTime.of(2005, 3, 2, 0, 0));

        assertEquals(expectedTimestamp, result.get(47).getTimestamp());
    }

    @Test
    void shouldSetCorrectConsumptionValues() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        assertEquals("0", result.get(0).getConsumption());
        assertEquals("0.461", result.get(12).getConsumption());
        assertEquals("0.231", result.get(47).getConsumption());
    }

    @Test
    void shouldThrowExceptionWhen300RecordAppearsBefore200Record() {
        String[] fields = valid300RecordFor30MinuteInterval().split(",", -1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> parser.parseNMIIntervalRecord(null, fields, 3)
        );

        assertTrue(exception.getMessage().contains("current NMI Data detail"));
    }

    @Test
    void shouldThrowExceptionWhenConsumptionValuesAreInsufficient() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String[] fields = "300,20050301,0,0,0,0,0".split(",", -1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3)
        );

        assertTrue(exception.getMessage().contains("Insufficient consumption value"));
    }

    @Test
    void shouldSkipInvalidConsumptionValues() {
        NMIDataDetail nmiDataDetail = new NMIDataDetail("NEM1201009", 30);

        String invalid300Record = "300,20050301," +
                "-1,0,0,0,0,0,0,0,0,0,0,0," +
                "0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327," +
                "1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760," +
                "0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231," +
                "A,,,20050310121004,20050310182204";

        String[] fields = invalid300Record.split(",", -1);

        List<NMIIntervalRecord> result =
                parser.parseNMIIntervalRecord(nmiDataDetail, fields, 3);

        assertEquals(47, result.size());
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2005, 3, 1, 1, 0)),
                result.get(0).getTimestamp());
    }

    private String valid300RecordFor30MinuteInterval() {
        return "300,20050301," +
                "0,0,0,0,0,0,0,0,0,0,0,0," +
                "0.461,0.810,0.568,1.234,1.353,1.507,1.344,1.773,0.848,1.271,0.895,1.327," +
                "1.013,1.793,0.988,0.985,0.876,0.555,0.760,0.938,0.566,0.512,0.970,0.760," +
                "0.731,0.615,0.886,0.531,0.774,0.712,0.598,0.670,0.587,0.657,0.345,0.231," +
                "A,,,20050310121004,20050310182204";
    }
}