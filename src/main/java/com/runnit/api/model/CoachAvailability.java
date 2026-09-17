package com.runnit.api.model;
import jakarta.persistence.*;
import java.time.LocalTime;
@Entity @Table(name = "coach_availability")
public class CoachAvailability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="coach_id", nullable=false) private Long coachId;
    @Column(nullable=false) private Integer weekday;
    @Column(name="start_time", nullable=false) private LocalTime startTime;
    @Column(name="end_time", nullable=false) private LocalTime endTime;
    @Column(nullable=false) private String timezone = "UTC";
    public CoachAvailability() {}
    public Long getId(){return id;} public Long getCoachId(){return coachId;} public Integer getWeekday(){return weekday;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public String getTimezone(){return timezone;}
    public void setId(Long v){id=v;} public void setCoachId(Long v){coachId=v;} public void setWeekday(Integer v){weekday=v;} public void setStartTime(LocalTime v){startTime=v;} public void setEndTime(LocalTime v){endTime=v;} public void setTimezone(String v){timezone=v;}
}
