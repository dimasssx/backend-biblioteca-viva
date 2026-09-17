package org.bibliotecaviva.backend.application.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PoemResponseDTO(
        UUID id,
        String title,
        String author,
        LocalDateTime publicationDate,
        String description,
        String type,
        String content,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        String studentClass,
        String rhymeScheme,
        String poemType
) implements WorkResponse {
}
