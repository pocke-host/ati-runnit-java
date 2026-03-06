package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "activity_reactions")
public class ActivityReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Reaction.ReactionType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public ActivityReaction() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Activity getActivity() { return activity; }
    public Reaction.ReactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setType(Reaction.ReactionType type) { this.type = type; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private Activity activity;
        private Reaction.ReactionType type;

        public Builder user(User user) { this.user = user; return this; }
        public Builder activity(Activity activity) { this.activity = activity; return this; }
        public Builder type(Reaction.ReactionType type) { this.type = type; return this; }

        public ActivityReaction build() {
            ActivityReaction r = new ActivityReaction();
            r.user = this.user;
            r.activity = this.activity;
            r.type = this.type;
            return r;
        }
    }
}
