package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Daily recovery/sleep/strain snapshot — the data that's actually unique to a
 * device like WHOOP versus a normal GPS watch. One row per user per day.
 * Source-agnostic by design (not "WhoopDaily") so a future Oura/Garmin body
 * battery integration can write into the same table.
 */
@Entity
@Table(name = "wellness_daily")
public class WellnessDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "WHOOP";

    @Column(name = "external_cycle_id", length = 50)
    private String externalCycleId;

    @Column(name = "recovery_score")
    private Integer recoveryScore;

    @Column(name = "hrv_milli")
    private Double hrvMilli;

    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;

    @Column(name = "sleep_performance_pct")
    private Integer sleepPerformancePct;

    @Column(name = "sleep_efficiency_pct")
    private Double sleepEfficiencyPct;

    @Column(name = "total_sleep_minutes")
    private Integer totalSleepMinutes;

    @Column(name = "strain")
    private Double strain;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public WellnessDaily() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public String getSource() { return source; }
    public String getExternalCycleId() { return externalCycleId; }
    public Integer getRecoveryScore() { return recoveryScore; }
    public Double getHrvMilli() { return hrvMilli; }
    public Integer getRestingHeartRate() { return restingHeartRate; }
    public Integer getSleepPerformancePct() { return sleepPerformancePct; }
    public Double getSleepEfficiencyPct() { return sleepEfficiencyPct; }
    public Integer getTotalSleepMinutes() { return totalSleepMinutes; }
    public Double getStrain() { return strain; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setSource(String source) { this.source = source; }
    public void setExternalCycleId(String externalCycleId) { this.externalCycleId = externalCycleId; }
    public void setRecoveryScore(Integer recoveryScore) { this.recoveryScore = recoveryScore; }
    public void setHrvMilli(Double hrvMilli) { this.hrvMilli = hrvMilli; }
    public void setRestingHeartRate(Integer restingHeartRate) { this.restingHeartRate = restingHeartRate; }
    public void setSleepPerformancePct(Integer sleepPerformancePct) { this.sleepPerformancePct = sleepPerformancePct; }
    public void setSleepEfficiencyPct(Double sleepEfficiencyPct) { this.sleepEfficiencyPct = sleepEfficiencyPct; }
    public void setTotalSleepMinutes(Integer totalSleepMinutes) { this.totalSleepMinutes = totalSleepMinutes; }
    public void setStrain(Double strain) { this.strain = strain; }
}
