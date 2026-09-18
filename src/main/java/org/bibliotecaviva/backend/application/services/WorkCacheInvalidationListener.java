package org.bibliotecaviva.backend.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.bibliotecaviva.backend.api.config.WorkCacheConfig.*;

@Component
@RequiredArgsConstructor
public class WorkCacheInvalidationListener {
    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(WorkCacheInvalidation event) {
        cacheManager.getCache(FRONT_PAGE).evict(DASHBOARD_KEY);
        var likes = cacheManager.getCache(LIKE_COUNTS);
        if (event.clearAllLikeCounts()) {
            likes.clear();
        } else if (event.workId() != null) {
            likes.evict(event.workId());
        }
    }
}
