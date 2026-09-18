package org.bibliotecaviva.backend.application.services;

import org.bibliotecaviva.backend.domain.entities.textual.Article;
import org.bibliotecaviva.backend.domain.exceptions.WorkNotFoundException;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkViewServiceTest {
    private final WorkRepository repository = mock(WorkRepository.class);
    private final WorkViewPersistenceService persistence = mock(WorkViewPersistenceService.class);
    private final UUID id = UUID.randomUUID();
    private final AtomicLong stored = new AtomicLong(10);
    private WorkViewService service;

    @BeforeEach
    void setup() {
        service = new WorkViewService(repository, persistence, 2);
        when(repository.findById(id)).thenAnswer(invocation -> Optional.of(
                Article.builder().id(id).viewCount(stored.get()).build()));
        doAnswer(invocation -> {
            stored.addAndGet(invocation.getArgument(1, Long.class));
            return null;
        }).when(persistence).add(eq(id), anyLong());
    }

    @Test
    void groupsViewsAndDoesNotCountPersistedBatchTwice() {
        var first = service.recordView(id);
        assertEquals(11, first.viewCount());
        assertEquals(10, first.work().getViewCount());
        assertEquals(12, service.recordView(id).viewCount());
        verifyNoInteractions(persistence);
        service.flush();
        verify(persistence).add(id, 2);
        assertEquals(13, service.recordView(id).viewCount());
        service.flush();
        assertEquals(13, stored.get());
        service.flush();
        verify(persistence, times(2)).add(eq(id), anyLong());
    }

    @Test
    void retainsRolledBackDeltaAndAddsNewViewsBeforeRetry() {
        doThrow(new IllegalStateException("rollback")).doAnswer(invocation -> {
            stored.addAndGet(invocation.getArgument(1, Long.class));
            return null;
        }).when(persistence).add(eq(id), anyLong());
        service.recordView(id);
        service.flush();
        assertEquals(10, stored.get());
        assertEquals(12, service.recordView(id).viewCount());
        service.flush();
        assertEquals(12, stored.get());
    }

    @Test
    void countsConcurrentRequestsExactlyOnce() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            var results = executor.invokeAll(IntStream.range(0, 100)
                    .<java.util.concurrent.Callable<Long>>mapToObj(i -> () -> service.recordView(id).viewCount())
                    .toList());
            assertEquals(100, results.stream().map(result -> {
                try { return result.get(); } catch (Exception e) { throw new AssertionError(e); }
            }).distinct().count());
        }
        service.flush();
        assertEquals(110, stored.get());
        verify(persistence).add(id, 100);
    }

    @Test
    void requestDuringFlushSeesCommittedCountWithoutDoubleCounting() throws Exception {
        var writing = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            writing.countDown();
            assertTrue(finish.await(5, TimeUnit.SECONDS));
            stored.addAndGet(invocation.getArgument(1, Long.class));
            return null;
        }).when(persistence).add(eq(id), anyLong());
        service.recordView(id);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var flush = executor.submit(service::flush);
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            var read = executor.submit(() -> service.recordView(id).viewCount());
            finish.countDown();
            flush.get(5, TimeUnit.SECONDS);
            assertEquals(12L, read.get(5, TimeUnit.SECONDS));
        } finally {
            finish.countDown();
        }
        service.flush();
        assertEquals(12, stored.get());
    }

    @Test
    void fallsBackToDirectWriteAtCapacityAndReleasesCapacityAfterFlush() {
        service = new WorkViewService(repository, persistence, 1);
        UUID other = UUID.randomUUID();
        when(repository.findById(other)).thenReturn(Optional.of(Article.builder().viewCount(20L).build()));
        service.recordView(id);
        assertEquals(21, service.recordView(other).viewCount());
        verify(persistence).add(other, 1);
        service.flush();
        clearInvocations(persistence);
        service.recordView(other);
        verifyNoInteractions(persistence);
    }

    @Test
    void missingWorksDoNotAccumulateViews() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(WorkNotFoundException.class, () -> service.recordView(id));
        service.flush();
        verifyNoInteractions(persistence);
    }

    @Test
    void shutdownFlushesPendingViews() {
        service.recordView(id);
        service.shutdown();
        assertEquals(11, stored.get());
    }
}
