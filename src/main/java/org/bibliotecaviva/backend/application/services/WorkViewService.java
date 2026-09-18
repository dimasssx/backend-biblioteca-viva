package org.bibliotecaviva.backend.application.services;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.bibliotecaviva.backend.domain.entities.Work;
import org.bibliotecaviva.backend.domain.exceptions.WorkNotFoundException;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

@Service
@Log4j2
public class WorkViewService {
    private final WorkRepository workRepository;
    private final WorkViewPersistenceService persistence;
    private final ConcurrentHashMap<UUID, Long> pending = new ConcurrentHashMap<>();
    private final Semaphore capacity;
    private final Object[] locks = IntStream.range(0, 256).mapToObj(i -> new Object()).toArray();

    public WorkViewService(WorkRepository workRepository, WorkViewPersistenceService persistence,
                           @Value("${work.views.max-pending-works:10000}") int maxPendingWorks) {
        if (maxPendingWorks < 1) throw new IllegalArgumentException("View buffer capacity must be positive");
        this.workRepository = workRepository;
        this.persistence = persistence;
        this.capacity = new Semaphore(maxPendingWorks);
    }

    public ViewedWork recordView(UUID id) {
        synchronized (lock(id)) {
            // Read and flush share the lock: a persisted batch cannot also be counted as pending.
            Work work = workRepository.findById(id)
                    .orElseThrow(() -> new WorkNotFoundException("Obra com id " + id + " não encontrada"));
            Long delta = pending.get(id);
            if (delta == null && !capacity.tryAcquire()) {
                persistence.add(id, 1);
                return new ViewedWork(work, work.getViewCount() + 1);
            }
            long updatedDelta = delta == null ? 1 : delta + 1;
            pending.put(id, updatedDelta);
            return new ViewedWork(work, work.getViewCount() + updatedDelta);
        }
    }

    @Scheduled(fixedDelayString = "${work.views.flush-interval-ms:10000}",
            initialDelayString = "${work.views.flush-interval-ms:10000}")
    public void flush() {
        for (UUID id : pending.keySet()) {
            synchronized (lock(id)) {
                Long delta = pending.get(id);
                if (delta == null) continue;
                try {
                    persistence.add(id, delta);
                    // A deleted work produces zero updated rows and its obsolete delta is discarded.
                    pending.remove(id);
                    capacity.release();
                } catch (RuntimeException ex) {
                    log.warn("Could not persist views for work {}; keeping delta {} for retry", id, delta, ex);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        flush();
    }

    private Object lock(UUID id) {
        return locks[(id.hashCode() & Integer.MAX_VALUE) % locks.length];
    }

    public record ViewedWork(Work work, long viewCount) { }
}
