package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "club_messages")
public class ClubMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public ClubMessage() {}

    public Long getId() { return id; }
    public Club getClub() { return club; }
    public User getUser() { return user; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setClub(Club club) { this.club = club; }
    public void setUser(User user) { this.user = user; }
    public void setContent(String content) { this.content = content; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Club club;
        private User user;
        private String content;

        public Builder club(Club club) { this.club = club; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder content(String content) { this.content = content; return this; }

        public ClubMessage build() {
            ClubMessage m = new ClubMessage();
            m.club = this.club;
            m.user = this.user;
            m.content = this.content;
            return m;
        }
    }
}
