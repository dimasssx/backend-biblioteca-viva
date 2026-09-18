package org.bibliotecaviva.backend.domain.entities.textual;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "other_work")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("Other")

/*
 * Categoria geral: obras que nao se encaixam nas demais categorias.
 * O link e a imagem sao opcionais.
 */
public class Other extends TextualWork {
    @Column(columnDefinition = "TEXT")
    private String url;
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** Cloudinary {@code public_id} — persisted to enable asset deletion on replace/delete. */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;
}
