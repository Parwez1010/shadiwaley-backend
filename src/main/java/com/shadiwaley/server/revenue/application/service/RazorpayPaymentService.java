package com.shadiwaley.server.revenue.application.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.domain.SubscriptionSource;
import com.shadiwaley.server.revenue.dto.request.AdminCreateRazorpayOrderRequest;
import com.shadiwaley.server.revenue.dto.request.CreateRazorpayOrderRequest;
import com.shadiwaley.server.revenue.dto.request.VerifyRazorpayPaymentRequest;
import com.shadiwaley.server.revenue.dto.response.RazorpayOrderResponse;
import com.shadiwaley.server.revenue.dto.response.RazorpayPaymentVerifyResponse;
import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import com.shadiwaley.server.revenue.infrastructure.entity.PaymentTransaction;
import com.shadiwaley.server.revenue.infrastructure.entity.RevenuePlan;
import com.shadiwaley.server.revenue.infrastructure.razorpay.RazorpayProperties;
import com.shadiwaley.server.revenue.infrastructure.repository.FamilySubscriptionRepository;
import com.shadiwaley.server.revenue.infrastructure.repository.PaymentTransactionRepository;
import com.shadiwaley.server.revenue.infrastructure.repository.RevenuePlanRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RevenuePlanRepository revenuePlanRepository;
    private final FamilySubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final RevenuePermissionService revenuePermissionService;

    @Transactional
    public RazorpayOrderResponse createOrder(CreateRazorpayOrderRequest request) throws Exception {

        UUID userId = AuthUser.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        RevenuePlan plan = revenuePlanRepository.findByCodeAndActiveTrue(request.getPlanCode())
                .orElseThrow(() -> new EntityNotFoundException("Active plan not found"));

        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Razorpay order can be created only for paid plans");
        }

        // Look up any existing active subscription for THIS user before creating the new one.
        subscriptionRepository
                .findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(user.getId())
                .ifPresent(existing -> {
                    existing.setCurrentSubscription(false);
                    existing.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                });

        FamilySubscription subscription = new FamilySubscription();
        subscription.setUserAccount(user);
        subscription.setUserProfile(profile);
        subscription.setPlan(plan);
        subscription.setPlanCode(plan.getCode());
        subscription.setPlanName(plan.getName());
        subscription.setAmount(plan.getPrice());
        subscription.setCurrency(plan.getCurrency());
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentStatus(RevenuePaymentStatus.PENDING);
        subscription.setCurrentSubscription(true);
        subscription.setSource(SubscriptionSource.FAMILY_ONBOARDING);
        subscription.setNote("Razorpay payment initiated");

        FamilySubscription savedSubscription = subscriptionRepository.save(subscription);


        PaymentTransaction payment = new PaymentTransaction();
        payment.setSubscription(savedSubscription);
        payment.setUserAccount(user);
        payment.setUserProfile(profile);
        payment.setPlanCode(plan.getCode());
        payment.setPlanName(plan.getName());
        payment.setAmount(plan.getPrice());
        payment.setCurrency(plan.getCurrency());
        payment.setPaymentMode(PaymentMode.RAZORPAY);
        payment.setPaymentStatus(RevenuePaymentStatus.PENDING);
        payment.setGatewayProvider("RAZORPAY");
        payment.setPaymentNote("Razorpay order created");

        PaymentTransaction savedPayment = paymentRepository.save(payment);

        int amountInPaise = plan.getPrice()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", plan.getCurrency());
        orderRequest.put("receipt", savedPayment.getId().toString());

        JSONObject notes = new JSONObject();
        notes.put("paymentId", savedPayment.getId().toString());
        notes.put("subscriptionId", savedSubscription.getId().toString());
        notes.put("userId", user.getId().toString());
        notes.put("planCode", plan.getCode());

        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);

        savedPayment.setGatewayOrderId(order.get("id"));
        paymentRepository.save(savedPayment);

        return RazorpayOrderResponse.builder()
                .subscriptionId(savedSubscription.getId())
                .paymentId(savedPayment.getId())
                .razorpayOrderId(order.get("id"))
                .razorpayKey(razorpayProperties.getKeyId())
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .planCode(plan.getCode())
                .planName(plan.getName())
                .build();
    }

    private RazorpayPaymentVerifyResponse toVerifyResponse(PaymentTransaction payment) {
        return RazorpayPaymentVerifyResponse.builder()
                .subscriptionId(payment.getSubscription().getId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus())
                .subscriptionStatus(payment.getSubscription().getSubscriptionStatus())
                .build();
    }

    private boolean verifySignature(
            String orderId,
            String paymentId,
            String actualSignature
    ) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpayProperties.getKeySecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            String expectedSignature = HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );

            return expectedSignature.equals(actualSignature);

        } catch (Exception ex) {
            return false;
        }
    }

    @Transactional
    public RazorpayOrderResponse createAdminOrder(AdminCreateRazorpayOrderRequest request) throws Exception {
        revenuePermissionService.assertCanManageFamily(request.getUserId());

        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        if (!profile.getUserAccount().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Profile does not belong to this family");
        }

        RevenuePlan plan = revenuePlanRepository.findByCodeAndActiveTrue(request.getPlanCode())
                .orElseThrow(() -> new EntityNotFoundException("Active plan not found"));

        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Razorpay order can be created only for paid plans");
        }

        subscriptionRepository
                .findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(user.getId())
                .ifPresent(existing -> {
                    existing.setCurrentSubscription(false);
                    subscriptionRepository.save(existing);
                });

        var employee = revenuePermissionService.getCurrentEmployeeOrNull();

        FamilySubscription subscription = new FamilySubscription();
        subscription.setUserAccount(user);
        subscription.setUserProfile(profile);
        subscription.setPlan(plan);
        subscription.setPlanCode(plan.getCode());
        subscription.setPlanName(plan.getName());
        subscription.setAmount(plan.getPrice());
        subscription.setCurrency(plan.getCurrency());
        subscription.setSubscriptionStatus(SubscriptionStatus.PAYMENT_PENDING);
        subscription.setPaymentStatus(RevenuePaymentStatus.PENDING);
        subscription.setCurrentSubscription(true);
        subscription.setSource(SubscriptionSource.PAYMENT_PANEL);
        subscription.setNote(request.getNote() != null ? request.getNote() : "Admin Razorpay order created");

        if (employee != null) {
            subscription.setAssignedByEmployee(employee);
            subscription.setAssignedByName(employee.getFullName());
        }

        FamilySubscription savedSubscription = subscriptionRepository.save(subscription);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setSubscription(savedSubscription);
        payment.setUserAccount(user);
        payment.setUserProfile(profile);
        payment.setPlanCode(plan.getCode());
        payment.setPlanName(plan.getName());
        payment.setAmount(plan.getPrice());
        payment.setCurrency(plan.getCurrency());
        payment.setPaymentMode(PaymentMode.RAZORPAY);
        payment.setPaymentStatus(RevenuePaymentStatus.PENDING);
        payment.setGatewayProvider("RAZORPAY");
        payment.setPaymentNote("Admin Razorpay order created");

        if (employee != null) {
            payment.setReceivedByEmployee(employee);
            payment.setReceivedByName(employee.getFullName());
        }

        PaymentTransaction savedPayment = paymentRepository.save(payment);

        int amountInPaise = plan.getPrice()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", plan.getCurrency());
        orderRequest.put("receipt", savedPayment.getId().toString());

        JSONObject notes = new JSONObject();
        notes.put("paymentId", savedPayment.getId().toString());
        notes.put("subscriptionId", savedSubscription.getId().toString());
        notes.put("userId", user.getId().toString());
        notes.put("createdBy", "ADMIN");
        notes.put("planCode", plan.getCode());

        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);

        savedPayment.setGatewayOrderId(order.get("id"));
        paymentRepository.save(savedPayment);

        return RazorpayOrderResponse.builder()
                .subscriptionId(savedSubscription.getId())
                .paymentId(savedPayment.getId())
                .razorpayOrderId(order.get("id"))
                .razorpayKey(razorpayProperties.getKeyId())
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .planCode(plan.getCode())
                .planName(plan.getName())
                .build();
    }

    @Transactional
    public RazorpayPaymentVerifyResponse verifyAdminPayment(VerifyRazorpayPaymentRequest request) {
        PaymentTransaction payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        revenuePermissionService.assertCanManageFamily(
                payment.getUserAccount().getId()
        );

        return verifyPaymentInternal(payment, request);
    }

    @Transactional
    public RazorpayPaymentVerifyResponse verifyPayment(VerifyRazorpayPaymentRequest request) {
        UUID userId = AuthUser.getCurrentUserId();

        PaymentTransaction payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        if (!payment.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot verify another user's payment");
        }

        return verifyPaymentInternal(payment, request);
    }

    private RazorpayPaymentVerifyResponse verifyPaymentInternal(
            PaymentTransaction payment,
            VerifyRazorpayPaymentRequest request
    ) {
        if (payment.getPaymentStatus() == RevenuePaymentStatus.PAID) {
            return toVerifyResponse(payment);
        }

        if (!request.getRazorpayOrderId().equals(payment.getGatewayOrderId())) {
            throw new IllegalArgumentException("Invalid Razorpay order id");
        }

        boolean validSignature = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!validSignature) {
            payment.setPaymentStatus(RevenuePaymentStatus.FAILED);
            payment.setPaymentNote("Razorpay signature verification failed");

            FamilySubscription subscription = payment.getSubscription();
            subscription.setPaymentStatus(RevenuePaymentStatus.FAILED);
            subscription.setSubscriptionStatus(SubscriptionStatus.PAYMENT_FAILED);

            subscriptionRepository.save(subscription);
            paymentRepository.save(payment);

            throw new IllegalArgumentException("Invalid Razorpay payment signature");
        }

        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        payment.setGatewaySignature(request.getRazorpaySignature());
        payment.setPaymentStatus(RevenuePaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment.setPaymentReference(request.getRazorpayPaymentId());
        payment.setPaymentNote("Razorpay payment verified successfully");

        FamilySubscription subscription = payment.getSubscription();
        subscription.setPaymentStatus(RevenuePaymentStatus.PAID);
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartAt(Instant.now());

        if (subscription.getPlan().getDurationDays() != null
                && subscription.getPlan().getDurationDays() > 0) {
            subscription.setEndAt(
                    Instant.now().plusSeconds(subscription.getPlan().getDurationDays() * 24L * 60L * 60L)
            );
        }

        subscriptionRepository.save(subscription);
        paymentRepository.save(payment);

        return toVerifyResponse(payment);
    }
}