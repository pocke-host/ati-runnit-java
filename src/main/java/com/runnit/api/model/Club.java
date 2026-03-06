package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sport")
    private String sport;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_private", nullable = false)
    private boolean privateClub = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Club() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public boolean isPrivateClub() { return privateClub; }
    public User getOwner() { return owner; }
    public int getMemberCount() { return memberCount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSport(String sport) { this.sport = sport; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPrivateClub(boolean privateClub) { this.privateClub = privateClub; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name;
        private String sport;
        private String description;
        private String imageUrl;
        private boolean privateClub = false;
        private User owner;
        private int memberCount = 1;

        public Builder name(String name) { this.name = name; return this; }
        public Builder sport(String sport) { this.sport = sport; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder privateClub(boolean privateClub) { this.privateClub = privateClub; return this; }
        public Builder owner(User owner) { this.owner = owner; return this; }
        public Builder memberCount(int memberCount) { this.memberCount = memberCount; return this; }

        public Club build() {
            Club c = new Club();
            c.name = this.name;
            c.sport = this.sport;
            c.description = this.description;
            c.imageUrl = this.imageUrl;
            c.privateClub = this.privateClub;
            c.owner = this.owner;
            c.memberCount = this.memberCount;
            return c;
        }
    }
}
