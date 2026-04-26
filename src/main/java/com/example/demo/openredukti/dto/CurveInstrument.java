package com.example.demo.openredukti.dto;

public class CurveInstrument {

    /** ISO date string "YYYY-MM-DD" – maturity / last payment date of the instrument */
    private String maturityDate;

    /** Par rate as a decimal, e.g. 0.055 for 5.5% */
    private double parRate;

    /**
     * One of: DEPOSIT, FRA, FUTURES, SWAP, OIS
     * Defaults to SWAP if absent.
     */
    private String instrumentType;

    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }

    public double getParRate() { return parRate; }
    public void setParRate(double parRate) { this.parRate = parRate; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
}
