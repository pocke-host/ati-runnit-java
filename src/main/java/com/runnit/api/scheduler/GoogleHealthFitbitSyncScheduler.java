package com.runnit.api.scheduler;

import com.runnit.api.service.GoogleHealthFitbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleHealthFitbitSyncScheduler {
    private final GoogleHealthFitbitService fitbit;

    @Scheduled(cron = "0 */30 * * * *")
    public void runBackstopSync() { fitbit.syncAllConnectedUsers(); }
}
