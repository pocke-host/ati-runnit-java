package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.billingportal.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final UserRepository userRepository;

    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${revenuecat.webhook.secret:}")
    private String revenuecatWebhookSecret;

    /** POST /api/billing/checkout-session */
    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String priceId = (String) body.get("priceId");
            if (priceId == null || priceId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "priceId is required"));
            }

            String customerId = getOrCreateCustomer(user);

            com.stripe.param.checkout.SessionCreateParams params =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                            .setCustomer(customerId)
                            .addLineItem(
                                    com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                            .setPrice(priceId)
                                            .setQuantity(1L)
                                            .build()
                            )
                            .setSuccessUrl(frontendUrl + "/subscribe/success?session_id={CHECKOUT_SESSION_ID}")
                            .setCancelUrl(frontendUrl + "/subscribe/cancel")
                            .setSubscriptionData(
                                    com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                                            .setTrialPeriodDays(14L)
                                            .build()
                            )
                            .build();

            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.create(params);
            log.info("Checkout session created: userId={} sessionId={}", userId, session.getId());
            return ResponseEntity.ok(Map.of("sessionId", session.getId()));

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error in createCheckoutSession: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in createCheckoutSession", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /** POST /api/billing/portal */
    @PostMapping("/portal")
    public ResponseEntity<?> createPortalSession(Authentication auth) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String customerId = getOrCreateCustomer(user);

            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(customerId)
                            .setReturnUrl(frontendUrl + "/billing")
                            .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error in createPortalSession: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in createPortalSession", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /** POST /api/billing/webhook — no auth, verified by Stripe signature */
    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Stripe webhook received: type={}", event.getType());

            switch (event.getType()) {
                case "customer.subscription.created":
                case "customer.subscription.updated": {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    updateUserSubscription(sub.getCustomer(), sub.getStatus());
                    break;
                }
                case "customer.subscription.deleted": {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    updateUserSubscription(sub.getCustomer(), "canceled");
                    break;
                }
                default:
                    log.debug("Unhandled Stripe event type: {}", event.getType());
                    break;
            }
            return ResponseEntity.ok(Map.of("received", true));
        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook signature"));
        } catch (Exception e) {
            log.error("Unexpected error in handleWebhook", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /** POST /api/billing/revenuecat-webhook — no auth, verified by shared secret header */
    @PostMapping("/revenuecat-webhook")
    @Transactional
    public ResponseEntity<?> handleRevenueCatWebhook(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (revenuecatWebhookSecret != null && !revenuecatWebhookSecret.isBlank()) {
                String expected = "Bearer " + revenuecatWebhookSecret;
                if (!expected.equals(authHeader)) {
                    log.warn("RevenueCat webhook: invalid authorization header");
                    return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) body.get("event");
            if (event == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing event"));
            }

            String type = (String) event.get("type");
            String appUserId = (String) event.get("app_user_id");
            String productId = (String) event.get("product_id");
            log.info("RevenueCat webhook: type={} appUserId={} productId={}", type, appUserId, productId);

            if (appUserId == null) {
                return ResponseEntity.ok(Map.of("received", true));
            }

            Long userId;
            try {
                userId = Long.parseLong(appUserId);
            } catch (NumberFormatException e) {
                log.warn("RevenueCat webhook: unparseable appUserId={}", appUserId);
                return ResponseEntity.ok(Map.of("received", true));
            }

            String status = mapRevenueCatEventToStatus(type);
            String tier = extractTierFromProductId(productId);

            userRepository.findById(userId).ifPresent(user -> {
                if (status != null) user.setSubscriptionStatus(status);
                if (tier != null) user.setSubscriptionTier(tier);
                userRepository.save(user);
                log.info("RevenueCat: updated userId={} status={} tier={}", userId, status, tier);
            });

            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("Unexpected error in handleRevenueCatWebhook", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /** POST /api/billing/revenuecat-sync — called by iOS after purchase to pull latest status */
    @PostMapping("/revenuecat-sync")
    @Transactional
    public ResponseEntity<?> syncRevenueCat(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            log.info("RevenueCat sync requested: userId={} currentStatus={}", userId, user.getSubscriptionStatus());
            return ResponseEntity.ok(Map.of(
                    "subscriptionStatus", user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : "none",
                    "subscriptionTier", user.getSubscriptionTier() != null ? user.getSubscriptionTier() : "free"
            ));
        } catch (Exception e) {
            log.error("Unexpected error in syncRevenueCat", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Finds or creates a Stripe Customer for the given user.
     * Stores the Stripe customerId on the User to avoid repeated Stripe searches.
     */
    private String getOrCreateCustomer(User user) throws Exception {
        // Use stored customerId if we already have one
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }

        // Search Stripe by email for an existing customer
        CustomerSearchParams search = CustomerSearchParams.builder()
                .setQuery("email:'" + user.getEmail() + "'")
                .build();
        var results = Customer.search(search);

        String customerId;
        if (!results.getData().isEmpty()) {
            customerId = results.getData().get(0).getId();
            log.debug("Found existing Stripe customer: userId={} customerId={}", user.getId(), customerId);
        } else {
            CustomerCreateParams createParams = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getDisplayName())
                    .build();
            customerId = Customer.create(createParams).getId();
            log.info("Created Stripe customer: userId={} customerId={}", user.getId(), customerId);
        }

        // Persist the Stripe customerId so future calls skip the search
        user.setStripeCustomerId(customerId);
        userRepository.save(user);

        return customerId;
    }

    private String mapRevenueCatEventToStatus(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "INITIAL_PURCHASE", "RENEWAL", "UNCANCELLATION" -> "active";
            case "TRIAL_STARTED" -> "trialing";
            case "CANCELLATION", "EXPIRATION" -> "canceled";
            case "BILLING_ISSUE" -> "past_due";
            default -> null;
        };
    }

    private String extractTierFromProductId(String productId) {
        if (productId == null) return null;
        if (productId.contains("duo")) return "duo";
        if (productId.contains("premium")) return "premium";
        return null;
    }

    /**
     * Updates the user's subscription_status based on a Stripe webhook event.
     * Looks up the user by their stored stripeCustomerId.
     */
    private void updateUserSubscription(String stripeCustomerId, String status) {
        userRepository.findByStripeCustomerId(stripeCustomerId).ifPresentOrElse(user -> {
            user.setSubscriptionStatus(status);
            userRepository.save(user);
            log.info("Updated subscription: userId={} stripeCustomerId={} status={}",
                    user.getId(), stripeCustomerId, status);
        }, () -> log.warn("Stripe webhook: no user found for customerId={}", stripeCustomerId));
    }
}
