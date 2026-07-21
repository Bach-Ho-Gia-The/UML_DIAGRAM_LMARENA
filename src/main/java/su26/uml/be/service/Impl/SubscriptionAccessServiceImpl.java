package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.service.SubscriptionAccessService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionAccessServiceImpl implements SubscriptionAccessService {

    SubscriptionRepository subscriptionRepository;

    @Override
    public Optional<Subscription> getActiveSubscription(UUID userId, LocalDateTime now) {
        return subscriptionRepository
                .findFirstByUser_IdAndStatusInAndStartDateLessThanEqualAndEndDateAfterOrderByEndDateDesc(
                        userId, List.of(SubscriptionStatus.ACTIVE), now, now);
    }
}
