package com.runnit.api.repository;
import com.runnit.api.model.TrainingFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrainingFolderItemRepository extends JpaRepository<TrainingFolderItem, Long> {
    List<TrainingFolderItem> findByFolderIdOrderByCreatedAtDesc(Long folderId);
    boolean existsByFolderIdAndItemTypeAndItemId(Long folderId, String itemType, Long itemId);
    void deleteByFolderIdAndItemTypeAndItemId(Long folderId, String itemType, Long itemId);
}
