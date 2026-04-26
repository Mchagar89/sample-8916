package com.example.demo.openredukti.dto;

import java.util.List;

public class BootstrapRequest {

    /** ISO date "YYYY-MM-DD" – the valuation/curve date */
    private String curveDate;

    /** ISO 4217 currency code, e.g. "USD", "EUR", "GBP". Defaults to "USD". */
    private String currency;

    /**
     * Interpolation algorithm for zero-rate construction.
     * One of: LINEAR, LOG_LINEAR, CUBIC_SPLINE, LOG_CUBIC_SPLINE, MONOTONE_CONVEX
     * Defaults to MONOTONE_CONVEX.
     */
    private String interpolatorType;

    /** Ordered list of market instruments used to bootstrap the curve */
    private List<CurveInstrument> instruments;

    public String getCurveDate() { return curveDate; }
    public void setCurveDate(String curveDate) { this.curveDate = curveDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getInterpolatorType() { return interpolatorType; }
    public void setInterpolatorType(String interpolatorType) { this.interpolatorType = interpolatorType; }

    public List<CurveInstrument> getInstruments() { return instruments; }
    public void setInstruments(List<CurveInstrument> instruments) { this.instruments = instruments; }
}
