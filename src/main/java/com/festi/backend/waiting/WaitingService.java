package com.festi.backend.waiting;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WaitingService {

    private final WaitingRepository waitingRepository;

    public List<WaitingDTO.Response> getMyWaitings(UUID userId) {
        return waitingRepository.findByUserIdOrderByRegisteredAtDesc(userId).stream()
                .map(WaitingDTO.Response::from)
                .toList();
    }
}
