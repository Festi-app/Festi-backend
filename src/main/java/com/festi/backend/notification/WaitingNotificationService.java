package com.festi.backend.notification;

import com.festi.backend.waiting.Waiting;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingNotificationService {

    private static final String CALLED_EVENT_TYPE = "CALLED";
    private static final String WAITINGS_URL = "/waitings";

    private final WaitingNotificationEventRepository eventRepository;
    private final PushMessageProperties pushMessageProperties;
    private final Clock clock;

    public void enqueueCalled(Waiting waiting) {
        PushMessageProperties.PushMessageTemplate template = pushMessageProperties.called();
        PushNotificationPayload payload = new PushNotificationPayload(
                template.title(), template.body(), template.icon(), WAITINGS_URL);
        eventRepository.save(new WaitingNotificationEvent(
                waiting, CALLED_EVENT_TYPE, payload.title(), payload.body(), payload.icon(), payload.url(),
                OffsetDateTime.now(clock)));
    }
}
