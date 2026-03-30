package org.bibliotecaviva.backend.domain.entities.visual;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.bibliotecaviva.backend.domain.entities.VisualWork;

@Entity
@Getter
@Setter
//@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("Infographic")
@SuperBuilder
/*
 Infográficos
 */
public class Infographic extends VisualWork {

}