package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "media_url", nullable = false)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "caption", length = 500)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private StoryVisibility visibility = StoryVisibility.PUBLIC;

    @ManyToMany
    @JoinTable(
        name = "story_close_friends",
        joinColumns = @JoinColumn(name = "story_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> closeFriends = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryView> views = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Story() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getMediaUrl() { return mediaUrl; }
    public MediaType getMediaType() { return mediaType; }
    public String getCaption() { return caption; }
    public StoryVisibility getVisibility() { return visibility; }
    public List<User> getCloseFriends() { return closeFriends; }
    public List<StoryView> getViews() { return views; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Boolean getIsActive() { return isActive; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setVisibility(StoryVisibility visibility) { this.visibility = visibility; }
    public void setCloseFriends(List<User> closeFriends) { this.closeFriends = closeFriends; }
    public void setViews(List<StoryView> views) { this.views = views; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canView(User viewer) {
        if (!isActive || isExpired()) {
            return false;
        }
        if (visibility == StoryVisibility.PUBLIC) {
            return true;
        }
        if (visibility == StoryVisibility.CLOSE_FRIENDS) {
            return closeFriends.contains(viewer);
        }
        return false;
    }

    public enum MediaType {
        PHOTO,
        VIDEO
    }

    public enum StoryVisibility {
        PUBLIC,
        CLOSE_FRIENDS
    }
}
