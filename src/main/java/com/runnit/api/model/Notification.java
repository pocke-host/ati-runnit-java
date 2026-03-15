package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {}

    public Long getId() { return id; }
    public Long getUserId() { return user != null ? user.getId() : null; }
    public User getUser() { return user; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public User getActor() { return actor; }
    public Long getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setType(String type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setActor(User actor) { this.actor = actor; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setRead(boolean read) { this.read = read; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private String type;
        private String message;
        private User actor;
        private Long referenceId;
        private String referenceType;
        private boolean read = false;

        public Builder user(User user) { this.user = user; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder actor(User actor) { this.actor = actor; return this; }
        public Builder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
        public Builder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public Builder read(boolean read) { this.read = read; return this; }

        public Notification build() {
            Notification n = new Notification();
            n.user = this.user;
            n.type = this.type;
            n.message = this.message;
            n.actor = this.actor;
            n.referenceId = this.referenceId;
            n.referenceType = this.referenceType;
            n.read = this.read;
            return n;
        }
    }
}
