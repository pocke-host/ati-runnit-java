package com.runnit.api.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "personal_records")
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "best_5k")
    private Integer best5k;

    @Column(name = "best_10k")
    private Integer best10k;

    @Column(name = "best_half")
    private Integer bestHalf;

    @Column(name = "best_marathon")
    private Integer bestMarathon;

    @Column(name = "longest_run")
    private Integer longestRun;

    @Column(name = "longest_ride")
    private Integer longestRide;

    @Column(name = "most_elevation")
    private Integer mostElevation;

    @Column(name = "fastest_pace")
    private Double fastestPace;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public PersonalRecord() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Integer getBest5k() { return best5k; }
    public Integer getBest10k() { return best10k; }
    public Integer getBestHalf() { return bestHalf; }
    public Integer getBestMarathon() { return bestMarathon; }
    public Integer getLongestRun() { return longestRun; }
    public Integer getLongestRide() { return longestRide; }
    public Integer getMostElevation() { return mostElevation; }
    public Double getFastestPace() { return fastestPace; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setBest5k(Integer best5k) { this.best5k = best5k; }
    public void setBest10k(Integer best10k) { this.best10k = best10k; }
    public void setBestHalf(Integer bestHalf) { this.bestHalf = bestHalf; }
    public void setBestMarathon(Integer bestMarathon) { this.bestMarathon = bestMarathon; }
    public void setLongestRun(Integer longestRun) { this.longestRun = longestRun; }
    public void setLongestRide(Integer longestRide) { this.longestRide = longestRide; }
    public void setMostElevation(Integer mostElevation) { this.mostElevation = mostElevation; }
    public void setFastestPace(Double fastestPace) { this.fastestPace = fastestPace; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private Integer best5k;
        private Integer best10k;
        private Integer bestHalf;
        private Integer bestMarathon;
        private Integer longestRun;
        private Integer longestRide;
        private Integer mostElevation;
        private Double fastestPace;
        private Instant updatedAt;

        public Builder user(User user) { this.user = user; return this; }
        public Builder best5k(Integer best5k) { this.best5k = best5k; return this; }
        public Builder best10k(Integer best10k) { this.best10k = best10k; return this; }
        public Builder bestHalf(Integer bestHalf) { this.bestHalf = bestHalf; return this; }
        public Builder bestMarathon(Integer bestMarathon) { this.bestMarathon = bestMarathon; return this; }
        public Builder longestRun(Integer longestRun) { this.longestRun = longestRun; return this; }
        public Builder longestRide(Integer longestRide) { this.longestRide = longestRide; return this; }
        public Builder mostElevation(Integer mostElevation) { this.mostElevation = mostElevation; return this; }
        public Builder fastestPace(Double fastestPace) { this.fastestPace = fastestPace; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public PersonalRecord build() {
            PersonalRecord r = new PersonalRecord();
            r.user = this.user;
            r.best5k = this.best5k;
            r.best10k = this.best10k;
            r.bestHalf = this.bestHalf;
            r.bestMarathon = this.bestMarathon;
            r.longestRun = this.longestRun;
            r.longestRide = this.longestRide;
            r.mostElevation = this.mostElevation;
            r.fastestPace = this.fastestPace;
            r.updatedAt = this.updatedAt;
            return r;
        }
    }
}
