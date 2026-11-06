package com.runnit.api.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/** Keeps ACTIVE Search API requests within its documented 2 calls/second limit. */
@Service
public class ActiveRequestGate {
    private final Semaphore permits = new Semaphore(1, true);
    private long lastRequestAt;

    public <T> T execute(Callable<T> request) {
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            waitForRateLimit();
            return request.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ACTIVE request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("ACTIVE request failed", e);
        } finally {
            if (acquired) permits.release();
        }
    }

    private synchronized void waitForRateLimit() throws InterruptedException {
        long waitMs = 500L - (System.currentTimeMillis() - lastRequestAt);
        if (waitMs > 0) Thread.sleep(waitMs);
        lastRequestAt = System.currentTimeMillis();
    }
}
