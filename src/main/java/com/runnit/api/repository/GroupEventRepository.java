package com.runnit.api.repository;

import com.runnit.api.model.GroupEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {
    List<GroupEvent> findByCreatorIdOrderByEventDatetimeAsc(Long creatorId);
}
