package com.runnit.api.controller;

import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.checkout.Session;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;

/** Marketplace catalog and booking boundary. Athletes never pay for Runnit access. */
@RestController @RequiredArgsConstructor
public class CoachMarketplaceController {
    private static final int COMMISSION_PERCENT = 15;
    private final CoachServiceRepository services;
    private final CoachAvailabilityRepository availability;
    private final CoachBookingRepository bookings;
    private final CoachReviewRepository reviews;
    private final CoachReportRepository reports;
    private final MarketplaceEventRepository events;
    private final UserRepository users;
    @Value("${stripe.secret.key:}") private String stripeSecretKey;
    @Value("${app.frontend.url:http://localhost:5173}") private String frontendUrl;

    @GetMapping("/api/coaches/{coachId}/services")
    public List<Map<String,Object>> listServices(@PathVariable Long coachId) { events.save(new MarketplaceEvent("SERVICE_VIEW",coachId,null,null,null)); return services.findByCoachIdAndActiveTrue(coachId).stream().map(this::serviceMap).toList(); }
    @GetMapping("/api/coach/services")
    public List<Map<String,Object>> myServices(Authentication auth) { return services.findByCoachId((Long) auth.getPrincipal()).stream().map(this::serviceMap).toList(); }

    @PostMapping("/api/coach/services") @Transactional
    public ResponseEntity<?> createService(@RequestBody Map<String,Object> body, Authentication auth) {
        Long coachId=(Long)auth.getPrincipal();
        CoachService s=new CoachService(); s.setCoachId(coachId); s.setTitle(required(body,"title",160)); s.setDescription((String)body.get("description")); s.setServiceType(value(body,"serviceType","COACHING")); s.setBillingType(value(body,"billingType","ONE_TIME")); s.setPriceCents(intValue(body,"priceCents")); s.setDurationMinutes(body.get("durationMinutes") == null ? null : intValue(body,"durationMinutes"));
        if(s.getPriceCents() < 0) return ResponseEntity.badRequest().body(Map.of("error","Price cannot be negative"));
        return ResponseEntity.ok(serviceMap(services.save(s)));
    }

    @PostMapping("/api/coach/connect/onboard") @Transactional
    public ResponseEntity<?> connectOnboard(Authentication auth) {
        try {
            if (stripeSecretKey.isBlank()) return ResponseEntity.status(503).body(Map.of("error", "Coach payments are not configured yet"));
            Stripe.apiKey = stripeSecretKey;
            User coach = users.findById((Long) auth.getPrincipal()).orElseThrow();
            if (!"coach".equalsIgnoreCase(coach.getRole())) return ResponseEntity.status(403).body(Map.of("error", "Only coaches can onboard for payouts"));
            if (coach.getStripeConnectAccountId() == null) {
                AccountCreateParams params = AccountCreateParams.builder().setType(AccountCreateParams.Type.EXPRESS).setCountry("US").setEmail(coach.getEmail()).build();
                coach.setStripeConnectAccountId(Account.create(params).getId());
                users.save(coach);
            }
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder().setAccount(coach.getStripeConnectAccountId()).setRefreshUrl(frontendUrl + "/coach/dashboard?connect=retry").setReturnUrl(frontendUrl + "/coach/dashboard?connect=complete").setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING).build();
            return ResponseEntity.ok(Map.of("url", AccountLink.create(linkParams).getUrl()));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", "Could not start payout onboarding")); }
    }

    @GetMapping("/api/coach/connect/status")
    public ResponseEntity<?> connectStatus(Authentication auth) {
        User coach=users.findById((Long)auth.getPrincipal()).orElseThrow();
        boolean connected=coach.getStripeConnectAccountId()!=null; boolean charges=false,payouts=false,details=false;
        if(connected && !stripeSecretKey.isBlank()) try { Stripe.apiKey=stripeSecretKey; Account a=Account.retrieve(coach.getStripeConnectAccountId()); charges=Boolean.TRUE.equals(a.getChargesEnabled()); payouts=Boolean.TRUE.equals(a.getPayoutsEnabled()); details=Boolean.TRUE.equals(a.getDetailsSubmitted()); } catch(Exception ignored) {}
        return ResponseEntity.ok(Map.of("connected",connected,"accountId",Optional.ofNullable(coach.getStripeConnectAccountId()).orElse(""),"verified",Boolean.TRUE.equals(coach.getCoachVerified()),"chargesEnabled",charges,"payoutsEnabled",payouts,"detailsSubmitted",details));
    }

    @PatchMapping("/api/coach/profile") @Transactional
    public ResponseEntity<?> updateCoachProfile(@RequestBody Map<String,Object> body, Authentication auth) {
        User coach=users.findById((Long)auth.getPrincipal()).orElseThrow(); if(!"coach".equalsIgnoreCase(coach.getRole()))return ResponseEntity.status(403).body(Map.of("error","Coach account required"));
        if(body.containsKey("monthlyRate")) coach.setMonthlyRate(new java.math.BigDecimal(String.valueOf(body.get("monthlyRate"))));
        if(body.containsKey("sportsCoached")) coach.setSportsCoached(String.join(",",(List<String>)body.get("sportsCoached")));
        if(body.containsKey("experience")) coach.setBio((String)body.get("experience"));
        if(body.containsKey("specialties")) coach.setCoachSpecialties(String.valueOf(body.get("specialties")));
        if(body.containsKey("certifications")) coach.setCoachCertifications(String.valueOf(body.get("certifications")));
        if(body.containsKey("privacy")) { String p=String.valueOf(body.get("privacy")); if(!Set.of("PUBLIC","PRIVATE").contains(p)) return ResponseEntity.badRequest().body(Map.of("error","Privacy must be PUBLIC or PRIVATE")); coach.setCoachPrivacy(p); }
        if(Boolean.TRUE.equals(body.get("acceptTerms"))) coach.setCoachTermsAcceptedAt(Instant.now());
        coach.setCoachOnboardingComplete(coach.getCoachTermsAcceptedAt()!=null && coach.getCoachSpecialties()!=null && !coach.getCoachSpecialties().isBlank());
        users.save(coach);
        return ResponseEntity.ok(Map.of("monthlyRate",Optional.ofNullable(coach.getMonthlyRate()).orElse(java.math.BigDecimal.ZERO),"sportsCoached",coach.getSportsCoached()==null?List.of():Arrays.asList(coach.getSportsCoached().split(",")),"privacy",coach.getCoachPrivacy(),"onboardingComplete",coach.getCoachOnboardingComplete()));
    }

    @PutMapping("/api/coach/services/{id}") @Transactional
    public ResponseEntity<?> updateService(@PathVariable Long id,@RequestBody Map<String,Object> body,Authentication auth){
        CoachService s=services.findById(id).orElse(null); if(s==null||!s.getCoachId().equals((Long)auth.getPrincipal())) return ResponseEntity.status(404).body(Map.of("error","Service not found"));
        if(body.containsKey("title"))s.setTitle(required(body,"title",160)); if(body.containsKey("description"))s.setDescription((String)body.get("description")); if(body.containsKey("priceCents"))s.setPriceCents(intValue(body,"priceCents")); if(body.containsKey("active"))s.setActive((Boolean)body.get("active")); return ResponseEntity.ok(serviceMap(services.save(s)));
    }

    @GetMapping("/api/coaches/{coachId}/availability") public List<CoachAvailability> listAvailability(@PathVariable Long coachId){return availability.findByCoachIdOrderByWeekdayAscStartTimeAsc(coachId);}
    @PutMapping("/api/coach/availability") @Transactional public List<CoachAvailability> saveAvailability(@RequestBody List<CoachAvailability> rows,Authentication auth){
        Long id=(Long)auth.getPrincipal(); availability.deleteAll(availability.findByCoachIdOrderByWeekdayAscStartTimeAsc(id)); rows.forEach(r->{r.setCoachId(id);r.setId(null);}); return availability.saveAll(rows);
    }

    @PostMapping("/api/coach/bookings") @Transactional
    public ResponseEntity<?> createBooking(@RequestBody Map<String,Object> body,Authentication auth){
        Long athleteId=(Long)auth.getPrincipal(); Long serviceId=longValue(body,"serviceId"); CoachService s=services.findById(serviceId).orElse(null); if(s==null||!Boolean.TRUE.equals(s.getActive()))return ResponseEntity.status(404).body(Map.of("error","Service not found"));
        if(s.getCoachId().equals(athleteId))return ResponseEntity.badRequest().body(Map.of("error","You cannot book your own service"));
        CoachBooking b=new CoachBooking(); b.setServiceId(s.getId()); b.setCoachId(s.getCoachId()); b.setAthleteId(athleteId); b.setAmountCents(s.getPriceCents()); b.setCommissionCents(Math.round(s.getPriceCents()*COMMISSION_PERCENT/100f)); b.setCoachAmountCents(s.getPriceCents()-b.getCommissionCents()); b.setStatus("PENDING_PAYMENT");
        if(body.get("scheduledStart")!=null)b.setScheduledStart(Instant.parse((String)body.get("scheduledStart"))); if(body.get("scheduledEnd")!=null)b.setScheduledEnd(Instant.parse((String)body.get("scheduledEnd")));
        CoachBooking saved=bookings.save(b); events.save(new MarketplaceEvent("BOOKING_CREATED",s.getCoachId(),athleteId,s.getId(),saved.getId())); return ResponseEntity.ok(bookingMap(saved));
    }

    @PostMapping("/api/athlete/bookings/{id}/checkout") @Transactional
    public ResponseEntity<?> checkout(@PathVariable Long id, Authentication auth) {
        try {
            if (stripeSecretKey.isBlank()) return ResponseEntity.status(503).body(Map.of("error", "Coach payments are not configured yet"));
            CoachBooking b=bookings.findById(id).orElse(null); if(b==null||!b.getAthleteId().equals((Long)auth.getPrincipal())) return ResponseEntity.status(404).body(Map.of("error","Booking not found"));
            User coach=users.findById(b.getCoachId()).orElseThrow(); if(coach.getStripeConnectAccountId()==null)return ResponseEntity.badRequest().body(Map.of("error","Coach has not completed payout setup"));
            Stripe.apiKey=stripeSecretKey;
            CoachService service=services.findById(b.getServiceId()).orElseThrow();
            SessionCreateParams.LineItem.PriceData.Builder price=SessionCreateParams.LineItem.PriceData.builder().setCurrency("usd").setUnitAmount(b.getAmountCents().longValue()).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(service.getTitle()).build());
            SessionCreateParams.Builder session=SessionCreateParams.builder().setSuccessUrl(frontendUrl+"/coaching/booking-success?booking_id="+id).setCancelUrl(frontendUrl+"/coaching/booking-cancelled?booking_id="+id).putMetadata("booking_id",String.valueOf(id)).addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData("MONTHLY".equals(service.getBillingType()) ? price.setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder().setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH).build()).build() : price.build()).build());
            if("MONTHLY".equals(service.getBillingType())) session.setMode(SessionCreateParams.Mode.SUBSCRIPTION).setSubscriptionData(SessionCreateParams.SubscriptionData.builder().setApplicationFeePercent(java.math.BigDecimal.valueOf(COMMISSION_PERCENT)).setTransferData(SessionCreateParams.SubscriptionData.TransferData.builder().setDestination(coach.getStripeConnectAccountId()).build()).build());
            else session.setMode(SessionCreateParams.Mode.PAYMENT).setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder().setApplicationFeeAmount(b.getCommissionCents().longValue()).setTransferData(SessionCreateParams.PaymentIntentData.TransferData.builder().setDestination(coach.getStripeConnectAccountId()).build()).putMetadata("booking_id",String.valueOf(id)).build());
            SessionCreateParams params=session.build();
            Session checkoutSession=Session.create(params); b.setStripeCheckoutSessionId(checkoutSession.getId()); bookings.save(b); events.save(new MarketplaceEvent("CHECKOUT_STARTED",b.getCoachId(),b.getAthleteId(),b.getServiceId(),b.getId())); return ResponseEntity.ok(Map.of("url",checkoutSession.getUrl()));
        } catch(Exception e){ return ResponseEntity.internalServerError().body(Map.of("error","Could not start checkout")); }
    }

    @PostMapping("/api/coach/bookings/{id}/refund") @Transactional
    public ResponseEntity<?> refund(@PathVariable Long id, Authentication auth) {
        try {
            if (stripeSecretKey.isBlank()) return ResponseEntity.status(503).body(Map.of("error", "Coach payments are not configured yet"));
            CoachBooking b=bookings.findById(id).orElse(null); if(b==null||!b.getCoachId().equals((Long)auth.getPrincipal()))return ResponseEntity.status(404).body(Map.of("error","Booking not found"));
            if(!"PAID".equals(b.getStatus())||b.getStripePaymentIntentId()==null)return ResponseEntity.badRequest().body(Map.of("error","Only paid bookings can be refunded"));
            Stripe.apiKey=stripeSecretKey; Refund.create(RefundCreateParams.builder().setPaymentIntent(b.getStripePaymentIntentId()).build()); b.setStatus("REFUNDED"); bookings.save(b); return ResponseEntity.ok(bookingMap(b));
        } catch(Exception e){ return ResponseEntity.internalServerError().body(Map.of("error","Refund could not be completed")); }
    }
    @GetMapping("/api/coach/bookings") public List<Map<String,Object>> coachBookings(Authentication a){return bookings.findByCoachIdOrderByCreatedAtDesc((Long)a.getPrincipal()).stream().map(this::bookingMap).toList();}
    @GetMapping("/api/coach/earnings") public Map<String,Object> earnings(Authentication a){
        List<CoachBooking> rows=bookings.findByCoachIdOrderByCreatedAtDesc((Long)a.getPrincipal());
        int gross=rows.stream().filter(b->Set.of("PAID","COMPLETED").contains(b.getStatus())).mapToInt(CoachBooking::getAmountCents).sum();
        int commission=rows.stream().filter(b->Set.of("PAID","COMPLETED").contains(b.getStatus())).mapToInt(CoachBooking::getCommissionCents).sum();
        return Map.of("grossCents",gross,"commissionCents",commission,"netCents",gross-commission,"paidBookings",rows.stream().filter(b->Set.of("PAID","COMPLETED").contains(b.getStatus())).count());
    }
    @GetMapping("/api/athlete/bookings") public List<Map<String,Object>> athleteBookings(Authentication a){return bookings.findByAthleteIdOrderByCreatedAtDesc((Long)a.getPrincipal()).stream().map(this::bookingMap).toList();}
    @PostMapping("/api/coach/bookings/{id}/cancel") @Transactional public ResponseEntity<?> cancel(@PathVariable Long id,@RequestBody(required=false) Map<String,Object> body,Authentication a){
        CoachBooking b=bookings.findById(id).orElse(null);Long uid=(Long)a.getPrincipal();if(b==null||(!b.getCoachId().equals(uid)&&!b.getAthleteId().equals(uid)))return ResponseEntity.status(404).body(Map.of("error","Booking not found")); if(Set.of("COMPLETED","REFUNDED").contains(b.getStatus()))return ResponseEntity.badRequest().body(Map.of("error","Booking cannot be cancelled"));b.setStatus("CANCELLATION_REQUESTED");b.setCancellationReason(body==null?null:(String)body.get("reason"));return ResponseEntity.ok(bookingMap(bookings.save(b)));
    }
    @PostMapping("/api/athlete/bookings/{id}/review") @Transactional public ResponseEntity<?> review(@PathVariable Long id,@RequestBody Map<String,Object> body,Authentication a){
        CoachBooking b=bookings.findById(id).orElse(null);Long uid=(Long)a.getPrincipal();if(b==null||!b.getAthleteId().equals(uid)||!"COMPLETED".equals(b.getStatus()))return ResponseEntity.badRequest().body(Map.of("error","Only completed bookings can be reviewed"));if(reviews.findByBookingId(id).isPresent())return ResponseEntity.status(409).body(Map.of("error","Booking already reviewed"));int rating=intValue(body,"rating");if(rating<1||rating>5)return ResponseEntity.badRequest().body(Map.of("error","Rating must be 1 to 5"));CoachReview r=new CoachReview();r.setBookingId(id);r.setCoachId(b.getCoachId());r.setAthleteId(uid);r.setRating(rating);r.setReviewText((String)body.get("reviewText"));return ResponseEntity.ok(reviewMap(reviews.save(r)));
    }
    @GetMapping("/api/coaches/{coachId}/reviews") public List<Map<String,Object>> coachReviews(@PathVariable Long coachId){return reviews.findByCoachIdOrderByCreatedAtDesc(coachId).stream().map(this::reviewMap).toList();}
    @PostMapping("/api/marketplace/reports") @Transactional public ResponseEntity<?> report(@RequestBody Map<String,Object> body, Authentication a){
        Long reporter=(Long)a.getPrincipal(); Long coachId=longValue(body,"coachId"); String reason=required(body,"reason",60); CoachReport r=new CoachReport(); r.setReporterId(reporter); r.setCoachId(coachId); r.setReason(reason); r.setDetails(body.get("details")==null?null:String.valueOf(body.get("details"))); if(body.get("bookingId")!=null)r.setBookingId(longValue(body,"bookingId")); return ResponseEntity.ok(reportMap(reports.save(r)));
    }
    @GetMapping("/api/coach/reports") public List<Map<String,Object>> myReports(Authentication a){return reports.findByCoachIdOrderByCreatedAtDesc((Long)a.getPrincipal()).stream().map(this::reportMap).toList();}
    @GetMapping("/api/coach/marketplace/analytics") public Map<String,Object> analytics(Authentication a){
        List<MarketplaceEvent> rows=events.findByCoachId((Long)a.getPrincipal()); Map<String,Long> counts=rows.stream().collect(Collectors.groupingBy(MarketplaceEvent::getEventType,Collectors.counting())); return Map.of("events",counts,"serviceViews",counts.getOrDefault("SERVICE_VIEW",0L),"bookings",counts.getOrDefault("BOOKING_CREATED",0L),"checkouts",counts.getOrDefault("CHECKOUT_STARTED",0L));
    }

    private Map<String,Object> serviceMap(CoachService s){Map<String,Object>m=new LinkedHashMap<>();m.put("id",s.getId());m.put("coachId",s.getCoachId());m.put("title",s.getTitle());m.put("description",s.getDescription());m.put("serviceType",s.getServiceType());m.put("billingType",s.getBillingType());m.put("priceCents",s.getPriceCents());m.put("durationMinutes",s.getDurationMinutes());m.put("active",s.getActive());return m;}
    private Map<String,Object> bookingMap(CoachBooking b){Map<String,Object>m=new LinkedHashMap<>();m.put("id",b.getId());m.put("serviceId",b.getServiceId());m.put("coachId",b.getCoachId());m.put("athleteId",b.getAthleteId());m.put("scheduledStart",b.getScheduledStart());m.put("scheduledEnd",b.getScheduledEnd());m.put("status",b.getStatus());m.put("amountCents",b.getAmountCents());m.put("commissionCents",b.getCommissionCents());m.put("coachAmountCents",b.getCoachAmountCents());return m;}
    private Map<String,Object> reviewMap(CoachReview r){Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getId());m.put("bookingId",r.getBookingId());m.put("coachId",r.getCoachId());m.put("athleteId",r.getAthleteId());m.put("rating",r.getRating());m.put("reviewText",r.getReviewText());m.put("createdAt",r.getCreatedAt());return m;}
    private Map<String,Object> reportMap(CoachReport r){Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getId());m.put("coachId",r.getCoachId());m.put("bookingId",r.getBookingId());m.put("reason",r.getReason());m.put("details",r.getDetails());m.put("status",r.getStatus());m.put("createdAt",r.getCreatedAt());return m;}
    private String required(Map<String,Object>b,String k,int max){String v=String.valueOf(b.getOrDefault(k,"")).trim();if(v.isEmpty()||v.length()>max)throw new IllegalArgumentException(k+" is required and must be <= "+max+" characters");return v;}
    private String value(Map<String,Object>b,String k,String d){return b.get(k)==null?d:String.valueOf(b.get(k));} private int intValue(Map<String,Object>b,String k){return ((Number)b.getOrDefault(k,0)).intValue();} private Long longValue(Map<String,Object>b,String k){return ((Number)b.get(k)).longValue();}
}
