package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "training_folder_items")
public class TrainingFolderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "folder_id", nullable = false) private TrainingFolder folder;
    @Column(name = "item_type", nullable = false, length = 20) private String itemType;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    public TrainingFolderItem() {}
    public Long getId(){return id;} public TrainingFolder getFolder(){return folder;} public String getItemType(){return itemType;}
    public Long getItemId(){return itemId;} public Instant getCreatedAt(){return createdAt;}
    public void setFolder(TrainingFolder v){folder=v;} public void setItemType(String v){itemType=v;} public void setItemId(Long v){itemId=v;}
}
