package org.bibliotecaviva.backend.application.dtos.request.textual;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.bibliotecaviva.backend.application.dtos.request.WorkRequest;

import java.time.LocalDateTime;

/**
 * DTO for {@link org.bibliotecaviva.backend.domain.entities.textual.Tale}
 */
public record TaleRequestDTO(
        @NotBlank(message = "Title cannot be blank") @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,
        @Email(message = "Author must be a valid email address(can be changed later)") @NotBlank(message = "Author cannot be blank")
        String author,
        @PastOrPresent(message = "Publication date cannot be in the future")
        LocalDateTime publicationDate,
        @NotBlank(message = "description cannot be blank") @Size(min = 15, message = "Description must be at least 15 characters long")
        String description,
        @NotBlank(message = "Content can not be blank") //Veriicar dps se coloca minimo e maximo de caracteres
        String content,
        @NotBlank(message = "Genre cannot be blank") @Size(min = 3, max = 100, message = "Genre must be between 3 and 100 characters")
        String genre
) implements WorkRequest {
}