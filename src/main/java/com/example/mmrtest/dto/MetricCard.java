package com.example.mmrtest.dto;

public class MetricCard {
    private String key;
    private String label;
    private double value;
    private String unit;
    private double baseline;
    private int score;
    private String description;

    public MetricCard() {
    }

    public MetricCard(String key, String label, double value, String unit, double baseline, int score, String description) {
        this.key = key;
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.baseline = baseline;
        this.score = score;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getBaseline() {
        return baseline;
    }

    public void setBaseline(double baseline) {
        this.baseline = baseline;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}