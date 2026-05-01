package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para operações de Chamados (Tickets).
 * 
 * Base path: /api/tickets
 * Todas as operações requerem autenticação (JWT).
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chamados", description = "Operações de gerenciamento de chamados/tickets")
public class TicketController {

    private final TicketService ticketService;

    // ========== CRUD ==========

    /**
     * POST /api/tickets
     * Cria um novo chamado.
     * 
     * Qualquer usuário autenticado pode criar.
     * Status inicial: ABERTO
     * Solicitante: usuário autenticado
     */
    @PostMapping
    @Operation(summary = "Criar chamado", description = "Cria um novo chamado. O solicitante é o usuário autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chamado criado com sucesso",
                     content = @Content(schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/tickets
     * Lista chamados com filtros e paginação.
     */
    @GetMapping
    @Operation(summary = "Listar chamados", description = "Lista chamados com filtros opcionais e paginação")
    @ApiResponse(responseCode = "200", description = "Lista de chamados")
    public ResponseEntity<TicketListResponse> listTickets(
            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) TicketStatus status,
            
            @Parameter(description = "Filtrar por prioridade")
            @RequestParam(required = false) TicketPriority priority,
            
            @Parameter(description = "Filtrar por categoria")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Filtrar por ID do solicitante")
            @RequestParam(required = false) UUID requesterId,
            
            @Parameter(description = "Filtrar por ID do responsável")
            @RequestParam(required = false) UUID assigneeId,
            
            @Parameter(description = "Busca textual em título e descrição")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Campo para ordenação")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Direção da ordenação (ASC/DESC)")
            @RequestParam(defaultValue = "DESC") String direction) {

        TicketFilterRequest filters = new TicketFilterRequest(
                status, priority, category, requesterId, assigneeId, null, search);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        TicketListResponse response = ticketService.listTickets(filters, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tickets/{id}
     * Busca chamado por ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar chamado", description = "Retorna detalhes completos de um chamado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado encontrado"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> getTicket(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID id) {
        TicketResponse response = ticketService.getTicket(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/tickets/{id}
     * Atualiza dados básicos do chamado.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar chamado", description = "Atualiza título, descrição e categoria")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request) {
        TicketResponse response = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(response);
    }

    // ========== STATUS ==========

    /**
     * PATCH /api/tickets/{id}/status
     * Altera status do chamado.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status", description = "Altera o status do chamado. Valida transições permitidas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado"),
        @ApiResponse(responseCode = "400", description = "Transição inválida"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        TicketResponse response = ticketService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    // ========== ATRIBUIÇÃO ==========

    /**
     * PATCH /api/tickets/{id}/assign
     * Atribui responsável ao chamado (ADMIN/GESTOR).
     */
    @PatchMapping("/{id}/assign")
    @Operation(summary = "Atribuir responsável", description = "Atribui um usuário como responsável. Apenas ADMIN ou GESTOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Responsável atribuído"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado ou usuário não encontrado")
    })
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request) {
        TicketResponse response = ticketService.assignTicket(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/tickets/{id}/assume
     * Usuário AGENTE assume o chamado.
     */
    @PatchMapping("/{id}/assume")
    @Operation(summary = "Assumir chamado", description = "O usuário autenticado se torna responsável. Apenas AGENTE, ADMIN ou GESTOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado assumido"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> assumeTicket(@PathVariable UUID id) {
        TicketResponse response = ticketService.assumeTicket(id);
        return ResponseEntity.ok(response);
    }

    // ========== FINALIZAÇÃO ==========

    /**
     * PATCH /api/tickets/{id}/cancel
     * Cancela o chamado.
     */
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar chamado", description = "Cancela o chamado. Solicitante pode cancelar próprio chamado aberto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado cancelado"),
        @ApiResponse(responseCode = "400", description = "Operação inválida para o status"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable UUID id) {
        TicketResponse response = ticketService.cancelTicket(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/tickets/{id}/resolve
     * Marca chamado como RESOLVIDO.
     */
    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolver chamado", description = "Marca chamado como RESOLVIDO. Define data de resolução.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado resolvido"),
        @ApiResponse(responseCode = "400", description = "Transição inválida"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable UUID id) {
        TicketResponse response = ticketService.resolveTicket(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/tickets/{id}/close
     * Fecha um chamado RESOLVIDO.
     */
    @PatchMapping("/{id}/close")
    @Operation(summary = "Fechar chamado", description = "Fecha chamado RESOLVIDO. Status terminal.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chamado fechado"),
        @ApiResponse(responseCode = "400", description = "Apenas chamados RESOLVIDOS podem ser fechados"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<TicketResponse> closeTicket(@PathVariable UUID id) {
        TicketResponse response = ticketService.closeTicket(id);
        return ResponseEntity.ok(response);
    }
}
