package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.billingportal.Session;
import com.stripe.model.checkout.Session.LineItemCollectionParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /** POST /api/billing/checkout-session — creates a Stripe Checkout session */
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
            if (priceId == null || priceId.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "priceId is required"));

            // Retrieve or create Stripe customer for this user
            String customerId = getOrCreateCustomer(user);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(frontendUrl + "/subscribe/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/subscribe/cancel")
                    .setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .setTrialPeriodDays(14L)
                                    .build()
                    )
                    .build();

            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            return ResponseEntity.ok(Map.of("sessionId", session.getId()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/billing/portal — opens the Stripe Customer Portal */
    @PostMapping("/portal")
    public ResponseEntity<?> createPortalSession(Authentication auth) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String customerId = getOrCreateCustomer(user);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(frontendUrl + "/billing")
                    .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/billing/webhook — Stripe webhook (no auth filter) */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            switch (event.getType()) {
                case "customer.subscription.created":
                case "customer.subscription.updated": {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    String customerId = sub.getCustomer();
                    String status     = sub.getStatus(); // "active", "trialing", "canceled", etc.
                    updateUserSubscription(customerId, status);
                    break;
                }
                case "customer.subscription.deleted": {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    updateUserSubscription(sub.getCustomer(), "canceled");
                    break;
                }
                default:
                    break;
            }
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Find existing Stripe customer by email, or create one */
    private String getOrCreateCustomer(User user) throws Exception {
        // Check if we stored a customerId on the user (add stripeCustomerId field to User if desired)
        // For now, search by email
        CustomerSearchParams search = CustomerSearchParams.builder()
                .setQuery("email:'" + user.getEmail() + "'")
                .build();
        var results = Customer.search(search);
        if (!results.getData().isEmpty()) {
            return results.getData().get(0).getId();
        }

        CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getUsername())
                .build();
        return Customer.create(createParams).getId();
    }

    private void updateUserSubscription(String stripeCustomerId, String status) {
        // Optional: look up user by stripeCustomerId and update their subscription status
        // Requires a stripeCustomerId column on the users table — add as a follow-up migration
    }
}
