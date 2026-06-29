package com.floenergy.model;

import java.sql.Timestamp;

public class NMIIntervalRecord {

    private String NMI;
    private Timestamp timestamp;
    private String consumption;

    public NMIIntervalRecord(String NMI, Timestamp timestamp, String consumption){
        this.NMI = NMI;
        this.timestamp = timestamp;
        this.consumption = consumption;
    }

    public String getConsumption() {
        return consumption;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getNMI() {
        return NMI;
    }
}
