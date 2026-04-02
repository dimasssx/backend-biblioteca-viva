package org.bibliotecaviva.backend.application.dtos.response.audiovisual;

import org.bibliotecaviva.backend.application.dtos.response.IWorkResponseDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record LibraLiteratureResponseDTO(
        UUID id,
        String title,
        String author,
        LocalDateTime publicationDate,
        String description,
        String type,
        String url
) implements IWorkResponseDTO {
}
