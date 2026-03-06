// ========== StoryCleanupScheduler.java ==========
package com.runnit.api.scheduler;

import com.runnit.api.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoryCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoryCleanupScheduler.class);
    private final StoryService storyService;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredStories() {
        log.info("Running expired stories cleanup");
        storyService.cleanupExpiredStories();
    }
}