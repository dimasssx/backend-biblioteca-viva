package org.bibliotecaviva.backend.api.controllers;


import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.aplication.dtos.response.WorkResponseDTO;
import org.bibliotecaviva.backend.aplication.services.WorkService;
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
    public ResponseEntity <List<WorkResponseDTO>> getAll(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(service.getAll(type));
    }

    @GetMapping("/{id}")
    public WorkResponseDTO getById(@PathVariable UUID id){
        return service.getById(id);
    }
}
