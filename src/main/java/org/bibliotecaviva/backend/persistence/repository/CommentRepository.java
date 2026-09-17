package org.bibliotecaviva.backend.persistence.repository;

import org.bibliotecaviva.backend.domain.entities.Comment;
import org.bibliotecaviva.backend.domain.entities.projections.CommentSummary;
import org.bibliotecaviva.backend.domain.entities.projections.CommentDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @Query(value = """
            SELECT c.id AS id, c.content AS content, u.name AS authorName,
                   c.createdAt AS createdAt, COUNT(l) AS likes,
                   r.id AS replyId, r.content AS replyContent,
                   ru.name AS replyAuthorName, r.createdAt AS replyCreatedAt
            FROM Comment c JOIN c.user u
            LEFT JOIN c.reply r LEFT JOIN r.user ru LEFT JOIN c.likedBy l
            WHERE c.work.id = :workId
            GROUP BY c.id, c.content, u.name, c.createdAt,
                     r.id, r.content, ru.name, r.createdAt
            ORDER BY c.createdAt DESC, c.id DESC
            """, countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.work.id = :workId")
    Page<CommentDetails> findByWorkIdOrderByCreatedAtDesc(@Param("workId") UUID workId, Pageable pageable);

    Long countByWork_Id(UUID workId);

    @Query("SELECT c.id AS id, c.content AS content, c.createdAt AS createdAt, " +
            "u.name AS userName, u.id as userId, " +
            "w.title AS workTitle, w.id as workId, " +
            "rp.content as replyContent, rp.id as replyId, rp.createdAt as replyCreatedAt, ru.name as replyAuthor " +
            "FROM Comment c JOIN c.user u JOIN c.work w " +
            "LEFT JOIN c.reply rp LEFT JOIN rp.user ru")
    Page<CommentSummary> findAllWithDetails(Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO comment_likes (user_id, comment_id)
            SELECT :userId, :commentId
            WHERE NOT EXISTS (
                SELECT 1 FROM comment_likes WHERE user_id = :userId AND comment_id = :commentId
            )
            """, nativeQuery = true)
    void likeComment(@Param("userId") UUID userId, @Param("commentId") UUID commentId);

    @Modifying
    @Query(value = "DELETE FROM comment_likes WHERE user_id = :userId AND comment_id = :commentId", nativeQuery = true)
    void unlikeComment(@Param("userId") UUID userId, @Param("commentId") UUID commentId);

    @Modifying
    @Query(value = """
            DELETE FROM comment_likes
            WHERE comment_id IN (
                SELECT id FROM comments WHERE user_id = :userId
            )
            """, nativeQuery = true)
    void deleteLikesFromCommentsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM comments WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT COUNT(*) FROM comment_likes WHERE comment_id = :commentId", nativeQuery = true)
    long getLikeCount(@Param("commentId") UUID commentId);


}
