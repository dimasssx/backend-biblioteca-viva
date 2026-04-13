package org.bibliotecaviva.backend.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.request.CommentRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.CommentResponseDTO;
import org.bibliotecaviva.backend.application.services.CommentService;
import org.bibliotecaviva.backend.domain.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/work/{workId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDTO> create(
            @PathVariable UUID workId,
            @RequestBody @Valid CommentRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(workId, dto.content(), user));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponseDTO>> getByWorkId(
            @PathVariable UUID workId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(commentService.getByWorkId(workId, pageable));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> update(
            @PathVariable UUID workId,
            @PathVariable UUID commentId,
            @RequestBody @Valid CommentRequestDTO dto) {
        return ResponseEntity.ok(commentService.update(commentId, dto.content()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID workId, @PathVariable UUID commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
