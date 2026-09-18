package org.bibliotecaviva.backend.application.services;

import org.bibliotecaviva.backend.application.dtos.CloudinaryUploadResult;
import org.bibliotecaviva.backend.application.dtos.request.visual.ArtRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.WorkResponse;
import org.bibliotecaviva.backend.application.mappers.WorkMapper;
import org.bibliotecaviva.backend.domain.entities.visual.Art;
import org.bibliotecaviva.backend.domain.exceptions.UserNotFoundException;
import org.bibliotecaviva.backend.domain.exceptions.WorkAlreadyExistsException;
import org.bibliotecaviva.backend.domain.exceptions.WorkNotFoundException;
import org.bibliotecaviva.backend.persistence.repository.CommentRepository;
import org.bibliotecaviva.backend.persistence.repository.UserRepository;
import org.bibliotecaviva.backend.persistence.repository.WorkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for upload orphan-prevention logic in {@link WorkService}.
 *
 * <p>Issue #17 — uploads must happen AFTER all validations so that a failed
 * validation never leaves an unreferenced asset in Cloudinary. When the DB
 * save fails after a successful upload, the service must compensate by
 * calling {@code deleteImage}.
 */
@ExtendWith(MockitoExtension.class)
class WorkServiceUploadOrphanTest {

    @Mock WorkRepository workRepository;
    @Mock WorkMapper workMapper;
    @Mock UserRepository userRepository;
    @Mock CommentRepository commentRepository;
    @Mock CloudinaryService cloudinaryService;

    @InjectMocks
    WorkService workService;

    private static final MockMultipartFile VALID_IMAGE = new MockMultipartFile(
            "image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

    // ── 1. Validation before upload ──────────────────────────────────────────

    @Test
    void uploadMustNotHappenWhenAuthorEmailNotFound() {
        var dto = artDto("author@missing.com", null);
        var art = new Art();

        when(workMapper.toEntity(dto)).thenReturn(art);
        when(userRepository.findByEmail("author@missing.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> workService.create(dto, VALID_IMAGE));

        // Cloudinary must NOT have been called
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    void uploadMustNotHappenWhenDuplicateTitleDetected() {
        var dto = artDto("author@ok.com", null);
        var art = new Art();
        art.setTitle("Mona Lisa");

        var user = new org.bibliotecaviva.backend.domain.entities.User();
        when(workMapper.toEntity(dto)).thenReturn(art);
        when(userRepository.findByEmail("author@ok.com")).thenReturn(Optional.of(user));
        when(workRepository.existsWorkByAuthorAndTitle(user, "Mona Lisa")).thenReturn(true);

        assertThrows(WorkAlreadyExistsException.class,
                () -> workService.create(dto, VALID_IMAGE));

        verify(cloudinaryService, never()).uploadImage(any());
    }

    // ── 2. Compensation when DB save fails after upload ──────────────────────

    @Test
    void orphanIsDeletedWhenDbSaveFailsAfterUpload() {
        var dto = artDto("author@ok.com", null);
        var art = new Art();
        art.setTitle("Starry Night");

        var user = new org.bibliotecaviva.backend.domain.entities.User();
        when(workMapper.toEntity(dto)).thenReturn(art);
        when(userRepository.findByEmail("author@ok.com")).thenReturn(Optional.of(user));
        when(workRepository.existsWorkByAuthorAndTitle(any(), anyString())).thenReturn(false);

        var uploaded = new CloudinaryUploadResult("https://res.cloudinary.com/img.jpg", "starry-night-id");
        when(cloudinaryService.uploadImage(any())).thenReturn(uploaded);

        // Simulate DB failure
        when(workRepository.save(any())).thenThrow(new RuntimeException("DB constraint violation"));

        assertThrows(RuntimeException.class,
                () -> workService.create(dto, VALID_IMAGE));

        // Compensation: orphan asset must be deleted
        verify(cloudinaryService).deleteImage("starry-night-id");
    }

    // ── 3. Delete removes the Cloudinary asset ────────────────────────────────

    @Test
    void deleteShouldRemoveCloudinaryAssetAfterDbDeletion() {
        UUID id = UUID.randomUUID();
        Art art = new Art();
        art.setCloudinaryPublicId("asset-to-delete");

        when(workRepository.findById(id)).thenReturn(Optional.of(art));

        workService.delete(id);

        verify(cloudinaryService).deleteImage("asset-to-delete");
    }

    @Test
    void deleteShouldNotFailWhenWorkHasNoCloudinaryAsset() {
        UUID id = UUID.randomUUID();
        Art art = new Art(); // no publicId set

        when(workRepository.findById(id)).thenReturn(Optional.of(art));

        workService.delete(id); // must not throw

        // deleteImage is still called but with null — CloudinaryService handles it safely
        verify(cloudinaryService).deleteImage(null);
    }

    @Test
    void deleteShouldFailWhenWorkNotFound() {
        UUID id = UUID.randomUUID();
        when(workRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(WorkNotFoundException.class, () -> workService.delete(id));
        verify(cloudinaryService, never()).deleteImage(any());
    }

    // ── 4. Update deletes old asset after successful save ─────────────────────

    @Test
    void updateShouldDeleteOldAssetAfterNewImageIsPersistedSuccessfully() {
        UUID id = UUID.randomUUID();
        Art art = new Art();
        art.setCloudinaryPublicId("old-asset-id");

        var dto = artDto("author@ok.com", null);
        var user = new org.bibliotecaviva.backend.domain.entities.User();
        WorkResponse response = org.mockito.Mockito.mock(WorkResponse.class);

        when(workRepository.findById(id)).thenReturn(Optional.of(art));
        when(userRepository.findByEmail("author@ok.com")).thenReturn(Optional.of(user));

        var uploaded = new CloudinaryUploadResult("https://res.cloudinary.com/new.jpg", "new-asset-id");
        when(cloudinaryService.uploadImage(any())).thenReturn(uploaded);
        when(workRepository.save(any())).thenReturn(art);
        when(workRepository.getLikeCount(id)).thenReturn(0L);
        when(commentRepository.countByWork_Id(id)).thenReturn(0L);
        when(workMapper.toDTO(any(), any(), any())).thenReturn(response);

        workService.update(id, dto, VALID_IMAGE);

        // Old asset must be removed
        verify(cloudinaryService).deleteImage("old-asset-id");
        // New publicId is now on the entity
        verify(workRepository).save(art);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ArtRequestDTO artDto(String email, String name) {
        return new ArtRequestDTO(
                "Test Art",
                email,
                name,
                LocalDateTime.now().minusDays(1),
                "Descrição longa com mais de 15 caracteres",
                "Turma A"
        );
    }
}
