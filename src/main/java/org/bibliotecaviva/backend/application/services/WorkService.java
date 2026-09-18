package org.bibliotecaviva.backend.application.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bibliotecaviva.backend.application.dtos.request.WorkRequest;
import org.bibliotecaviva.backend.application.dtos.request.audiovisual.LibraLiteratureRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.audiovisual.MultimediaRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.textual.*;
import org.bibliotecaviva.backend.application.dtos.request.visual.ArtRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.visual.InfographicRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.HomePageDashboardResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.LikeResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.WorkResponse;
import org.bibliotecaviva.backend.application.dtos.response.WorkSummaryResponseDTO;
import org.bibliotecaviva.backend.application.mappers.WorkMapper;
import org.bibliotecaviva.backend.domain.entities.User;
import org.bibliotecaviva.backend.domain.entities.Work;
import org.bibliotecaviva.backend.domain.entities.audiovisual.LibraLiterature;
import org.bibliotecaviva.backend.domain.entities.audiovisual.Multimedia;
import org.bibliotecaviva.backend.domain.entities.textual.*;
import org.bibliotecaviva.backend.domain.entities.visual.Art;
import org.bibliotecaviva.backend.domain.entities.visual.Infographic;
import org.bibliotecaviva.backend.domain.enums.WorkTypes;
import org.bibliotecaviva.backend.domain.exceptions.UserNotFoundException;
import org.bibliotecaviva.backend.domain.exceptions.WorkAlreadyExistsException;
import org.bibliotecaviva.backend.domain.exceptions.WorkNotFoundException;
import org.bibliotecaviva.backend.persistence.repository.CommentRepository;
import org.bibliotecaviva.backend.persistence.repository.UserRepository;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import org.bibliotecaviva.backend.domain.entities.visual.VisualWork;

@Log4j2
@Service
@RequiredArgsConstructor

public class WorkService {

    private final WorkRepository workRepository;
    private final WorkMapper workMapper;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Puxa direto da tabela works usando uma interface com atributos genericos
     * para evitar fazer joins desnecessários
     */
    public Page<WorkSummaryResponseDTO> getAll(WorkTypes type, Pageable pageable) {
        String types = type == null ? null : WorkTypes.fromString(type.name()).getValue();
        return workRepository.findAllSummary(types, pageable)
                .map(workMapper::toWorkSummary);
    }

    // todo: - verificar view count, batch update com cache se necessário
    // - da pra melhorar a performace pq ta fazendo o join com todas as tabelas
    // desnecessariamente
    public WorkResponse getById(UUID id) {
        var work = workRepository.findById(id)
                .orElseThrow(() -> new WorkNotFoundException("Obra com id " + id + " não encontrada"));
        workRepository.incrementViewCount(id);

        return workMapper.toDTO(work, workRepository.getLikeCount(id), commentRepository.countByWork_Id(id));
    }

    @Transactional
    public <T extends WorkRequest> WorkResponse create(T dto) {
        return createInternal(dto, null);
    }

    @Transactional
    public <T extends WorkRequest> WorkResponse create(T dto, MultipartFile image) {
        return createInternal(dto, image);
    }

    private <T extends WorkRequest> WorkResponse createInternal(T dto, MultipartFile image) {
        validateAuthorship(dto);

        Work work = switch (dto) {
            case EssayRequestDTO d          -> workMapper.toEntity(d);
            case ArtRequestDTO d            -> workMapper.toEntity(d);
            case CordelRequestDTO d         -> workMapper.toEntity(d);
            case ShortStoryRequestDTO d     -> workMapper.toEntity(d);
            case TaleRequestDTO d           -> workMapper.toEntity(d);
            case ArticleRequestDTO d        -> workMapper.toEntity(d);
            case InfographicRequestDTO d    -> workMapper.toEntity(d);
            case MultimediaRequestDTO d     -> workMapper.toEntity(d);
            case LibraLiteratureRequestDTO d -> workMapper.toEntity(d);
            case PoemRequestDTO d           -> workMapper.toEntity(d);
            case OtherRequestDTO d          -> workMapper.toEntity(d);
            default -> throw new IllegalArgumentException(
                    "Tipo não mapeado: " + dto.getClass().getSimpleName());
        };

        // ── 1. Validate author/title BEFORE touching Cloudinary ──────────────
        if (dto.authorEmail() != null && dto.authorName() == null) {
            var user = userRepository.findByEmail(dto.authorEmail())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Usuário não encontrado com email: " + dto.authorEmail()));
            if (workRepository.existsWorkByAuthorAndTitle(user, work.getTitle())) {
                throw new WorkAlreadyExistsException("Obra com mesmo título já existe para este autor");
            }
            work.setAuthor(user);
        } else {
            if (workRepository.existsWorkByAuthorNameAndTitle(dto.authorName(), work.getTitle())) {
                throw new WorkAlreadyExistsException("Obra com mesmo título já existe para este autor");
            }
            work.setAuthorName(dto.authorName());
        }

        if (work instanceof Cordel && hasText(((CordelRequestDTO) dto).artName())) {
            ((Cordel) work).setIllustration(findArtByTitle(((CordelRequestDTO) dto).artName()));
        }

        // ── 2. Upload AFTER validation — compensate if persistence fails ──────
        String uploadedPublicId = null;
        try {
            if (work instanceof VisualWork visualWork && image != null && !image.isEmpty()) {
                var uploaded = cloudinaryService.uploadImage(image);
                visualWork.setUrl(uploaded.url());
                visualWork.setCloudinaryPublicId(uploaded.publicId());
                uploadedPublicId = uploaded.publicId();
            }
            if (work instanceof Other other && image != null && !image.isEmpty()) {
                var uploaded = cloudinaryService.uploadImage(image);
                other.setImageUrl(uploaded.url());
                other.setCloudinaryPublicId(uploaded.publicId());
                uploadedPublicId = uploaded.publicId();
            }

            work.setViewCount(0L);
            return workMapper.toDTO(workRepository.save(work), 0L, 0L);

        } catch (RuntimeException ex) {
            // If DB persistence failed after a successful upload, clean up the orphan
            if (uploadedPublicId != null) {
                log.warn("Persistence failed after Cloudinary upload — deleting orphan asset: {}", uploadedPublicId);
                cloudinaryService.deleteImage(uploadedPublicId);
            }
            throw ex;
        }
    }

    @Transactional
    public <T extends WorkRequest> WorkResponse update(UUID id, T dto) {
        return updateInternal(id, dto, null);
    }

    @Transactional
    public <T extends WorkRequest> WorkResponse update(UUID id, T dto, MultipartFile image) {
        return updateInternal(id, dto, image);
    }

    private <T extends WorkRequest> WorkResponse updateInternal(UUID id, T dto, MultipartFile image) {
        Work work = workRepository.findById(id)
                .orElseThrow(() -> new WorkNotFoundException("Obra não encontrada"));
        validateAuthorship(dto);
        switch (dto) {
            case EssayRequestDTO d           -> workMapper.partialUpdate(d, requireSubtype(work, Essay.class));
            case ArtRequestDTO d             -> workMapper.partialUpdate(d, requireSubtype(work, Art.class));
            case CordelRequestDTO d          -> workMapper.partialUpdate(d, requireSubtype(work, Cordel.class));
            case ShortStoryRequestDTO d      -> workMapper.partialUpdate(d, requireSubtype(work, ShortStory.class));
            case TaleRequestDTO d            -> workMapper.partialUpdate(d, requireSubtype(work, Tale.class));
            case ArticleRequestDTO d         -> workMapper.partialUpdate(d, requireSubtype(work, Article.class));
            case InfographicRequestDTO d     -> workMapper.partialUpdate(d, requireSubtype(work, Infographic.class));
            case MultimediaRequestDTO d      -> workMapper.partialUpdate(d, requireSubtype(work, Multimedia.class));
            case LibraLiteratureRequestDTO d -> workMapper.partialUpdate(d, requireSubtype(work, LibraLiterature.class));
            case PoemRequestDTO d            -> workMapper.partialUpdate(d, requireSubtype(work, Poem.class));
            case OtherRequestDTO d           -> workMapper.partialUpdate(d, requireSubtype(work, Other.class));
            default -> throw new IllegalArgumentException(
                    "Tipo não mapeado: " + dto.getClass().getSimpleName());
        }

        if (dto.authorEmail() != null && dto.authorName() == null) {
            var user = userRepository.findByEmail(dto.authorEmail())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Usuário não encontrado com email: " + dto.authorEmail()));
            work.setAuthor(user);
            work.setAuthorName(null);
        } else {
            work.setAuthorName(dto.authorName());
            work.setAuthor(null);
        }

        if (work instanceof Cordel && hasText(((CordelRequestDTO) dto).artName())) {
            ((Cordel) work).setIllustration(findArtByTitle(((CordelRequestDTO) dto).artName()));
        }

        // ── Replace image: capture old publicId, upload new, delete old on success ──
        if (image != null && !image.isEmpty()) {
            if (work instanceof VisualWork visualWork) {
                String oldPublicId = visualWork.getCloudinaryPublicId();
                String newPublicId = null;
                try {
                    var uploaded = cloudinaryService.uploadImage(image);
                    visualWork.setUrl(uploaded.url());
                    visualWork.setCloudinaryPublicId(uploaded.publicId());
                    newPublicId = uploaded.publicId();
                    var saved = workRepository.save(work);
                    cloudinaryService.deleteImage(oldPublicId);  // best-effort cleanup of old asset
                    return workMapper.toDTO(saved, workRepository.getLikeCount(id),
                            commentRepository.countByWork_Id(id));
                } catch (RuntimeException ex) {
                    if (newPublicId != null) {
                        log.warn("Update persistence failed — deleting orphan asset: {}", newPublicId);
                        cloudinaryService.deleteImage(newPublicId);
                    }
                    throw ex;
                }
            } else if (work instanceof Other other) {
                String oldPublicId = other.getCloudinaryPublicId();
                String newPublicId = null;
                try {
                    var uploaded = cloudinaryService.uploadImage(image);
                    other.setImageUrl(uploaded.url());
                    other.setCloudinaryPublicId(uploaded.publicId());
                    newPublicId = uploaded.publicId();
                    var saved = workRepository.save(work);
                    cloudinaryService.deleteImage(oldPublicId);
                    return workMapper.toDTO(saved, workRepository.getLikeCount(id),
                            commentRepository.countByWork_Id(id));
                } catch (RuntimeException ex) {
                    if (newPublicId != null) {
                        log.warn("Update persistence failed — deleting orphan asset: {}", newPublicId);
                        cloudinaryService.deleteImage(newPublicId);
                    }
                    throw ex;
                }
            }
        }

        return workMapper.toDTO(workRepository.save(work), workRepository.getLikeCount(id),
                commentRepository.countByWork_Id(id));
    }

    @Transactional
    public void delete(UUID id) {
        Work work = workRepository.findById(id)
                .orElseThrow(() -> new WorkNotFoundException("Obra com id " + id + " não encontrada"));

        // Capture publicId before deleting the DB record
        String publicId = null;
        if (work instanceof VisualWork vw) {
            publicId = vw.getCloudinaryPublicId();
        } else if (work instanceof Other other) {
            publicId = other.getCloudinaryPublicId();
        }

        workRepository.deleteLikesByWorkId(id);
        workRepository.clearIllustrationReferences(id);
        workRepository.deleteById(id);

        // Best-effort remote cleanup — runs after the DB transaction commits
        cloudinaryService.deleteImage(publicId);
    }

    public List<UUID> getLikedWorkIds(User user) {
        return userRepository.findLikedWorkIdsByUserId(user.getId());
    }

    // TODO: fazer sistema melhor de like ou colocar um limitador de request ou
    // cachear ( mesmo problema das views)
    // pode dar gargalo fazer update toda hora assim
    @Transactional
    public LikeResponseDTO like(UUID workId, User user) {

        if (!workRepository.existsById(workId)) throw new WorkNotFoundException("Obra com id " + workId + " não encontrada");
        var userId = user.getId();
        userRepository.likeWork(userId, workId);
        return new LikeResponseDTO(true, workRepository.getLikeCount(workId));
    }

    @Transactional
    public LikeResponseDTO unLike(UUID workId, User user) {
        if (!workRepository.existsById(workId)) throw new WorkNotFoundException("Obra com id " + workId + " não encontrada");
        var userId = user.getId();

        userRepository.unlikeWork(userId, workId);

        return new LikeResponseDTO(false, workRepository.getLikeCount(workId));
    }

    public HomePageDashboardResponseDTO getFrontPageData() {
        var counts = workRepository.countPerType().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));
        var works = Arrays.stream(WorkTypes.values())
                .flatMap(type -> workRepository.findTop5ByType(type.getValue()).stream())
                .map(workMapper::toWorkSummary)
                .toList();
        var mostLikes = workRepository.getMostLikedWorks().stream().map(workMapper::toWorkSummary).toList();
        return new HomePageDashboardResponseDTO(
                counts.getOrDefault("LibraLiterature", 0L).intValue(),
                counts.getOrDefault("Multimedia", 0L).intValue(),
                counts.getOrDefault("Article", 0L).intValue(),
                counts.getOrDefault("Cordel", 0L).intValue(),
                counts.getOrDefault("Essay", 0L).intValue(),
                counts.getOrDefault("ShortStory", 0L).intValue(),
                counts.getOrDefault("Tale", 0L).intValue(),
                counts.getOrDefault("Art", 0L).intValue(),
                counts.getOrDefault("Infographic", 0L).intValue(),
                counts.getOrDefault("Poem", 0L).intValue(),
                counts.getOrDefault("Other", 0L).intValue(),
                works,
                mostLikes);
    }

    public Long countWorks() {
        return workRepository.count();
    }

    private static void validateAuthorship(WorkRequest dto) {
        if (dto.authorEmail() != null && dto.authorName() != null) {
            throw new IllegalArgumentException("Informe apenas um dos campos: email ou nome");
        }
        if (!hasText(dto.authorEmail()) && !hasText(dto.authorName())) {
            throw new IllegalArgumentException("Forneça um usuário cadastrado ou o nome do autor");
        }
    }

    private static <W extends Work> W requireSubtype(Work work, Class<W> type) {
        if (!type.isInstance(work)) {
            throw new IllegalArgumentException("Tipo da obra incompatível com a rota solicitada");
        }
        return type.cast(work);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Art findArtByTitle(String title) {
        return workRepository.findArtByTitle(title)
                .orElseThrow(() -> new WorkNotFoundException(
                        "Obra de arte com nome " + title + " não encontrada"));
    }
}
