package org.bibliotecaviva.backend.aplication.dtos.response.textual;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bibliotecaviva.backend.aplication.dtos.response.TextualWorkResponseDTO;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EssayResponseDTO extends TextualWorkResponseDTO {
    private Integer rate;
    private String theme;
    private String themeDescription;
    private String feedback;
}
