package su26.uml.be.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.plan.entity.PlanFeature;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.usage.entity.UserQuota;
import su26.uml.be.features.usage.repository.UserQuotaRepository;
import su26.uml.be.features.usage.service.Impl.QuotaServiceImpl;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.common.constant.enums.SubscriptionStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

    @Mock
    UserQuotaRepository userQuotaRepository;
    @Mock
    SubscriptionRepository subscriptionRepository;
    @Mock
    PlanRepository planRepository;
    @Mock
    UserRepository userRepository;

    QuotaServiceImpl service;

    UUID userId = UUID.randomUUID();
    UUID subId = UUID.randomUUID();
    UUID newSubId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    UUID freePlanId = UUID.randomUUID();

    Plan proPlan;
    Plan freePlan;
    Subscription activeSub;
    Subscription newSub;

    LocalDateTime future = LocalDateTime.of(2026, 8, 16, 15, 0);
    LocalDateTime past = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        service = new QuotaServiceImpl(userQuotaRepository, subscriptionRepository, planRepository, userRepository);
        // @Value không được inject trong unit test thuần → set thủ công.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "periodDays", 30);

        proPlan = Plan.builder()
                .id(planId)
                .name("PRO")
                .price(java.math.BigDecimal.valueOf(9.99))
                .status(PlanStatus.ACTIVE)
                .planFeatures(List.of(
                        PlanFeature.builder()
                                .featureKey(PlanFeatureKey.AI_QUERIES)
                                .limitValue(1000)
                                .build()
                ))
                .build();

        freePlan = Plan.builder()
                .id(freePlanId)
                .name("FREE")
                .price(java.math.BigDecimal.ZERO)
                .status(PlanStatus.ACTIVE)
                .planFeatures(List.of(
                        PlanFeature.builder()
                                .featureKey(PlanFeatureKey.AI_QUERIES)
                                .limitValue(10)
                                .build()
                ))
                .build();

        activeSub = Subscription.builder()
                .id(subId)
                .plan(proPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now().minusDays(10))
                .endDate(future)
                .build();

        newSub = Subscription.builder()
                .id(newSubId)
                .plan(proPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now())
                .endDate(now().plusDays(30))
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    /** resetAt của gói Free phải nằm trong [before+30d, after+30d] (reset lăn 30 ngày). */
    private void assertResetAtRoughly30DaysFromNow(LocalDateTime resetAt, LocalDateTime before, LocalDateTime after) {
        assertNotNull(resetAt);
        assertFalse(resetAt.isBefore(before.plusDays(30)), "resetAt phải >= before+30d");
        assertFalse(resetAt.isAfter(after.plusDays(30)), "resetAt phải <= after+30d");
    }

    // ─── syncQuotaToCurrentPlan: subId unchanged, resetAt future → no reset ───

    @Test
    void syncQuota_sameSubId_noReset() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(50)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(future)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSub));
        when(userQuotaRepository.tryReserveAi(userId)).thenReturn(1);

        service.reserveAiRequest(userId);

        verify(userQuotaRepository, never()).save(any());
    }

    // ─── syncQuotaToCurrentPlan: subId changed → reset ───

    @Test
    void syncQuota_subChanged_resetsQuota() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(50)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(future)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(newSub));
        when(userQuotaRepository.tryReserveAi(userId)).thenReturn(1);

        service.reserveAiRequest(userId);

        assertEquals(0, quota.getAiUsed());
        assertEquals(1000, quota.getAiLimit());
        assertEquals(newSubId, quota.getSubscriptionId());
        assertEquals(newSub.getEndDate(), quota.getResetAt());
        verify(userQuotaRepository).save(quota);
    }

    // ─── syncQuotaToCurrentPlan: sub expired → fallback + reset ───

    @Test
    void syncQuota_subExpired_fallsBackToCheapest() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(50)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(past)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE))
                .thenReturn(Optional.of(freePlan));
        when(userQuotaRepository.tryReserveAi(userId)).thenReturn(1);

        LocalDateTime before = now();
        service.reserveAiRequest(userId);
        LocalDateTime after = now();

        assertEquals(0, quota.getAiUsed());
        assertEquals(10, quota.getAiLimit());
        assertNull(quota.getSubscriptionId());
        // Gói Free (không có sub) → reset lăn 30 ngày kể từ bây giờ.
        assertResetAtRoughly30DaysFromNow(quota.getResetAt(), before, after);
        verify(userQuotaRepository).save(quota);
    }

    // ─── syncQuotaToCurrentPlan: resetAt expired, subId unchanged → reset used ───

    @Test
    void syncQuota_periodExpired_resetsUsed() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(50)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(past)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSub));
        when(userQuotaRepository.tryReserveAi(userId)).thenReturn(1);

        service.reserveAiRequest(userId);

        assertEquals(0, quota.getAiUsed());
        assertEquals(1000, quota.getAiLimit());
        assertEquals(subId, quota.getSubscriptionId());
        assertEquals(future, quota.getResetAt());
        verify(userQuotaRepository).save(quota);
    }

    // ─── getOrCreate: first time with active sub ───

    @Test
    void getOrCreate_firstTime_withActiveSub_createsWithSnapshot() {
        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSub));
        when(userQuotaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.getQuota(userId);

        var captor = ArgumentCaptor.forClass(UserQuota.class);
        verify(userQuotaRepository).save(captor.capture());
        UserQuota saved = captor.getValue();

        assertEquals(1000, saved.getAiLimit());
        assertEquals(subId, saved.getSubscriptionId());
        assertEquals(future, saved.getResetAt());
    }

    // ─── getOrCreate: first time without active sub → fallback ───

    @Test
    void getOrCreate_firstTime_noActiveSub_fallsBackToCheapest() {
        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE))
                .thenReturn(Optional.of(freePlan));
        when(userQuotaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime before = now();
        service.getQuota(userId);
        LocalDateTime after = now();

        var captor = ArgumentCaptor.forClass(UserQuota.class);
        verify(userQuotaRepository).save(captor.capture());
        UserQuota saved = captor.getValue();

        assertEquals(10, saved.getAiLimit());
        assertNull(saved.getSubscriptionId());
        // Gói Free (không có sub) → reset lăn 30 ngày kể từ bây giờ.
        assertResetAtRoughly30DaysFromNow(saved.getResetAt(), before, after);
    }

    // ─── resetOnPlanChange: after payment → resets ───

    @Test
    void resetOnPlanChange_afterPayment_resetsQuota() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(50)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(future)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(newSub));

        service.resetOnPlanChange(userId);

        assertEquals(0, quota.getAiUsed());
        assertEquals(0, quota.getExportUsed());
        assertEquals(1000, quota.getAiLimit());
        assertEquals(newSubId, quota.getSubscriptionId());
        assertEquals(newSub.getEndDate(), quota.getResetAt());
        verify(userQuotaRepository).save(quota);
    }

    // ─── reserveAiRequest: quota exceeded → throws ───

    @Test
    void reserveAiRequest_quotaExceeded_throws() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(1000)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(future)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSub));
        when(userQuotaRepository.tryReserveAi(userId)).thenReturn(0);

        var ex = assertThrows(AppException.class, () -> service.reserveAiRequest(userId));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getErrorCode());
    }

    // ─── rollbackAiRequest: decrements ───

    @Test
    void rollbackAiRequest_decrements() {
        when(userQuotaRepository.rollbackAi(userId)).thenReturn(1);

        service.rollbackAiRequest(userId);

        verify(userQuotaRepository).rollbackAi(userId);
    }

    // ─── getQuota: returns current state ───

    @Test
    void getQuota_returnsState() {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .aiUsed(5)
                .aiLimit(1000)
                .subscriptionId(subId)
                .resetAt(future)
                .build();

        when(userQuotaRepository.findByUserId(userId)).thenReturn(Optional.of(quota));
        when(subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(eq(userId), any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSub));

        var result = service.getQuota(userId);

        assertEquals(5, result.getUsed());
        assertEquals(1000, result.getLimit());
        assertEquals(future, result.getResetAt());
    }
}
