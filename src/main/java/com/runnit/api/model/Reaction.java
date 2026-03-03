package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reactions")
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", nullable = false)
    private Moment moment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReactionType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Reaction() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Moment getMoment() { return moment; }
    public ReactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setMoment(Moment moment) { this.moment = moment; }
    public void setType(ReactionType type) { this.type = type; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private Moment moment;
        private ReactionType type;

        public Builder user(User user) { this.user = user; return this; }
        public Builder moment(Moment moment) { this.moment = moment; return this; }
        public Builder type(ReactionType type) { this.type = type; return this; }

        public Reaction build() {
            Reaction r = new Reaction();
            r.user = this.user;
            r.moment = this.moment;
            r.type = this.type;
            return r;
        }
    }

    public enum ReactionType {
        FIRE, CLAP, HEART, STRONG, THUMBS_UP
    }
}
