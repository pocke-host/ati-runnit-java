package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "invite_events")
public class InviteEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    /** Null when the visitor wasn't authenticated at the time of the hit. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id")
    private User visitor;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InviteEvent() {}

    public Long getId() { return id; }
    public User getInviter() { return inviter; }
    public User getVisitor() { return visitor; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setInviter(User inviter) { this.inviter = inviter; }
    public void setVisitor(User visitor) { this.visitor = visitor; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User inviter;
        private User visitor;
        private String eventType;

        public Builder inviter(User inviter) { this.inviter = inviter; return this; }
        public Builder visitor(User visitor) { this.visitor = visitor; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }

        public InviteEvent build() {
            InviteEvent e = new InviteEvent();
            e.inviter = this.inviter;
            e.visitor = this.visitor;
            e.eventType = this.eventType;
            return e;
        }
    }
}
