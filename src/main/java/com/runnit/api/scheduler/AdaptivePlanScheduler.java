package com.runnit.api.scheduler;

import com.runnit.api.service.AdaptivePlanService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdaptivePlanScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdaptivePlanScheduler.class);
    private final AdaptivePlanService adaptivePlanService;

    // Run nightly at 3am — after most wearables finish their overnight sync
    // window, before athletes wake up and check the app. Catches missed
    // workouts and ACWR/TSB drift that no new activity would trigger.
    @Scheduled(cron = "0 0 3 * * *")
    public void runNightlySweep() {
        log.info("Running adaptive plan nightly sweep");
        adaptivePlanService.runNightlySweepForAllActivePlans();
    }
}
