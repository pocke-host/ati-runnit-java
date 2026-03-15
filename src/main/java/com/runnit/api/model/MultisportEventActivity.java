package com.runnit.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "multisport_event_activities")
public class MultisportEventActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private MultisportEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "sequence_order")
    private Integer sequenceOrder = 0;

    @Column(name = "segment_label")
    private String segmentLabel; // e.g. "SWIM", "T1", "BIKE", "T2", "RUN"

    public MultisportEventActivity() {}

    public Long getId() { return id; }
    public MultisportEvent getEvent() { return event; }
    public Activity getActivity() { return activity; }
    public Integer getSequenceOrder() { return sequenceOrder; }
    public String getSegmentLabel() { return segmentLabel; }

    public void setEvent(MultisportEvent event) { this.event = event; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public void setSegmentLabel(String segmentLabel) { this.segmentLabel = segmentLabel; }
}
