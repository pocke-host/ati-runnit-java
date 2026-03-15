package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multisport_events")
public class MultisportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "event_type", nullable = false)
    private String eventType = "TRIATHLON";

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<MultisportEventActivity> segments = new ArrayList<>();

    public MultisportEvent() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getEventType() { return eventType; }
    public LocalDate getEventDate() { return eventDate; }
    public String getNotes() { return notes; }
    public Boolean getIsPublic() { return isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MultisportEventActivity> getSegments() { return segments; }

    public void setUser(User user) { this.user = user; }
    public void setName(String name) { this.name = name; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
