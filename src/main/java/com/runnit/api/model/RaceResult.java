package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "race_results")
public class RaceResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "race_name", nullable = false) private String raceName;
    @Column(name = "race_date") private LocalDate raceDate;
    @Column(name = "distance", length = 40) private String distance;
    @Column(name = "finish_time_seconds") private Integer finishTimeSeconds;
    @Column(name = "placement") private Integer placement;
    @Column(name = "source", nullable = false, length = 40) private String source;
    @Column(name = "external_result_id", length = 160) private String externalResultId;
    @Column(name = "result_url", length = 500) private String resultUrl;
    @Column(name = "verified", nullable = false) private boolean verified;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public RaceResult() {}
    public Long getId(){return id;} public Long getUserId(){return userId;} public String getRaceName(){return raceName;}
    public LocalDate getRaceDate(){return raceDate;} public String getDistance(){return distance;}
    public Integer getFinishTimeSeconds(){return finishTimeSeconds;} public Integer getPlacement(){return placement;}
    public String getSource(){return source;} public String getExternalResultId(){return externalResultId;}
    public String getResultUrl(){return resultUrl;} public boolean isVerified(){return verified;} public Instant getCreatedAt(){return createdAt;}
    public void setUserId(Long v){userId=v;} public void setRaceName(String v){raceName=v;} public void setRaceDate(LocalDate v){raceDate=v;}
    public void setDistance(String v){distance=v;} public void setFinishTimeSeconds(Integer v){finishTimeSeconds=v;}
    public void setPlacement(Integer v){placement=v;} public void setSource(String v){source=v;} public void setExternalResultId(String v){externalResultId=v;}
    public void setResultUrl(String v){resultUrl=v;} public void setVerified(boolean v){verified=v;}
}
