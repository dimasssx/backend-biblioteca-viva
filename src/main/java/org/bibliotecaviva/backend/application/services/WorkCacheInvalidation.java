package org.bibliotecaviva.backend.application.services;

import java.util.UUID;

/** Application event consumed only after the enclosing mutation commits. */
public record WorkCacheInvalidation(UUID workId, boolean clearAllLikeCounts) {
    public static WorkCacheInvalidation dashboard() {
        return new WorkCacheInvalidation(null, false);
    }

    public static WorkCacheInvalidation work(UUID workId) {
        return new WorkCacheInvalidation(workId, false);
    }

    public static WorkCacheInvalidation all() {
        return new WorkCacheInvalidation(null, true);
    }
}
