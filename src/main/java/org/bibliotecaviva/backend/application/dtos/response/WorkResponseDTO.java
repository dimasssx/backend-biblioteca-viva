package org.bibliotecaviva.backend.application.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Show basic data from works")
public record WorkResponseDTO(
        UUID id,
        String title,
        String author,
        LocalDateTime publicationDate,
        String description,
        String type
) implements IWorkResponseDTO {
}

