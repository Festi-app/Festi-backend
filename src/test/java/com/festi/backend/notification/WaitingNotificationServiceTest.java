package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.waiting.Waiting;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WaitingNotificationServiceTest {

    @Mock
    private WaitingNotificationEventRepository eventRepository;

    @Mock
    private PushSubscriptionRepository subscriptionRepository;

    @Mock
    private PushNotificationDeliveryRepository deliveryRepository;

    @Mock
    private WebPushSender webPushSender;

    private WaitingNotificationService service;

    @BeforeEach
    void setUp() {
        PushMessageProperties properties = new PushMessageProperties(
                new PushMessageProperties.PushMessageTemplate(
                        "입장 안내", "부스로 방문해 주세요.", "/icons/called.png"));
        Clock clock = Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
        service = new WaitingNotificationService(
                eventRepository, subscriptionRepository, deliveryRepository, webPushSender, properties, clock);
        when(eventRepository.save(any(WaitingNotificationEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCalledEventAndSendsSnapshotPayloadToCurrentSubscription() throws Exception {
        Waiting waiting = waiting();
        PushSubscription subscription = subscription(waiting.getUser());
        when(subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(webPushSender.send(eq(subscription), any(PushNotificationPayload.class)))
                .thenReturn(WebPushSender.SendResult.sent(201));

        service.notifyCalled(waiting);

        ArgumentCaptor<WaitingNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(WaitingNotificationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CALLED");
        assertThat(eventCaptor.getValue().getTitle()).isEqualTo("입장 안내");
        assertThat(eventCaptor.getValue().getBody()).isEqualTo("부스로 방문해 주세요.");
        assertThat(eventCaptor.getValue().getIcon()).isEqualTo("/icons/called.png");
        assertThat(eventCaptor.getValue().getUrl()).isEqualTo("/waitings");

        ArgumentCaptor<PushNotificationPayload> payloadCaptor =
                ArgumentCaptor.forClass(PushNotificationPayload.class);
        verify(webPushSender).send(eq(subscription), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().url()).isEqualTo("/waitings");

        ArgumentCaptor<PushNotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(PushNotificationDelivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getStatus()).isEqualTo(PushNotificationDeliveryStatus.SENT);
        assertThat(deliveryCaptor.getValue().getResponseStatus()).isEqualTo(201);
        assertThat(deliveryCaptor.getValue().getAttemptedAt())
                .isEqualTo(OffsetDateTime.parse("2026-05-25T00:00:00Z"));
    }

    @Test
    void preservesCalledEventWithoutDeliveriesWhenUserHasNoSubscriptions() {
        Waiting waiting = waiting();
        when(subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId()))
                .thenReturn(List.of());

        service.notifyCalled(waiting);

        verify(eventRepository).save(any(WaitingNotificationEvent.class));
        verify(deliveryRepository, times(0)).save(any(PushNotificationDelivery.class));
    }

    @Test
    void retriesTransientDeliveryFailureOnceAndStoresBothAttempts() throws Exception {
        Waiting waiting = waiting();
        PushSubscription subscription = subscription(waiting.getUser());
        when(subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(webPushSender.send(eq(subscription), any(PushNotificationPayload.class)))
                .thenReturn(WebPushSender.SendResult.failed(503, "Push endpoint responded with HTTP 503.", true))
                .thenReturn(WebPushSender.SendResult.sent(201));

        service.notifyCalled(waiting);

        ArgumentCaptor<PushNotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(PushNotificationDelivery.class);
        verify(deliveryRepository, times(2)).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(PushNotificationDelivery::getStatus)
                .containsExactly(PushNotificationDeliveryStatus.FAILED, PushNotificationDeliveryStatus.SENT);
    }

    @Test
    void doesNotRetryPermanentDeliveryFailure() throws Exception {
        Waiting waiting = waiting();
        PushSubscription subscription = subscription(waiting.getUser());
        when(subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(webPushSender.send(eq(subscription), any(PushNotificationPayload.class)))
                .thenReturn(WebPushSender.SendResult.failed(410, "Push endpoint responded with HTTP 410.", false));

        service.notifyCalled(waiting);

        verify(deliveryRepository).save(any(PushNotificationDelivery.class));
        verify(webPushSender).send(eq(subscription), any(PushNotificationPayload.class));
    }

    @Test
    void retriesTransportExceptionOnceAndRecordsTheRecoveredAttempt() throws Exception {
        Waiting waiting = waiting();
        PushSubscription subscription = subscription(waiting.getUser());
        when(subscriptionRepository.findByUserIdAndFestivalId(
                waiting.getUser().getId(), waiting.getUser().getFestivalId()))
                .thenReturn(List.of(subscription));
        when(webPushSender.send(eq(subscription), any(PushNotificationPayload.class)))
                .thenThrow(new IOException("connection reset"))
                .thenReturn(WebPushSender.SendResult.sent(201));

        service.notifyCalled(waiting);

        ArgumentCaptor<PushNotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(PushNotificationDelivery.class);
        verify(deliveryRepository, times(2)).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(PushNotificationDelivery::getStatus)
                .containsExactly(PushNotificationDeliveryStatus.FAILED, PushNotificationDeliveryStatus.SENT);
        assertThat(deliveryCaptor.getAllValues().getFirst().getFailureReason())
                .isEqualTo("Web Push transport failed: IOException.");
    }

    private Waiting waiting() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        User user = new User(festival, "alice123", "hashed", "Alice", "01012345678");
        Booth booth = new Booth("Night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        Waiting waiting = new Waiting(booth, user, (short) 2);
        ReflectionTestUtils.setField(waiting, "id", UUID.randomUUID());
        return waiting;
    }

    private PushSubscription subscription(User user) {
        return new PushSubscription(user, "https://push.example.com/subscription/1", "p256dh", "auth");
    }
}
