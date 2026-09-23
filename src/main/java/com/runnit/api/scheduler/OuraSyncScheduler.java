package com.runnit.api.scheduler;

import com.runnit.api.service.OuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OuraSyncScheduler {
    private final OuraService ouraService;

    /** Poll Oura periodically because the public API flow is pull-based. */
    @Scheduled(cron = "0 */30 * * * *")
    public void runBackstopSync() { ouraService.syncAllConnectedUsers(); }
}
