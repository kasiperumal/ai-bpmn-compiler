package com.example.aibpmn.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * Dynamically generated Drools fact model for process data.
 * This class is auto-generated based on rule expressions.
 * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.
 */
public class ProcessData {

    private Integer days;
    private Boolean isPeakPeriod;
    private Date startDate;
    private Date endDate;
    private Boolean isEligible;
    private Map<String, Object> data;

    public ProcessData() {
        this.data = new HashMap<>();
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Boolean getIsPeakPeriod() {
        return isPeakPeriod;
    }

    public void setIsPeakPeriod(Boolean isPeakPeriod) {
        this.isPeakPeriod = isPeakPeriod;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsEligible() {
        return isEligible;
    }

    public void setIsEligible(Boolean isEligible) {
        this.isEligible = isEligible;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ProcessData{" +
                "days=" + days +
                ", isPeakPeriod=" + isPeakPeriod +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isEligible=" + isEligible +
                ", data=" + data +
                '}';
    }
}
