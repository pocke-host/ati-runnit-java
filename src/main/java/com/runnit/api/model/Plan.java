package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sport")
    private String sport;

    @Column(name = "goal")
    private String goal;

    @Column(name = "level")
    private String level;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "days_per_week")
    private Integer daysPerWeek;

    @Column(name = "total_weeks")
    private Integer totalWeeks;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_race_date")
    private LocalDate targetRaceDate;

    @Column(name = "current_weekly_meters")
    private Integer currentWeeklyMeters;

    @Column(name = "target_seconds")
    private Integer targetSeconds;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("week_number ASC, day ASC")
    private List<PlanWorkout> workouts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Plan() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getGoal() { return goal; }
    public String getLevel() { return level; }
    public boolean isActive() { return active; }
    public Integer getDaysPerWeek() { return daysPerWeek; }
    public Integer getTotalWeeks() { return totalWeeks; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getTargetRaceDate() { return targetRaceDate; }
    public Integer getCurrentWeeklyMeters() { return currentWeeklyMeters; }
    public Integer getTargetSeconds() { return targetSeconds; }
    public List<PlanWorkout> getWorkouts() { return workouts; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setName(String name) { this.name = name; }
    public void setSport(String sport) { this.sport = sport; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setLevel(String level) { this.level = level; }
    public void setActive(boolean active) { this.active = active; }
    public void setDaysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; }
    public void setTotalWeeks(Integer totalWeeks) { this.totalWeeks = totalWeeks; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setTargetRaceDate(LocalDate targetRaceDate) { this.targetRaceDate = targetRaceDate; }
    public void setCurrentWeeklyMeters(Integer currentWeeklyMeters) { this.currentWeeklyMeters = currentWeeklyMeters; }
    public void setTargetSeconds(Integer targetSeconds) { this.targetSeconds = targetSeconds; }
    public void setWorkouts(List<PlanWorkout> workouts) { this.workouts = workouts; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private String name;
        private String sport;
        private String goal;
        private String level;
        private boolean active = false;
        private Integer daysPerWeek;
        private Integer totalWeeks;
        private LocalDate startDate;
        private LocalDate targetRaceDate;
        private Integer currentWeeklyMeters;
        private Integer targetSeconds;

        public Builder user(User user) { this.user = user; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder goal(String goal) { this.goal = goal; return this; }
        public Builder level(String level) { this.level = level; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder daysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; return this; }
        public Builder totalWeeks(Integer totalWeeks) { this.totalWeeks = totalWeeks; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder targetRaceDate(LocalDate targetRaceDate) { this.targetRaceDate = targetRaceDate; return this; }
        public Builder currentWeeklyMeters(Integer currentWeeklyMeters) { this.currentWeeklyMeters = currentWeeklyMeters; return this; }
        public Builder targetSeconds(Integer targetSeconds) { this.targetSeconds = targetSeconds; return this; }

        public Plan build() {
            Plan p = new Plan();
            p.user = this.user;
            p.name = this.name;
            p.sport = this.sport;
            p.goal = this.goal;
            p.level = this.level;
            p.active = this.active;
            p.daysPerWeek = this.daysPerWeek;
            p.totalWeeks = this.totalWeeks;
            p.startDate = this.startDate;
            p.targetRaceDate = this.targetRaceDate;
            p.currentWeeklyMeters = this.currentWeeklyMeters;
            p.targetSeconds = this.targetSeconds;
            return p;
        }
    }
}
