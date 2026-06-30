package com.floenergy.core;

import com.floenergy.core.util.ConsumptionValueValidator;
import com.floenergy.core.util.IntervalTimestampCalculator;
import com.floenergy.model.NMIDataDetail;
import com.floenergy.model.NMIIntervalRecord;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.floenergy.contants.MDConstants.ALLOWED_INTERVAL_LENGTH;

public class Nem12Parser implements MDParser {

    private static final Logger LOGGER =
            Logger.getLogger(Nem12Parser.class.getName());

    @Override
    public Integer extractRecordIndicator(String[] fields) {
        String recordIndicator = fields[0];
        if(recordIndicator == null || recordIndicator.isBlank() || recordIndicator.length() == 0){
            throw new RuntimeException("record Indicator is null or blank");
        }
        return Integer.parseInt(recordIndicator);
    }

    @Override
    public NMIDataDetail parseNMIDataDetail(String[] fields) {
        String NMI = fields[1];
        String intervalLength = fields[8];
        if(NMI == null || NMI.isBlank()){
            throw new RuntimeException("NMI is null or blank");
        }
        if(!ALLOWED_INTERVAL_LENGTH.contains(intervalLength)){
            throw new RuntimeException(intervalLength + " Interval length not allowed");
        }
        NMIDataDetail nmiDataDetail = new NMIDataDetail(NMI,Integer.parseInt(intervalLength));
        return nmiDataDetail;
    }

    @Override
    public List<NMIIntervalRecord> parseNMIIntervalRecord(NMIDataDetail nmiDataDetail, String[] fields, int lineNumber) {
        if(nmiDataDetail == null){
            throw new RuntimeException("current NMI Data detail(200) Not found or invalid for 300 record");
        }
        List<NMIIntervalRecord> meterDataIntervalsForTheDay = new ArrayList<>();
        String intervalDate = fields[1];
        Integer intervalLengthInMinutes = nmiDataDetail.getIntervalLength();
        Integer expectedIntervals = (1440/intervalLengthInMinutes);
        LocalDate localDate = IntervalTimestampCalculator.parseNemDate(intervalDate);

        if(fields.length < expectedIntervals + 2){
            throw new RuntimeException("Insufficient consumption value for the interval length " + intervalLengthInMinutes
                    + " on interval date "+ localDate + "of  NMI " + nmiDataDetail.getCurrentNMI());
        }

        for (int i = 1; i <= expectedIntervals; i++) {
            String consumptionValue = fields[i+1];
            List<String> errors = new ArrayList<>();
            Optional<BigDecimal> consumption = ConsumptionValueValidator.parseConsumption(consumptionValue,lineNumber, i, errors);
            if(consumption.isEmpty()){
                LOGGER.log(Level.WARNING,"Invalid Consumption Value " + errors.get(0));
                continue;
            }
            Timestamp timestamp = IntervalTimestampCalculator.calculateIntervalTimestamp(localDate, i, intervalLengthInMinutes);
            meterDataIntervalsForTheDay.add(new NMIIntervalRecord(nmiDataDetail.getCurrentNMI(), timestamp, consumptionValue));
        }
        return  meterDataIntervalsForTheDay;
    }


}
