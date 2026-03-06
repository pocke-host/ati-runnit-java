package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "story_views", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"story_id", "user_id"})
})
public class StoryView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    public StoryView() {}

    public Long getId() { return id; }
    public Story getStory() { return story; }
    public User getUser() { return user; }
    public LocalDateTime getViewedAt() { return viewedAt; }

    public void setId(Long id) { this.id = id; }
    public void setStory(Story story) { this.story = story; }
    public void setUser(User user) { this.user = user; }
}
