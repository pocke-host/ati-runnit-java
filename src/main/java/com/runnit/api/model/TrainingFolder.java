package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "training_folders")
public class TrainingFolder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 120) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 20) private String color;
    @Column(name = "target_date") private java.time.LocalDate targetDate;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    public TrainingFolder() {}
    public Long getId(){return id;} public User getUser(){return user;} public String getName(){return name;}
    public String getDescription(){return description;} public String getColor(){return color;}
    public java.time.LocalDate getTargetDate(){return targetDate;} public Instant getCreatedAt(){return createdAt;}
    public void setUser(User v){user=v;} public void setName(String v){name=v;} public void setDescription(String v){description=v;}
    public void setColor(String v){color=v;} public void setTargetDate(java.time.LocalDate v){targetDate=v;}
}
