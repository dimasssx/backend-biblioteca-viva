package org.bibliotecaviva.backend.domain.entities.textual;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bibliotecaviva.backend.domain.entities.TextualWork;

@Entity
@Getter
@Setter
@NoArgsConstructor
@DiscriminatorValue("Article")
//armazena os artigos e reportagens

public class Article extends TextualWork {
}
