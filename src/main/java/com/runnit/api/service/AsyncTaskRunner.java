package com.runnit.api.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs a task off the calling thread. Exists as its own bean because @Async only takes
 * effect on a call that goes through the Spring proxy — a class calling its own @Async
 * method directly (self-invocation) bypasses the proxy and just runs synchronously with
 * no error or warning. Each OAuth integration's handleCallback() injects this and routes
 * its post-connect historical backfill through it instead of calling an @Async method on
 * itself, so the redirect back to the frontend isn't blocked on that backfill.
 */
@Service
public class AsyncTaskRunner {

    @Async
    public void run(Runnable task) {
        task.run();
    }
}
