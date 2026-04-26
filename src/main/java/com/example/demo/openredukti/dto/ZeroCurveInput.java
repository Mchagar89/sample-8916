package com.example.demo.openredukti.dto;

import java.util.List;

/** Pre-computed zero curve supplied to a valuation request. */
public class ZeroCurveInput {

    private int id = 1;
    private List<ZeroCurvePoint> points;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public List<ZeroCurvePoint> getPoints() { return points; }
    public void setPoints(List<ZeroCurvePoint> points) { this.points = points; }

    public static class ZeroCurvePoint {
        private String maturityDate;
        private double discountFactor;

        public String getMaturityDate() { return maturityDate; }
        public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }

        public double getDiscountFactor() { return discountFactor; }
        public void setDiscountFactor(double discountFactor) { this.discountFactor = discountFactor; }
    }
}
