package com.example.demo.openredukti.dto;

public class SwapValuationRequest {

    /** ISO date "YYYY-MM-DD" – today's pricing date */
    private String tradeDate;

    /** Swap start date */
    private String effectiveDate;

    /** Swap maturity date */
    private String terminationDate;

    /** ISO 4217 currency code */
    private String currency;

    /** Notional amount in currency units */
    private double notional;

    /** Fixed coupon rate as a decimal, e.g. 0.055 for 5.5% */
    private double fixedRate;

    /** true = receive floating / pay fixed; false = receive fixed / pay floating */
    private boolean payFixed;

    /**
     * 0 = PV only, 1 = PV + delta (first-order DV01), 2 = PV + delta + gamma
     * Defaults to 1.
     */
    private int derivativeOrder = 1;

    /** Pre-computed zero curve to use for discounting/projection */
    private ZeroCurveInput zeroCurve;

    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getTerminationDate() { return terminationDate; }
    public void setTerminationDate(String terminationDate) { this.terminationDate = terminationDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getNotional() { return notional; }
    public void setNotional(double notional) { this.notional = notional; }

    public double getFixedRate() { return fixedRate; }
    public void setFixedRate(double fixedRate) { this.fixedRate = fixedRate; }

    public boolean isPayFixed() { return payFixed; }
    public void setPayFixed(boolean payFixed) { this.payFixed = payFixed; }

    public int getDerivativeOrder() { return derivativeOrder; }
    public void setDerivativeOrder(int derivativeOrder) { this.derivativeOrder = derivativeOrder; }

    public ZeroCurveInput getZeroCurve() { return zeroCurve; }
    public void setZeroCurve(ZeroCurveInput zeroCurve) { this.zeroCurve = zeroCurve; }
}
