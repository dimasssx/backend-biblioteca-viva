package org.bibliotecaviva.backend.domain.entities.audiovisual;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bibliotecaviva.backend.domain.entities.AudioVisualWork;


@Entity
@Getter
@Setter
@NoArgsConstructor
@DiscriminatorValue("LibraLiterature")
public class LibraLiterature extends AudioVisualWork {
}
