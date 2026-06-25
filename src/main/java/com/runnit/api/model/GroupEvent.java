package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_events")
public class GroupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(name = "sport_type", nullable = false)
    private String sportType = "RUN";

    @Column(name = "event_datetime", nullable = false)
    private LocalDateTime eventDatetime;

    @Column(name = "location_name")
    private String locationName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 100)
    private String city;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @Column(name = "max_attendees")
    private Integer maxAttendees;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GroupEvent() {}

    public Long getId() { return id; }
    public User getCreator() { return creator; }
    public String getTitle() { return title; }
    public String getSportType() { return sportType; }
    public LocalDateTime getEventDatetime() { return eventDatetime; }
    public String getLocationName() { return locationName; }
    public String getDescription() { return description; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCity() { return city; }
    public boolean isPublic() { return isPublic; }
    public Integer getMaxAttendees() { return maxAttendees; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreator(User creator) { this.creator = creator; }
    public void setTitle(String title) { this.title = title; }
    public void setSportType(String sportType) { this.sportType = sportType; }
    public void setEventDatetime(LocalDateTime eventDatetime) { this.eventDatetime = eventDatetime; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public void setDescription(String description) { this.description = description; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setCity(String city) { this.city = city; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
}
