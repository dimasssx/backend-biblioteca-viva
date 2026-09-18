package org.bibliotecaviva.backend.application.services;

import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkViewPersistenceService {
    private final WorkRepository workRepository;

    // Returning through this separate bean's proxy confirms commit before the buffer is cleared.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void add(UUID workId, long delta) {
        workRepository.incrementViewCount(workId, delta);
    }
}
