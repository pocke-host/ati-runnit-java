package com.runnit.api.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/** Keeps aggregate RunSignup concurrency within the provider's recommended limit. */
@Service
public class RunSignupRequestGate {
    private final Semaphore permits = new Semaphore(2, true);

    public <T> T execute(Callable<T> request) {
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            return request.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RunSignup request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("RunSignup request failed", e);
        } finally {
            if (acquired) permits.release();
        }
    }
}
