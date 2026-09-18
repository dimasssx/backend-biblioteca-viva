package org.bibliotecaviva.backend.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class WorkCacheConfig {
    public static final String FRONT_PAGE = "frontPage";
    public static final String DASHBOARD_KEY = "dashboard";
    public static final String LIKE_COUNTS = "workLikeCounts";
    // Do not expose uncommitted reads to other requests via the shared cache.
    public static final String OUTSIDE_TRANSACTION =
            "!T(org.springframework.transaction.support.TransactionSynchronizationManager).isActualTransactionActive()";

    @Bean
    CacheManager cacheManager(@Value("${work.cache.front-page-ttl:30s}") Duration frontPageTtl,
                              @Value("${work.cache.like-count-ttl:30s}") Duration likeCountTtl,
                              @Value("${work.cache.like-count-max-size:10000}") long likeCountMaxSize) {
        if (frontPageTtl.isZero() || frontPageTtl.isNegative()
                || likeCountTtl.isZero() || likeCountTtl.isNegative() || likeCountMaxSize < 1) {
            throw new IllegalArgumentException("Cache TTLs and capacity must be positive");
        }
        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new CaffeineCache(FRONT_PAGE, Caffeine.newBuilder().recordStats().maximumSize(1)
                        .expireAfterWrite(frontPageTtl).build(), false),
                new CaffeineCache(LIKE_COUNTS, Caffeine.newBuilder().recordStats().maximumSize(likeCountMaxSize)
                        .expireAfterWrite(likeCountTtl).build(), false)));
        return manager;
    }
}
