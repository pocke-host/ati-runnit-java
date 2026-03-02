package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_workouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "day", nullable = false)
    private Integer day;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;
}
