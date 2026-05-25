package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.waiting.Waiting;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WaitingNotificationOutboxCoordinatorTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-05-25T00:00:00Z");

    @Mock
    private WaitingNotificationEventRepository eventRepository;

    @Mock
    private PushSubscriptionRepository subscriptionRepository;

    @Mock
    private PushNotificationDeliveryRepository deliveryRepository;

    private WaitingNotificationOutboxCoordinator coordinator;

    @BeforeEach
    void setUp() {
        PushDeliveryWorkerProperties properties = new PushDeliveryWorkerProperties(
                true, 1000, 20, 2, 30, 300);
        Clock clock = Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
        coordinator = new WaitingNotificationOutboxCoordinator(
                eventRepository, subscriptionRepository, deliveryRepository, properties, clock);
    }

    @Test
    void claimsAvailableEventAndCreatesDispatchTaskForCurrentSubscription() {
        WaitingNotificationEvent event = event();
        PushSubscription subscription = subscription(event.getWaiting().getUser());
        when(eventRepository.findNextAvailableForUpdate(NOW)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findByUserIdAndFestivalId(
                event.getWaiting().getUser().getId(), event.getWaiting().getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(deliveryRepository.findFirstByEventIdAndEndpointOrderByCreatedAtDesc(
                event.getId(), subscription.getEndpoint())).thenReturn(Optional.empty());

        Optional<WaitingNotificationOutboxCoordinator.DispatchTask> result = coordinator.claimNextAvailable();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().subscriptions()).containsExactly(subscription);
        assertThat(result.orElseThrow().attemptCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(WaitingNotificationEventStatus.PROCESSING);
        verify(eventRepository).requeueExpiredProcessing(NOW.minusSeconds(300), NOW);
    }

    @Test
    void omitsEndpointThatAlreadyHasTerminalDeliveryResult() {
        WaitingNotificationEvent event = event();
        PushSubscription subscription = subscription(event.getWaiting().getUser());
        PushNotificationDelivery delivery = new PushNotificationDelivery(event, subscription);
        delivery.markSent(201, NOW);
        when(eventRepository.findNextAvailableForUpdate(NOW)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findByUserIdAndFestivalId(
                event.getWaiting().getUser().getId(), event.getWaiting().getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(deliveryRepository.findFirstByEventIdAndEndpointOrderByCreatedAtDesc(
                event.getId(), subscription.getEndpoint())).thenReturn(Optional.of(delivery));

        WaitingNotificationOutboxCoordinator.DispatchTask result = coordinator.claimNextAvailable().orElseThrow();

        assertThat(result.subscriptions()).isEmpty();
    }

    private WaitingNotificationEvent event() {
        Waiting waiting = waiting();
        WaitingNotificationEvent event = new WaitingNotificationEvent(
                waiting, "CALLED", "title", "body", null, "/waitings", NOW);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private Waiting waiting() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        User user = new User(festival, "alice123", "hashed", "Alice", "01012345678");
        Booth booth = new Booth("Night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        return new Waiting(booth, user, (short) 2);
    }

    private PushSubscription subscription(User user) {
        return new PushSubscription(user, "https://push.example.com/subscription/1", "p256dh", "auth");
    }
}
