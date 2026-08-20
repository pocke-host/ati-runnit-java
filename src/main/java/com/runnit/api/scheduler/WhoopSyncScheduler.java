package com.runnit.api.scheduler;

import com.runnit.api.service.WhoopService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhoopSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(WhoopSyncScheduler.class);
    private final WhoopService whoopService;

    // Every minute — backstop for missed webhook deliveries, and keeps the
    // refresh token alive so it never expires purely from disuse. Cheap to run
    // this often since WhoopService.syncAllConnectedUsers() skips anyone synced
    // within the last hour (see STALENESS_HOURS) — nearly every tick is a no-op
    // query that finds zero stale users. Running this frequently only matters
    // for shrinking detection latency during whatever windows the instance is
    // actually awake — it can't do anything while the process itself is asleep.
    @Scheduled(cron = "0 * * * * *")
    public void runBackstopSync() {
        log.info("Running WHOOP backstop sync for all connected users");
        whoopService.syncAllConnectedUsers();
    }
}
