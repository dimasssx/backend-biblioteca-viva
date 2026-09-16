package org.bibliotecaviva.backend.persistence.repository;

import org.bibliotecaviva.backend.domain.entities.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {

    @Modifying
    @Query("UPDATE News n SET n.author = null WHERE n.author.id = :userId")
    void clearAuthorByUserId(@Param("userId") UUID userId);
}
