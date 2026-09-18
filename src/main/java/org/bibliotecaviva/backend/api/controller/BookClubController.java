package org.bibliotecaviva.backend.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.dtos.request.BookClubRequestDTO;
import org.bibliotecaviva.backend.application.dtos.response.BookClubParticipants;
import org.bibliotecaviva.backend.application.dtos.response.BookClubResponseDTO;
import org.bibliotecaviva.backend.application.dtos.response.SubscribeResponseDTO;
import org.bibliotecaviva.backend.application.services.BookClubService;
import org.bibliotecaviva.backend.domain.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookclub")
@RequiredArgsConstructor
@Tag(name = "Book Club",
        description = """
                Controller that handles book club management
                """
)
public class BookClubController {
    private final BookClubService bookClubService;

    @PostMapping //todo: falta permitir prof e verificar se é o dono para permitir edição e remoção
    @PreAuthorize("hasAnyRole('ADMIN', 'CURADOR')")
    @Operation(description = "Cria um novo encontro do Clube do Livro")
    @ApiResponse(responseCode = "201", description = "Clube do livro criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos", content = @Content)
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    public ResponseEntity<BookClubResponseDTO> create(@RequestBody @Valid BookClubRequestDTO requestDTO,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookClubService.create(requestDTO, user));
    }

    @GetMapping("/next")
    @Operation(description = "Retorna o próximo encontro agendado do Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Próximo clube do livro encontrado")
    @ApiResponse(responseCode = "404", description = "Nenhum clube do livro futuro agendado", content = @Content)
    public ResponseEntity<BookClubResponseDTO> getNext() {
        return ResponseEntity.ok(bookClubService.getNext());
    }

    //todo: Revisar getAll e getById, colocar paginação, se for somente 1 por mes, deixar restrito para professor e admin
    //      se for varios, vai ter que ter um getByMonth ou algo do tipo, ou deixar como ta e sempre aparecer os proximos
    @GetMapping
    @Operation(description = "Retorna a listagem paginada de todos os encontros do Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Lista de clubes do livro recuperada com sucesso")
    public ResponseEntity<Page<BookClubResponseDTO>> getAll(
            @Parameter(hidden = true) @PageableDefault(sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookClubService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(description = "Busca os detalhes de um encontro do Clube do Livro por ID")
    @ApiResponse(responseCode = "200", description = "Clube do livro encontrado")
    @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado", content = @Content)
    public ResponseEntity<BookClubResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookClubService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CURADOR')")
    @Operation(description = "Atualiza os dados de um encontro do Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Clube do livro atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado", content = @Content)
    public ResponseEntity<BookClubResponseDTO> update(@PathVariable UUID id,
                                                      @RequestBody @Valid BookClubRequestDTO requestDTO,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookClubService.update(id, requestDTO, user));
    }

    @DeleteMapping("/{id}")//so admin/prof
    @PreAuthorize("hasAnyRole('ADMIN', 'CURADOR')")
    @Operation(description = "Remove um encontro do Clube do Livro")
    @ApiResponse(responseCode = "204", description = "Clube do livro removido com sucesso")
    @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content)
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal User user) {
        bookClubService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants")
    @Operation(description = "Lista todos os participantes inscritos em um encontro do Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Lista de participantes recuperada com sucesso")
    @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado", content = @Content)
    public ResponseEntity<BookClubParticipants> getParticipants(@PathVariable UUID id){
        return ResponseEntity.ok(bookClubService.getParticipants(id));
    }

    @PostMapping("/{id}/subscribe")
    @Operation(description = "Inscreve o usuário autenticado no Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Inscrição realizada com sucesso")
    @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content)
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado", content = @Content)
    @ApiResponse(responseCode = "409", description = "Usuário já inscrito no clube", content = @Content)
    public ResponseEntity<SubscribeResponseDTO> subscribe(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookClubService.subscribe(id, user));
    }

    @PostMapping("/{id}/unsubscribe")
    @Operation(description = "Cancela a inscrição do usuário autenticado no Clube do Livro")
    @ApiResponse(responseCode = "200", description = "Inscrição cancelada com sucesso")
    @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content)
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Clube do livro não encontrado ou usuário não inscrito", content = @Content)
    public ResponseEntity<SubscribeResponseDTO> unsubscribe(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookClubService.unsubscribe(id, user));
    }

}
