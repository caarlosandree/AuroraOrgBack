package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.CommentResponse;
import br.com.api.auroraorg.ticket.dto.CreateCommentRequest;
import br.com.api.auroraorg.ticket.dto.UpdateCommentRequest;
import br.com.api.auroraorg.ticket.service.TicketCommentService;
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

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operações de Comentários em Chamados.
 *
 * Base path: /api/tickets/{ticketId}/comments
 * Todas as operações requerem autenticação (JWT).
 *
 * Endpoints:
 * - POST: Adicionar comentário
 * - GET: Listar comentários
 * - PUT: Editar comentário
 * - DELETE: Remover comentário (soft delete)
 */
@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comentários", description = "Operações de comentários em chamados")
public class TicketCommentController {

    private final TicketCommentService commentService;

    /**
     * POST /api/tickets/{ticketId}/comments
     * Adiciona um novo comentário ao chamado.
     *
     * Permissões:
     * - SOLICITANTE: apenas comentários PUBLICO em seus próprios chamados
     * - AGENTE: comentários PUBLICO ou INTERNO
     * - ADMIN: qualquer tipo de comentário
     * - GESTOR: por padrão não pode comentar
     */
    @PostMapping
    @Operation(summary = "Adicionar comentário",
               description = "Adiciona um novo comentário ao chamado. Respeita regras de visibilidade por perfil.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comentário criado com sucesso",
                     content = @Content(schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou operação não permitida"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para comentar"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/tickets/{ticketId}/comments
     * Lista os comentários do chamado.
     *
     * Filtros por perfil:
     * - SOLICITANTE: apenas comentários públicos não removidos
     * - AGENTE/ADMIN/GESTOR: todos os comentários não removidos
     */
    @GetMapping
    @Operation(summary = "Listar comentários",
               description = "Lista comentários do chamado conforme visibilidade permitida ao perfil")
    @ApiResponse(responseCode = "200", description = "Lista de comentários")
    public ResponseEntity<List<CommentResponse>> listComments(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(commentService.listComments(ticketId, pageable).getContent());
    }

    /**
     * GET /api/tickets/{ticketId}/comments/{commentId}
     * Busca um comentário específico.
     */
    @GetMapping("/{commentId}")
    @Operation(summary = "Buscar comentário",
               description = "Retorna detalhes de um comentário específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comentário encontrado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar"),
        @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    public ResponseEntity<CommentResponse> getComment(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "ID do comentário", required = true)
            @PathVariable UUID commentId) {
        CommentResponse response = commentService.getComment(ticketId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/tickets/{ticketId}/comments/{commentId}
     * Atualiza um comentário existente.
     *
     * Permissões:
     * - Apenas autor ou ADMIN pode editar
     * - Comentários removidos não podem ser editados
     */
    @PutMapping("/{commentId}")
    @Operation(summary = "Editar comentário",
               description = "Atualiza o conteúdo de um comentário. Apenas autor ou ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comentário atualizado"),
        @ApiResponse(responseCode = "400", description = "Comentário removido ou dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para editar"),
        @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "ID do comentário", required = true)
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResponse response = commentService.updateComment(ticketId, commentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/tickets/{ticketId}/comments/{commentId}
     * Remove logicamente um comentário (soft delete).
     *
     * Permissões:
     * - Apenas autor ou ADMIN pode remover
     * - Não exclui fisicamente, apenas marca como removido
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Remover comentário",
               description = "Remove logicamente um comentário (soft delete). Apenas autor ou ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comentário removido"),
        @ApiResponse(responseCode = "400", description = "Comentário já removido"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para remover"),
        @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    public ResponseEntity<Void> removeComment(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "ID do comentário", required = true)
            @PathVariable UUID commentId) {
        commentService.removeComment(ticketId, commentId);
        return ResponseEntity.noContent().build();
    }
}
