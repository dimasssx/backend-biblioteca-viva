package org.bibliotecaviva.backend.aplication.dtos.response.audiovisual;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bibliotecaviva.backend.aplication.dtos.response.AudioVisualWorkResponseDTO;

@Schema(name = "LibraLiteratureResponseDTO")
@Getter
@NoArgsConstructor
@SuperBuilder
public class LibraLiteratureResponseDTO extends AudioVisualWorkResponseDTO {
}
