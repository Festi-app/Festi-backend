package com.festi.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingNotificationDeliveryScheduler {

    private final WaitingNotificationDeliveryWorker worker;
    private final PushDeliveryWorkerProperties properties;

    @Scheduled(fixedDelayString = "${festi.push.delivery.worker.poll-delay-millis:1000}")
    public void deliverPendingEvents() {
        for (int processed = 0; processed < properties.batchSize(); processed++) {
            if (!worker.processNext()) {
                return;
            }
        }
    }
}
