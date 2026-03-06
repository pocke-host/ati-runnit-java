package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "challenges")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sport")
    private String sport;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "prize")
    private String prize;

    @Column(name = "participant_count", nullable = false)
    private int participantCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Challenge() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSport() { return sport; }
    public String getImageUrl() { return imageUrl; }
    public Instant getEndDate() { return endDate; }
    public String getPrize() { return prize; }
    public int getParticipantCount() { return participantCount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSport(String sport) { this.sport = sport; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public void setPrize(String prize) { this.prize = prize; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name;
        private String description;
        private String sport;
        private String imageUrl;
        private Instant endDate;
        private String prize;
        private int participantCount = 0;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder endDate(Instant endDate) { this.endDate = endDate; return this; }
        public Builder prize(String prize) { this.prize = prize; return this; }
        public Builder participantCount(int participantCount) { this.participantCount = participantCount; return this; }

        public Challenge build() {
            Challenge c = new Challenge();
            c.name = this.name;
            c.description = this.description;
            c.sport = this.sport;
            c.imageUrl = this.imageUrl;
            c.endDate = this.endDate;
            c.prize = this.prize;
            c.participantCount = this.participantCount;
            return c;
        }
    }
}
