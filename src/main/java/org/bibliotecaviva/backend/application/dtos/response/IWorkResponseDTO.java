package org.bibliotecaviva.backend.application.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * contraotos que os dtos devem seguir (atritutos em comuns a todos)
 */
public interface IWorkResponseDTO {
    UUID id();
    String title();
    String author();
    LocalDateTime publicationDate();
    String description();
    String type();
}
