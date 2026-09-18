package org.bibliotecaviva.backend.application.services;

import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.bibliotecaviva.backend.api.config.WorkCacheConfig.*;

@Service
@RequiredArgsConstructor
public class WorkLikeCountService {
    private final WorkRepository workRepository;

    @Cacheable(cacheNames = LIKE_COUNTS, key = "#workId", sync = true, condition = OUTSIDE_TRANSACTION)
    public long getCount(UUID workId) {
        return workRepository.getLikeCount(workId);
    }
}
