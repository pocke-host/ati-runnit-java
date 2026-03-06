package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id")
    private Moment moment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Comment() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Moment getMoment() { return moment; }
    public Activity getActivity() { return activity; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setMoment(Moment moment) { this.moment = moment; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setContent(String content) { this.content = content; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private Moment moment;
        private Activity activity;
        private String content;

        public Builder user(User user) { this.user = user; return this; }
        public Builder moment(Moment moment) { this.moment = moment; return this; }
        public Builder activity(Activity activity) { this.activity = activity; return this; }
        public Builder content(String content) { this.content = content; return this; }

        public Comment build() {
            Comment c = new Comment();
            c.user = this.user;
            c.moment = this.moment;
            c.activity = this.activity;
            c.content = this.content;
            return c;
        }
    }
}
