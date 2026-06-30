package com.floenergy.core;

import com.floenergy.model.NMIDataDetail;
import com.floenergy.model.NMIIntervalRecord;

import java.util.List;

public interface MDParser {

    Integer extractRecordIndicator(String[] fields);
    NMIDataDetail parseNMIDataDetail(String[] fields);
    List<NMIIntervalRecord> parseNMIIntervalRecord(NMIDataDetail nmiDataDetail, String[] fields, int linenumber);
}
