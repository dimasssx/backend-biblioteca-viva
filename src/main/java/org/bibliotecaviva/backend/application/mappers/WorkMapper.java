package org.bibliotecaviva.backend.application.mappers;


import org.bibliotecaviva.backend.application.dtos.request.audiovisual.LibraLiteratureRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.audiovisual.MultimediaRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.textual.*;
import org.bibliotecaviva.backend.application.dtos.request.visual.ArtRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.visual.InfographicRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.PoemResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.WorkResponse;
import org.bibliotecaviva.backend.application.dtos.response.WorkSummaryResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.audiovisual.LibraLiteratureResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.audiovisual.MultimediaResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.textual.*;
import org.bibliotecaviva.backend.application.dtos.response.visual.ArtResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.visual.IllustrationResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.visual.InfographicResponseDTO;
import org.bibliotecaviva.backend.domain.entities.Work;
import org.bibliotecaviva.backend.domain.entities.audiovisual.LibraLiterature;
import org.bibliotecaviva.backend.domain.entities.audiovisual.Multimedia;
import org.bibliotecaviva.backend.domain.entities.projections.WorkSummary;
import org.bibliotecaviva.backend.domain.entities.textual.*;
import org.bibliotecaviva.backend.domain.entities.visual.Art;
import org.bibliotecaviva.backend.domain.entities.visual.Infographic;
import org.mapstruct.*;

import java.time.Duration;

@Mapper(componentModel = "spring")
public interface WorkMapper {


    default WorkResponse toDTO(Work work, Long likeCount, Long commentCount) {
        return toDTO(work, likeCount, commentCount, work.getViewCount());
    }

    default WorkResponse toDTO(Work work, Long likeCount, Long commentCount, Long viewCount) {
        return switch (work) {
            case LibraLiterature w -> toLibraLiteratureResponseDTO(w, likeCount, commentCount, viewCount);
            case Multimedia w -> toMultimediaResponseDTO(w, likeCount, commentCount, viewCount);
            case Article w -> toArticleResponseDTO(w, likeCount, commentCount, viewCount);
            case Cordel w -> toCordelResponseDTO(w, likeCount, commentCount, viewCount);
            case Essay w -> toEssayResponseDTO(w, likeCount, commentCount, viewCount);
            case ShortStory w -> toShortStoryResponseDTO(w, likeCount, commentCount, viewCount);
            case Tale w -> toTaleResponseDTO(w, likeCount, commentCount, viewCount);
            case Art w -> toArtResponseDTO(w, likeCount, commentCount, viewCount);
            case Infographic w -> toInfographicReponseDTO(w, likeCount, commentCount, viewCount);
            case Poem w -> toPoemResponseDTO(w, likeCount, commentCount, viewCount);
            case Other w -> toOtherResponseDTO(w, likeCount, commentCount, viewCount);
            default -> throw new IllegalStateException("Unexpected value: " + work);
        };
    }

    default Duration map(Long value) {
        return value == null ? null : Duration.ofSeconds(value);
    }

    // mapeamento pra work summary
    WorkSummaryResponseDTO toWorkSummary(WorkSummary work);

    // mapeamentos específicos de cada entidade
    default LibraLiteratureResponseDTO toLibraLiteratureResponseDTO(LibraLiterature libraLiterature, Long likeCount, Long commentCount) {
        return toLibraLiteratureResponseDTO(libraLiterature, likeCount, commentCount, libraLiterature.getViewCount());
    }

    @Mapping(target = "author", expression = "java(libraLiterature.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    LibraLiteratureResponseDTO toLibraLiteratureResponseDTO(LibraLiterature libraLiterature, Long likeCount, Long commentCount, Long viewCount);

    default MultimediaResponseDTO toMultimediaResponseDTO(Multimedia multimedia, Long likeCount, Long commentCount) {
        return toMultimediaResponseDTO(multimedia, likeCount, commentCount, multimedia.getViewCount());
    }

    @Mapping(target = "author", expression = "java(multimedia.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    MultimediaResponseDTO toMultimediaResponseDTO(Multimedia multimedia, Long likeCount, Long commentCount, Long viewCount);

    default ArticleResponseDTO toArticleResponseDTO(Article article, Long likeCount, Long commentCount) {
        return toArticleResponseDTO(article, likeCount, commentCount, article.getViewCount());
    }

    @Mapping(target = "author", expression = "java(article.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    ArticleResponseDTO toArticleResponseDTO(Article article, Long likeCount, Long commentCount, Long viewCount);

    default PoemResponseDTO toPoemResponseDTO(Poem poem, Long likeCount, Long commentCount) {
        return toPoemResponseDTO(poem, likeCount, commentCount, poem.getViewCount());
    }

    @Mapping(target = "author", expression = "java(poem.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    PoemResponseDTO toPoemResponseDTO(Poem poem, Long likeCount, Long commentCount, Long viewCount);

    default CordelResponseDTO toCordelResponseDTO(Cordel cordel, Long likeCount, Long commentCount) {
        return toCordelResponseDTO(cordel, likeCount, commentCount, cordel.getViewCount());
    }

    @Mapping(target = "author", expression = "java(cordel.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    CordelResponseDTO toCordelResponseDTO(Cordel cordel, Long likeCount, Long commentCount, Long viewCount);

    default OtherResponseDTO toOtherResponseDTO(Other other, Long likeCount, Long commentCount) {
        return toOtherResponseDTO(other, likeCount, commentCount, other.getViewCount());
    }

    @Mapping(target = "author", expression = "java(other.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    OtherResponseDTO toOtherResponseDTO(Other other, Long likeCount, Long commentCount, Long viewCount);

    IllustrationResponseDTO toIllustrationResponseDTO(Art art);

    default EssayResponseDTO toEssayResponseDTO(Essay essay, Long likeCount, Long commentCount) {
        return toEssayResponseDTO(essay, likeCount, commentCount, essay.getViewCount());
    }

    @Mapping(target = "author", expression = "java(essay.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    EssayResponseDTO toEssayResponseDTO(Essay essay, Long likeCount, Long commentCount, Long viewCount);

    default ShortStoryResponseDTO toShortStoryResponseDTO(ShortStory shortStory, Long likeCount, Long commentCount) {
        return toShortStoryResponseDTO(shortStory, likeCount, commentCount, shortStory.getViewCount());
    }

    @Mapping(target = "author", expression = "java(shortStory.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    ShortStoryResponseDTO toShortStoryResponseDTO(ShortStory shortStory, Long likeCount, Long commentCount, Long viewCount);

    default TaleResponseDTO toTaleResponseDTO(Tale tale, Long likeCount, Long commentCount) {
        return toTaleResponseDTO(tale, likeCount, commentCount, tale.getViewCount());
    }

    @Mapping(target = "author", expression = "java(tale.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    TaleResponseDTO toTaleResponseDTO(Tale tale, Long likeCount, Long commentCount, Long viewCount);

    default ArtResponseDTO toArtResponseDTO(Art art, Long likeCount, Long commentCount) {
        return toArtResponseDTO(art, likeCount, commentCount, art.getViewCount());
    }

    @Mapping(target = "author", expression = "java(art.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    ArtResponseDTO toArtResponseDTO(Art art, Long likeCount, Long commentCount, Long viewCount);

    default InfographicResponseDTO toInfographicReponseDTO(Infographic infographic, Long likeCount, Long commentCount) {
        return toInfographicReponseDTO(infographic, likeCount, commentCount, infographic.getViewCount());
    }

    @Mapping(target = "author", expression = "java(infographic.resolveAuthorName())")
    @Mapping(target = "viewCount", source = "viewCount")
    InfographicResponseDTO toInfographicReponseDTO(Infographic infographic, Long likeCount, Long commentCount, Long viewCount);

    // daqui pra baixo separar ppor classe do mapper se prcisar, ver depois
    @Mapping(target = "author", ignore = true)
    LibraLiterature toEntity(LibraLiteratureRequestDTO libraLiteratureRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(LibraLiteratureRequestDTO libraLiteratureRequestDTO, @MappingTarget LibraLiterature libraLiterature);

    @Mapping(target = "author", ignore = true)
    Multimedia toEntity(MultimediaRequestDTO multimediaRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(MultimediaRequestDTO multimediaRequestDTO, @MappingTarget Multimedia multimedia);

    @Mapping(target = "author", ignore = true)
    Article toEntity(ArticleRequestDTO articleRequestDTO);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(ArticleRequestDTO articleRequestDTO, @MappingTarget Article article);

    @Mapping(target = "author", ignore = true)
    Poem toEntity(PoemRequestDTO poemRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(PoemRequestDTO poemRequestDTO, @MappingTarget Poem poem);

    @Mapping(target = "author", ignore = true)
    Cordel toEntity(CordelRequestDTO cordelRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(CordelRequestDTO cordelRequestDTO, @MappingTarget Cordel cordel);

    @Mapping(target = "author", ignore = true)
    Essay toEntity(EssayRequestDTO essayRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(EssayRequestDTO essayRequestDTO, @MappingTarget Essay essay);

    @Mapping(target = "author", ignore = true)
    ShortStory toEntity(ShortStoryRequestDTO shortStoryRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(ShortStoryRequestDTO shortStoryRequestDTO, @MappingTarget ShortStory shortStory);

    @Mapping(target = "author", ignore = true)
    Tale toEntity(TaleRequestDTO taleRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(TaleRequestDTO taleRequestDTO, @MappingTarget Tale tale);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "author", ignore = true)
    Art toEntity(ArtRequestDTO artRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(ArtRequestDTO artRequestDTO, @MappingTarget Art art);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "author", ignore = true)
    Infographic toEntity(InfographicRequestDTO infographicRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(InfographicRequestDTO infographicRequestDTO, @MappingTarget Infographic infographic);

    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "author", ignore = true)
    Other toEntity(OtherRequestDTO otherRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "author", ignore = true)
    void partialUpdate(OtherRequestDTO otherRequestDTO, @MappingTarget Other other);
}
