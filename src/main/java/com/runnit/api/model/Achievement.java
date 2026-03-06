package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_id", nullable = false)
    private String badgeId;

    @CreationTimestamp
    @Column(name = "earned_at", nullable = false, updatable = false)
    private Instant earnedAt;

    public Achievement() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getBadgeId() { return badgeId; }
    public Instant getEarnedAt() { return earnedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setBadgeId(String badgeId) { this.badgeId = badgeId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private String badgeId;

        public Builder user(User user) { this.user = user; return this; }
        public Builder badgeId(String badgeId) { this.badgeId = badgeId; return this; }

        public Achievement build() {
            Achievement a = new Achievement();
            a.user = this.user;
            a.badgeId = this.badgeId;
            return a;
        }
    }
}
