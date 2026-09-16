package org.bibliotecaviva.backend.persistence.repository;

import org.bibliotecaviva.backend.domain.entities.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentReplyRepository extends JpaRepository<CommentReply, UUID> {

    Optional<CommentReply> findByCommentId(UUID commentId);

    @Modifying
    @Query(value = "DELETE FROM comment_replies WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
