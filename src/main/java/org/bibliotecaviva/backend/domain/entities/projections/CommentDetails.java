package org.bibliotecaviva.backend.domain.entities.projections;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CommentDetails {
    UUID getId();
    String getContent();
    String getAuthorName();
    LocalDateTime getCreatedAt();
    Long getLikes();
    UUID getReplyId();
    String getReplyContent();
    String getReplyAuthorName();
    LocalDateTime getReplyCreatedAt();
}
