package org.bibliotecaviva.backend.api.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.request.audiovisual.LibraLiteratureRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.audiovisual.MultimediaRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.textual.*;
import org.bibliotecaviva.backend.application.dtos.request.visual.ArtRequestDTO;
import org.bibliotecaviva.backend.application.dtos.request.visual.InfographicRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.IWorkResponseDTO;
import org.bibliotecaviva.backend.application.services.WorkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService service;

    @GetMapping
    public ResponseEntity<List<IWorkResponseDTO>> getAll(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(service.getAll(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IWorkResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/articles")
    public ResponseEntity<IWorkResponseDTO> createArticle(@RequestBody @Valid ArticleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<IWorkResponseDTO> updateArticle(@PathVariable UUID id, @RequestBody @Valid ArticleRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/cordels")
    public ResponseEntity<IWorkResponseDTO> createCordel(@RequestBody @Valid CordelRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/cordels/{id}")
    public ResponseEntity<IWorkResponseDTO> updateCordel(@PathVariable UUID id, @RequestBody @Valid CordelRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/essays")
    public ResponseEntity<IWorkResponseDTO> createEssay(@RequestBody @Valid EssayRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/essays/{id}")
    public ResponseEntity<IWorkResponseDTO> updateEssay(@PathVariable UUID id, @RequestBody @Valid EssayRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/short-stories")
    public ResponseEntity<IWorkResponseDTO> createShortStory(@RequestBody @Valid ShortStoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/short-stories/{id}")
    public ResponseEntity<IWorkResponseDTO> updateShortStory(@PathVariable UUID id, @RequestBody @Valid ShortStoryRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/tales")
    public ResponseEntity<IWorkResponseDTO> createTale(@RequestBody @Valid TaleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/tales/{id}")
    public ResponseEntity<IWorkResponseDTO> updateTale(@PathVariable UUID id, @RequestBody @Valid TaleRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/arts")
    public ResponseEntity<IWorkResponseDTO> createArt(@RequestBody @Valid ArtRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/arts/{id}")
    public ResponseEntity<IWorkResponseDTO> updateArt(@PathVariable UUID id, @RequestBody @Valid ArtRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/infographics")
    public ResponseEntity<IWorkResponseDTO> createInfographic(@RequestBody @Valid InfographicRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/infographics/{id}")
    public ResponseEntity<IWorkResponseDTO> updateInfographic(@PathVariable UUID id, @RequestBody @Valid InfographicRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/multimedias")
    public ResponseEntity<IWorkResponseDTO> createMultimedia(@RequestBody @Valid MultimediaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/multimedias/{id}")
    public ResponseEntity<IWorkResponseDTO> updateMultimedia(@PathVariable UUID id, @RequestBody @Valid MultimediaRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/libra-literatures")
    public ResponseEntity<IWorkResponseDTO> createLibraLiterature(@RequestBody @Valid LibraLiteratureRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/libra-literatures/{id}")
    public ResponseEntity<IWorkResponseDTO> updateLibraLiterature(@PathVariable UUID id, @RequestBody @Valid LibraLiteratureRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}
