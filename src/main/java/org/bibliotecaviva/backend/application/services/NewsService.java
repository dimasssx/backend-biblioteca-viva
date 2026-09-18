package org.bibliotecaviva.backend.application.services;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.request.NewsRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.NewsResponseDTO;
import org.bibliotecaviva.backend.application.mappers.NewsMapper;
import org.bibliotecaviva.backend.domain.entities.News;
import org.bibliotecaviva.backend.domain.entities.User;
import org.bibliotecaviva.backend.domain.enums.Role;
import org.bibliotecaviva.backend.domain.exceptions.ForbiddenException;
import org.bibliotecaviva.backend.domain.exceptions.NotFoundException;
import org.bibliotecaviva.backend.persistence.repository.NewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public NewsResponseDTO create(NewsRequestDTO dto, MultipartFile image, User author) {
        News news = newsMapper.toEntity(dto, author);

        String uploadedPublicId = null;
        try {
            if (image != null && !image.isEmpty()) {
                var uploaded = cloudinaryService.uploadImage(image);
                news.setImageUrl(uploaded.url());
                news.setCloudinaryPublicId(uploaded.publicId());
                uploadedPublicId = uploaded.publicId();
            }

            return newsMapper.toDto(newsRepository.save(news));
        } catch (RuntimeException ex) {
            if (uploadedPublicId != null) {
                log.warn("Persistence failed after Cloudinary upload — deleting orphan news asset: {}", uploadedPublicId);
                cloudinaryService.deleteImage(uploadedPublicId);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDTO> getAll(Pageable pageable) {
        return newsRepository.findAll(pageable).map(newsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public NewsResponseDTO getById(UUID id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notícia não encontrada"));
        return newsMapper.toDto(news);
    }

    @Transactional
    public NewsResponseDTO update(UUID id, NewsRequestDTO dto, MultipartFile image, User user) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notícia não encontrada"));
        verifyOwnership(user, news);
        newsMapper.partialUpdate(dto, news);
        news.setUpdatedAt(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            String oldPublicId = news.getCloudinaryPublicId();
            String newPublicId = null;
            try {
                var uploaded = cloudinaryService.uploadImage(image);
                news.setImageUrl(uploaded.url());
                news.setCloudinaryPublicId(uploaded.publicId());
                newPublicId = uploaded.publicId();
                var saved = newsRepository.save(news);
                cloudinaryService.deleteImage(oldPublicId);
                return newsMapper.toDto(saved);
            } catch (RuntimeException ex) {
                if (newPublicId != null) {
                    log.warn("Update persistence failed — deleting orphan news asset: {}", newPublicId);
                    cloudinaryService.deleteImage(newPublicId);
                }
                throw ex;
            }
        }

        return newsMapper.toDto(newsRepository.save(news));
    }

    @Transactional
    public void delete(UUID id, User user) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notícia não encontrada"));
        verifyOwnership(user, news);
        String publicId = news.getCloudinaryPublicId();
        newsRepository.delete(news);
        cloudinaryService.deleteImage(publicId);
    }

    private void verifyOwnership(User user, News news) {
        if (user.getRole() == Role.ADMIN) return;
        if (news.getAuthor() == null || !news.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Forbidden");
        }
    }
}
