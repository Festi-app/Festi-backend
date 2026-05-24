package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private WaitingNotificationService service;

    @BeforeEach
    void setUp() {
        PushMessageProperties properties = new PushMessageProperties(
                new PushMessageProperties.PushMessageTemplate(
                        "입장 안내", "부스로 방문해 주세요.", "/icons/called.png"));
        Clock clock = Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
        service = new WaitingNotificationService(eventRepository, properties, clock);
        when(eventRepository.save(any(WaitingNotificationEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void enqueuesCalledEventWithPayloadSnapshotForLaterDelivery() {
        Waiting waiting = waiting();

        service.enqueueCalled(waiting);

        ArgumentCaptor<WaitingNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(WaitingNotificationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CALLED");
        assertThat(eventCaptor.getValue().getTitle()).isEqualTo("입장 안내");
        assertThat(eventCaptor.getValue().getBody()).isEqualTo("부스로 방문해 주세요.");
        assertThat(eventCaptor.getValue().getIcon()).isEqualTo("/icons/called.png");
        assertThat(eventCaptor.getValue().getUrl()).isEqualTo("/waitings");
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(WaitingNotificationEventStatus.PENDING);
        assertThat(eventCaptor.getValue().getAvailableAt())
                .isEqualTo(OffsetDateTime.parse("2026-05-25T00:00:00Z"));
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
}
