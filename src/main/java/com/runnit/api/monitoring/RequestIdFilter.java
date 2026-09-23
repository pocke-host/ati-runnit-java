package com.runnit.api.monitoring;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter implements Filter {
    private final MonitoringService monitoring;
    public RequestIdFilter(MonitoringService monitoring) { this.monitoring = monitoring; }

    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request; HttpServletResponse out = (HttpServletResponse) response;
        String requestId = StringUtils.hasText(http.getHeader("X-Request-Id")) ? http.getHeader("X-Request-Id") : UUID.randomUUID().toString();
        long started = System.nanoTime(); MDC.put("requestId", requestId); out.setHeader("X-Request-Id", requestId);
        try { chain.doFilter(request, response); }
        finally {
            int status = out.getStatus(); String path = http.getRequestURI();
            if (status >= 500) { monitoring.increment("http_5xx"); classify(path); }
            if ("/api/auth/login".equals(path) && status >= 400) monitoring.increment("login_failures");
            if (path.contains("/oauth") && status >= 400) monitoring.increment("oauth_callback_failures");
            if (path.contains("/webhook") && status >= 400) monitoring.increment("webhook_failures");
            MDC.remove("requestId");
        }
    }
    private void classify(String path) { if (path.contains("whoop") || path.contains("coros")) monitoring.increment("sync_failures"); if (path.contains("billing") || path.contains("stripe")) monitoring.increment("payment_failures"); }
}
