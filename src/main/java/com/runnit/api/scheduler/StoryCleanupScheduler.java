// ========== StoryCleanupScheduler.java ==========
package com.runnit.api.scheduler;

import com.runnit.api.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoryCleanupScheduler {

    private final StoryService storyService;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredStories() {
        log.info("Running expired stories cleanup");
        storyService.cleanupExpiredStories();
    }
}