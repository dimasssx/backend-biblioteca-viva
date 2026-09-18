package org.bibliotecaviva.backend.domain.entities.visual;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.bibliotecaviva.backend.domain.entities.Work;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public abstract class VisualWork extends Work {
    @Column(columnDefinition = "TEXT")
    private String url;

    /** Cloudinary {@code public_id} — persisted to enable asset deletion on replace/delete. */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;
}

