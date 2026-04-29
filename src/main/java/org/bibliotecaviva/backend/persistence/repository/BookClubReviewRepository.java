package org.bibliotecaviva.backend.persistence.repository;

import org.bibliotecaviva.backend.domain.entities.BookClubReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookClubReviewRepository extends JpaRepository<BookClubReview, UUID> {
    Page<BookClubReview> findByBookClubId(UUID bookClubId, Pageable pageable);
}
