package com.runnit.api.dto;

/** Total strength training volume over a trailing window — computed on-read, nothing stored. */
public class StrengthVolumeResponse {

    private final int days;
    private final double totalVolumeKg;
    private final int totalSets;
    private final int sessionCount;

    public StrengthVolumeResponse(int days, double totalVolumeKg, int totalSets, int sessionCount) {
        this.days = days;
        this.totalVolumeKg = totalVolumeKg;
        this.totalSets = totalSets;
        this.sessionCount = sessionCount;
    }

    public int getDays() { return days; }
    public double getTotalVolumeKg() { return totalVolumeKg; }
    public int getTotalSets() { return totalSets; }
    public int getSessionCount() { return sessionCount; }
}
