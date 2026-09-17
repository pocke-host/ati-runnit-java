# Production monitoring checklist

The API exposes Spring Boot Actuator health, info, and metrics endpoints. Configure the hosting provider or an uptime service to poll `/actuator/health` and alert on non-2xx responses.

Recommended alerts:

- API 5xx rate: alert when 5xx responses exceed 1% for 5 minutes.
- Database: alert when `/actuator/health` is down or the DB pool reports connection timeouts.
- Stripe: alert on `PAYMENT_FAILED`, `DISPUTED`, or `REFUNDED` booking status changes and failed webhook delivery attempts.
- Webhooks: alert on repeated Stripe signature failures or 4xx/5xx responses at `/api/billing/webhook`.
- OAuth: alert on repeated callback failures and users entering a reconnect-required state.
- Deployments: alert when health checks fail after a release or Flyway migration fails.

Keep application logs at `INFO` in production via `RUNNIT_LOG_LEVEL=INFO`. Send structured logs to the hosting provider's log drain or an error-monitoring service, and never include access tokens, client secrets, or webhook payload secrets in alerts.
