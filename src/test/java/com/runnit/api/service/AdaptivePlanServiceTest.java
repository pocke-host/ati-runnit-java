package com.runnit.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdaptivePlanService.decide() — the pure rule-decision logic,
 * no Spring context / no database, matching this repo's plain-JUnit-5 style
 * used by SanitizationUtilTest.
 */
class AdaptivePlanServiceTest {

    @Test
    void r1_hardRisk_downgradesTempoToEasyAndSoftens15Percent() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.6, 0.0, null, "HIGH_RISK", true, "TEMPO", false);

        assertNotNull(d);
        assertEquals("EASY", d.newWorkoutType());
        assertEquals(0.85, d.durationDistanceFactor(), 0.0001);
        assertEquals(1.15, d.paceFactor(), 0.0001);
        assertTrue(d.reason().contains("1.60"));
        assertTrue(d.reason().contains("high injury risk"));
    }

    @Test
    void r1_doesNotFireOnEasyWorkout() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.6, 0.0, null, "HIGH_RISK", true, "EASY", false);
        assertNull(d);
    }

    @Test
    void r2_deepFatigueAndLowRecovery_downgradesToRecovery() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, -25.0, 20, "OPTIMAL", true, "INTERVAL", false);

        assertNotNull(d);
        assertEquals("RECOVERY", d.newWorkoutType());
        assertEquals(0.85, d.durationDistanceFactor(), 0.0001);
        assertEquals(1.15, d.paceFactor(), 0.0001);
        assertTrue(d.reason().contains("recovery score is 20%"));
    }

    @Test
    void r2_doesNotFireWithoutWellnessData() {
        // recoveryScore == null (no wearable connected) must never be treated as "bad"
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, -25.0, null, "OPTIMAL", true, "INTERVAL", false);
        assertNull(d);
    }

    @Test
    void r3_moderateRisk_softensWithoutChangingType() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.4, 0.0, null, "OPTIMAL", true, "TEMPO", false);

        assertNotNull(d);
        assertNull(d.newWorkoutType());
        assertEquals(0.92, d.durationDistanceFactor(), 0.0001);
        assertEquals(1.08, d.paceFactor(), 0.0001);
    }

    @Test
    void r4_detraining_neverAdaptsRegardlessOfOtherSignals() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                0.6, 5.0, 80, "DETRAINING", true, "TEMPO", true);
        assertNull(d);
    }

    @Test
    void r5_progression_tightensTempoPaceConservatively() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, 5.0, 70, "OPTIMAL", true, "TEMPO", true);

        assertNotNull(d);
        assertNull(d.newWorkoutType());
        assertEquals(1.0, d.durationDistanceFactor(), 0.0001);
        assertEquals(0.95, d.paceFactor(), 0.0001);
    }

    @Test
    void r5_doesNotFireWithoutEnoughData() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, 5.0, 70, "OPTIMAL", false, "TEMPO", true);
        assertNull(d);
    }

    @Test
    void r5_doesNotFireOnNonHardWorkoutType() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, 5.0, 70, "OPTIMAL", true, "EASY", true);
        assertNull(d);
    }

    @Test
    void noRuleFires_returnsNull() {
        AdaptivePlanService.Decision d = AdaptivePlanService.decide(
                1.0, 0.0, 70, "OPTIMAL", true, "EASY", false);
        assertNull(d);
    }
}
