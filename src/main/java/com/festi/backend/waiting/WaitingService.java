package com.festi.backend.waiting;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WaitingService {

    private final WaitingRepository waitingRepository;

    public WaitingService(WaitingRepository waitingRepository) {
        this.waitingRepository = waitingRepository;
    }

    public List<WaitingDTO.Response> getMyWaitings(UUID userId) {
        return waitingRepository.findByUserIdOrderByRegisteredAtDesc(userId).stream()
                .map(WaitingDTO.Response::from)
                .toList();
    }
}
