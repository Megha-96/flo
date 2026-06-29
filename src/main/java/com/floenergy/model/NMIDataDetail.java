package com.floenergy.model;

public class NMIDataDetail {

    private String currentNMI;
    private Integer intervalLength;

    public NMIDataDetail(String currentNMI, Integer intervalLength){
        this.currentNMI = currentNMI;
        this.intervalLength = intervalLength;
    }

    public Integer getIntervalLength() {
        return intervalLength;
    }

    public String getCurrentNMI() {
        return currentNMI;
    }
}
