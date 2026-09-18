package org.bibliotecaviva.backend.integration;

import org.bibliotecaviva.backend.application.services.*;
import org.bibliotecaviva.backend.application.dtos.request.textual.ArticleRequestDTO;
import org.bibliotecaviva.backend.domain.entities.User;
import org.bibliotecaviva.backend.domain.entities.textual.Article;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import static org.bibliotecaviva.backend.api.config.WorkCacheConfig.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workperformance;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "work.views.flush-interval-ms=3600000"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WorkPerformanceIntegrationTest extends IntegrationTestSupport {
    @Autowired WorkService works;
    @Autowired WorkViewService views;
    @Autowired WorkViewPersistenceService viewPersistence;
    @Autowired WorkLikeCountService likes;
    @Autowired CommentService comments;
    @Autowired UserManagementService users;
    @Autowired CacheManager caches;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactions;
    private User author;
    private Article article;

    @BeforeEach
    void setup() {
        caches.getCache(FRONT_PAGE).clear();
        caches.getCache(LIKE_COUNTS).clear();
        author = createActiveAdmin();
        article = createArticleInDatabase(author);
    }

    @AfterEach
    void cleanup() {
        views.flush();
        if (workRepository.existsById(article.getId())) works.delete(article.getId());
        if (userRepository.existsById(author.getId())) users.deleteUser(author.getId());
        caches.getCache(FRONT_PAGE).clear();
        caches.getCache(LIKE_COUNTS).clear();
    }

    @Test
    void warmDashboardSkipsAllThirteenQueriesAndExpires() {
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var first = works.getFrontPageData();
        assertEquals(13, statistics.getPrepareStatementCount());
        var cached = works.getFrontPageData();
        assertSame(first, cached);
        assertEquals(13, statistics.getPrepareStatementCount());

        var cache = (CaffeineCache) caches.getCache(FRONT_PAGE);
        var expiration = cache.getNativeCache().policy().expireAfterWrite().orElseThrow();
        assertEquals(Duration.ofSeconds(30), expiration.getExpiresAfter());
        try {
            expiration.setExpiresAfter(Duration.ZERO);
            assertNotSame(first, works.getFrontPageData());
            assertEquals(26, statistics.getPrepareStatementCount());
        } finally {
            expiration.setExpiresAfter(Duration.ofSeconds(30));
        }
    }

    @Test
    void likeCountsAreCachedAndInvalidatedAfterActualChanges() {
        var id = article.getId();
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertEquals(0, likes.getCount(id));
        assertEquals(0, likes.getCount(id));
        assertEquals(1, statistics.getPrepareStatementCount());
        var first = works.getFrontPageData();
        assertEquals(1, works.like(id, author).likeCount());
        assertEquals(1, likes.getCount(id));
        assertNotSame(first, works.getFrontPageData());
        var afterLike = works.getFrontPageData();
        assertEquals(1, works.like(id, author).likeCount());
        assertSame(afterLike, works.getFrontPageData());
        assertEquals(0, works.unLike(id, author).likeCount());
        assertEquals(0, likes.getCount(id));
        assertNotSame(afterLike, works.getFrontPageData());
    }

    @Test
    void concurrentDuplicateLikesRemainIdempotent() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(
                    () -> works.like(article.getId(), author),
                    () -> works.like(article.getId(), author)));
            for (var result : results) assertNotNull(result.get());
        }
        assertEquals(1, workRepository.getLikeCount(article.getId()));
    }

    @Test
    void invalidationWaitsForCommitAndRollbackPreservesCache() {
        var first = works.getFrontPageData();
        likes.getCount(article.getId());
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            events.publishEvent(WorkCacheInvalidation.all());
            assertSame(first, caches.getCache(FRONT_PAGE).get(DASHBOARD_KEY).get());
            status.setRollbackOnly();
        });
        assertSame(first, works.getFrontPageData());
        assertNotNull(caches.getCache(LIKE_COUNTS).get(article.getId()));
        tx.executeWithoutResult(status -> {
            events.publishEvent(WorkCacheInvalidation.all());
            assertSame(first, caches.getCache(FRONT_PAGE).get(DASHBOARD_KEY).get());
        });
        assertNull(caches.getCache(FRONT_PAGE).get(DASHBOARD_KEY));
        assertNull(caches.getCache(LIKE_COUNTS).get(article.getId()));
    }

    @Test
    void rolledBackLikeDoesNotPublishUncommittedCount() {
        var id = article.getId();
        assertEquals(0, likes.getCount(id));
        var first = works.getFrontPageData();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertEquals(1, works.like(id, author).likeCount());
            assertEquals(1, likes.getCount(id));
            assertEquals(0L, caches.getCache(LIKE_COUNTS).get(id).get());
            status.setRollbackOnly();
        });
        assertEquals(0, likes.getCount(id));
        assertEquals(0, workRepository.getLikeCount(id));
        assertSame(first, works.getFrontPageData());
    }

    @Test
    void createUpdateAndDeleteInvalidateDashboard() {
        var first = works.getFrontPageData();
        var request = new ArticleRequestDTO("Nova obra", null, "Autor externo", article.getPublicationDate(),
                "Descricao da nova obra", "Conteudo", "Turma A");
        var created = works.create(request);
        try {
            var afterCreate = works.getFrontPageData();
            assertNotSame(first, afterCreate);
            works.update(created.id(), new ArticleRequestDTO("Obra atualizada", null, "Autor externo",
                    article.getPublicationDate(), "Descricao da nova obra", "Conteudo", "Turma A"));
            var afterUpdate = works.getFrontPageData();
            assertNotSame(afterCreate, afterUpdate);
            likes.getCount(created.id());
            works.delete(created.id());
            assertNull(caches.getCache(LIKE_COUNTS).get(created.id()));
            assertNotSame(afterUpdate, works.getFrontPageData());
        } finally {
            if (workRepository.existsById(created.id())) works.delete(created.id());
        }
    }

    @Test
    void commentCountChangesInvalidateDashboard() {
        var first = works.getFrontPageData();
        var comment = comments.create(article.getId(), "Comentario", author);
        var afterCreate = works.getFrontPageData();
        assertNotSame(first, afterCreate);
        comments.delete(comment.id(), author);
        assertNotSame(afterCreate, works.getFrontPageData());
    }

    @Test
    void deletingUserInvalidatesCountsAndDashboard() {
        var reader = createActiveStudent();
        works.like(article.getId(), reader);
        assertEquals(1, likes.getCount(article.getId()));
        var first = works.getFrontPageData();
        users.deleteUser(reader.getId());
        assertEquals(0, likes.getCount(article.getId()));
        assertNotSame(first, works.getFrontPageData());
    }

    @Test
    void detailsIncludeCurrentViewAndOnlyFlushWritesCounter() {
        var first = works.getFrontPageData();
        assertEquals(1, works.getById(article.getId()).viewCount());
        assertEquals(2, works.getById(article.getId()).viewCount());
        assertEquals(0, workRepository.findById(article.getId()).orElseThrow().getViewCount());
        views.flush();
        assertEquals(2, workRepository.findById(article.getId()).orElseThrow().getViewCount());
        assertEquals(3, works.getById(article.getId()).viewCount());
        assertSame(first, works.getFrontPageData());
    }

    @Test
    void editingStaleEntityCannotOverwritePersistedViews() {
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            var stale = workRepository.findById(article.getId()).orElseThrow();
            assertEquals(0, stale.getViewCount());
            viewPersistence.add(article.getId(), 5);
            stale.setTitle("Titulo atualizado");
            workRepository.saveAndFlush(stale);
        });
        var updated = workRepository.findById(article.getId()).orElseThrow();
        assertEquals(5, updated.getViewCount());
        assertEquals("Titulo atualizado", updated.getTitle());
    }

    @Test
    void flushingDeletedWorkDoesNotRecreateItOrRetainDelta() {
        works.getById(article.getId());
        works.delete(article.getId());
        views.flush();
        assertFalse(workRepository.existsById(article.getId()));
    }
}
