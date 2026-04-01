package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "moments")
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(name = "route_snapshot_url")
    private String routeSnapshotUrl;

    @Column(name = "song_title")
    private String songTitle;

    @Column(name = "song_artist")
    private String songArtist;

    @Column(name = "song_link")
    private String songLink;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "day_key", nullable = false)
    private LocalDate dayKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Moment() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Activity getActivity() { return activity; }
    public String getPhotoUrl() { return photoUrl; }
    public String getRouteSnapshotUrl() { return routeSnapshotUrl; }
    public String getSongTitle() { return songTitle; }
    public String getSongArtist() { return songArtist; }
    public String getSongLink() { return songLink; }
    public String getCaption() { return caption; }
    public LocalDate getDayKey() { return dayKey; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setRouteSnapshotUrl(String routeSnapshotUrl) { this.routeSnapshotUrl = routeSnapshotUrl; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }
    public void setSongArtist(String songArtist) { this.songArtist = songArtist; }
    public void setSongLink(String songLink) { this.songLink = songLink; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setDayKey(LocalDate dayKey) { this.dayKey = dayKey; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private Activity activity;
        private String photoUrl;
        private String routeSnapshotUrl;
        private String songTitle;
        private String songArtist;
        private String songLink;
        private String caption;
        private LocalDate dayKey;

        public Builder user(User user) { this.user = user; return this; }
        public Builder activity(Activity activity) { this.activity = activity; return this; }
        public Builder photoUrl(String photoUrl) { this.photoUrl = photoUrl; return this; }
        public Builder routeSnapshotUrl(String routeSnapshotUrl) { this.routeSnapshotUrl = routeSnapshotUrl; return this; }
        public Builder songTitle(String songTitle) { this.songTitle = songTitle; return this; }
        public Builder songArtist(String songArtist) { this.songArtist = songArtist; return this; }
        public Builder songLink(String songLink) { this.songLink = songLink; return this; }
        public Builder caption(String caption) { this.caption = caption; return this; }
        public Builder dayKey(LocalDate dayKey) { this.dayKey = dayKey; return this; }

        public Moment build() {
            Moment m = new Moment();
            m.user = this.user;
            m.activity = this.activity;
            m.photoUrl = this.photoUrl;
            m.routeSnapshotUrl = this.routeSnapshotUrl;
            m.songTitle = this.songTitle;
            m.songArtist = this.songArtist;
            m.songLink = this.songLink;
            m.caption = this.caption;
            m.dayKey = this.dayKey;
            return m;
        }
    }
}
