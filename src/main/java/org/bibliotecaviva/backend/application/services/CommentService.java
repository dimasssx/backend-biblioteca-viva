package org.bibliotecaviva.backend.application.services;

import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.response.CommentResponseDTO;
import org.bibliotecaviva.backend.domain.entities.Comment;
import org.bibliotecaviva.backend.domain.entities.User;
import org.bibliotecaviva.backend.domain.exceptions.WorkNotFoundException;
import org.bibliotecaviva.backend.persistance.repository.CommentRepository;
import org.bibliotecaviva.backend.persistance.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final WorkRepository workRepository;

    @Transactional
    public CommentResponseDTO create(UUID workId, String content, User user) {
        var work = workRepository.findById(workId)
                .orElseThrow(() -> new WorkNotFoundException("Obra com id " + workId + " não encontrada"));

        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .work(work)
                .build();

        Comment saved = commentRepository.save(comment);
        return toDTO(saved);
    }

    public Page<CommentResponseDTO> getByWorkId(UUID workId, Pageable pageable) {
        if (!workRepository.existsById(workId)) {
            throw new WorkNotFoundException("Obra com id " + workId + " não encontrada");
        }
        return commentRepository.findByWorkIdOrderByCreatedAtDesc(workId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public CommentResponseDTO update(UUID commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentário com id " + commentId + " não encontrado"));
        comment.setContent(content);
        return toDTO(commentRepository.save(comment));
    }

    @Transactional
    public void delete(UUID commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("Comentário com id " + commentId + " não encontrado");
        }
        commentRepository.deleteById(commentId);
    }

    private CommentResponseDTO toDTO(Comment comment) {
        return new CommentResponseDTO(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getName(),
                comment.getCreatedAt()
        );
    }
}
