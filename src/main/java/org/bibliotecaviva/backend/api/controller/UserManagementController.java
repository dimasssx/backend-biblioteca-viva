package org.bibliotecaviva.backend.api.controller;

import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.response.UserResponseDTO;
import org.bibliotecaviva.backend.application.services.UserManagementService;
import org.bibliotecaviva.backend.domain.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor

public class UserManagementController {

    private final UserManagementService userManagementService;

    //todo: dtos tratar enum, user disable and user locked no handler
    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {
        if(status != null && !status.isBlank()){
            return ResponseEntity.ok(userManagementService.getUsersByStatus(Enum.valueOf(Status.class, status.toUpperCase()), pageable));
        }
        return ResponseEntity.ok(userManagementService.getAllUsers(pageable));
    }

    @PatchMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id){
        userManagementService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectUser(@PathVariable UUID id){
        userManagementService.rejectUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockUser(@PathVariable UUID id){
        userManagementService.blockUser(id);
        return ResponseEntity.noContent().build();
    }

    //todo: registrar conta de professor / trocar role pra prof
}
