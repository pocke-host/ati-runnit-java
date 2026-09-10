package com.runnit.api.repository;
import com.runnit.api.model.TrainingFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrainingFolderRepository extends JpaRepository<TrainingFolder, Long> {
    List<TrainingFolder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
