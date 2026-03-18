package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "race_bookmarks")
public class RaceBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No FK constraints — PlanetScale doesn't support them
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "external_race_id", length = 100)
    private String externalRaceId;

    @Column(name = "race_name", nullable = false)
    private String raceName;

    @Column(name = "race_date")
    private LocalDate raceDate;

    @Column(name = "race_type", length = 50)
    private String raceType;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "race_url", length = 500)
    private String raceUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RaceBookmark() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getExternalRaceId() { return externalRaceId; }
    public String getRaceName() { return raceName; }
    public LocalDate getRaceDate() { return raceDate; }
    public String getRaceType() { return raceType; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getRaceUrl() { return raceUrl; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setExternalRaceId(String externalRaceId) { this.externalRaceId = externalRaceId; }
    public void setRaceName(String raceName) { this.raceName = raceName; }
    public void setRaceDate(LocalDate raceDate) { this.raceDate = raceDate; }
    public void setRaceType(String raceType) { this.raceType = raceType; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setRaceUrl(String raceUrl) { this.raceUrl = raceUrl; }
}
